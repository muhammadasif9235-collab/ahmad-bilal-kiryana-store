package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.Customer
import com.example.data.LedgerEntry
import com.example.data.Order
import com.example.data.OrderItem
import com.example.data.Product
import com.example.data.RationCardBill
import com.example.data.RationCardItem
import com.example.data.RationDonation
import com.example.data.StockMovement
import com.google.firebase.Timestamp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

fun Long.toFirebaseTimestamp(): Timestamp = Timestamp(Date(if (this > 0L) this else System.currentTimeMillis()))

fun Timestamp?.toEpochMs(): Long = this?.toDate()?.time ?: System.currentTimeMillis()

object OrderItemJsonSerializer {
    fun encode(items: List<OrderItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("productId", item.productId)
            obj.put("nameUrdu", item.nameUrdu)
            obj.put("nameEnglish", item.nameEnglish)
            obj.put("unit", item.unit)
            obj.put("quantity", item.quantity)
            obj.put("unitPrice", item.unitPrice)
            obj.put("costPrice", item.costPrice)
            obj.put("lineTotal", item.lineTotal)
            array.put(obj)
        }
        return array.toString()
    }

    fun decode(raw: String): List<OrderItem> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        OrderItem(
                            productId = obj.optString("productId", ""),
                            nameUrdu = obj.optString("nameUrdu", ""),
                            nameEnglish = obj.optString("nameEnglish", ""),
                            unit = obj.optString("unit", "kg"),
                            quantity = obj.optDouble("quantity", 1.0),
                            unitPrice = obj.optDouble("unitPrice", 0.0),
                            costPrice = obj.optDouble("costPrice", 0.0),
                            lineTotal = obj.optDouble("lineTotal", 0.0)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val nameUrdu: String,
    val nameEnglish: String,
    val category: String,
    val unit: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val marginPercent: Double,
    val autoPriceEnabled: Boolean,
    val stockQty: Double,
    val lowStockThreshold: Double,
    val barcode: String,
    val createdAtMs: Long,
    val updatedAtMs: Long
) {
    fun toDomain(): Product = Product(
        id = id,
        nameUrdu = nameUrdu,
        nameEnglish = nameEnglish,
        category = category,
        unit = unit,
        costPrice = costPrice,
        sellingPrice = sellingPrice,
        marginPercent = marginPercent,
        autoPriceEnabled = autoPriceEnabled,
        stockQty = stockQty,
        lowStockThreshold = lowStockThreshold,
        barcode = barcode,
        createdAt = createdAtMs.toFirebaseTimestamp(),
        updatedAt = updatedAtMs.toFirebaseTimestamp()
    )
}

fun Product.toEntity(nowMs: Long = System.currentTimeMillis()): ProductEntity = ProductEntity(
    id = id,
    nameUrdu = nameUrdu,
    nameEnglish = nameEnglish,
    category = category,
    unit = unit,
    costPrice = costPrice,
    sellingPrice = sellingPrice,
    marginPercent = marginPercent,
    autoPriceEnabled = autoPriceEnabled,
    stockQty = stockQty,
    lowStockThreshold = lowStockThreshold,
    barcode = barcode,
    createdAtMs = createdAt?.toEpochMs() ?: nowMs,
    updatedAtMs = updatedAt?.toEpochMs() ?: nowMs
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val address: String,
    val balance: Double,
    val createdAtMs: Long,
    val updatedAtMs: Long
) {
    fun toDomain(): Customer = Customer(
        id = id,
        name = name,
        phone = phone,
        address = address,
        balance = balance,
        createdAt = createdAtMs.toFirebaseTimestamp(),
        updatedAt = updatedAtMs.toFirebaseTimestamp()
    )
}

