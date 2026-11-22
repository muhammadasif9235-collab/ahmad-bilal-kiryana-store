package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ProductCategory {
    FLOUR_RICE,
    GHEE_OIL,
    SUGAR_PULSES,
    TEA_DAIRY,
    SPICES,
    SOAP_CLEANING,
    DRINKS_SNACKS,
    OTHER;

    companion object {
        fun safeValueOf(raw: String?): ProductCategory =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}

enum class OrderStatus {
    PENDING,
    COMPLETED,
    CANCELLED;

    companion object {
        fun safeValueOf(raw: String?): OrderStatus =
            entries.firstOrNull { it.name == raw } ?: COMPLETED
    }
}

enum class PaymentMethod {
    CASH,
    UDHAR,
    PARTIAL;

    companion object {
        fun safeValueOf(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: CASH
    }
}

enum class LedgerEntryType {
    DEBIT,
    CREDIT;

    companion object {
        fun safeValueOf(raw: String?): LedgerEntryType =
            entries.firstOrNull { it.name == raw } ?: DEBIT
    }
}

enum class StockMovementType {
    STOCK_IN,
    STOCK_OUT,
    SALE,
    ADJUSTMENT;

    companion object {
        fun safeValueOf(raw: String?): StockMovementType =
            entries.firstOrNull { it.name == raw } ?: ADJUSTMENT
    }
}

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val nameUrdu: String = "",
    val nameEnglish: String = "",
    val category: String = ProductCategory.OTHER.name,
    val unit: String = "kg",
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val marginPercent: Double = 8.0,
    val autoPriceEnabled: Boolean = true,
    val stockQty: Double = 0.0,
    val lowStockThreshold: Double = 5.0,
    val barcode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): ProductCategory = ProductCategory.safeValueOf(category)
    fun profitPerUnit(): Double = (sellingPrice - costPrice).coerceAtLeast(0.0)
    fun isLowStock(): Boolean = stockQty <= lowStockThreshold
}

@Keep
@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val balance: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrderItem(
    val productId: String = "",
    val nameUrdu: String = "",
    val nameEnglish: String = "",
    val unit: String = "kg",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val lineTotal: Double = 0.0
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "nameUrdu" to nameUrdu,
        "nameEnglish" to nameEnglish,
        "unit" to unit,
        "quantity" to quantity,
        "unitPrice" to unitPrice,
        "costPrice" to costPrice,
        "lineTotal" to lineTotal
    )
}

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val billNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val status: String = OrderStatus.COMPLETED.name,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingUdhar: Double = 0.0,
    val totalCost: Double = 0.0,
    val profitAmount: Double = 0.0,
    val items: List<OrderItem> = emptyList(),
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): OrderStatus = OrderStatus.safeValueOf(status)
    fun paymentMethodEnum(): PaymentMethod = PaymentMethod.safeValueOf(paymentMethod)
}

@Keep
@IgnoreExtraProperties
data class LedgerEntry(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val entryType: String = LedgerEntryType.DEBIT.name,
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val orderId: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun entryTypeEnum(): LedgerEntryType = LedgerEntryType.safeValueOf(entryType)
}

@Keep
@IgnoreExtraProperties
data class StockMovement(
    @DocumentId val id: String = "",
    val productId: String = "",
    val productNameUrdu: String = "",
    val productNameEnglish: String = "",
    val movementType: String = StockMovementType.STOCK_IN.name,
    val quantity: Double = 0.0,
    val unitCost: Double = 0.0,
    val unitPrice: Double = 0.0,
    val profitDelta: Double = 0.0,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun movementTypeEnum(): StockMovementType = StockMovementType.safeValueOf(movementType)
}

data class CartItem(
    val product: Product,
    val quantity: Double = 1.0,
    val customUnitPrice: Double? = null
) {
    val effectivePrice: Double get() = customUnitPrice ?: product.sellingPrice
    val lineTotal: Double get() = effectivePrice * quantity
    val lineCost: Double get() = product.costPrice * quantity
}

enum class DonationEntryType {
    DONATION_IN,
    RATION_OUT;

    companion object {
        fun safeValueOf(raw: String?): DonationEntryType =
            entries.firstOrNull { it.name == raw } ?: DONATION_IN
    }
}

enum class DonationStatus {
    PENDING,
    VERIFIED,
    REJECTED;

    companion object {
        fun safeValueOf(raw: String?): DonationStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class RationDonation(
    @DocumentId val id: String = "",
    val entryType: String = DonationEntryType.DONATION_IN.name,
    val personName: String = "",
    val phone: String = "",
    val amount: Double = 0.0,
    val rationSummary: String = "",
    val paymentRef: String = "",
    val senderAccount: String = "",
    val transactionId: String = "",
    val receiptImagePath: String = "",
    val status: String = DonationStatus.PENDING.name,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun entryTypeEnum(): DonationEntryType = DonationEntryType.safeValueOf(entryType)
    fun statusEnum(): DonationStatus = DonationStatus.safeValueOf(status)
}

@Keep
@IgnoreExtraProperties
data class RationCardItem(
    @DocumentId val id: String = "",
    val nameUrdu: String = "",
    val nameEnglish: String = "",
    val unit: String = "kg",
    val price: Double = 0.0,
    val sortOrder: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RationCardBill(
    @DocumentId val id: String = "",
    val billNumber: String = "",
    val cardUid: String = "",
    val cardNumber: String = "",
    val beneficiaryName: String = "",
    val cnic: String = "",
    val phone: String = "",
    val monthKey: String = "",
    val subsidyLimit: Double = 3000.0,
    val totalAmount: Double = 0.0,
    val subsidyUsed: Double = 0.0,
    val cashTopUp: Double = 0.0,
    val remainingSubsidy: Double = 0.0,
    val items: List<OrderItem> = emptyList(),
    val scannedViaNfc: Boolean = false,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class RationCartItem(
    val item: RationCardItem,
    val quantity: Double = 1.0,
    val customUnitPrice: Double? = null
) {
    val effectivePrice: Double get() = customUnitPrice ?: item.price
    val lineTotal: Double get() = effectivePrice * quantity
}
