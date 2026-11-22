package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.R
import com.example.data.local.ShopLocalDatabase
import com.example.data.local.toEntity
import com.example.data.local.toFirebaseTimestamp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Optional one-tap Google Cloud Backup & Restore service.
 * Only touches Firestore when the user has explicitly signed in with Google.
 * Never uses anonymous authentication and strictly honors owner-scoped Firestore rules,
 * including append-only rules on ledgerEntries, stockMovements, donations, and rationCardBills.
 */
class ShopCloudBackupService(
    private val context: Context,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private val db: FirebaseFirestore by lazy { createConfiguredFirestore(context.applicationContext) }

    fun isGoogleSignedIn(): Boolean = auth.currentUser != null

    fun currentGoogleEmail(): String? = auth.currentUser?.email

    private fun requireUid(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Google Sign-In is required for Cloud Backup.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    suspend fun backupLocalToCloud(localDb: ShopLocalDatabase): Result<Int> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required"))
        val dao = localDb.shopDao()

        return try {
            requireUid()
            val ops = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
            val root = userDoc(uid)

            // 1. Mutable collections (safe to set/overwrite owned docs)
            for (p in dao.getAllProductsOnce()) {
                ops.add(
                    root.collection("products").document(p.id) to mapOf(
                        "nameUrdu" to p.nameUrdu.take(120),
                        "nameEnglish" to p.nameEnglish.take(120),
                        "category" to p.category,
                        "unit" to p.unit.take(30),
                        "costPrice" to p.costPrice.coerceIn(0.0, 1_000_000.0),
                        "sellingPrice" to p.sellingPrice.coerceIn(0.0, 1_000_000.0),
                        "marginPercent" to p.marginPercent.coerceIn(0.0, 500.0),
                        "autoPriceEnabled" to p.autoPriceEnabled,
                        "stockQty" to p.stockQty.coerceIn(0.0, 1_000_000.0),
                        "lowStockThreshold" to p.lowStockThreshold.coerceIn(0.0, 100_000.0),
                        "barcode" to p.barcode.take(64),
                        "createdAt" to p.createdAtMs.toFirebaseTimestamp(),
                        "updatedAt" to p.updatedAtMs.toFirebaseTimestamp()
                    )
                )
            }

            for (c in dao.getAllCustomersOnce()) {
                ops.add(
                    root.collection("customers").document(c.id) to mapOf(
                        "name" to c.name.take(100),
                        "phone" to c.phone.take(30),
                        "address" to c.address.take(200),
                        "balance" to c.balance.coerceIn(-10_000_000.0, 10_000_000.0),
                        "createdAt" to c.createdAtMs.toFirebaseTimestamp(),
                        "updatedAt" to c.updatedAtMs.toFirebaseTimestamp()
                    )
                )
            }

            for (o in dao.getAllOrdersOnce()) {
                val domain = o.toDomain()
                ops.add(
                    root.collection("orders").document(o.id) to mapOf(
                        "billNumber" to o.billNumber.take(40),
                        "customerId" to o.customerId.take(100),
                        "customerName" to o.customerName.take(100),
                        "status" to o.status,
                        "paymentMethod" to o.paymentMethod,
                        "subtotal" to o.subtotal.coerceIn(0.0, 10_000_000.0),
                        "discount" to o.discount.coerceIn(0.0, 10_000_000.0),
                        "totalAmount" to o.totalAmount.coerceIn(0.0, 10_000_000.0),
                        "paidAmount" to o.paidAmount.coerceIn(0.0, 10_000_000.0),
                        "remainingUdhar" to o.remainingUdhar.coerceIn(0.0, 10_000_000.0),
                        "totalCost" to o.totalCost.coerceIn(0.0, 10_000_000.0),
                        "profitAmount" to o.profitAmount.coerceIn(-10_000_000.0, 10_000_000.0),
                        "items" to domain.items.take(100).map { it.toFirestoreMap() },
                        "note" to o.note.take(300),
                        "createdAt" to o.createdAtMs.toFirebaseTimestamp(),
                        "updatedAt" to o.updatedAtMs.toFirebaseTimestamp()
                    )
                )
            }

            for (item in dao.getAllRationCardItemsOnce()) {
                ops.add(
                    root.collection("rationItems").document(item.id) to mapOf(
                        "nameUrdu" to item.nameUrdu.take(120),
                        "nameEnglish" to item.nameEnglish.take(120),
                        "unit" to item.unit.take(30),
                        "price" to item.price.coerceIn(0.0, 1_000_000.0),
                        "sortOrder" to item.sortOrder.coerceIn(0, 10_000),
                        "createdAt" to item.createdAtMs.toFirebaseTimestamp(),
                        "updatedAt" to item.updatedAtMs.toFirebaseTimestamp()
                    )
                )
            }

            // 2. Append-only collections (only create docs not yet present in Firestore)
            val existingLedgerIds = fetchExistingIds(root.collection("ledgerEntries"))
            for (l in dao.getAllLedgerEntriesOnce()) {
                if (l.id !in existingLedgerIds) {
                    ops.add(
                        root.collection("ledgerEntries").document(l.id) to mapOf(
                            "customerId" to l.customerId.take(100),
                            "customerName" to l.customerName.take(100),
                            "entryType" to l.entryType,
                            "amount" to l.amount.coerceIn(0.01, 10_000_000.0),
                            "balanceAfter" to l.balanceAfter.coerceIn(-10_000_000.0, 10_000_000.0),
                            "orderId" to l.orderId.take(100),
                            "note" to l.note.take(300),
                            "createdAt" to l.createdAtMs.toFirebaseTimestamp()
                        )
                    )
                }
            }

            val existingMoveIds = fetchExistingIds(root.collection("stockMovements"))
            for (m in dao.getAllStockMovementsOnce()) {
                if (m.id !in existingMoveIds) {
                    ops.add(
                        root.collection("stockMovements").document(m.id) to mapOf(
                            "productId" to m.productId.take(100),
                            "productNameUrdu" to m.productNameUrdu.take(120),
                            "productNameEnglish" to m.productNameEnglish.take(120),
                            "movementType" to m.movementType,
                            "quantity" to m.quantity.coerceIn(0.01, 1_000_000.0),
                            "unitCost" to m.unitCost.coerceIn(0.0, 1_000_000.0),
                            "unitPrice" to m.unitPrice.coerceIn(0.0, 1_000_000.0),
                            "profitDelta" to m.profitDelta.coerceIn(-10_000_000.0, 10_000_000.0),
                            "note" to m.note.take(300),
                            "createdAt" to m.createdAtMs.toFirebaseTimestamp()
                        )
                    )
                }
            }

            val existingDonationIds = fetchExistingIds(root.collection("donations"))
            for (d in dao.getAllDonationsOnce()) {
                if (d.id !in existingDonationIds) {
                    ops.add(
                        root.collection("donations").document(d.id) to mapOf(
                            "entryType" to d.entryType,
                            "personName" to d.personName.take(100),
                            "phone" to d.phone.take(30),
                            "amount" to d.amount.coerceIn(1.0, 10_000_000.0),
                            "rationSummary" to d.rationSummary.take(300),
                            "paymentRef" to d.paymentRef.take(100),
                            "createdAt" to d.createdAtMs.toFirebaseTimestamp()
                        )
                    )
                }
            }

            val existingRationBillIds = fetchExistingIds(root.collection("rationCardBills"))
            for (b in dao.getAllRationCardBillsOnce()) {
                if (b.id !in existingRationBillIds) {
                    val domain = b.toDomain()
                    ops.add(
                        root.collection("rationCardBills").document(b.id) to mapOf(
                            "billNumber" to b.billNumber.take(40),
                            "cardUid" to b.cardUid.take(100),
                            "cardNumber" to b.cardNumber.take(60),
                            "beneficiaryName" to b.beneficiaryName.take(100),
                            "cnic" to b.cnic.take(30),
                            "phone" to b.phone.take(30),
                            "monthKey" to b.monthKey.take(20),
                            "subsidyLimit" to b.subsidyLimit.coerceIn(0.0, 1_000_000.0),
                            "totalAmount" to b.totalAmount.coerceIn(0.01, 10_000_000.0),
                            "subsidyUsed" to b.subsidyUsed.coerceIn(0.0, 1_000_000.0),
                            "cashTopUp" to b.cashTopUp.coerceIn(0.0, 10_000_000.0),
                            "remainingSubsidy" to b.remainingSubsidy.coerceIn(0.0, 1_000_000.0),
                            "items" to domain.items.take(100).map { it.toFirestoreMap() },
                            "scannedViaNfc" to b.scannedViaNfc,
                            "note" to b.note.take(300),
                            "createdAt" to b.createdAtMs.toFirebaseTimestamp()
                        )
                    )
                }
            }

            // Commit in safe chunks under Firestore's 500-op batch limit
            for (chunk in ops.chunked(400)) {
                val batch = db.batch()
                for ((ref, payload) in chunk) {
                    batch.set(ref, payload)
                }
                batch.commit().await()
            }

            Result.success(ops.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun restoreCloudToLocal(localDb: ShopLocalDatabase): Result<Int> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required"))
        val root = userDoc(uid)
        return try {
            requireUid()
            val products = root.collection("products").get().await()
                .toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val customers = root.collection("customers").get().await()
                .toObjects(Customer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val orders = root.collection("orders").get().await()
                .toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val ledger = root.collection("ledgerEntries").get().await()
                .toObjects(LedgerEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val movements = root.collection("stockMovements").get().await()
                .toObjects(StockMovement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val donations = root.collection("donations").get().await()
                .toObjects(RationDonation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val rationItems = root.collection("rationItems").get().await()
                .toObjects(RationCardItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val rationBills = root.collection("rationCardBills").get().await()
                .toObjects(RationCardBill::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            val dao = localDb.shopDao()
            localDb.withTransaction {
                if (products.isNotEmpty()) dao.upsertProducts(products.filter { it.id.isNotBlank() }.map { it.toEntity() })
                if (customers.isNotEmpty()) dao.upsertCustomers(customers.filter { it.id.isNotBlank() }.map { it.toEntity() })
                if (orders.isNotEmpty()) dao.upsertOrders(orders.filter { it.id.isNotBlank() }.map { it.toEntity() })
                if (ledger.isNotEmpty()) dao.upsertLedgerEntries(ledger.filter { it.id.isNotBlank() }.map { it.toEntity() })
                if (movements.isNotEmpty()) dao.upsertStockMovements(movements.filter { it.id.isNotBlank() }.map { it.toEntity() })
                if (donations.isNotEmpty()) dao.upsertDonations(donations.filter { it.id.isNotBlank() }.map { it.toEntity() })
                if (rationItems.isNotEmpty()) dao.upsertRationCardItems(rationItems.filter { it.id.isNotBlank() }.map { it.toEntity() })
                if (rationBills.isNotEmpty()) dao.upsertRationCardBills(rationBills.filter { it.id.isNotBlank() }.map { it.toEntity() })
            }

            val totalRestored = products.size + customers.size + orders.size + ledger.size +
                movements.size + donations.size + rationItems.size + rationBills.size
            Result.success(totalRestored)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$uid")
            Result.failure(e)
        }
    }

    private suspend fun fetchExistingIds(colRef: com.google.firebase.firestore.CollectionReference): Set<String> {
        return try {
            colRef.get().await().documents.map { it.id }.toSet()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptySet()
        }
    }

    companion object {
        @Volatile
        private var cachedFirestore: FirebaseFirestore? = null

        fun createConfiguredFirestore(appContext: Context): FirebaseFirestore {
            cachedFirestore?.let { return it }
            return synchronized(this) {
                cachedFirestore?.let { return it }
                val databaseId = appContext.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(
                            PersistentCacheSettings.newBuilder()
                                .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                                .build()
                        )
                        .build()
                    firestore.firestoreSettings = settings
                } catch (e: IllegalStateException) {
                    Log.d("ShopCloudBackup", "Firestore settings already applied: ${e.message}")
                }
                cachedFirestore = firestore
                firestore
            }
        }
    }
}