fun Customer.toEntity(nowMs: Long = System.currentTimeMillis()): CustomerEntity = CustomerEntity(
    id = id,
    name = name,
    phone = phone,
    address = address,
    balance = balance,
    createdAtMs = createdAt?.toEpochMs() ?: nowMs,
    updatedAtMs = updatedAt?.toEpochMs() ?: nowMs
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val billNumber: String,
    val customerId: String,
    val customerName: String,
    val status: String,
    val paymentMethod: String,
    val subtotal: Double,
    val discount: Double,
    val totalAmount: Double,
    val paidAmount: Double,
    val remainingUdhar: Double,
    val totalCost: Double,
    val profitAmount: Double,
    val itemsJson: String,
    val note: String,
    val createdAtMs: Long,
    val updatedAtMs: Long
) {
    fun toDomain(): Order = Order(
        id = id,
        billNumber = billNumber,
        customerId = customerId,
        customerName = customerName,
        status = status,
        paymentMethod = paymentMethod,
        subtotal = subtotal,
        discount = discount,
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        remainingUdhar = remainingUdhar,
        totalCost = totalCost,
        profitAmount = profitAmount,
        items = OrderItemJsonSerializer.decode(itemsJson),
        note = note,
        createdAt = createdAtMs.toFirebaseTimestamp(),
        updatedAt = updatedAtMs.toFirebaseTimestamp()
    )
}

fun Order.toEntity(nowMs: Long = System.currentTimeMillis()): OrderEntity = OrderEntity(
    id = id,
    billNumber = billNumber,
    customerId = customerId,
    customerName = customerName,
    status = status,
    paymentMethod = paymentMethod,
    subtotal = subtotal,
    discount = discount,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    remainingUdhar = remainingUdhar,
    totalCost = totalCost,
    profitAmount = profitAmount,
    itemsJson = OrderItemJsonSerializer.encode(items),
    note = note,
    createdAtMs = createdAt?.toEpochMs() ?: nowMs,
    updatedAtMs = updatedAt?.toEpochMs() ?: nowMs
)

@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val customerName: String,
    val entryType: String,
    val amount: Double,
    val balanceAfter: Double,
    val orderId: String,
    val note: String,
    val createdAtMs: Long
) {
    fun toDomain(): LedgerEntry = LedgerEntry(
        id = id,
        customerId = customerId,
        customerName = customerName,
        entryType = entryType,
        amount = amount,
        balanceAfter = balanceAfter,
        orderId = orderId,
        note = note,
        createdAt = createdAtMs.toFirebaseTimestamp()
    )
}

fun LedgerEntry.toEntity(nowMs: Long = System.currentTimeMillis()): LedgerEntryEntity = LedgerEntryEntity(
    id = id,
    customerId = customerId,
    customerName = customerName,
    entryType = entryType,
    amount = amount,
    balanceAfter = balanceAfter,
    orderId = orderId,
    note = note,
    createdAtMs = createdAt?.toEpochMs() ?: nowMs
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val productNameUrdu: String,
    val productNameEnglish: String,
    val movementType: String,
    val quantity: Double,
    val unitCost: Double,
    val unitPrice: Double,
    val profitDelta: Double,
    val note: String,
    val createdAtMs: Long
) {
    fun toDomain(): StockMovement = StockMovement(
        id = id,
        productId = productId,
        productNameUrdu = productNameUrdu,
        productNameEnglish = productNameEnglish,
        movementType = movementType,
        quantity = quantity,
        unitCost = unitCost,
        unitPrice = unitPrice,
        profitDelta = profitDelta,
        note = note,
        createdAt = createdAtMs.toFirebaseTimestamp()
    )
}

fun StockMovement.toEntity(nowMs: Long = System.currentTimeMillis()): StockMovementEntity = StockMovementEntity(
    id = id,
    productId = productId,
    productNameUrdu = productNameUrdu,
    productNameEnglish = productNameEnglish,
    movementType = movementType,
    quantity = quantity,
    unitCost = unitCost,
    unitPrice = unitPrice,
    profitDelta = profitDelta,
    note = note,
    createdAtMs = createdAt?.toEpochMs() ?: nowMs
)

@Entity(tableName = "ration_donations")
data class RationDonationEntity(
    @PrimaryKey val id: String,
    val entryType: String,
    val personName: String,
    val phone: String,
    val amount: Double,
    val rationSummary: String,
    val paymentRef: String,
    val senderAccount: String = "",
    val transactionId: String = "",
    val receiptImagePath: String = "",
    val status: String = "PENDING",
    val createdAtMs: Long
) {
    fun toDomain(): RationDonation = RationDonation(
        id = id,
        entryType = entryType,
        personName = personName,
        phone = phone,
        amount = amount,
        rationSummary = rationSummary,
        paymentRef = paymentRef,
        senderAccount = senderAccount,
        transactionId = transactionId,
        receiptImagePath = receiptImagePath,
        status = status,
        createdAt = createdAtMs.toFirebaseTimestamp()
    )
}

