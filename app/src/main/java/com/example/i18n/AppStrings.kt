package com.example.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.data.ProductCategory
import com.example.data.StockMovementType
import java.text.NumberFormat
import java.util.Locale

enum class AppLanguage(val code: String) {
    URDU("ur"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: URDU
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.URDU }

fun AppLanguage.pick(urdu: String, english: String): String =
    if (this == AppLanguage.URDU) urdu else english

fun formatPkr(amount: Double, language: AppLanguage): String {
    val rounded = if (amount % 1.0 == 0.0) amount.toLong().toString() else String.format(Locale.US, "%.1f", amount)
    val formatted = try {
        val nf = NumberFormat.getNumberInstance(Locale.US)
        nf.maximumFractionDigits = 1
        nf.format(amount)
    } catch (_: Exception) {
        rounded
    }
    return if (language == AppLanguage.URDU) "روپے $formatted" else "Rs. $formatted"
}

fun formatQty(qty: Double): String {
    return if (qty % 1.0 == 0.0) qty.toLong().toString() else String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
}

fun ProductCategory.label(language: AppLanguage): String = when (this) {
    ProductCategory.FLOUR_RICE -> language.pick("آٹا اور چاول", "Flour & Rice")
    ProductCategory.GHEE_OIL -> language.pick("گھی اور آئل", "Ghee & Oil")
    ProductCategory.SUGAR_PULSES -> language.pick("چینی اور دالیں", "Sugar & Pulses")
    ProductCategory.TEA_DAIRY -> language.pick("چائے اور دودھ", "Tea & Dairy")
    ProductCategory.SPICES -> language.pick("مصالحہ جات", "Spices")
    ProductCategory.SOAP_CLEANING -> language.pick("صابن اور صفائی", "Soap & Care")
    ProductCategory.DRINKS_SNACKS -> language.pick("مشروبات و بسکٹ", "Drinks & Snacks")
    ProductCategory.OTHER -> language.pick("دیگر اشیاء", "Other Items")
}

fun unitLabel(unit: String, language: AppLanguage): String = when (unit.lowercase()) {
    "kg" -> language.pick("کلو", "kg")
    "bag" -> language.pick("تھیلا", "bag")
    "liter", "l" -> language.pick("لیٹر", "L")
    "tin" -> language.pick("ٹن", "tin")
    "pack" -> language.pick("پیکٹ", "pack")
    "dozen" -> language.pick("درجن", "dozen")
    "bottle" -> language.pick("بوتل", "bottle")
    "piece", "pcs" -> language.pick("عدد", "pcs")
    else -> unit
}

fun PaymentMethod.label(language: AppLanguage): String = when (this) {
    PaymentMethod.CASH -> language.pick("نقد", "Cash")
    PaymentMethod.UDHAR -> language.pick("ادھار", "Udhar")
    PaymentMethod.PARTIAL -> language.pick("جزوی نقد/ادھار", "Partial")
}

fun OrderStatus.label(language: AppLanguage): String = when (this) {
    OrderStatus.PENDING -> language.pick("زیرِ التواء آرڈر", "Pending Order")
    OrderStatus.COMPLETED -> language.pick("مکمل بل", "Completed")
    OrderStatus.CANCELLED -> language.pick("منسوخ", "Cancelled")
}

fun StockMovementType.label(language: AppLanguage): String = when (this) {
    StockMovementType.STOCK_IN -> language.pick("اسٹاک ان (خریداری)", "Stock In")
    StockMovementType.STOCK_OUT -> language.pick("اسٹاک آؤٹ (اخراج)", "Stock Out")
    StockMovementType.SALE -> language.pick("فروخت", "Sale")
    StockMovementType.ADJUSTMENT -> language.pick("درستگی", "Adjustment")
}
