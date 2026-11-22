package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShopRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext)
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // --- REALTIME OBSERVATION FLOWS (Work Offline via Persistent Disk Cache + Cloud Sync) ---

    fun observeProducts(userId: String): Flow<List<Product>> = flow {
        val path = "users/$userId/products"
        emitAll(
            userDoc(userId).collection("products")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.nameUrdu }
                }
                .catch { error ->
                    logStreamError(error, userId, path)
                    throw error
                }
        )
    }

    fun observeCustomers(userId: String): Flow<List<Customer>> = flow {
        val path = "users/$userId/customers"
        emitAll(
            userDoc(userId).collection("customers")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Customer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.balance }
                }
                .catch { error ->
                    logStreamError(error, userId, path)
                    throw error
                }
        )
    }

    fun observeOrders(userId: String): Flow<List<Order>> = flow {
        val path = "users/$userId/orders"
        emitAll(
            userDoc(userId).collection("orders")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, userId, path)
                    throw error
                }
        )
    }

    fun observeLedgerEntries(userId: String): Flow<List<LedgerEntry>> = flow {
        val path = "users/$userId/ledgerEntries"
        emitAll(
            userDoc(userId).collection("ledgerEntries")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LedgerEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, userId, path)
                    throw error
                }
        )
    }

    fun observeStockMovements(userId: String): Flow<List<StockMovement>> = flow {
        val path = "users/$userId/stockMovements"
        emitAll(
            userDoc(userId).collection("stockMovements")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(StockMovement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, userId, path)
                    throw error
                }
        )
    }

    fun observeDonations(userId: String): Flow<List<RationDonation>> = flow {
        val path = "users/$userId/donations"
        emitAll(
            userDoc(userId).collection("donations")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(RationDonation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, userId, path)
                    throw error
                }
        )
    }

    fun observeRationCardItems(userId: String): Flow<List<RationCardItem>> = flow {
        val path = "users/$userId/rationItems"
        emitAll(
            userDoc(userId).collection("rationItems")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(RationCardItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy<RationCardItem> { it.sortOrder }.thenBy { it.nameUrdu })
                }
                .catch { error ->
                    logStreamError(error, userId, path)
                    throw error
                }
        )
    }

    fun observeRationCardBills(userId: String): Flow<List<RationCardBill>> = flow {
        val path = "users/$userId/rationCardBills"
        emitAll(
            userDoc(userId).collection("rationCardBills")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(RationCardBill::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, userId, path)
                    throw error
                }
        )
    }

    private fun logStreamError(error: Throwable, userId: String, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
    }

    // --- SEED GUJRAT CATALOG IF NEW SHOP ---

    suspend fun seedGujratCatalogIfEmpty(): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/products"
        return try {
            val existingSnap = userDoc(uid).collection("products").limit(1).get().await()
            if (!existingSnap.isEmpty) return Result.success(0)

            val batch = db.batch()
            for (seed in GujratCatalogData.initialProducts) {
                val docRef = userDoc(uid).collection("products").document(seed.docId)
                batch.set(
                    docRef,
                    mapOf(
                        "nameUrdu" to seed.nameUrdu,
                        "nameEnglish" to seed.nameEnglish,
                        "category" to seed.category.name,
                        "unit" to seed.unit,
                        "costPrice" to seed.costPrice,
                        "sellingPrice" to seed.sellingPrice,
                        "marginPercent" to seed.marginPercent,
                        "autoPriceEnabled" to true,
                        "stockQty" to seed.stockQty,
                        "lowStockThreshold" to seed.lowStockThreshold,
                        "barcode" to seed.barcode,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(GujratCatalogData.initialProducts.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- PRODUCT CRUD & AUTO/MANUAL PRICE UPDATES ---

    suspend fun saveProduct(
        existingId: String?,
        nameUrdu: String,
        nameEnglish: String,
        category: ProductCategory,
        unit: String,
        costPrice: Double,
        sellingPrice: Double,
        marginPercent: Double,
        autoPriceEnabled: Boolean,
        stockQty: Double,
        lowStockThreshold: Double,
        barcode: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanUrdu = nameUrdu.trim().take(120)
        val cleanEng = nameEnglish.trim().ifBlank { cleanUrdu }.take(120)
        val cleanUnit = unit.trim().ifBlank { "kg" }.take(30)
        val safeCost = costPrice.coerceIn(0.0, 1_000_000.0)
        val safeMargin = marginPercent.coerceIn(0.0, 500.0)
        val computedSelling = if (autoPriceEnabled) {
            GujratCatalogData.calculateAutoSellingPrice(safeCost, safeMargin)
        } else {
            sellingPrice.coerceIn(0.0, 1_000_000.0)
        }
        val safeStock = stockQty.coerceIn(0.0, 1_000_000.0)
        val safeLow = lowStockThreshold.coerceIn(0.0, 100_000.0)
        val cleanBarcode = barcode.trim().take(64)

        if (cleanUrdu.isBlank()) return Result.failure(IllegalArgumentException("Product name required"))

        val productsCol = userDoc(uid).collection("products")
        val isUpdate = !existingId.isNullOrBlank()
        val docRef = if (isUpdate) productsCol.document(existingId!!) else productsCol.document()
        val docPath = docRef.path

        return try {
            requireUserId()
            if (isUpdate) {
                docRef.update(
                    mapOf(
                        "nameUrdu" to cleanUrdu,
                        "nameEnglish" to cleanEng,
                        "category" to category.name,
                        "unit" to cleanUnit,
                        "costPrice" to safeCost,
                        "sellingPrice" to computedSelling,
                        "marginPercent" to safeMargin,
                        "autoPriceEnabled" to autoPriceEnabled,
                        "stockQty" to safeStock,
                        "lowStockThreshold" to safeLow,
                        "barcode" to cleanBarcode,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "nameUrdu" to cleanUrdu,
                        "nameEnglish" to cleanEng,
                        "category" to category.name,
                        "unit" to cleanUnit,
                        "costPrice" to safeCost,
                        "sellingPrice" to computedSelling,
                        "marginPercent" to safeMargin,
                        "autoPriceEnabled" to autoPriceEnabled,
                        "stockQty" to safeStock,
                        "lowStockThreshold" to safeLow,
                        "barcode" to cleanBarcode,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docPath = "users/$uid/products/$productId"
        return try {
            requireUserId()
            require(productId.isNotBlank())
            userDoc(uid).collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Automatically updates selling prices for all products with autoPriceEnabled = true
     * according to District Gujrat Mandi reference cost rates and each item's configured profit margin.
     */
    suspend fun applyGujratMarketRates(products: List<Product>, fallbackMarginPercent: Double): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/products"
        return try {
            requireUserId()
            val autoProducts = products.filter { it.autoPriceEnabled && it.id.isNotBlank() }
            if (autoProducts.isEmpty()) return Result.success(0)

            var updatedCount = 0
            val chunks = autoProducts.chunked(400)
            for (chunk in chunks) {
                val batch = db.batch()
                for (prod in chunk) {
                    val marketCost = GujratCatalogData.gujratMarketCostLookup(prod)
                    val margin = if (prod.marginPercent > 0) prod.marginPercent else fallbackMarginPercent
                    val newSelling = GujratCatalogData.calculateAutoSellingPrice(marketCost, margin)
                    val docRef = userDoc(uid).collection("products").document(prod.id)
                    batch.update(
                        docRef,
                        mapOf(
                            "costPrice" to marketCost,
                            "sellingPrice" to newSelling,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    updatedCount++
                }
                batch.commit().await()
            }
            Result.success(updatedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- STOCK IN / STOCK OUT ---

    suspend fun recordStockMovement(
        product: Product,
        type: StockMovementType,
        quantity: Double,
        newUnitCost: Double,
        note: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/stockMovements"
        return try {
            requireUserId()
            require(product.id.isNotBlank())
            val safeQty = quantity.coerceIn(0.01, 1_000_000.0)
            val safeCost = newUnitCost.coerceIn(0.0, 1_000_000.0)

            val updatedStock = when (type) {
                StockMovementType.STOCK_IN -> (product.stockQty + safeQty).coerceAtMost(1_000_000.0)
                StockMovementType.STOCK_OUT, StockMovementType.SALE -> (product.stockQty - safeQty).coerceAtLeast(0.0)
                StockMovementType.ADJUSTMENT -> safeQty
            }

            val updatedSellingPrice = if (type == StockMovementType.STOCK_IN && product.autoPriceEnabled) {
                GujratCatalogData.calculateAutoSellingPrice(safeCost, product.marginPercent)
            } else {
                product.sellingPrice
            }

            val profitDelta = when (type) {
                StockMovementType.STOCK_OUT -> -(safeCost * safeQty)
                else -> 0.0
            }

            val batch = db.batch()
            val prodRef = userDoc(uid).collection("products").document(product.id)
            batch.update(
                prodRef,
                mapOf(
                    "stockQty" to updatedStock,
                    "costPrice" to safeCost,
                    "sellingPrice" to updatedSellingPrice,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val movRef = userDoc(uid).collection("stockMovements").document()
            batch.set(
                movRef,
                mapOf(
                    "productId" to product.id,
                    "productNameUrdu" to product.nameUrdu.take(120),
                    "productNameEnglish" to product.nameEnglish.take(120),
                    "movementType" to type.name,
                    "quantity" to safeQty,
                    "unitCost" to safeCost,
                    "unitPrice" to updatedSellingPrice,
                    "profitDelta" to profitDelta,
                    "note" to note.trim().take(300),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- CUSTOMER UDHAR KHATA ---

    suspend fun addCustomer(name: String, phone: String, address: String, openingUdhar: Double): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanName = name.trim().take(100)
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Customer name required"))
        val safeBalance = openingUdhar.coerceIn(-10_000_000.0, 10_000_000.0)
        val docRef = userDoc(uid).collection("customers").document()

        return try {
            requireUserId()
            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "name" to cleanName,
                    "phone" to phone.trim().take(30),
                    "address" to address.trim().take(200),
                    "balance" to safeBalance,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (safeBalance > 0.0) {
                val entryRef = userDoc(uid).collection("ledgerEntries").document()
                batch.set(
                    entryRef,
                    mapOf(
                        "customerId" to docRef.id,
                        "customerName" to cleanName,
                        "entryType" to LedgerEntryType.DEBIT.name,
                        "amount" to safeBalance,
                        "balanceAfter" to safeBalance,
                        "orderId" to "",
                        "note" to "سابقہ بقایا / Opening Udhar",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordLedgerTransaction(
        customer: Customer,
        entryType: LedgerEntryType,
        amount: Double,
        note: String
    ): Result<LedgerEntry> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val safeAmount = amount.coerceIn(1.0, 10_000_000.0)
        val newBalance = when (entryType) {
            LedgerEntryType.DEBIT -> (customer.balance + safeAmount).coerceIn(-10_000_000.0, 10_000_000.0)
            LedgerEntryType.CREDIT -> (customer.balance - safeAmount).coerceIn(-10_000_000.0, 10_000_000.0)
        }
        val cleanNote = note.trim().take(300)
        val path = "users/$uid/ledgerEntries"

        return try {
            requireUserId()
            require(customer.id.isNotBlank())
            val batch = db.batch()
            val custRef = userDoc(uid).collection("customers").document(customer.id)
            batch.update(
                custRef,
                mapOf(
                    "balance" to newBalance,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val entryRef = userDoc(uid).collection("ledgerEntries").document()
            batch.set(
                entryRef,
                mapOf(
                    "customerId" to customer.id,
                    "customerName" to customer.name.take(100),
                    "entryType" to entryType.name,
                    "amount" to safeAmount,
                    "balanceAfter" to newBalance,
                    "orderId" to "",
                    "note" to cleanNote,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(
                LedgerEntry(
                    id = entryRef.id,
                    customerId = customer.id,
                    customerName = customer.name,
                    entryType = entryType.name,
                    amount = safeAmount,
                    balanceAfter = newBalance,
                    orderId = "",
                    note = cleanNote
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- ORDERS & AUTOMATIC BILLING (Deducts Stock, Updates Udhar Khata, Logs Profit) ---

    suspend fun createOrderOrBill(
        cartItems: List<CartItem>,
        customer: Customer?,
        customerNameInput: String,
        paymentMethod: PaymentMethod,
        discount: Double,
        paidAmountInput: Double,
        saveAsPendingOrder: Boolean,
        note: String
    ): Result<Order> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (cartItems.isEmpty()) return Result.failure(IllegalArgumentException("Cart is empty"))

        val cleanCustomerName = customerNameInput.trim()
            .ifBlank { customer?.name ?: "نقد کسٹمر" }
            .take(100)
        val subtotal = cartItems.sumOf { it.lineTotal }.coerceIn(0.0, 10_000_000.0)
        val safeDiscount = discount.coerceIn(0.0, subtotal)
        val totalAmount = (subtotal - safeDiscount).coerceAtLeast(0.0)
        val totalCost = cartItems.sumOf { it.lineCost }.coerceIn(0.0, 10_000_000.0)
        val profitAmount = (totalAmount - totalCost).coerceIn(-10_000_000.0, 10_000_000.0)

        val paidAmount = when {
            saveAsPendingOrder -> 0.0
            paymentMethod == PaymentMethod.CASH -> totalAmount
            paymentMethod == PaymentMethod.UDHAR -> 0.0
            else -> paidAmountInput.coerceIn(0.0, totalAmount)
        }
        val remainingUdhar = if (saveAsPendingOrder) 0.0 else (totalAmount - paidAmount).coerceAtLeast(0.0)
        val status = if (saveAsPendingOrder) OrderStatus.PENDING else OrderStatus.COMPLETED

        val billNo = "AB-" + SimpleDateFormat("MMdd-HHmmss", Locale.US).format(Date())
        val orderItems = cartItems.take(100).map {
            OrderItem(
                productId = it.product.id,
                nameUrdu = it.product.nameUrdu.take(120),
                nameEnglish = it.product.nameEnglish.take(120),
                unit = it.product.unit.take(30),
                quantity = it.quantity,
                unitPrice = it.effectivePrice,
                costPrice = it.product.costPrice,
                lineTotal = it.lineTotal
            )
        }

        val orderRef = userDoc(uid).collection("orders").document()

        return try {
            requireUserId()
            val batch = db.batch()
            batch.set(
                orderRef,
                mapOf(
                    "billNumber" to billNo,
                    "customerId" to (customer?.id ?: ""),
                    "customerName" to cleanCustomerName,
                    "status" to status.name,
                    "paymentMethod" to paymentMethod.name,
                    "subtotal" to subtotal,
                    "discount" to safeDiscount,
                    "totalAmount" to totalAmount,
                    "paidAmount" to paidAmount,
                    "remainingUdhar" to remainingUdhar,
                    "totalCost" to totalCost,
                    "profitAmount" to profitAmount,
                    "items" to orderItems.map { it.toFirestoreMap() },
                    "note" to note.trim().take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // If COMPLETED, immediately deduct stock, log SALE stock movements, and post Udhar Khata debit
            if (status == OrderStatus.COMPLETED) {
                for (ci in cartItems.take(100)) {
                    if (ci.product.id.isNotBlank()) {
                        val newStock = (ci.product.stockQty - ci.quantity).coerceAtLeast(0.0)
                        val prodRef = userDoc(uid).collection("products").document(ci.product.id)
                        batch.update(
                            prodRef,
                            mapOf(
                                "stockQty" to newStock,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        val movRef = userDoc(uid).collection("stockMovements").document()
                        batch.set(
                            movRef,
                            mapOf(
                                "productId" to ci.product.id,
                                "productNameUrdu" to ci.product.nameUrdu.take(120),
                                "productNameEnglish" to ci.product.nameEnglish.take(120),
                                "movementType" to StockMovementType.SALE.name,
                                "quantity" to ci.quantity.coerceAtLeast(0.01),
                                "unitCost" to ci.product.costPrice,
                                "unitPrice" to ci.effectivePrice,
                                "profitDelta" to ((ci.effectivePrice - ci.product.costPrice) * ci.quantity),
                                "note" to "بل #$billNo - $cleanCustomerName".take(300),
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }

                if (remainingUdhar > 0.0 && customer != null && customer.id.isNotBlank()) {
                    val updatedBalance = (customer.balance + remainingUdhar).coerceIn(-10_000_000.0, 10_000_000.0)
                    val custRef = userDoc(uid).collection("customers").document(customer.id)
                    batch.update(
                        custRef,
                        mapOf(
                            "balance" to updatedBalance,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val ledgerRef = userDoc(uid).collection("ledgerEntries").document()
                    batch.set(
                        ledgerRef,
                        mapOf(
                            "customerId" to customer.id,
                            "customerName" to customer.name.take(100),
                            "entryType" to LedgerEntryType.DEBIT.name,
                            "amount" to remainingUdhar,
                            "balanceAfter" to updatedBalance,
                            "orderId" to orderRef.id,
                            "note" to "بل #$billNo ادھار".take(300),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            batch.commit().await()

            Result.success(
                Order(
                    id = orderRef.id,
                    billNumber = billNo,
                    customerId = customer?.id ?: "",
                    customerName = cleanCustomerName,
                    status = status.name,
                    paymentMethod = paymentMethod.name,
                    subtotal = subtotal,
                    discount = safeDiscount,
                    totalAmount = totalAmount,
                    paidAmount = paidAmount,
                    remainingUdhar = remainingUdhar,
                    totalCost = totalCost,
                    profitAmount = profitAmount,
                    items = orderItems,
                    note = note.trim().take(300)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun completePendingOrder(
        order: Order,
        products: List<Product>,
        customer: Customer?,
        paymentMethod: PaymentMethod
    ): Result<Order> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = userDoc(uid).collection("orders").document(order.id)
        val paid = if (paymentMethod == PaymentMethod.CASH) order.totalAmount else 0.0
        val udhar = if (paymentMethod == PaymentMethod.UDHAR) order.totalAmount else 0.0

        return try {
            requireUserId()
            require(order.id.isNotBlank() && order.statusEnum() == OrderStatus.PENDING)
            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    "status" to OrderStatus.COMPLETED.name,
                    "paymentMethod" to paymentMethod.name,
                    "paidAmount" to paid,
                    "remainingUdhar" to udhar,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val prodMap = products.associateBy { it.id }
            for (item in order.items) {
                val prod = prodMap[item.productId]
                if (prod != null) {
                    val newStock = (prod.stockQty - item.quantity).coerceAtLeast(0.0)
                    batch.update(
                        userDoc(uid).collection("products").document(prod.id),
                        mapOf("stockQty" to newStock, "updatedAt" to FieldValue.serverTimestamp())
                    )
                    val movRef = userDoc(uid).collection("stockMovements").document()
                    batch.set(
                        movRef,
                        mapOf(
                            "productId" to prod.id,
                            "productNameUrdu" to prod.nameUrdu.take(120),
                            "productNameEnglish" to prod.nameEnglish.take(120),
                            "movementType" to StockMovementType.SALE.name,
                            "quantity" to item.quantity.coerceAtLeast(0.01),
                            "unitCost" to item.costPrice,
                            "unitPrice" to item.unitPrice,
                            "profitDelta" to ((item.unitPrice - item.costPrice) * item.quantity),
                            "note" to "آرڈر مکمل #${order.billNumber}".take(300),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            if (udhar > 0.0 && customer != null && customer.id.isNotBlank()) {
                val updatedBalance = (customer.balance + udhar).coerceIn(-10_000_000.0, 10_000_000.0)
                batch.update(
                    userDoc(uid).collection("customers").document(customer.id),
                    mapOf("balance" to updatedBalance, "updatedAt" to FieldValue.serverTimestamp())
                )
                val ledgerRef = userDoc(uid).collection("ledgerEntries").document()
                batch.set(
                    ledgerRef,
                    mapOf(
                        "customerId" to customer.id,
                        "customerName" to customer.name.take(100),
                        "entryType" to LedgerEntryType.DEBIT.name,
                        "amount" to udhar,
                        "balanceAfter" to updatedBalance,
                        "orderId" to order.id,
                        "note" to "آرڈر #${order.billNumber} ادھار".take(300),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(
                order.copy(
                    status = OrderStatus.COMPLETED.name,
                    paymentMethod = paymentMethod.name,
                    paidAmount = paid,
                    remainingUdhar = udhar
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelPendingOrder(orderId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = userDoc(uid).collection("orders").document(orderId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "status" to OrderStatus.CANCELLED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordRationDonation(
        entryType: DonationEntryType,
        personName: String,
        phone: String,
        amount: Double,
        rationSummary: String,
        paymentRef: String
    ): Result<RationDonation> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanName = personName.trim().ifBlank {
            if (entryType == DonationEntryType.DONATION_IN) "مخیر حضرات" else "مستحق خاندان"
        }.take(100)
        val safeAmount = amount.coerceIn(1.0, 10_000_000.0)
        val cleanPhone = phone.trim().take(30)
        val cleanSummary = rationSummary.trim().take(300)
        val cleanRef = paymentRef.trim().take(100)
        val docRef = userDoc(uid).collection("donations").document()

        return try {
            requireUserId()
            docRef.set(
                mapOf(
                    "entryType" to entryType.name,
                    "personName" to cleanName,
                    "phone" to cleanPhone,
                    "amount" to safeAmount,
                    "rationSummary" to cleanSummary,
                    "paymentRef" to cleanRef,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(
                RationDonation(
                    id = docRef.id,
                    entryType = entryType.name,
                    personName = cleanName,
                    phone = cleanPhone,
                    amount = safeAmount,
                    rationSummary = cleanSummary,
                    paymentRef = cleanRef
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- MARYAM NAWAZ RATION CARD (PUNJAB RS. 3,000 NFC PROGRAM) ---

    suspend fun seedMaryamRationItemsIfEmpty(): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/rationItems"
        return try {
            val existingSnap = userDoc(uid).collection("rationItems").limit(1).get().await()
            if (!existingSnap.isEmpty) return Result.success(0)

            val batch = db.batch()
            for (seed in MaryamRationCatalogData.defaultItems) {
                val docRef = userDoc(uid).collection("rationItems").document(seed.docId)
                batch.set(
                    docRef,
                    mapOf(
                        "nameUrdu" to seed.nameUrdu,
                        "nameEnglish" to seed.nameEnglish,
                        "unit" to seed.unit,
                        "price" to seed.price,
                        "sortOrder" to seed.sortOrder,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(MaryamRationCatalogData.defaultItems.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveRationCardItem(
        existingId: String?,
        nameUrdu: String,
        nameEnglish: String,
        unit: String,
        price: Double,
        sortOrder: Int
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanUrdu = nameUrdu.trim().take(120)
        if (cleanUrdu.isBlank()) return Result.failure(IllegalArgumentException("Item name required"))
        val cleanEng = nameEnglish.trim().ifBlank { cleanUrdu }.take(120)
        val cleanUnit = unit.trim().ifBlank { "kg" }.take(30)
        val safePrice = price.coerceIn(0.0, 1_000_000.0)
        val safeOrder = sortOrder.coerceIn(0, 10_000)

        val col = userDoc(uid).collection("rationItems")
        val isUpdate = !existingId.isNullOrBlank()
        val docRef = if (isUpdate) col.document(existingId!!) else col.document()

        return try {
            requireUserId()
            if (isUpdate) {
                docRef.update(
                    mapOf(
                        "nameUrdu" to cleanUrdu,
                        "nameEnglish" to cleanEng,
                        "unit" to cleanUnit,
                        "price" to safePrice,
                        "sortOrder" to safeOrder,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "nameUrdu" to cleanUrdu,
                        "nameEnglish" to cleanEng,
                        "unit" to cleanUnit,
                        "price" to safePrice,
                        "sortOrder" to safeOrder,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteRationCardItem(itemId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docPath = "users/$uid/rationItems/$itemId"
        return try {
            requireUserId()
            require(itemId.isNotBlank())
            userDoc(uid).collection("rationItems").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createRationCardBill(
        cartItems: List<RationCartItem>,
        cardUid: String,
        cardNumber: String,
        beneficiaryName: String,
        cnic: String,
        phone: String,
        subsidyLimit: Double,
        alreadyUsedThisMonth: Double,
        scannedViaNfc: Boolean,
        note: String
    ): Result<RationCardBill> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (cartItems.isEmpty()) return Result.failure(IllegalArgumentException("Select at least one ration item"))

        val cleanName = beneficiaryName.trim().ifBlank { "راشن کارڈ صارف" }.take(100)
        val cleanUid = cardUid.trim().take(100)
        val cleanCardNo = cardNumber.trim().take(60)
        val cleanCnic = cnic.trim().take(30)
        val cleanPhone = phone.trim().take(30)
        val cleanNote = note.trim().take(300)
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val billNo = "MN-" + SimpleDateFormat("MMdd-HHmmss", Locale.US).format(Date())

        val safeLimit = subsidyLimit.coerceIn(0.0, 1_000_000.0)
        val availableBeforeBill = (safeLimit - alreadyUsedThisMonth).coerceAtLeast(0.0)
        val totalAmount = cartItems.sumOf { it.lineTotal }.coerceIn(1.0, 10_000_000.0)
        val subsidyUsed = totalAmount.coerceAtMost(availableBeforeBill)
        val cashTopUp = (totalAmount - subsidyUsed).coerceAtLeast(0.0)
        val remainingSubsidy = (availableBeforeBill - subsidyUsed).coerceAtLeast(0.0)

        val orderItems = cartItems.take(100).map {
            OrderItem(
                productId = it.item.id,
                nameUrdu = it.item.nameUrdu.take(120),
                nameEnglish = it.item.nameEnglish.take(120),
                unit = it.item.unit.take(30),
                quantity = it.quantity,
                unitPrice = it.effectivePrice,
                costPrice = it.effectivePrice,
                lineTotal = it.lineTotal
            )
        }

        val docRef = userDoc(uid).collection("rationCardBills").document()
        return try {
            requireUserId()
            docRef.set(
                mapOf(
                    "billNumber" to billNo,
                    "cardUid" to cleanUid,
                    "cardNumber" to cleanCardNo,
                    "beneficiaryName" to cleanName,
                    "cnic" to cleanCnic,
                    "phone" to cleanPhone,
                    "monthKey" to monthKey,
                    "subsidyLimit" to safeLimit,
                    "totalAmount" to totalAmount,
                    "subsidyUsed" to subsidyUsed,
                    "cashTopUp" to cashTopUp,
                    "remainingSubsidy" to remainingSubsidy,
                    "items" to orderItems.map { it.toFirestoreMap() },
                    "scannedViaNfc" to scannedViaNfc,
                    "note" to cleanNote,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(
                RationCardBill(
                    id = docRef.id,
                    billNumber = billNo,
                    cardUid = cleanUid,
                    cardNumber = cleanCardNo,
                    beneficiaryName = cleanName,
                    cnic = cleanCnic,
                    phone = cleanPhone,
                    monthKey = monthKey,
                    subsidyLimit = safeLimit,
                    totalAmount = totalAmount,
                    subsidyUsed = subsidyUsed,
                    cashTopUp = cashTopUp,
                    remainingSubsidy = remainingSubsidy,
                    items = orderItems,
                    scannedViaNfc = scannedViaNfc,
                    note = cleanNote
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncPendingWritesToCloud(): Result<Unit> {
        return try {
            requireUserId()
            db.waitForPendingWrites().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ShopRepository", "Cloud sync waiting/offline: ${e.message}")
            Result.failure(e)
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
                    // Enable persistent local disk cache for full Offline Mode support
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(
                            PersistentCacheSettings.newBuilder()
                                .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                                .build()
                        )
                        .build()
                    firestore.firestoreSettings = settings
                } catch (e: IllegalStateException) {
                    // Firestore already initialized in this process; persistent cache already active
                    Log.d("ShopRepository", "Firestore settings already applied: ${e.message}")
                }
                cachedFirestore = firestore
                firestore
            }
        }
    }
}