fun RationDonation.toEntity(nowMs: Long = System.currentTimeMillis()): RationDonationEntity = RationDonationEntity(
    id = id,
    entryType = entryType,
    personName = personName,
    phone = phone,
    amount = amount,
    rationSummary = rationSummary,
    paymentRef = paymentRef,
    senderAccount = senderAccount,
    transactionId = transactionId,
    receiptImagePath = receiptImagePath,
    status = status,
    createdAtMs = createdAt?.toEpochMs() ?: nowMs
)

@Entity(tableName = "ration_card_items")
data class RationCardItemEntity(
    @PrimaryKey val id: String,
    val nameUrdu: String,
    val nameEnglish: String,
    val unit: String,
    val price: Double,
    val sortOrder: Int,
    val createdAtMs: Long,
    val updatedAtMs: Long
) {
    fun toDomain(): RationCardItem = RationCardItem(
        id = id,
        nameUrdu = nameUrdu,
        nameEnglish = nameEnglish,
        unit = unit,
        price = price,
        sortOrder = sortOrder,
        createdAt = createdAtMs.toFirebaseTimestamp(),
        updatedAt = updatedAtMs.toFirebaseTimestamp()
    )
}

fun RationCardItem.toEntity(nowMs: Long = System.currentTimeMillis()): RationCardItemEntity = RationCardItemEntity(
    id = id,
    nameUrdu = nameUrdu,
    nameEnglish = nameEnglish,
    unit = unit,
    price = price,
    sortOrder = sortOrder,
    createdAtMs = createdAt?.toEpochMs() ?: nowMs,
    updatedAtMs = updatedAt?.toEpochMs() ?: nowMs
)

@Entity(tableName = "ration_card_bills")
data class RationCardBillEntity(
    @PrimaryKey val id: String,
    val billNumber: String,
    val cardUid: String,
    val cardNumber: String,
    val beneficiaryName: String,
    val cnic: String,
    val phone: String,
    val monthKey: String,
    val subsidyLimit: Double,
    val totalAmount: Double,
    val subsidyUsed: Double,
    val cashTopUp: Double,
    val remainingSubsidy: Double,
    val itemsJson: String,
    val scannedViaNfc: Boolean,
    val note: String,
    val createdAtMs: Long
) {
    fun toDomain(): RationCardBill = RationCardBill(
        id = id,
        billNumber = billNumber,
        cardUid = cardUid,
        cardNumber = cardNumber,
        beneficiaryName = beneficiaryName,
        cnic = cnic,
        phone = phone,
        monthKey = monthKey,
        subsidyLimit = subsidyLimit,
        totalAmount = totalAmount,
        subsidyUsed = subsidyUsed,
        cashTopUp = cashTopUp,
        remainingSubsidy = remainingSubsidy,
        items = OrderItemJsonSerializer.decode(itemsJson),
        scannedViaNfc = scannedViaNfc,
        note = note,
        createdAt = createdAtMs.toFirebaseTimestamp()
    )
}

fun RationCardBill.toEntity(nowMs: Long = System.currentTimeMillis()): RationCardBillEntity = RationCardBillEntity(
    id = id,
    billNumber = billNumber,
    cardUid = cardUid,
    cardNumber = cardNumber,
    beneficiaryName = beneficiaryName,
    cnic = cnic,
    phone = phone,
    monthKey = monthKey,
    subsidyLimit = subsidyLimit,
    totalAmount = totalAmount,
    subsidyUsed = subsidyUsed,
    cashTopUp = cashTopUp,
    remainingSubsidy = remainingSubsidy,
    itemsJson = OrderItemJsonSerializer.encode(items),
    scannedViaNfc = scannedViaNfc,
    note = note,
    createdAtMs = createdAt?.toEpochMs() ?: nowMs
)
