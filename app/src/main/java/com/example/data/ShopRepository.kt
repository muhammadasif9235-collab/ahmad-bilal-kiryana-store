package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.data.local.CustomerEntity
import com.example.data.local.LedgerEntryEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemJsonSerializer
import com.example.data.local.ProductEntity
import com.example.data.local.RationCardBillEntity
import com.example.data.local.RationCardItemEntity
import com.example.data.local.RationDonationEntity
import com.example.data.local.ShopLocalDatabase
import com.example.data.local.StockMovementEntity
import com.example.data.local.toFirebaseTimestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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

    // --- AUTO-SEED INITIAL CATALOGS ON FIRST LAUNCH ---

    suspend fun seedGujratCatalogIfEmpty(): Result<Int> {
        return try {
            if (dao.countProducts() > 0) return Result.success(0)
            val now = System.currentTimeMillis()
            val entities = GujratCatalogData.initialProducts.map { seed ->
                ProductEntity(
                    id = seed.docId,
                    nameUrdu = seed.nameUrdu,
                    nameEnglish = seed.nameEnglish,
                    category = seed.category.name,
                    unit = seed.unit,
                    costPrice = seed.costPrice,
                    sellingPrice = seed.sellingPrice,
                    marginPercent = seed.marginPercent,
                    autoPriceEnabled = true,
                    stockQty = seed.stockQty,
                    lowStockThreshold = seed.lowStockThreshold,
                    barcode = seed.barcode,
                    createdAtMs = now,
                    updatedAtMs = now
                )
            }
            dao.upsertProducts(entities)
            Result.success(entities.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed Gujrat catalog", e)
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

    suspend fun applyGujratMarketRates(products: List<Product>, fallbackMarginPercent: Double): Result<Int> {
        return try {
            val autoProducts = products.filter { it.autoPriceEnabled && it.id.isNotBlank() }
            if (autoProducts.isEmpty()) return Result.success(0)
            val now = System.currentTimeMillis()
            var updatedCount = 0
            localDb.withTransaction {
                for (prod in autoProducts) {
                    val marketCost = GujratCatalogData.gujratMarketCostLookup(prod)
                    val margin = if (prod.marginPercent > 0) prod.marginPercent else fallbackMarginPercent
                    val newSelling = GujratCatalogData.calculateAutoSellingPrice(marketCost, margin)
                    dao.updateProductPrices(prod.id, marketCost, newSelling, now)
                    updatedCount++
                }
            }
            Result.success(updatedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error applying Gujrat market rates", e)
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

    // --- ORDERS & AUTOMATIC BILLING ---

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

        val orderId = newId()
        val now = System.currentTimeMillis()
        val cleanNote = note.trim().take(300)

        return try {
            localDb.withTransaction {
                dao.upsertOrder(
                    OrderEntity(
                        id = orderId,
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
                        itemsJson = OrderItemJsonSerializer.encode(orderItems),
                        note = cleanNote,
                        createdAtMs = now,
                        updatedAtMs = now
                    )
                )

                if (status == OrderStatus.COMPLETED) {
                    for (ci in cartItems.take(100)) {
                        if (ci.product.id.isNotBlank()) {
                            val newStock = (ci.product.stockQty - ci.quantity).coerceAtLeast(0.0)
                            dao.updateProductStock(ci.product.id, newStock, now)
                            dao.insertStockMovement(
                                StockMovementEntity(
                                    id = newId(),
                                    productId = ci.product.id,
                                    productNameUrdu = ci.product.nameUrdu.take(120),
                                    productNameEnglish = ci.product.nameEnglish.take(120),
                                    movementType = StockMovementType.SALE.name,
                                    quantity = ci.quantity.coerceAtLeast(0.01),
                                    unitCost = ci.product.costPrice,
                                    unitPrice = ci.effectivePrice,
                                    profitDelta = (ci.effectivePrice - ci.product.costPrice) * ci.quantity,
                                    note = "بل #$billNo - $cleanCustomerName".take(300),
                                    createdAtMs = now
                                )
                            )
                        }
                    }

                    if (remainingUdhar > 0.0 && customer != null && customer.id.isNotBlank()) {
                        val updatedBalance = (customer.balance + remainingUdhar).coerceIn(-10_000_000.0, 10_000_000.0)
                        dao.updateCustomerBalance(customer.id, updatedBalance, now)
                        dao.insertLedgerEntry(
                            LedgerEntryEntity(
                                id = newId(),
                                customerId = customer.id,
                                customerName = customer.name.take(100),
                                entryType = LedgerEntryType.DEBIT.name,
                                amount = remainingUdhar,
                                balanceAfter = updatedBalance,
                                orderId = orderId,
                                note = "بل #$billNo ادھار".take(300),
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
                        val newStock = (prod.stockQty - item.quantity).coerceAtLeast(0.0)
                        dao.updateProductStock(prod.id, newStock, now)
                        dao.insertStockMovement(
                            StockMovementEntity(
                                id = newId(),
                                productId = prod.id,
                                productNameUrdu = prod.nameUrdu.take(120),
                                productNameEnglish = prod.nameEnglish.take(120),
                                movementType = StockMovementType.SALE.name,
                                quantity = item.quantity.coerceAtLeast(0.01),
                                unitCost = item.costPrice,
                                unitPrice = item.unitPrice,
                                profitDelta = (item.unitPrice - item.costPrice) * item.quantity,
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
        alreadyUsedThisMonth: Double,
        scannedViaNfc: Boolean,
        note: String
    ): Result<RationCardBill> {
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

        val id = newId()
        val now = System.currentTimeMillis()
        return try {
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
            Log.e(TAG, "Error creating ration card bill", e)
            Result.failure(e)
        }
    }

    // --- OPTIONAL GOOGLE CLOUD BACKUP & RESTORE ---

    fun isCloudConnected(): Boolean = cloudBackupService.isGoogleSignedIn()

    suspend fun backupLocalToCloud(): Result<Int> = cloudBackupService.backupLocalToCloud(localDb)

    suspend fun restoreCloudToLocal(): Result<Int> = cloudBackupService.restoreCloudToLocal(localDb)

    companion object {
        private const val TAG = "ShopRepository"
    }
}
