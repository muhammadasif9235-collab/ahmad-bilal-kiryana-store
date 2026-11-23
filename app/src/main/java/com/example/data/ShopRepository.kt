package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.data.local.CustomerEntity
import com.example.data.local.DailyCashEntryEntity
import com.example.data.local.LedgerEntryEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemJsonSerializer
import com.example.data.local.ProductEntity
import com.example.data.local.RationCardBillEntity
import com.example.data.local.RationCardItemEntity
import com.example.data.local.RationDonationEntity
import com.example.data.local.ShopLocalDatabase
import com.example.data.local.StockMovementEntity
import com.example.data.local.WholesalerEntity
import com.example.data.local.WholesalerLedgerEntryEntity
import com.example.data.local.toFirebaseTimestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.round

/**
 * Local-first ShopRepository backed by Room SQLite so standalone APKs run 100% reliably
 * without Google Sign-In or Firebase SHA-1 setup, with optional one-tap Google Cloud Backup/Restore.
 */
class ShopRepository(
    private val localDb: ShopLocalDatabase,
    private val cloudBackupService: ShopCloudBackupService
) {
    constructor(context: Context) : this(
        localDb = ShopLocalDatabase.getInstance(context.applicationContext),
        cloudBackupService = ShopCloudBackupService(context.applicationContext)
    )

    private val dao get() = localDb.shopDao()

    private fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(20)

    private fun roundMoney(value: Double): Double = round(value * 100.0) / 100.0

    private fun roundWeight(value: Double): Double = round(value * 10_000.0) / 10_000.0

    private fun formatQtyCompact(qty: Double): String {
        val rounded = round(qty * 1000.0) / 1000.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.3f", rounded).trimEnd('0').trimEnd('.')
        }
    }

    // --- REACTIVE LOCAL ROOM FLOWS (Zero network or sign-in dependency) ---

    fun observeProducts(): Flow<List<Product>> =
        dao.observeProducts().map { list -> list.map { it.toDomain() } }

    fun observeCustomers(): Flow<List<Customer>> =
        dao.observeCustomers().map { list -> list.map { it.toDomain() } }

    fun observeOrders(): Flow<List<Order>> =
        dao.observeOrders().map { list -> list.map { it.toDomain() } }

    fun observeLedgerEntries(): Flow<List<LedgerEntry>> =
        dao.observeLedgerEntries().map { list -> list.map { it.toDomain() } }

    fun observeStockMovements(): Flow<List<StockMovement>> =
        dao.observeStockMovements().map { list -> list.map { it.toDomain() } }

    fun observeDonations(): Flow<List<RationDonation>> =
        dao.observeDonations().map { list -> list.map { it.toDomain() } }

    fun observeRationCardItems(): Flow<List<RationCardItem>> =
        dao.observeRationCardItems().map { list -> list.map { it.toDomain() } }

    fun observeRationCardBills(): Flow<List<RationCardBill>> =
        dao.observeRationCardBills().map { list -> list.map { it.toDomain() } }

    fun observeDailyCashEntries(): Flow<List<DailyCashEntry>> =
        dao.observeDailyCashEntries().map { list -> list.map { it.toDomain() } }

    fun observeWholesalers(): Flow<List<Wholesaler>> =
        dao.observeWholesalers().map { list -> list.map { it.toDomain() } }

    fun observeWholesalerEntries(): Flow<List<WholesalerLedgerEntry>> =
        dao.observeWholesalerEntries().map { list -> list.map { it.toDomain() } }

    // --- AUTO-SEED INITIAL CATALOGS ON FIRST LAUNCH ---

        suspend fun seedGujratCatalogIfEmpty(): Result<Int> {
        return try {
            val now = System.currentTimeMillis()
            val existing = dao.getAllProductsOnce()
            val existingIds = existing.map { it.id }.toSet()

            // Remove old loose milk item if present
            if ("prod_fresh_milk" in existingIds) {
                dao.deleteProductById("prod_fresh_milk")
            }

            // Reset pre-seeded demo stock on legacy items that have no manual stock movements
            val movedProductIds = dao.getAllStockMovementsOnce().map { it.productId }.toSet()
            val legacyPreseededToZero = existing.filter { prod ->
                prod.id.startsWith("prod_") &&
                    prod.id != "prod_fresh_milk" &&
                    prod.id !in movedProductIds &&
                    prod.stockQty > 0.0
            }.map { prod ->
                prod.copy(
                    stockQty = 0.0,
                    costPrice = 0.0,
                    sellingPrice = 0.0,
                    updatedAtMs = now
                )
            }
            if (legacyPreseededToZero.isNotEmpty()) {
                dao.upsertProducts(legacyPreseededToZero)
            }

            // Add all new Kiryana items (with 0 stock and 0 price) without duplicating existing ones
            val missingSeeds = GujratCatalogData.initialProducts.filter { it.docId !in existingIds }
            if (missingSeeds.isNotEmpty()) {
                val entities = missingSeeds.map { seed ->
                    ProductEntity(
                        id = seed.docId,
                        nameUrdu = seed.nameUrdu,
                        nameEnglish = seed.nameEnglish,
                        category = seed.category.name,
                        unit = seed.unit,
                        costPrice = 0.0,
                        sellingPrice = 0.0,
                        marginPercent = seed.marginPercent,
                        autoPriceEnabled = true,
                        stockQty = 0.0,
                        lowStockThreshold = seed.lowStockThreshold,
                        barcode = seed.barcode,
                        createdAtMs = now,
                        updatedAtMs = now
                    )
                }
                dao.upsertProducts(entities)
            }
            Result.success(missingSeeds.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync Kiryana catalog", e)
            Result.failure(e)
        }
        }
        
    suspend fun seedMaryamRationItemsIfEmpty(): Result<Int> {
        return try {
            if (dao.countRationCardItems() > 0) return Result.success(0)
            val now = System.currentTimeMillis()
            val entities = MaryamRationCatalogData.defaultItems.map { seed ->
                RationCardItemEntity(
                    id = seed.docId,
                    nameUrdu = seed.nameUrdu,
                    nameEnglish = seed.nameEnglish,
                    unit = seed.unit,
                    price = seed.price,
                    sortOrder = seed.sortOrder,
                    createdAtMs = now,
                    updatedAtMs = now
                )
            }
            dao.upsertRationCardItems(entities)
            Result.success(entities.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed Maryam Ration items", e)
            Result.failure(e)
        }
    }

    // --- PRODUCT CRUD & GUJRAT MARKET RATES ---

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
        val cleanUrdu = nameUrdu.trim().take(120)
        if (cleanUrdu.isBlank()) return Result.failure(IllegalArgumentException("Product name required"))
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

        return try {
            val now = System.currentTimeMillis()
            val id = existingId?.takeIf { it.isNotBlank() } ?: newId()
            val existing = if (!existingId.isNullOrBlank()) dao.getProductById(id) else null
            dao.upsertProduct(
                ProductEntity(
                    id = id,
                    nameUrdu = cleanUrdu,
                    nameEnglish = cleanEng,
                    category = category.name,
                    unit = cleanUnit,
                    costPrice = safeCost,
                    sellingPrice = computedSelling,
                    marginPercent = safeMargin,
                    autoPriceEnabled = autoPriceEnabled,
                    stockQty = safeStock,
                    lowStockThreshold = safeLow,
                    barcode = cleanBarcode,
                    createdAtMs = existing?.createdAtMs ?: now,
                    updatedAtMs = now
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving product", e)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            require(productId.isNotBlank())
            dao.deleteProductById(productId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting product", e)
            Result.failure(e)
        }
    }

        suspend fun applyGujratMarketRates(
        products: List<Product>,
        fallbackMarginPercent: Double,
        locationHint: String = "",
        countryCode: String = "PK"
    ): Result<Int> {
        return try {
            val autoProducts = products.filter { it.autoPriceEnabled && it.id.isNotBlank() }
            if (autoProducts.isEmpty()) return Result.success(0)

            val loc = locationHint.lowercase()
            val cleanCountry = countryCode.trim().uppercase().ifBlank { "PK" }

            // Regional & international market cost multiplier based on detected location
            val regionFactor = when {
                cleanCountry != "PK" && !loc.contains("pakistan") &&
                    !loc.contains("punjab") && !loc.contains("sindh") &&
                    !loc.contains("kpk") && !loc.contains("balochistan") &&
                    !loc.contains("kashmir") && !loc.contains("gujrat") &&
                    !loc.contains("tanda") -> {
                    when (cleanCountry) {
                        "SA", "AE", "QA", "KW", "OM", "BH" -> 1.18 // Gulf / Middle East market
                        "GB", "US", "CA", "AU", "DE", "FR", "IT", "ES" -> 1.35 // UK / US / Europe market
                        else -> 1.15 // International market
                    }
                }
                loc.contains("balochistan") || loc.contains("quetta") || loc.contains("gwadar") ||
                    loc.contains("بلوچستان") || loc.contains("کوئٹہ") -> 1.06
                loc.contains("kashmir") || loc.contains("ajk") || loc.contains("muzaffarabad") ||
                    loc.contains("mirpur") || loc.contains("gilgit") || loc.contains("skardu") ||
                    loc.contains("کشمیر") || loc.contains("گلگت") || loc.contains("مظفرآباد") -> 1.05
                loc.contains("khyber") || loc.contains("kpk") || loc.contains("peshawar") ||
                    loc.contains("mardan") || loc.contains("swat") || loc.contains("abbottabad") ||
                    loc.contains("خیبر") || loc.contains("پشاور") -> 1.03
                loc.contains("sindh") || loc.contains("karachi") || loc.contains("hyderabad") ||
                    loc.contains("sukkur") || loc.contains("سندھ") || loc.contains("کراچی") -> 1.02
                else -> 1.00 // Punjab (Tanda, Gujrat, Lahore, Rawalpindi, Faisalabad, Multan, etc.)
            }

            val now = System.currentTimeMillis()
            var updatedCount = 0
            localDb.withTransaction {
                for (prod in autoProducts) {
                    val baseMarketCost = GujratCatalogData.gujratMarketCostLookup(prod)
                    val regionalMarketCost = kotlin.math.round(baseMarketCost * regionFactor)
                    val margin = if (prod.marginPercent > 0) prod.marginPercent else fallbackMarginPercent
                    val newSelling = GujratCatalogData.calculateAutoSellingPrice(regionalMarketCost, margin)
                    dao.updateProductPrices(prod.id, regionalMarketCost, newSelling, now)
                    updatedCount++
                }
            }
            Result.success(updatedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error applying location-based market rates", e)
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
        return try {
            require(product.id.isNotBlank())
            val safeQty = roundWeight(quantity.coerceIn(0.001, 1_000_000.0))
            val safeCost = newUnitCost.coerceIn(0.0, 1_000_000.0)
            val updatedStock = when (type) {
                StockMovementType.STOCK_IN -> roundWeight((product.stockQty + safeQty).coerceAtMost(1_000_000.0))
                StockMovementType.STOCK_OUT, StockMovementType.SALE -> roundWeight((product.stockQty - safeQty).coerceAtLeast(0.0))
                StockMovementType.ADJUSTMENT -> safeQty
            }
            val updatedSellingPrice = if (type == StockMovementType.STOCK_IN && product.autoPriceEnabled) {
                GujratCatalogData.calculateAutoSellingPrice(safeCost, product.marginPercent)
            } else {
                product.sellingPrice
            }
            val profitDelta = if (type == StockMovementType.STOCK_OUT) -(safeCost * safeQty) else 0.0
            val now = System.currentTimeMillis()

            localDb.withTransaction {
                dao.updateProductStockAndPrices(product.id, updatedStock, safeCost, updatedSellingPrice, now)
                dao.insertStockMovement(
                    StockMovementEntity(
                        id = newId(),
                        productId = product.id,
                        productNameUrdu = product.nameUrdu.take(120),
                        productNameEnglish = product.nameEnglish.take(120),
                        movementType = type.name,
                        quantity = safeQty,
                        unitCost = safeCost,
                        unitPrice = updatedSellingPrice,
                        profitDelta = profitDelta,
                        note = note.trim().take(300),
                        createdAtMs = now
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error recording stock movement", e)
            Result.failure(e)
        }
    }

    // --- CUSTOMER UDHAR KHATA ---

    suspend fun addCustomer(name: String, phone: String, address: String, openingUdhar: Double): Result<String> {
        val cleanName = name.trim().take(100)
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Customer name required"))
        val safeBalance = openingUdhar.coerceIn(-10_000_000.0, 10_000_000.0)
        val customerId = newId()
        val now = System.currentTimeMillis()

        return try {
            localDb.withTransaction {
                dao.upsertCustomer(
                    CustomerEntity(
                        id = customerId,
                        name = cleanName,
                        phone = phone.trim().take(30),
                        address = address.trim().take(200),
                        balance = safeBalance,
                        createdAtMs = now,
                        updatedAtMs = now
                    )
                )
                if (safeBalance > 0.0) {
                    dao.insertLedgerEntry(
                        LedgerEntryEntity(
                            id = newId(),
                            customerId = customerId,
                            customerName = cleanName,
                            entryType = LedgerEntryType.DEBIT.name,
                            amount = safeBalance,
                            balanceAfter = safeBalance,
                            orderId = "",
                            note = "سابقہ بقایا / Opening Udhar",
                            createdAtMs = now
                        )
                    )
                }
            }
            Result.success(customerId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error adding customer", e)
            Result.failure(e)
        }
    }

    suspend fun recordLedgerTransaction(
        customer: Customer,
        entryType: LedgerEntryType,
        amount: Double,
        note: String
    ): Result<LedgerEntry> {
        val safeAmount = amount.coerceIn(1.0, 10_000_000.0)
        val newBalance = when (entryType) {
            LedgerEntryType.DEBIT -> (customer.balance + safeAmount).coerceIn(-10_000_000.0, 10_000_000.0)
            LedgerEntryType.CREDIT -> (customer.balance - safeAmount).coerceIn(-10_000_000.0, 10_000_000.0)
        }
        val cleanNote = note.trim().take(300)
        val entryId = newId()
        val now = System.currentTimeMillis()

        return try {
            require(customer.id.isNotBlank())
            localDb.withTransaction {
                dao.updateCustomerBalance(customer.id, newBalance, now)
                dao.insertLedgerEntry(
                    LedgerEntryEntity(
                        id = entryId,
                        customerId = customer.id,
                        customerName = customer.name.take(100),
                        entryType = entryType.name,
                        amount = safeAmount,
                        balanceAfter = newBalance,
                        orderId = "",
                        note = cleanNote,
                        createdAtMs = now
                    )
                )
                // Auto-sync Khata cash recovery into Daily Cash In/Out
                if (entryType == LedgerEntryType.CREDIT) {
                    dao.upsertDailyCashEntry(
                        DailyCashEntryEntity(
                            id = newId(),
                            entryType = DailyCashType.CASH_IN.name,
                            paymentSource = DailyPaymentSource.CASH.name,
                            categoryOrTitle = "کھاتہ نقد وصولی (${customer.name})".take(120),
                            amount = safeAmount,
                            referenceId = entryId.take(12),
                            note = cleanNote.ifBlank { "Customer Khata Recovery" },
                            entryDateMs = now,
                            createdAtMs = now
                        )
                    )
                }
            }
            Result.success(
                LedgerEntry(
                    id = entryId,
                    customerId = customer.id,
                    customerName = customer.name,
                    entryType = entryType.name,
                    amount = safeAmount,
                    balanceAfter = newBalance,
                    orderId = "",
                    note = cleanNote,
                    createdAt = now.toFirebaseTimestamp()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error recording ledger transaction", e)
            Result.failure(e)
        }
    }

    suspend fun updateLedgerTransaction(
        entry: LedgerEntry,
        newType: LedgerEntryType,
        newAmount: Double,
        newNote: String
    ): Result<Unit> {
        val safeAmount = newAmount.coerceIn(1.0, 10_000_000.0)
        val cleanNote = newNote.trim().take(300)
        val now = System.currentTimeMillis()
        return try {
            require(entry.id.isNotBlank() && entry.customerId.isNotBlank())
            localDb.withTransaction {
                val allForCustomer = dao.getLedgerEntriesByCustomerOnce(entry.customerId)
                var running = 0.0
                val updated = allForCustomer.map { item ->
                    val edited = if (item.id == entry.id) {
                        item.copy(
                            entryType = newType.name,
                            amount = safeAmount,
                            note = cleanNote
                        )
                    } else {
                        item
                    }
                    running = if (edited.entryType == LedgerEntryType.DEBIT.name) {
                        (running + edited.amount).coerceIn(-10_000_000.0, 10_000_000.0)
                    } else {
                        (running - edited.amount).coerceIn(-10_000_000.0, 10_000_000.0)
                    }
                    edited.copy(balanceAfter = running)
                }
                if (updated.isNotEmpty()) {
                    dao.upsertLedgerEntries(updated)
                }
                dao.updateCustomerBalance(entry.customerId, running, now)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating ledger transaction", e)
            Result.failure(e)
        }
    }

    suspend fun deleteLedgerTransaction(entry: LedgerEntry): Result<Unit> {
        val now = System.currentTimeMillis()
        return try {
            require(entry.id.isNotBlank() && entry.customerId.isNotBlank())
            localDb.withTransaction {
                dao.deleteLedgerEntryById(entry.id)
                val remaining = dao.getLedgerEntriesByCustomerOnce(entry.customerId)
                var running = 0.0
                val recalculated = remaining.map { item ->
                    running = if (item.entryType == LedgerEntryType.DEBIT.name) {
                        (running + item.amount).coerceIn(-10_000_000.0, 10_000_000.0)
                    } else {
                        (running - item.amount).coerceIn(-10_000_000.0, 10_000_000.0)
                    }
                    item.copy(balanceAfter = running)
                }
                if (recalculated.isNotEmpty()) {
                    dao.upsertLedgerEntries(recalculated)
                }
                dao.updateCustomerBalance(entry.customerId, running, now)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting ledger transaction", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        return try {
            require(customerId.isNotBlank())
            localDb.withTransaction {
                dao.deleteLedgerEntriesByCustomerId(customerId)
                dao.deleteCustomerById(customerId)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting customer", e)
            Result.failure(e)
        }
    }

    // --- ORDERS & AUTOMATIC BILLING (Auto-linked with Khata & Daily Cash) ---

    suspend fun createOrderOrBill(
        cartItems: List<CartItem>,
        customer: Customer?,
        customerNameInput: String,
        customerPhoneInput: String = "",
        paymentMethod: PaymentMethod,
        discount: Double,
        paidAmountInput: Double,
        saveAsPendingOrder: Boolean,
        note: String
    ): Result<Order> {
        if (cartItems.isEmpty()) return Result.failure(IllegalArgumentException("Cart is empty"))

        val cleanCustomerName = customerNameInput.trim()
            .ifBlank { customer?.name ?: "نقد کسٹمر" }
            .take(100)
        val orderItems = cartItems.take(100).map {
            val exactLineTotal = roundMoney(it.lineTotal).coerceIn(0.0, 10_000_000.0)
            OrderItem(
                productId = it.product.id,
                nameUrdu = it.product.nameUrdu.take(120),
                nameEnglish = it.product.nameEnglish.take(120),
                unit = it.product.unit.take(30),
                quantity = roundWeight(it.quantity.coerceAtLeast(0.001)),
                unitPrice = it.effectivePrice,
                costPrice = it.product.costPrice,
                lineTotal = exactLineTotal
            )
        }
        val subtotal = roundMoney(orderItems.sumOf { it.lineTotal }).coerceIn(0.0, 10_000_000.0)
        val safeDiscount = roundMoney(discount.coerceIn(0.0, subtotal))
        val totalAmount = roundMoney((subtotal - safeDiscount).coerceAtLeast(0.0))
        val totalCost = roundMoney(cartItems.sumOf { it.lineCost }).coerceIn(0.0, 10_000_000.0)
        val profitAmount = roundMoney(totalAmount - totalCost).coerceIn(-10_000_000.0, 10_000_000.0)

        val paidAmount = when {
            saveAsPendingOrder -> 0.0
            paymentMethod == PaymentMethod.CASH -> totalAmount
            paymentMethod == PaymentMethod.UDHAR -> 0.0
            else -> roundMoney(paidAmountInput.coerceIn(0.0, totalAmount))
        }
        val remainingUdhar = if (saveAsPendingOrder) 0.0 else roundMoney((totalAmount - paidAmount).coerceAtLeast(0.0))
        val status = if (saveAsPendingOrder) OrderStatus.PENDING else OrderStatus.COMPLETED

        val billNo = "AB-" + SimpleDateFormat("MMdd-HHmmss", Locale.US).format(Date())

        val orderId = newId()
        val now = System.currentTimeMillis()
        val cleanNote = note.trim().take(300)
        val cleanPhoneInput = customerPhoneInput.trim().take(30)
        val itemizedSummary = orderItems.take(5).joinToString("، ") {
            "${it.nameUrdu} (${formatQtyCompact(it.quantity)} ${it.unit})"
        }
        val creditLedgerNote = buildString {
            append("ادھار بل #$billNo")
            if (itemizedSummary.isNotBlank()) append(" — $itemizedSummary")
            if (paidAmount > 0.0) append(" | نقد وصول: ${paidAmount.toLong()}")
        }.take(300)

        return try {
            var resolvedCustomerId = customer?.id ?: ""
            var resolvedCustomerName = cleanCustomerName

            localDb.withTransaction {
                // Auto-link with Khata when credit/udhar balance > 0: reuse existing or auto-create new customer profile
                var linkedCustomer: CustomerEntity? = if (resolvedCustomerId.isNotBlank()) {
                    dao.getCustomerById(resolvedCustomerId)
                } else {
                    null
                }

                if (status == OrderStatus.COMPLETED && remainingUdhar > 0.0 && linkedCustomer == null) {
                    val allCustomers = dao.getAllCustomersOnce()
                    val normalizedInputPhone = cleanPhoneInput.filter { it.isDigit() }
                    val matched = allCustomers.firstOrNull { c ->
                        val existingDigits = c.phone.filter { it.isDigit() }
                        c.name.equals(cleanCustomerName, ignoreCase = true) ||
                            (normalizedInputPhone.length >= 7 && existingDigits == normalizedInputPhone)
                    }
                    linkedCustomer = if (matched != null) {
                        if (matched.phone.isBlank() && cleanPhoneInput.isNotBlank()) {
                            val updatedCust = matched.copy(phone = cleanPhoneInput, updatedAtMs = now)
                            dao.upsertCustomer(updatedCust)
                            updatedCust
                        } else {
                            matched
                        }
                    } else {
                        val createdCustId = newId()
                        val newCust = CustomerEntity(
                            id = createdCustId,
                            name = cleanCustomerName,
                            phone = cleanPhoneInput,
                            address = "",
                            balance = 0.0,
                            createdAtMs = now,
                            updatedAtMs = now
                        )
                        dao.upsertCustomer(newCust)
                        newCust
                    }
                }

                if (linkedCustomer != null) {
                    resolvedCustomerId = linkedCustomer.id
                    resolvedCustomerName = linkedCustomer.name
                }

                dao.upsertOrder(
                    OrderEntity(
                        id = orderId,
                        billNumber = billNo,
                        customerId = resolvedCustomerId,
                        customerName = resolvedCustomerName,
                        status = status.name,
                        paymentMethod = paymentMethod.name,
                        subtotal = subtotal,
                        discount = safeDiscount,
                        totalAmount = totalAmount,
                        paidAmount = paidAmount,
                        remainingUdhar = remainingUdhar,
                        totalCost = totalCost,
                        profitAmount = profitAmount,
                        itemsJson = OrderItemJsonSerializer.encode(orderItems),
                        note = cleanNote,
                        createdAtMs = now,
                        updatedAtMs = now
                    )
                )

                if (status == OrderStatus.COMPLETED) {
                    for (ci in cartItems.take(100)) {
                        if (ci.product.id.isNotBlank()) {
                            val currentDbStock = dao.getProductById(ci.product.id)?.stockQty ?: ci.product.stockQty
                            val deductedQty = roundWeight(ci.quantity.coerceAtLeast(0.001))
                            val newStock = roundWeight((currentDbStock - deductedQty).coerceAtLeast(0.0))
                            dao.updateProductStock(ci.product.id, newStock, now)
                            dao.insertStockMovement(
                                StockMovementEntity(
                                    id = newId(),
                                    productId = ci.product.id,
                                    productNameUrdu = ci.product.nameUrdu.take(120),
                                    productNameEnglish = ci.product.nameEnglish.take(120),
                                    movementType = StockMovementType.SALE.name,
                                    quantity = deductedQty,
                                    unitCost = ci.product.costPrice,
                                    unitPrice = ci.effectivePrice,
                                    profitDelta = roundMoney((ci.effectivePrice - ci.product.costPrice) * ci.quantity),
                                    note = "بل #$billNo - $resolvedCustomerName".take(300),
                                    createdAtMs = now
                                )
                            )
                        }
                    }

                    // Automatically post unpaid/credit portion into Khata ledger
                    if (remainingUdhar > 0.0 && linkedCustomer != null) {
                        val updatedBalance = roundMoney(linkedCustomer.balance + remainingUdhar).coerceIn(-10_000_000.0, 10_000_000.0)
                        dao.updateCustomerBalance(linkedCustomer.id, updatedBalance, now)
                        dao.insertLedgerEntry(
                            LedgerEntryEntity(
                                id = newId(),
                                customerId = linkedCustomer.id,
                                customerName = linkedCustomer.name.take(100),
                                entryType = LedgerEntryType.DEBIT.name,
                                amount = remainingUdhar,
                                balanceAfter = updatedBalance,
                                orderId = orderId,
                                note = creditLedgerNote,
                                createdAtMs = now
                            )
                        )
                    }

                    // Automatically record upfront cash received in Daily Cash In/Out ledger
                    if (paidAmount > 0.0) {
                        dao.upsertDailyCashEntry(
                            DailyCashEntryEntity(
                                id = newId(),
                                entryType = DailyCashType.CASH_IN.name,
                                paymentSource = DailyPaymentSource.CASH.name,
                                categoryOrTitle = "بل نقد وصولی #$billNo ($resolvedCustomerName)".take(120),
                                amount = paidAmount,
                                referenceId = billNo,
                                note = itemizedSummary.take(300),
                                entryDateMs = now,
                                createdAtMs = now
                            )
                        )
                    }
                }
            }

            Result.success(
                Order(
                    id = orderId,
                    billNumber = billNo,
                    customerId = resolvedCustomerId,
                    customerName = resolvedCustomerName,
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
                    note = cleanNote,
                    createdAt = now.toFirebaseTimestamp(),
                    updatedAt = now.toFirebaseTimestamp()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error creating order/bill", e)
            Result.failure(e)
        }
    }

    suspend fun completePendingOrder(
        order: Order,
        products: List<Product>,
        customer: Customer?,
        paymentMethod: PaymentMethod
    ): Result<Order> {
        val paid = if (paymentMethod == PaymentMethod.CASH) order.totalAmount else 0.0
        val udhar = if (paymentMethod == PaymentMethod.UDHAR) order.totalAmount else 0.0
        val now = System.currentTimeMillis()

        return try {
            require(order.id.isNotBlank() && order.statusEnum() == OrderStatus.PENDING)
            localDb.withTransaction {
                dao.updateOrderCompletion(
                    id = order.id,
                    status = OrderStatus.COMPLETED.name,
                    paymentMethod = paymentMethod.name,
                    paidAmount = paid,
                    remainingUdhar = udhar,
                    updatedAtMs = now
                )
                val prodMap = products.associateBy { it.id }
                for (item in order.items) {
                    val prod = prodMap[item.productId]
                    if (prod != null) {
                        val currentDbStock = dao.getProductById(prod.id)?.stockQty ?: prod.stockQty
                        val deductedQty = roundWeight(item.quantity.coerceAtLeast(0.001))
                        val newStock = roundWeight((currentDbStock - deductedQty).coerceAtLeast(0.0))
                        dao.updateProductStock(prod.id, newStock, now)
                        dao.insertStockMovement(
                            StockMovementEntity(
                                id = newId(),
                                productId = prod.id,
                                productNameUrdu = prod.nameUrdu.take(120),
                                productNameEnglish = prod.nameEnglish.take(120),
                                movementType = StockMovementType.SALE.name,
                                quantity = deductedQty,
                                unitCost = item.costPrice,
                                unitPrice = item.unitPrice,
                                profitDelta = roundMoney((item.unitPrice - item.costPrice) * item.quantity),
                                note = "آرڈر مکمل #${order.billNumber}".take(300),
                                createdAtMs = now
                            )
                        )
                    }
                }
                if (udhar > 0.0 && customer != null && customer.id.isNotBlank()) {
                    val updatedBalance = (customer.balance + udhar).coerceIn(-10_000_000.0, 10_000_000.0)
                    dao.updateCustomerBalance(customer.id, updatedBalance, now)
                    dao.insertLedgerEntry(
                        LedgerEntryEntity(
                            id = newId(),
                            customerId = customer.id,
                            customerName = customer.name.take(100),
                            entryType = LedgerEntryType.DEBIT.name,
                            amount = udhar,
                            balanceAfter = updatedBalance,
                            orderId = order.id,
                            note = "آرڈر #${order.billNumber} ادھار".take(300),
                            createdAtMs = now
                        )
                    )
                }
            }
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
            Log.e(TAG, "Error completing pending order", e)
            Result.failure(e)
        }
    }

    suspend fun cancelPendingOrder(orderId: String): Result<Unit> {
        return try {
            dao.updateOrderStatus(orderId, OrderStatus.CANCELLED.name, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling order", e)
            Result.failure(e)
        }
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        return try {
            require(orderId.isNotBlank())
            dao.deleteOrderById(orderId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting order", e)
            Result.failure(e)
        }
    }

    /**
     * Full manual bill editing & amount adjustment:
     * Updates items, quantities/weights, rates, discount/extra charges, manual Final Bill Total override,
     * payment method, and paid amount. Automatically reconciles stock deltas for completed bills and
     * recalculates the linked Customer Khata ledger & running balances in a single atomic transaction.
     */
    suspend fun updateExistingOrder(
        existingOrder: Order,
        updatedItems: List<OrderItem>,
        discount: Double,
        extraCharges: Double,
        finalTotalOverride: Double?,
        paymentMethod: PaymentMethod,
        paidAmountInput: Double,
        note: String
    ): Result<Order> {
        val now = System.currentTimeMillis()
        val sanitizedItems = updatedItems
            .filter { it.quantity > 0.0 && it.unitPrice >= 0.0 }
            .map { item ->
                val cleanQty = roundWeight(item.quantity.coerceAtLeast(0.001))
                val cleanPrice = roundMoney(item.unitPrice.coerceAtLeast(0.0))
                val cleanCost = roundMoney(item.costPrice.coerceAtLeast(0.0))
                item.copy(
                    nameUrdu = item.nameUrdu.trim().take(120),
                    nameEnglish = item.nameEnglish.trim().ifBlank { item.nameUrdu.trim() }.take(120),
                    unit = item.unit.trim().ifBlank { "pcs" }.take(24),
                    quantity = cleanQty,
                    unitPrice = cleanPrice,
                    costPrice = cleanCost,
                    lineTotal = roundMoney(cleanQty * cleanPrice)
                )
            }
        val subtotal = roundMoney(sanitizedItems.sumOf { it.lineTotal })
        val safeDiscount = roundMoney(discount.coerceAtLeast(0.0))
        val safeExtra = roundMoney(extraCharges.coerceAtLeast(0.0))
        val computedTotal = roundMoney((subtotal - safeDiscount + safeExtra).coerceAtLeast(0.0))
        val totalAmount = roundMoney(
            (finalTotalOverride?.coerceAtLeast(0.0) ?: computedTotal).coerceIn(0.0, 50_000_000.0)
        )
        val totalCost = roundMoney(sanitizedItems.sumOf { it.costPrice * it.quantity })
        val profitAmount = roundMoney(totalAmount - totalCost)

        val status = existingOrder.statusEnum()
        val paidAmount = when {
            status != OrderStatus.COMPLETED -> 0.0
            paymentMethod == PaymentMethod.CASH -> totalAmount
            paymentMethod == PaymentMethod.UDHAR -> 0.0
            else -> roundMoney(paidAmountInput.coerceIn(0.0, totalAmount))
        }
        val remainingUdhar = when {
            status != OrderStatus.COMPLETED -> 0.0
            paymentMethod == PaymentMethod.CASH -> 0.0
            paymentMethod == PaymentMethod.UDHAR -> totalAmount
            else -> roundMoney((totalAmount - paidAmount).coerceAtLeast(0.0))
        }
        val cleanNote = note.trim().take(400)

        return try {
            require(existingOrder.id.isNotBlank()) { "Invalid bill ID" }
            require(sanitizedItems.isNotEmpty() || totalAmount > 0.0) { "Bill must contain items or a valid total" }

            var preservedCreatedAtMs = existingOrder.createdAt?.toDate()?.time ?: now

            localDb.withTransaction {
                val dbOrder = dao.getOrderById(existingOrder.id)
                if (dbOrder != null) {
                    preservedCreatedAtMs = dbOrder.createdAtMs
                }
                val oldItems = if (dbOrder != null) {
                    OrderItemJsonSerializer.decode(dbOrder.itemsJson)
                } else {
                    existingOrder.items
                }

                // 1. Reconcile product stock deltas for COMPLETED bills
                if (status == OrderStatus.COMPLETED) {
                    val oldQtyByProduct = oldItems
                        .filter { it.productId.isNotBlank() }
                        .groupBy { it.productId }
                        .mapValues { (_, list) -> list.sumOf { it.quantity } }
                    val newQtyByProduct = sanitizedItems
                        .filter { it.productId.isNotBlank() }
                        .groupBy { it.productId }
                        .mapValues { (_, list) -> list.sumOf { it.quantity } }

                    val allProductIds = (oldQtyByProduct.keys + newQtyByProduct.keys).distinct()
                    for (prodId in allProductIds) {
                        val oldQty = oldQtyByProduct[prodId] ?: 0.0
                        val newQty = newQtyByProduct[prodId] ?: 0.0
                        val diff = roundWeight(newQty - oldQty)
                        if (kotlin.math.abs(diff) >= 0.0005) {
                            val prod = dao.getProductById(prodId) ?: continue
                            val updatedStock = roundWeight((prod.stockQty - diff).coerceAtLeast(0.0))
                            dao.updateProductStock(prodId, updatedStock, now)
                            dao.insertStockMovement(
                                StockMovementEntity(
                                    id = newId(),
                                    productId = prodId,
                                    productNameUrdu = prod.nameUrdu.take(120),
                                    productNameEnglish = prod.nameEnglish.take(120),
                                    movementType = StockMovementType.ADJUSTMENT.name,
                                    quantity = kotlin.math.abs(diff),
                                    unitCost = prod.costPrice,
                                    unitPrice = prod.sellingPrice,
                                    profitDelta = 0.0,
                                    note = "بل ایڈٹ #${existingOrder.billNumber} (${if (diff > 0) "اضافہ" else "واپسی"})".take(300),
                                    createdAtMs = now
                                )
                            )
                        }
                    }
                }

                // 2. Save updated Order record
                dao.upsertOrder(
                    OrderEntity(
                        id = existingOrder.id,
                        billNumber = existingOrder.billNumber,
                        customerId = existingOrder.customerId,
                        customerName = existingOrder.customerName,
                        status = status.name,
                        paymentMethod = paymentMethod.name,
                        subtotal = subtotal,
                        discount = safeDiscount,
                        totalAmount = totalAmount,
                        paidAmount = paidAmount,
                        remainingUdhar = remainingUdhar,
                        totalCost = totalCost,
                        profitAmount = profitAmount,
                        itemsJson = OrderItemJsonSerializer.encode(sanitizedItems),
                        note = cleanNote,
                        createdAtMs = preservedCreatedAtMs,
                        updatedAtMs = now
                    )
                )

                // 3. Synchronize linked Customer Khata ledger & recalculate chronological running balances
                if (status == OrderStatus.COMPLETED && existingOrder.customerId.isNotBlank()) {
                    val customer = dao.getCustomerById(existingOrder.customerId)
                    if (customer != null) {
                        val entries = dao.getLedgerEntriesByCustomerOnce(customer.id).toMutableList()
                        val linkedIdx = entries.indexOfFirst { it.orderId == existingOrder.id }
                        val itemSummary = sanitizedItems.take(3).joinToString("، ") {
                            "${it.nameUrdu} (${formatQtyBrief(it.quantity)})"
                        }
                        val updatedLedgerNote = buildString {
                            append("بل #${existingOrder.billNumber} (ترمیم شدہ)")
                            if (itemSummary.isNotBlank()) append(" — $itemSummary")
                            if (cleanNote.isNotBlank()) append(" | $cleanNote")
                        }.take(300)

                        if (remainingUdhar > 0.0) {
                            if (linkedIdx >= 0) {
                                entries[linkedIdx] = entries[linkedIdx].copy(
                                    entryType = LedgerEntryType.DEBIT.name,
                                    amount = remainingUdhar,
                                    note = updatedLedgerNote
                                )
                            } else {
                                entries.add(
                                    LedgerEntryEntity(
                                        id = newId(),
                                        customerId = customer.id,
                                        customerName = customer.name.take(100),
                                        entryType = LedgerEntryType.DEBIT.name,
                                        amount = remainingUdhar,
                                        balanceAfter = 0.0,
                                        orderId = existingOrder.id,
                                        note = updatedLedgerNote,
                                        createdAtMs = preservedCreatedAtMs
                                    )
                                )
                            }
                        } else if (linkedIdx >= 0) {
                            val removed = entries.removeAt(linkedIdx)
                            dao.deleteLedgerEntryById(removed.id)
                        }

                        // Recalculate running balanceAfter chronologically
                        var runningBalance = 0.0
                        val sorted = entries.sortedBy { it.createdAtMs }
                        val recomputed = sorted.map { item ->
                            val delta = if (item.entryType == LedgerEntryType.DEBIT.name) item.amount else -item.amount
                            runningBalance = roundMoney((runningBalance + delta).coerceIn(-10_000_000.0, 10_000_000.0))
                            item.copy(balanceAfter = runningBalance)
                        }
                        if (recomputed.isNotEmpty()) {
                            dao.upsertLedgerEntries(recomputed)
                        }
                        dao.updateCustomerBalance(customer.id, runningBalance, now)
                    }
                }
            }

            Result.success(
                existingOrder.copy(
                    status = status.name,
                    paymentMethod = paymentMethod.name,
                    subtotal = subtotal,
                    discount = safeDiscount,
                    totalAmount = totalAmount,
                    paidAmount = paidAmount,
                    remainingUdhar = remainingUdhar,
                    totalCost = totalCost,
                    profitAmount = profitAmount,
                    items = sanitizedItems,
                    note = cleanNote,
                    createdAt = preservedCreatedAtMs.toFirebaseTimestamp(),
                    updatedAt = now.toFirebaseTimestamp()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating existing order", e)
            Result.failure(e)
        }
    }

    // --- RATION CHARITY FUND (HBL Direct Transfer with Pending Owner Verification) ---

    suspend fun recordRationDonation(
        entryType: DonationEntryType,
        personName: String,
        phone: String,
        amount: Double,
        rationSummary: String,
        paymentRef: String,
        senderAccount: String = "",
        transactionId: String = "",
        receiptImagePath: String = ""
    ): Result<RationDonation> {
        val cleanName = personName.trim().take(100)
        val cleanPhone = phone.trim().take(30)
        val cleanSender = senderAccount.trim().take(100)
        val cleanTid = transactionId.trim().take(100)
        val cleanReceiptPath = receiptImagePath.trim().take(400)
        val safeAmount = amount.coerceIn(0.0, 10_000_000.0)

        if (safeAmount <= 0.0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }

        if (entryType == DonationEntryType.DONATION_IN) {
            if (cleanName.isBlank()) {
                return Result.failure(IllegalArgumentException("Donor full name is required"))
            }
            if (cleanPhone.isBlank()) {
                return Result.failure(IllegalArgumentException("Phone number is required"))
            }
            if (cleanSender.isBlank()) {
                return Result.failure(IllegalArgumentException("Sender account number / title is required"))
            }
            if (cleanTid.isBlank()) {
                return Result.failure(IllegalArgumentException("HBL Transaction ID (TID) is required"))
            }
        }

        val finalName = cleanName.ifBlank { "مستحق خاندان" }
        val cleanSummary = rationSummary.trim().take(300)
        val cleanRef = paymentRef.trim().ifBlank {
            if (entryType == DonationEntryType.DONATION_IN) "HBL TID: $cleanTid" else "Shop Counter"
        }.take(100)
        val initialStatus = if (entryType == DonationEntryType.DONATION_IN) {
            DonationStatus.PENDING
        } else {
            DonationStatus.VERIFIED
        }

        val id = newId()
        val now = System.currentTimeMillis()

        return try {
            dao.insertDonation(
                RationDonationEntity(
                    id = id,
                    entryType = entryType.name,
                    personName = finalName,
                    phone = cleanPhone,
                    amount = safeAmount,
                    rationSummary = cleanSummary,
                    paymentRef = cleanRef,
                    senderAccount = cleanSender,
                    transactionId = cleanTid,
                    receiptImagePath = cleanReceiptPath,
                    status = initialStatus.name,
                    createdAtMs = now
                )
            )
            Result.success(
                RationDonation(
                    id = id,
                    entryType = entryType.name,
                    personName = finalName,
                    phone = cleanPhone,
                    amount = safeAmount,
                    rationSummary = cleanSummary,
                    paymentRef = cleanRef,
                    senderAccount = cleanSender,
                    transactionId = cleanTid,
                    receiptImagePath = cleanReceiptPath,
                    status = initialStatus.name,
                    createdAt = now.toFirebaseTimestamp()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error recording ration donation", e)
            Result.failure(e)
        }
    }

    suspend fun updateDonationStatus(donationId: String, status: DonationStatus): Result<Unit> {
        return try {
            require(donationId.isNotBlank())
            dao.updateDonationStatus(donationId, status.name)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating donation status", e)
            Result.failure(e)
        }
    }

    // --- MARYAM NAWAZ RATION CARD ---

    suspend fun saveRationCardItem(
        existingId: String?,
        nameUrdu: String,
        nameEnglish: String,
        unit: String,
        price: Double,
        sortOrder: Int
    ): Result<Unit> {
        val cleanUrdu = nameUrdu.trim().take(120)
        if (cleanUrdu.isBlank()) return Result.failure(IllegalArgumentException("Item name required"))
        val cleanEng = nameEnglish.trim().ifBlank { cleanUrdu }.take(120)
        val cleanUnit = unit.trim().ifBlank { "kg" }.take(30)
        val safePrice = price.coerceIn(0.0, 1_000_000.0)
        val safeOrder = sortOrder.coerceIn(0, 10_000)
        val id = existingId?.takeIf { it.isNotBlank() } ?: newId()
        val now = System.currentTimeMillis()

        return try {
            dao.upsertRationCardItem(
                RationCardItemEntity(
                    id = id,
                    nameUrdu = cleanUrdu,
                    nameEnglish = cleanEng,
                    unit = cleanUnit,
                    price = safePrice,
                    sortOrder = safeOrder,
                    createdAtMs = now,
                    updatedAtMs = now
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving ration card item", e)
            Result.failure(e)
        }
    }

    suspend fun deleteRationCardItem(itemId: String): Result<Unit> {
        return try {
            require(itemId.isNotBlank())
            dao.deleteRationCardItemById(itemId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting ration card item", e)
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
        feeDeduction: Double = MaryamRationCatalogData.DEFAULT_SERVICE_FEE_PKR,
        alreadyUsedThisMonth: Double,
        scannedViaNfc: Boolean,
        note: String
    ): Result<RationCardBill> {
        if (cartItems.isEmpty()) return Result.failure(IllegalArgumentException("Select at least one POS item"))

        val cleanName = beneficiaryName.trim().ifBlank { "POS کارڈ صارف" }.take(100)
        val cleanUid = cardUid.trim().take(100)
        val cleanCardNo = cardNumber.trim().take(60)
        val cleanCnic = cnic.trim().take(30)
        val cleanPhone = phone.trim().take(30)
        val cleanNote = note.trim().take(300)
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val billNo = "POS-" + SimpleDateFormat("MMdd-HHmmss", Locale.US).format(Date())

        val safeLimit = subsidyLimit.coerceIn(0.0, 1_000_000.0)
        val safeFee = feeDeduction.coerceIn(0.0, safeLimit)
        val netLimit = (safeLimit - safeFee).coerceAtLeast(0.0)
        val availableBeforeBill = (netLimit - alreadyUsedThisMonth).coerceAtLeast(0.0)
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

        val id = newId()
        val now = System.currentTimeMillis()
        return try {
            localDb.withTransaction {
                dao.insertRationCardBill(
                    RationCardBillEntity(
                        id = id,
                        billNumber = billNo,
                        cardUid = cleanUid,
                        cardNumber = cleanCardNo,
                        beneficiaryName = cleanName,
                        cnic = cleanCnic,
                        phone = cleanPhone,
                        monthKey = monthKey,
                        subsidyLimit = safeLimit,
                        feeDeduction = safeFee,
                        netSubsidyLimit = netLimit,
                        totalAmount = totalAmount,
                        subsidyUsed = subsidyUsed,
                        cashTopUp = cashTopUp,
                        remainingSubsidy = remainingSubsidy,
                        itemsJson = OrderItemJsonSerializer.encode(orderItems),
                        scannedViaNfc = scannedViaNfc,
                        note = cleanNote,
                        createdAtMs = now
                    )
                )
                // Auto-deduct matching items from shop inventory stock
                val allProducts = dao.getAllProductsOnce()
                for (ci in cartItems.take(100)) {
                    val matchedProd = allProducts.firstOrNull { p ->
                        p.nameUrdu.equals(ci.item.nameUrdu, ignoreCase = true) ||
                            p.nameEnglish.equals(ci.item.nameEnglish, ignoreCase = true) ||
                            p.nameUrdu.contains(ci.item.nameUrdu.substringBefore("(").trim(), ignoreCase = true)
                    }
                    if (matchedProd != null) {
                        val updatedQty = (matchedProd.stockQty - ci.quantity).coerceAtLeast(0.0)
                        dao.updateProductStock(matchedProd.id, updatedQty, now)
                    }
                }
                // Auto-sync POS card transaction into Daily Cash In/Out
                dao.upsertDailyCashEntry(
                    DailyCashEntryEntity(
                        id = newId(),
                        entryType = DailyCashType.CASH_IN.name,
                        paymentSource = if (cashTopUp > 0.0 && subsidyUsed <= 0.0) DailyPaymentSource.CASH.name else DailyPaymentSource.ONLINE.name,
                        categoryOrTitle = "POS کارڈ بل #$billNo ($cleanName)".take(120),
                        amount = totalAmount,
                        referenceId = cleanCardNo.ifBlank { billNo }.take(100),
                        note = "کارڈ: ${subsidyUsed.toLong()} | فیس: ${safeFee.toLong()} | اضافی نقد: ${cashTopUp.toLong()}".take(300),
                        entryDateMs = now,
                        createdAtMs = now
                    )
                )
            }
            Result.success(
                RationCardBill(
                    id = id,
                    billNumber = billNo,
                    cardUid = cleanUid,
                    cardNumber = cleanCardNo,
                    beneficiaryName = cleanName,
                    cnic = cleanCnic,
                    phone = cleanPhone,
                    monthKey = monthKey,
                    subsidyLimit = safeLimit,
                    feeDeduction = safeFee,
                    netSubsidyLimit = netLimit,
                    totalAmount = totalAmount,
                    subsidyUsed = subsidyUsed,
                    cashTopUp = cashTopUp,
                    remainingSubsidy = remainingSubsidy,
                    items = orderItems,
                    scannedViaNfc = scannedViaNfc,
                    note = cleanNote,
                    createdAt = now.toFirebaseTimestamp()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error creating POS card bill", e)
            Result.failure(e)
        }
    }

    suspend fun deleteRationCardBill(billId: String): Result<Unit> {
        return try {
            require(billId.isNotBlank())
            dao.deleteRationCardBillById(billId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting POS card bill", e)
            Result.failure(e)
        }
    }

    // --- DAILY CASH IN / OUT LEDGER ---

    suspend fun saveDailyCashEntry(
        existingId: String?,
        entryType: DailyCashType,
        paymentSource: DailyPaymentSource,
        categoryOrTitle: String,
        amount: Double,
        referenceId: String,
        note: String,
        entryDateMs: Long
    ): Result<DailyCashEntry> {
        val safeAmount = amount.coerceIn(0.0, 100_000_000.0)
        if (safeAmount <= 0.0) return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        val cleanTitle = categoryOrTitle.trim().ifBlank {
            if (entryType == DailyCashType.CASH_IN) "آج کی مکمل سیل / Daily Sale" else "خریداری / اخراجات"
        }.take(120)
        val cleanRef = referenceId.trim().take(100)
        val cleanNote = note.trim().take(300)
        val id = existingId?.takeIf { it.isNotBlank() } ?: newId()
        val now = System.currentTimeMillis()
        val finalDateMs = if (entryDateMs > 0L) entryDateMs else now

        return try {
            val entity = DailyCashEntryEntity(
                id = id,
                entryType = entryType.name,
                paymentSource = paymentSource.name,
                categoryOrTitle = cleanTitle,
                amount = safeAmount,
                referenceId = cleanRef,
                note = cleanNote,
                entryDateMs = finalDateMs,
                createdAtMs = now
            )
            dao.upsertDailyCashEntry(entity)
            Result.success(entity.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving daily cash entry", e)
            Result.failure(e)
        }
    }

    suspend fun deleteDailyCashEntry(entryId: String): Result<Unit> {
        return try {
            require(entryId.isNotBlank())
            dao.deleteDailyCashEntryById(entryId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting daily cash entry", e)
            Result.failure(e)
        }
    }

    // --- WHOLESALERS / SUPPLIERS IN / OUT LEDGER ---

    suspend fun saveWholesaler(
        existingId: String?,
        name: String,
        phone: String,
        cityOrMarket: String,
        notes: String
    ): Result<String> {
        val cleanName = name.trim().take(120)
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Wholesaler name is required"))
        val id = existingId?.takeIf { it.isNotBlank() } ?: newId()
        val now = System.currentTimeMillis()

        return try {
            val existing = if (!existingId.isNullOrBlank()) dao.getWholesalerById(id) else null
            dao.upsertWholesaler(
                WholesalerEntity(
                    id = id,
                    name = cleanName,
                    phone = phone.trim().take(30),
                    cityOrMarket = cityOrMarket.trim().take(120),
                    notes = notes.trim().take(300),
                    totalPurchased = existing?.totalPurchased ?: 0.0,
                    totalPaid = existing?.totalPaid ?: 0.0,
                    payableBalance = existing?.payableBalance ?: 0.0,
                    createdAtMs = existing?.createdAtMs ?: now,
                    updatedAtMs = now
                )
            )
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving wholesaler", e)
            Result.failure(e)
        }
    }

    suspend fun deleteWholesaler(wholesalerId: String): Result<Unit> {
        return try {
            require(wholesalerId.isNotBlank())
            localDb.withTransaction {
                dao.deleteWholesalerEntriesByWholesalerId(wholesalerId)
                dao.deleteWholesalerById(wholesalerId)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting wholesaler", e)
            Result.failure(e)
        }
    }

    suspend fun recordWholesalerEntry(
        wholesaler: Wholesaler,
        entryType: WholesalerEntryType,
        amount: Double,
        invoiceNumber: String,
        description: String,
        paymentSource: DailyPaymentSource,
        referenceId: String,
        entryDateMs: Long
    ): Result<WholesalerLedgerEntry> {
        val safeAmount = amount.coerceIn(0.0, 100_000_000.0)
        if (safeAmount <= 0.0) return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        if (wholesaler.id.isBlank()) return Result.failure(IllegalArgumentException("Invalid wholesaler"))

        val entryId = newId()
        val now = System.currentTimeMillis()
        val finalDateMs = if (entryDateMs > 0L) entryDateMs else now

        return try {
            var savedEntry: WholesalerLedgerEntry? = null
            localDb.withTransaction {
                val existingList = dao.getWholesalerEntriesByWholesalerOnce(wholesaler.id)
                val newEntity = WholesalerLedgerEntryEntity(
                    id = entryId,
                    wholesalerId = wholesaler.id,
                    wholesalerName = wholesaler.name.take(120),
                    entryType = entryType.name,
                    invoiceNumber = invoiceNumber.trim().take(60),
                    description = description.trim().take(300),
                    paymentSource = paymentSource.name,
                    referenceId = referenceId.trim().take(100),
                    amount = safeAmount,
                    balanceAfter = 0.0,
                    entryDateMs = finalDateMs,
                    createdAtMs = now
                )
                val combined = (existingList + newEntity).sortedWith(
                    compareBy<WholesalerLedgerEntryEntity> { it.entryDateMs }.thenBy { it.createdAtMs }
                )
                var runningPurchased = 0.0
                var runningPaid = 0.0
                var runningBalance = 0.0
                val recalculated = combined.map { item ->
                    if (item.entryType == WholesalerEntryType.STOCK_IN.name) {
                        runningPurchased += item.amount
                        runningBalance += item.amount
                    } else {
                        runningPaid += item.amount
                        runningBalance -= item.amount
                    }
                    item.copy(balanceAfter = runningBalance)
                }
                dao.upsertWholesalerEntries(recalculated)
                dao.updateWholesalerTotals(
                    id = wholesaler.id,
                    totalPurchased = runningPurchased,
                    totalPaid = runningPaid,
                    payableBalance = runningBalance,
                    updatedAtMs = now
                )
                // Auto-sync Wholesaler payment outflow into Daily Cash In/Out
                if (entryType == WholesalerEntryType.PAYMENT_OUT) {
                    dao.upsertDailyCashEntry(
                        DailyCashEntryEntity(
                            id = newId(),
                            entryType = DailyCashType.CASH_OUT.name,
                            paymentSource = paymentSource.name,
                            categoryOrTitle = "ہول سیلر ادائیگی (${wholesaler.name})".take(120),
                            amount = safeAmount,
                            referenceId = referenceId.trim().take(100),
                            note = description.trim().ifBlank { "Supplier Payment Out" }.take(300),
                            entryDateMs = finalDateMs,
                            createdAtMs = now
                        )
                    )
                }
                savedEntry = recalculated.first { it.id == entryId }.toDomain()
            }
            Result.success(savedEntry!!)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error recording wholesaler entry", e)
            Result.failure(e)
        }
    }

    suspend fun updateWholesalerEntry(
        entry: WholesalerLedgerEntry,
        newType: WholesalerEntryType,
        newAmount: Double,
        newInvoiceNumber: String,
        newDescription: String,
        newPaymentSource: DailyPaymentSource,
        newReferenceId: String
    ): Result<Unit> {
        val safeAmount = newAmount.coerceIn(0.0, 100_000_000.0)
        if (safeAmount <= 0.0) return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        val now = System.currentTimeMillis()
        return try {
            require(entry.id.isNotBlank() && entry.wholesalerId.isNotBlank())
            localDb.withTransaction {
                val all = dao.getWholesalerEntriesByWholesalerOnce(entry.wholesalerId)
                var runningPurchased = 0.0
                var runningPaid = 0.0
                var runningBalance = 0.0
                val recalculated = all.map { item ->
                    val edited = if (item.id == entry.id) {
                        item.copy(
                            entryType = newType.name,
                            amount = safeAmount,
                            invoiceNumber = newInvoiceNumber.trim().take(60),
                            description = newDescription.trim().take(300),
                            paymentSource = newPaymentSource.name,
                            referenceId = newReferenceId.trim().take(100)
                        )
                    } else {
                        item
                    }
                    if (edited.entryType == WholesalerEntryType.STOCK_IN.name) {
                        runningPurchased += edited.amount
                        runningBalance += edited.amount
                    } else {
                        runningPaid += edited.amount
                        runningBalance -= edited.amount
                    }
                    edited.copy(balanceAfter = runningBalance)
                }
                if (recalculated.isNotEmpty()) {
                    dao.upsertWholesalerEntries(recalculated)
                }
                dao.updateWholesalerTotals(
                    id = entry.wholesalerId,
                    totalPurchased = runningPurchased,
                    totalPaid = runningPaid,
                    payableBalance = runningBalance,
                    updatedAtMs = now
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating wholesaler entry", e)
            Result.failure(e)
        }
    }

    suspend fun deleteWholesalerEntry(entry: WholesalerLedgerEntry): Result<Unit> {
        return try {
            require(entry.id.isNotBlank() && entry.wholesalerId.isNotBlank())
            val now = System.currentTimeMillis()
            localDb.withTransaction {
                dao.deleteWholesalerEntryById(entry.id)
                val remaining = dao.getWholesalerEntriesByWholesalerOnce(entry.wholesalerId)
                var runningPurchased = 0.0
                var runningPaid = 0.0
                var runningBalance = 0.0
                val recalculated = remaining.map { item ->
                    if (item.entryType == WholesalerEntryType.STOCK_IN.name) {
                        runningPurchased += item.amount
                        runningBalance += item.amount
                    } else {
                        runningPaid += item.amount
                        runningBalance -= item.amount
                    }
                    item.copy(balanceAfter = runningBalance)
                }
                if (recalculated.isNotEmpty()) {
                    dao.upsertWholesalerEntries(recalculated)
                }
                dao.updateWholesalerTotals(
                    id = entry.wholesalerId,
                    totalPurchased = runningPurchased,
                    totalPaid = runningPaid,
                    payableBalance = runningBalance,
                    updatedAtMs = now
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting wholesaler entry", e)
            Result.failure(e)
        }
    }

    // --- OPTIONAL GOOGLE CLOUD BACKUP & RESTORE ---

    fun isCloudConnected(): Boolean = cloudBackupService.isGoogleSignedIn()

    suspend fun backupLocalToCloud(): Result<Int> = cloudBackupService.backupLocalToCloud(localDb)

    suspend fun restoreCloudToLocal(): Result<Int> = cloudBackupService.restoreCloudToLocal(localDb)

    private fun formatQtyBrief(qty: Double): String {
        val rounded = roundWeight(qty)
        return if (kotlin.math.abs(rounded - rounded.toLong()) < 0.0005) {
            rounded.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.3f", rounded).trimEnd('0').trimEnd('.')
        }
    }

    companion object {
        private const val TAG = "ShopRepository"
    }
}
