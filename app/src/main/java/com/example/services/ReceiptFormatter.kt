package com.example.services

import com.example.data.DailyCashEntry
import com.example.data.DailyCashType
import com.example.data.DailyPaymentSource
import com.example.data.LedgerEntry
import com.example.data.LedgerEntryType
import com.example.data.Order
import com.example.data.PaymentMethod
import com.example.data.Product
import com.example.data.RationCardBill
import com.example.data.Wholesaler
import com.example.data.WholesalerEntryType
import com.example.data.WholesalerLedgerEntry
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.formatQty
import com.example.i18n.label
import com.example.i18n.unitLabel
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptFormatter {

    const val SHOP_NAME_URDU = "احمد بلال کریانہ سٹور ٹانڈہ"
    const val SHOP_NAME_ENGLISH = "Ahmad Bilal Kiryana Store, Tanda"
    const val SHOP_CITY_URDU = "ٹانڈہ، ضلع گجرات، پنجاب، پاکستان"
    const val SHOP_CITY_ENGLISH = "Tanda, District Gujrat, Punjab, Pakistan"

    fun formatDateTime(epochMillis: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(Date(epochMillis))
    }

    /**
     * Plain text receipt for WhatsApp / SMS sharing.
     */
    fun buildShareText(
        order: Order,
        language: AppLanguage,
        shopPhone: String,
        shopNameUrdu: String = SHOP_NAME_URDU,
        shopNameEnglish: String = SHOP_NAME_ENGLISH,
        shopAddressUrdu: String = SHOP_CITY_URDU,
        shopAddressEnglish: String = SHOP_CITY_ENGLISH
    ): String {
        val dateStr = formatDateTime(order.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val nameUr = shopNameUrdu.ifBlank { SHOP_NAME_URDU }
        val nameEn = shopNameEnglish.ifBlank { SHOP_NAME_ENGLISH }
        val addrUr = shopAddressUrdu.ifBlank { SHOP_CITY_URDU }
        val addrEn = shopAddressEnglish.ifBlank { SHOP_CITY_ENGLISH }
        val sb = StringBuilder()
        if (language == AppLanguage.URDU) {
            sb.appendLine("============================")
            sb.appendLine("    $nameUr")
            sb.appendLine("    $addrUr")
            if (shopPhone.isNotBlank()) sb.appendLine("    فون: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("بل نمبر: ${order.billNumber}")
            sb.appendLine("تاریخ: $dateStr")
            sb.appendLine("کسٹمر: ${order.customerName}")
            sb.appendLine("ادائیگی: ${order.paymentMethodEnum().label(AppLanguage.URDU)}")
            sb.appendLine("----------------------------")
            order.items.forEachIndexed { idx, item ->
                sb.appendLine(
                    "${idx + 1}. ${item.nameUrdu} (${formatQty(item.quantity)} ${unitLabel(item.unit, AppLanguage.URDU)}) x ${item.unitPrice.toLong()} = ${formatPkr(item.lineTotal, AppLanguage.URDU)}"
                )
            }
            sb.appendLine("----------------------------")
            if (order.discount > 0) {
                sb.appendLine("کل رقم: ${formatPkr(order.subtotal, AppLanguage.URDU)}")
                sb.appendLine("رعایت: -${formatPkr(order.discount, AppLanguage.URDU)}")
            }
            sb.appendLine("واجب الادا کل: ${formatPkr(order.totalAmount, AppLanguage.URDU)}")
            sb.appendLine("وصول شدہ: ${formatPkr(order.paidAmount, AppLanguage.URDU)}")
            if (order.remainingUdhar > 0) {
                sb.appendLine("بقایا ادھار: ${formatPkr(order.remainingUdhar, AppLanguage.URDU)}")
            }
            sb.appendLine("============================")
            sb.appendLine("خریداری کا شکریہ! دوبارہ تشریف لائیں")
        } else {
            sb.appendLine("============================")
            sb.appendLine("  $nameEn")
            sb.appendLine("  $addrEn")
            if (shopPhone.isNotBlank()) sb.appendLine("  Phone: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("Bill #: ${order.billNumber}")
            sb.appendLine("Date: $dateStr")
            sb.appendLine("Customer: ${order.customerName}")
            sb.appendLine("Payment: ${order.paymentMethodEnum().label(AppLanguage.ENGLISH)}")
            sb.appendLine("----------------------------")
            order.items.forEachIndexed { idx, item ->
                sb.appendLine(
                    "${idx + 1}. ${item.nameEnglish} (${formatQty(item.quantity)} ${unitLabel(item.unit, AppLanguage.ENGLISH)}) x ${item.unitPrice.toLong()} = ${formatPkr(item.lineTotal, AppLanguage.ENGLISH)}"
                )
            }
            sb.appendLine("----------------------------")
            if (order.discount > 0) {
                sb.appendLine("Subtotal: ${formatPkr(order.subtotal, AppLanguage.ENGLISH)}")
                sb.appendLine("Discount: -${formatPkr(order.discount, AppLanguage.ENGLISH)}")
            }
            sb.appendLine("Grand Total: ${formatPkr(order.totalAmount, AppLanguage.ENGLISH)}")
            sb.appendLine("Paid: ${formatPkr(order.paidAmount, AppLanguage.ENGLISH)}")
            if (order.remainingUdhar > 0) {
                sb.appendLine("Remaining Udhar: ${formatPkr(order.remainingUdhar, AppLanguage.ENGLISH)}")
            }
            sb.appendLine("============================")
            sb.appendLine("Thank you for shopping with us!")
        }
        return sb.toString()
    }

    /**
     * Rich RTL/LTR HTML receipt for Android PrintManager (Wi-Fi / USB / Save as PDF).
     */
    fun buildPrintableHtml(
        order: Order,
        language: AppLanguage,
        shopPhone: String,
        shopNameUrdu: String = SHOP_NAME_URDU,
        shopNameEnglish: String = SHOP_NAME_ENGLISH,
        shopAddressUrdu: String = SHOP_CITY_URDU,
        shopAddressEnglish: String = SHOP_CITY_ENGLISH
    ): String {
        val isUrdu = language == AppLanguage.URDU
        val dir = if (isUrdu) "rtl" else "ltr"
        val dateStr = formatDateTime(order.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val rows = order.items.joinToString("\n") { item ->
            val name = if (isUrdu) item.nameUrdu else item.nameEnglish
            val qtyStr = "${formatQty(item.quantity)} ${unitLabel(item.unit, language)}"
            """
            <tr>
                <td style="padding:6px 4px;border-bottom:1px dashed #ccc;">${escapeHtml(name)}</td>
                <td style="padding:6px 4px;border-bottom:1px dashed #ccc;text-align:center;">${escapeHtml(qtyStr)}</td>
                <td style="padding:6px 4px;border-bottom:1px dashed #ccc;text-align:center;">${item.unitPrice.toLong()}</td>
                <td style="padding:6px 4px;border-bottom:1px dashed #ccc;text-align:end;font-weight:bold;">${item.lineTotal.toLong()}</td>
            </tr>
            """.trimIndent()
        }

        val shopTitle = if (isUrdu) shopNameUrdu.ifBlank { SHOP_NAME_URDU } else shopNameEnglish.ifBlank { SHOP_NAME_ENGLISH }
        val shopSub = if (isUrdu) shopAddressUrdu.ifBlank { SHOP_CITY_URDU } else shopAddressEnglish.ifBlank { SHOP_CITY_ENGLISH }
        val billLbl = if (isUrdu) "بل نمبر" else "Bill #"
        val dateLbl = if (isUrdu) "تاریخ" else "Date"
        val custLbl = if (isUrdu) "کسٹمر" else "Customer"
        val payLbl = if (isUrdu) "طریقہ ادائیگی" else "Payment"
        val colItem = if (isUrdu) "اشیاء" else "Item"
        val colQty = if (isUrdu) "مقدار" else "Qty"
        val colRate = if (isUrdu) "ریٹ" else "Rate"
        val colTotal = if (isUrdu) "رقم" else "Amount"
        val grandLbl = if (isUrdu) "کل واجب الادا" else "Grand Total"
        val paidLbl = if (isUrdu) "وصول شدہ" else "Paid"
        val udharLbl = if (isUrdu) "بقایا ادھار" else "Udhar Balance"
        val footerMsg = if (isUrdu) "خریداری کا شکریہ — $shopTitle" else "Thank you for visiting $shopTitle"

        return """
            <!DOCTYPE html>
            <html dir="$dir" lang="${language.code}">
            <head>
            <meta charset="UTF-8" />
            <style>
                body { font-family: sans-serif; color: #111; margin: 16px; max-width: 420px; }
                .header { text-align: center; border-bottom: 2px solid #0F5132; padding-bottom: 10px; margin-bottom: 12px; }
                .title { font-size: 22px; font-weight: bold; color: #0F5132; margin: 0; }
                .sub { font-size: 13px; color: #444; margin: 4px 0 0 0; }
                .meta { font-size: 13px; margin-bottom: 10px; line-height: 1.7; }
                table { width: 100%; border-collapse: collapse; font-size: 13px; }
                th { background: #F1ECE1; padding: 7px 4px; text-align: start; border-bottom: 1px solid #0F5132; }
                .totals { margin-top: 12px; border-top: 2px solid #0F5132; padding-top: 8px; font-size: 14px; }
                .total-row { display: flex; justify-content: space-between; margin: 4px 0; }
                .grand { font-size: 17px; font-weight: bold; color: #0F5132; }
                .udhar { color: #C81E1E; font-weight: bold; }
                .footer { text-align: center; margin-top: 18px; font-size: 12px; color: #555; border-top: 1px dashed #999; padding-top: 8px; }
            </style>
            </head>
            <body>
                <div class="header">
                    <p class="title">${escapeHtml(shopTitle)}</p>
                    <p class="sub">${escapeHtml(shopSub)}</p>
                    ${if (shopPhone.isNotBlank()) "<p class='sub'>${escapeHtml(shopPhone)}</p>" else ""}
                </div>
                <div class="meta">
                    <div><strong>$billLbl:</strong> ${escapeHtml(order.billNumber)}</div>
                    <div><strong>$dateLbl:</strong> ${escapeHtml(dateStr)}</div>
                    <div><strong>$custLbl:</strong> ${escapeHtml(order.customerName)}</div>
                    <div><strong>$payLbl:</strong> ${escapeHtml(order.paymentMethodEnum().label(language))}</div>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>$colItem</th>
                            <th style="text-align:center;">$colQty</th>
                            <th style="text-align:center;">$colRate</th>
                            <th style="text-align:end;">$colTotal</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                </table>
                <div class="totals">
                    <div class="total-row grand"><span>$grandLbl</span><span>${escapeHtml(formatPkr(order.totalAmount, language))}</span></div>
                    <div class="total-row"><span>$paidLbl</span><span>${escapeHtml(formatPkr(order.paidAmount, language))}</span></div>
                    ${if (order.remainingUdhar > 0) "<div class='total-row udhar'><span>$udharLbl</span><span>${escapeHtml(formatPkr(order.remainingUdhar, language))}</span></div>" else ""}
                </div>
                <div class="footer">$footerMsg</div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Builds 58mm ESC/POS command byte buffer (32 columns) for Bluetooth thermal receipt printers.
     */
    fun buildEscPosBytes(
        order: Order,
        shopPhone: String,
        shopNameEnglish: String = SHOP_NAME_ENGLISH,
        shopAddressEnglish: String = SHOP_CITY_ENGLISH
    ): ByteArray {
        val out = ByteArrayOutputStream()
        fun cmd(vararg bytes: Int) {
            bytes.forEach { out.write(it) }
        }
        fun line(text: String) {
            out.write("$text\n".toByteArray(Charsets.UTF_8))
        }

        val cleanName = shopNameEnglish.ifBlank { SHOP_NAME_ENGLISH }.uppercase(Locale.US).take(32)
        val cleanAddr = shopAddressEnglish.ifBlank { SHOP_CITY_ENGLISH }.take(32)

        // Initialize printer
        cmd(0x1B, 0x40)
        // Center align + Bold
        cmd(0x1B, 0x61, 0x01)
        cmd(0x1B, 0x45, 0x01)
        line(cleanName)
        cmd(0x1B, 0x45, 0x00)
        line(cleanAddr)
        if (shopPhone.isNotBlank()) line("Tel: $shopPhone")
        line("--------------------------------")

        // Left align
        cmd(0x1B, 0x61, 0x00)
        line("Bill #: ${order.billNumber}")
        line("Date  : ${formatDateTime(order.createdAt?.toDate()?.time ?: System.currentTimeMillis())}")
        line("Cust  : ${order.customerName.take(24)}")
        line("Pay   : ${order.paymentMethod}")
        line("--------------------------------")
        line("Item             Qty      Total")
        line("--------------------------------")
        for (item in order.items) {
            val label = item.nameEnglish.ifBlank { item.nameUrdu }.take(16).padEnd(16)
            val qty = formatQty(item.quantity).take(6).padStart(6)
            val amt = item.lineTotal.toLong().toString().padStart(8)
            line("$label $qty $amt")
        }
        line("--------------------------------")
        if (order.discount > 0) {
            line("Subtotal : Rs. ${order.subtotal.toLong()}")
            line("Discount : -Rs. ${order.discount.toLong()}")
        }
        cmd(0x1B, 0x45, 0x01)
        line("TOTAL    : Rs. ${order.totalAmount.toLong()}")
        cmd(0x1B, 0x45, 0x00)
        line("PAID     : Rs. ${order.paidAmount.toLong()}")
        if (order.remainingUdhar > 0) {
            line("UDHAR    : Rs. ${order.remainingUdhar.toLong()}")
        }
        line("--------------------------------")
        cmd(0x1B, 0x61, 0x01)
        line("Shukriya! Visit Again")
        line("\n\n\n")
        // Paper cut command (partial cut, ignored gracefully on printers without cutter)
        cmd(0x1D, 0x56, 0x41, 0x03)
        return out.toByteArray()
    }

    fun buildPaymentShareText(
        entry: LedgerEntry,
        language: AppLanguage,
        shopPhone: String = "",
        shopNameUrdu: String = SHOP_NAME_URDU,
        shopNameEnglish: String = SHOP_NAME_ENGLISH,
        shopAddressUrdu: String = SHOP_CITY_URDU,
        shopAddressEnglish: String = SHOP_CITY_ENGLISH
    ): String {
        val dateStr = formatDateTime(entry.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val isCredit = entry.entryTypeEnum() == LedgerEntryType.CREDIT
        val nameUr = shopNameUrdu.ifBlank { SHOP_NAME_URDU }
        val nameEn = shopNameEnglish.ifBlank { SHOP_NAME_ENGLISH }
        val addrUr = shopAddressUrdu.ifBlank { SHOP_CITY_URDU }
        val addrEn = shopAddressEnglish.ifBlank { SHOP_CITY_ENGLISH }
        return if (language == AppLanguage.URDU) {
            val typeText = if (isCredit) "وصولی رسید (نقد کھاتہ)" else "ادھار اندراج (کھاتہ)"
            buildString {
                appendLine("============================")
                appendLine("  $nameUr")
                appendLine("  $addrUr")
                if (shopPhone.isNotBlank()) appendLine("  فون: $shopPhone")
                appendLine("============================")
                appendLine(typeText)
                appendLine("کسٹمر: ${entry.customerName}")
                appendLine("رقم: ${formatPkr(entry.amount, AppLanguage.URDU)}")
                appendLine("نیا بقایا ادھار: ${formatPkr(entry.balanceAfter, AppLanguage.URDU)}")
                appendLine("تاریخ: $dateStr")
                if (entry.note.isNotBlank()) appendLine("تفصیل: ${entry.note}")
                appendLine("============================")
                append("شکریہ — $nameUr")
            }
        } else {
            val typeText = if (isCredit) "Khata Payment Receipt (Cash)" else "Khata Udhar Debit Entry"
            buildString {
                appendLine("============================")
                appendLine("  $nameEn")
                appendLine("  $addrEn")
                if (shopPhone.isNotBlank()) appendLine("  Phone: $shopPhone")
                appendLine("============================")
                appendLine(typeText)
                appendLine("Customer: ${entry.customerName}")
                appendLine("Amount: ${formatPkr(entry.amount, AppLanguage.ENGLISH)}")
                appendLine("Updated Balance: ${formatPkr(entry.balanceAfter, AppLanguage.ENGLISH)}")
                appendLine("Date: $dateStr")
                if (entry.note.isNotBlank()) appendLine("Note: ${entry.note}")
                appendLine("============================")
                append("Thank you — $nameEn")
            }
        }
    }

    /**
     * Rich RTL/LTR HTML Khata receipt for Android PrintManager (Wi-Fi / USB / Save as PDF).
     */
    fun buildPaymentPrintableHtml(
        entry: LedgerEntry,
        language: AppLanguage,
        shopPhone: String = "",
        shopNameUrdu: String = SHOP_NAME_URDU,
        shopNameEnglish: String = SHOP_NAME_ENGLISH,
        shopAddressUrdu: String = SHOP_CITY_URDU,
        shopAddressEnglish: String = SHOP_CITY_ENGLISH
    ): String {
        val isUrdu = language == AppLanguage.URDU
        val dir = if (isUrdu) "rtl" else "ltr"
        val isCredit = entry.entryTypeEnum() == LedgerEntryType.CREDIT
        val dateStr = formatDateTime(entry.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val shopTitle = if (isUrdu) shopNameUrdu.ifBlank { SHOP_NAME_URDU } else shopNameEnglish.ifBlank { SHOP_NAME_ENGLISH }
        val shopSub = if (isUrdu) shopAddressUrdu.ifBlank { SHOP_CITY_URDU } else shopAddressEnglish.ifBlank { SHOP_CITY_ENGLISH }
        val badgeText = if (isCredit) {
            if (isUrdu) "کھاتہ وصولی رسید" else "KHATA PAYMENT RECEIPT"
        } else {
            if (isUrdu) "ادھار کھاتہ اندراج" else "KHATA UDHAR ENTRY"
        }
        val accent = if (isCredit) "#0F5132" else "#C81E1E"
        return """
            <!DOCTYPE html>
            <html dir="$dir" lang="${language.code}">
            <head>
            <meta charset="UTF-8" />
            <style>
                body { font-family: sans-serif; color: #111; margin: 16px; max-width: 420px; }
                .header { text-align: center; border-bottom: 2px solid $accent; padding-bottom: 10px; margin-bottom: 12px; }
                .title { font-size: 21px; font-weight: bold; color: #0F5132; margin: 0; }
                .badge { display: inline-block; background: $accent; color: #fff; font-size: 12px; padding: 4px 12px; border-radius: 12px; margin-top: 6px; }
                .sub { font-size: 12px; color: #444; margin: 4px 0 0 0; }
                .box { background: #F7F4EC; padding: 12px; border-radius: 10px; line-height: 1.9; font-size: 14px; }
                .amount { font-size: 22px; font-weight: bold; color: $accent; text-align: center; margin: 14px 0; }
                .footer { text-align: center; margin-top: 16px; font-size: 12px; color: #555; border-top: 1px dashed #999; padding-top: 8px; }
            </style>
            </head>
            <body>
                <div class="header">
                    <p class="title">${escapeHtml(shopTitle)}</p>
                    <p class="sub">${escapeHtml(shopSub)}</p>
                    ${if (shopPhone.isNotBlank()) "<p class='sub'>${escapeHtml(shopPhone)}</p>" else ""}
                    <div class="badge">${escapeHtml(badgeText)}</div>
                </div>
                <div class="amount">${escapeHtml(formatPkr(entry.amount, language))}</div>
                <div class="box">
                    <div><strong>${if (isUrdu) "کسٹمر" else "Customer"}:</strong> ${escapeHtml(entry.customerName)}</div>
                    <div><strong>${if (isUrdu) "تاریخ" else "Date"}:</strong> ${escapeHtml(dateStr)}</div>
                    <div><strong>${if (isUrdu) "نیا بقایا ادھار" else "Updated Balance"}:</strong> ${escapeHtml(formatPkr(entry.balanceAfter, language))}</div>
                    ${if (entry.note.isNotBlank()) "<div><strong>${if (isUrdu) "تفصیل" else "Note"}:</strong> ${escapeHtml(entry.note)}</div>" else ""}
                </div>
                <div class="footer">${if (isUrdu) "شکریہ" else "Thank you"} — ${escapeHtml(shopTitle)}</div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Builds 58mm ESC/POS byte buffer (32 columns) for Khata Payment / Udhar receipts.
     */
    fun buildPaymentEscPosBytes(
        entry: LedgerEntry,
        shopPhone: String,
        shopNameEnglish: String = SHOP_NAME_ENGLISH,
        shopAddressEnglish: String = SHOP_CITY_ENGLISH
    ): ByteArray {
        val out = ByteArrayOutputStream()
        fun cmd(vararg bytes: Int) { bytes.forEach { out.write(it) } }
        fun line(text: String) { out.write("$text\n".toByteArray(Charsets.UTF_8)) }

        val isCredit = entry.entryTypeEnum() == LedgerEntryType.CREDIT
        val cleanName = shopNameEnglish.ifBlank { SHOP_NAME_ENGLISH }.uppercase(Locale.US).take(32)
        val cleanAddr = shopAddressEnglish.ifBlank { SHOP_CITY_ENGLISH }.take(32)

        cmd(0x1B, 0x40)
        cmd(0x1B, 0x61, 0x01)
        cmd(0x1B, 0x45, 0x01)
        line(cleanName)
        cmd(0x1B, 0x45, 0x00)
        line(cleanAddr)
        if (shopPhone.isNotBlank()) line("Tel: $shopPhone")
        line("--------------------------------")
        cmd(0x1B, 0x45, 0x01)
        line(if (isCredit) "KHATA PAYMENT RECEIPT" else "KHATA UDHAR SLIP")
        cmd(0x1B, 0x45, 0x00)
        line("--------------------------------")
        cmd(0x1B, 0x61, 0x00)
        line("Date : ${formatDateTime(entry.createdAt?.toDate()?.time ?: System.currentTimeMillis())}")
        line("Cust : ${entry.customerName.take(25)}")
        line("Type : ${if (isCredit) "CASH RECEIVED" else "UDHAR GIVEN"}")
        if (entry.note.isNotBlank()) line("Note : ${entry.note.take(25)}")
        line("--------------------------------")
        cmd(0x1B, 0x45, 0x01)
        line("AMOUNT  : Rs. ${entry.amount.toLong()}")
        line("BALANCE : Rs. ${entry.balanceAfter.toLong()}")
        cmd(0x1B, 0x45, 0x00)
        line("--------------------------------")
        cmd(0x1B, 0x61, 0x01)
        line("Shukriya! Visit Again")
        line("\n\n\n")
        cmd(0x1D, 0x56, 0x41, 0x03)
        return out.toByteArray()
    }

    fun buildRationCardShareText(
        bill: RationCardBill,
        language: AppLanguage,
        shopPhone: String,
        shopNameUrdu: String = SHOP_NAME_URDU,
        shopNameEnglish: String = SHOP_NAME_ENGLISH,
        shopAddressUrdu: String = SHOP_CITY_URDU,
        shopAddressEnglish: String = SHOP_CITY_ENGLISH
    ): String {
        val dateStr = formatDateTime(bill.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val netLimit = if (bill.netSubsidyLimit > 0.0) bill.netSubsidyLimit else (bill.subsidyLimit - bill.feeDeduction).coerceAtLeast(0.0)
        val nameUr = shopNameUrdu.ifBlank { SHOP_NAME_URDU }
        val nameEn = shopNameEnglish.ifBlank { SHOP_NAME_ENGLISH }
        val addrUr = shopAddressUrdu.ifBlank { SHOP_CITY_URDU }
        val addrEn = shopAddressEnglish.ifBlank { SHOP_CITY_ENGLISH }
        val sb = StringBuilder()
        if (language == AppLanguage.URDU) {
            sb.appendLine("============================")
            sb.appendLine("  $nameUr")
            sb.appendLine("  POS / پی او ایس کارڈ رسید")
            sb.appendLine("  $addrUr")
            if (shopPhone.isNotBlank()) sb.appendLine("  فون: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("رسید نمبر: ${bill.billNumber}")
            sb.appendLine("تاریخ: $dateStr")
            sb.appendLine("صارف کا نام: ${bill.beneficiaryName}")
            if (bill.phone.isNotBlank()) sb.appendLine("موبائل: ${bill.phone}")
            if (bill.cnic.isNotBlank()) sb.appendLine("شناختی کارڈ: ${bill.cnic}")
            if (bill.cardNumber.isNotBlank()) sb.appendLine("کارڈ / BOP نمبر: ${bill.cardNumber}")
            if (bill.cardUid.isNotBlank()) sb.appendLine("NFC UID: ${bill.cardUid}")
            sb.appendLine("کل کارڈ: ${formatPkr(bill.subsidyLimit, AppLanguage.URDU)} | فیس کٹوتی: ${formatPkr(bill.feeDeduction, AppLanguage.URDU)} | خالص راشن: ${formatPkr(netLimit, AppLanguage.URDU)}")
            sb.appendLine("----------------------------")
            bill.items.forEachIndexed { idx, item ->
                sb.appendLine(
                    "${idx + 1}. ${item.nameUrdu} (${formatQty(item.quantity)} ${unitLabel(item.unit, AppLanguage.URDU)}) x ${item.unitPrice.toLong()} = ${formatPkr(item.lineTotal, AppLanguage.URDU)}"
                )
            }
            sb.appendLine("----------------------------")
            sb.appendLine("کل بل: ${formatPkr(bill.totalAmount, AppLanguage.URDU)}")
            sb.appendLine("خالص POS کارڈ سے کٹوتی: ${formatPkr(bill.subsidyUsed, AppLanguage.URDU)}")
            if (bill.cashTopUp > 0) sb.appendLine("اضافی نقد ادائیگی: ${formatPkr(bill.cashTopUp, AppLanguage.URDU)}")
            sb.appendLine("بقایا خالص بیلنس: ${formatPkr(bill.remainingSubsidy, AppLanguage.URDU)}")
            sb.appendLine("============================")
            sb.appendLine("خریداری کا شکریہ! $nameUr")
        } else {
            sb.appendLine("============================")
            sb.appendLine("  $nameEn")
            sb.appendLine("  POS Card Receipt")
            sb.appendLine("  $addrEn")
            if (shopPhone.isNotBlank()) sb.appendLine("  Phone: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("Receipt #: ${bill.billNumber}")
            sb.appendLine("Date: $dateStr")
            sb.appendLine("Customer: ${bill.beneficiaryName}")
            if (bill.phone.isNotBlank()) sb.appendLine("Mobile: ${bill.phone}")
            if (bill.cnic.isNotBlank()) sb.appendLine("CNIC: ${bill.cnic}")
            if (bill.cardNumber.isNotBlank()) sb.appendLine("Card / BOP #: ${bill.cardNumber}")
            if (bill.cardUid.isNotBlank()) sb.appendLine("NFC UID: ${bill.cardUid}")
            sb.appendLine("Card Quota: ${formatPkr(bill.subsidyLimit, AppLanguage.ENGLISH)} | Fee: ${formatPkr(bill.feeDeduction, AppLanguage.ENGLISH)} | Net Ration: ${formatPkr(netLimit, AppLanguage.ENGLISH)}")
            sb.appendLine("----------------------------")
            bill.items.forEachIndexed { idx, item ->
                sb.appendLine(
                    "${idx + 1}. ${item.nameEnglish} (${formatQty(item.quantity)} ${unitLabel(item.unit, AppLanguage.ENGLISH)}) x ${item.unitPrice.toLong()} = ${formatPkr(item.lineTotal, AppLanguage.ENGLISH)}"
                )
            }
            sb.appendLine("----------------------------")
            sb.appendLine("Total Bill: ${formatPkr(bill.totalAmount, AppLanguage.ENGLISH)}")
            sb.appendLine("Net POS Card Used: ${formatPkr(bill.subsidyUsed, AppLanguage.ENGLISH)}")
            if (bill.cashTopUp > 0) sb.appendLine("Extra Cash Paid: ${formatPkr(bill.cashTopUp, AppLanguage.ENGLISH)}")
            sb.appendLine("Remaining Net Balance: ${formatPkr(bill.remainingSubsidy, AppLanguage.ENGLISH)}")
            sb.appendLine("============================")
            sb.appendLine("Thank you for shopping at $nameEn")
        }
        return sb.toString()
    }

    fun buildRationCardPrintableHtml(
        bill: RationCardBill,
        language: AppLanguage,
        shopPhone: String,
        shopNameUrdu: String = SHOP_NAME_URDU,
        shopNameEnglish: String = SHOP_NAME_ENGLISH,
        shopAddressUrdu: String = SHOP_CITY_URDU,
        shopAddressEnglish: String = SHOP_CITY_ENGLISH
    ): String {
        val isUrdu = language == AppLanguage.URDU
        val dir = if (isUrdu) "rtl" else "ltr"
        val dateStr = formatDateTime(bill.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val netLimit = if (bill.netSubsidyLimit > 0.0) bill.netSubsidyLimit else (bill.subsidyLimit - bill.feeDeduction).coerceAtLeast(0.0)
        val rows = bill.items.joinToString("\n") { item ->
            val name = if (isUrdu) item.nameUrdu else item.nameEnglish
            val qtyStr = "${formatQty(item.quantity)} ${unitLabel(item.unit, language)}"
            """
            <tr>
                <td style="padding:6px 4px;border-bottom:1px dashed #ccc;">${escapeHtml(name)}</td>
                <td style="padding:6px 4px;border-bottom:1px dashed #ccc;text-align:center;">${escapeHtml(qtyStr)}</td>
                <td style="padding:6px 4px;border-bottom:1px dashed #ccc;text-align:center;">${item.unitPrice.toLong()}</td>
                <td style="padding:6px 4px;border-bottom:1px dashed #ccc;text-align:end;font-weight:bold;">${item.lineTotal.toLong()}</td>
            </tr>
            """.trimIndent()
        }
        val shopTitle = if (isUrdu) shopNameUrdu.ifBlank { SHOP_NAME_URDU } else shopNameEnglish.ifBlank { SHOP_NAME_ENGLISH }
        val shopSub = if (isUrdu) shopAddressUrdu.ifBlank { SHOP_CITY_URDU } else shopAddressEnglish.ifBlank { SHOP_CITY_ENGLISH }
        val programTitle = if (isUrdu) {
            "POS / پی او ایس رسید (کل ${bill.subsidyLimit.toLong()} | فیس ${bill.feeDeduction.toLong()} | خالص ${netLimit.toLong()})"
        } else {
            "POS Card Receipt (Total ${bill.subsidyLimit.toLong()} | Fee ${bill.feeDeduction.toLong()} | Net ${netLimit.toLong()})"
        }
        return """
            <!DOCTYPE html>
            <html dir="$dir" lang="${language.code}">
            <head>
            <meta charset="UTF-8" />
            <style>
                body { font-family: sans-serif; color: #111; margin: 16px; max-width: 420px; }
                .header { text-align: center; border-bottom: 2px solid #084C36; padding-bottom: 10px; margin-bottom: 10px; }
                .title { font-size: 21px; font-weight: bold; color: #084C36; margin: 0; }
                .badge { display: inline-block; background: #C88A14; color: #fff; font-size: 12px; padding: 3px 10px; border-radius: 12px; margin-top: 6px; }
                .sub { font-size: 12px; color: #444; margin: 4px 0 0 0; }
                .meta { font-size: 13px; margin-bottom: 10px; line-height: 1.7; background: #F7F4EC; padding: 8px 10px; border-radius: 8px; }
                table { width: 100%; border-collapse: collapse; font-size: 13px; }
                th { background: #E6F4EC; padding: 7px 4px; text-align: start; border-bottom: 1px solid #084C36; }
                .totals { margin-top: 12px; border-top: 2px solid #084C36; padding-top: 8px; font-size: 14px; }
                .row { display: flex; justify-content: space-between; margin: 4px 0; }
                .grand { font-size: 16px; font-weight: bold; color: #084C36; }
                .footer { text-align: center; margin-top: 16px; font-size: 11px; color: #555; border-top: 1px dashed #999; padding-top: 8px; }
            </style>
            </head>
            <body>
                <div class="header">
                    <p class="title">${escapeHtml(shopTitle)}</p>
                    <div class="badge">${escapeHtml(programTitle)}</div>
                    <p class="sub">${escapeHtml(shopSub)}</p>
                    ${if (shopPhone.isNotBlank()) "<p class='sub'>${escapeHtml(shopPhone)}</p>" else ""}
                </div>
                <div class="meta">
                    <div><strong>${if (isUrdu) "رسید نمبر" else "Receipt #"}:</strong> ${escapeHtml(bill.billNumber)} (${escapeHtml(bill.monthKey)})</div>
                    <div><strong>${if (isUrdu) "تاریخ" else "Date"}:</strong> ${escapeHtml(dateStr)}</div>
                    <div><strong>${if (isUrdu) "صارف" else "Customer"}:</strong> ${escapeHtml(bill.beneficiaryName)}</div>
                    ${if (bill.phone.isNotBlank()) "<div><strong>${if (isUrdu) "موبائل" else "Phone"}:</strong> ${escapeHtml(bill.phone)}</div>" else ""}
                    ${if (bill.cnic.isNotBlank()) "<div><strong>CNIC:</strong> ${escapeHtml(bill.cnic)}</div>" else ""}
                    ${if (bill.cardNumber.isNotBlank()) "<div><strong>${if (isUrdu) "کارڈ / BOP نمبر" else "Card / BOP #"}:</strong> ${escapeHtml(bill.cardNumber)}</div>" else ""}
                    ${if (bill.cardUid.isNotBlank()) "<div><strong>NFC UID:</strong> ${escapeHtml(bill.cardUid)}</div>" else ""}
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>${if (isUrdu) "اشیاء" else "Item"}</th>
                            <th style="text-align:center;">${if (isUrdu) "مقدار" else "Qty"}</th>
                            <th style="text-align:center;">${if (isUrdu) "ریٹ" else "Rate"}</th>
                            <th style="text-align:end;">${if (isUrdu) "رقم" else "Total"}</th>
                        </tr>
                    </thead>
                    <tbody>$rows</tbody>
                </table>
                <div class="totals">
                    <div class="row"><span>${if (isUrdu) "کل کارڈ کوٹہ" else "Total Card Quota"}</span><span>${escapeHtml(formatPkr(bill.subsidyLimit, language))}</span></div>
                    <div class="row"><span>${if (isUrdu) "سروس فیس کٹوتی" else "Service Fee Deduction"}</span><span>-${escapeHtml(formatPkr(bill.feeDeduction, language))}</span></div>
                    <div class="row"><span>${if (isUrdu) "خالص قابلِ استعمال راشن" else "Net Ration Value"}</span><span>${escapeHtml(formatPkr(netLimit, language))}</span></div>
                    <div class="row grand"><span>${if (isUrdu) "کل راشن بل" else "Total POS Bill"}</span><span>${escapeHtml(formatPkr(bill.totalAmount, language))}</span></div>
                    <div class="row"><span>${if (isUrdu) "کارڈ سے کٹوتی" else "POS Card Used"}</span><span>${escapeHtml(formatPkr(bill.subsidyUsed, language))}</span></div>
                    ${if (bill.cashTopUp > 0) "<div class='row'><span>${if (isUrdu) "اضافی نقد ادائیگی" else "Extra Cash Paid"}</span><span>${escapeHtml(formatPkr(bill.cashTopUp, language))}</span></div>" else ""}
                    <div class="row"><span>${if (isUrdu) "بقایا خالص بیلنس" else "Remaining Net Balance"}</span><span>${escapeHtml(formatPkr(bill.remainingSubsidy, language))}</span></div>
                </div>
                <div class="footer">${escapeHtml(shopTitle)} • ${if (isUrdu) "رجسٹرڈ POS کریانہ مرچنٹ" else "Registered POS Kiryana Merchant"}</div>
            </body>
            </html>
        """.trimIndent()
    }

    fun buildRationCardEscPosBytes(
        bill: RationCardBill,
        shopPhone: String,
        shopNameEnglish: String = SHOP_NAME_ENGLISH,
        shopAddressEnglish: String = SHOP_CITY_ENGLISH
    ): ByteArray {
        val out = ByteArrayOutputStream()
        fun cmd(vararg bytes: Int) { bytes.forEach { out.write(it) } }
        fun line(text: String) { out.write("$text\n".toByteArray(Charsets.UTF_8)) }
        val netLimit = if (bill.netSubsidyLimit > 0.0) bill.netSubsidyLimit else (bill.subsidyLimit - bill.feeDeduction).coerceAtLeast(0.0)
        val cleanName = shopNameEnglish.ifBlank { SHOP_NAME_ENGLISH }.uppercase(Locale.US).take(32)
        val cleanAddr = shopAddressEnglish.ifBlank { SHOP_CITY_ENGLISH }.take(32)

        cmd(0x1B, 0x40)
        cmd(0x1B, 0x61, 0x01)
        cmd(0x1B, 0x45, 0x01)
        line(cleanName)
        cmd(0x1B, 0x45, 0x00)
        line(cleanAddr)
        line("POS CARD RECEIPT")
        line("Card:${bill.subsidyLimit.toLong()} Fee:${bill.feeDeduction.toLong()} Net:${netLimit.toLong()}")
        if (shopPhone.isNotBlank()) line("Tel: $shopPhone")
        line("--------------------------------")
        cmd(0x1B, 0x61, 0x00)
        line("Bill # : ${bill.billNumber}")
        line("Month  : ${bill.monthKey}")
        line("Date   : ${formatDateTime(bill.createdAt?.toDate()?.time ?: System.currentTimeMillis())}")
        line("Name   : ${bill.beneficiaryName.take(23)}")
        if (bill.phone.isNotBlank()) line("Phone  : ${bill.phone}")
        if (bill.cnic.isNotBlank()) line("CNIC   : ${bill.cnic}")
        if (bill.cardNumber.isNotBlank()) line("Card # : ${bill.cardNumber}")
        if (bill.cardUid.isNotBlank()) line("NFC ID : ${bill.cardUid}")
        line("--------------------------------")
        line("Item             Qty      Total")
        line("--------------------------------")
        for (item in bill.items) {
            val label = item.nameEnglish.ifBlank { item.nameUrdu }.take(16).padEnd(16)
            val qty = formatQty(item.quantity).take(6).padStart(6)
            val amt = item.lineTotal.toLong().toString().padStart(8)
            line("$label $qty $amt")
        }
        line("--------------------------------")
        line("CARD QUOTA : Rs. ${bill.subsidyLimit.toLong()}")
        line("FEE DEDUCT : Rs. ${bill.feeDeduction.toLong()}")
        line("NET RATION : Rs. ${netLimit.toLong()}")
        cmd(0x1B, 0x45, 0x01)
        line("TOTAL BILL : Rs. ${bill.totalAmount.toLong()}")
        line("CARD USED  : Rs. ${bill.subsidyUsed.toLong()}")
        cmd(0x1B, 0x45, 0x00)
        if (bill.cashTopUp > 0) line("EXTRA CASH : Rs. ${bill.cashTopUp.toLong()}")
        line("REMAINING  : Rs. ${bill.remainingSubsidy.toLong()}")
        line("--------------------------------")
        cmd(0x1B, 0x61, 0x01)
        line("Shukriya! Visit Again")
        line("\n\n\n")
        cmd(0x1D, 0x56, 0x41, 0x03)
        return out.toByteArray()
    }

    fun buildWholesalerEntryShareText(
        wholesaler: Wholesaler,
        entry: WholesalerLedgerEntry,
        language: AppLanguage,
        shopPhone: String
    ): String {
        val dateStr = formatDateTime(entry.entryDateMs)
        val isStockIn = entry.entryTypeEnum() == WholesalerEntryType.STOCK_IN
        val sourceText = when (entry.paymentSourceEnum()) {
            DailyPaymentSource.CASH -> if (language == AppLanguage.URDU) "نقد (Cash)" else "Cash"
            DailyPaymentSource.HBL_QR -> if (language == AppLanguage.URDU) "HBL QR / بینک" else "HBL QR / Bank"
            DailyPaymentSource.ONLINE -> if (language == AppLanguage.URDU) "آن لائن ٹرانسفر" else "Online Transfer"
        }
        val sb = StringBuilder()
        if (language == AppLanguage.URDU) {
            sb.appendLine("============================")
            sb.appendLine("  $SHOP_NAME_URDU")
            sb.appendLine("  ہول سیلر / سپلائر کھاتہ رسید")
            if (shopPhone.isNotBlank()) sb.appendLine("  فون: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("ہول سیلر: ${wholesaler.name}")
            if (wholesaler.cityOrMarket.isNotBlank()) sb.appendLine("شہر / مارکیٹ: ${wholesaler.cityOrMarket}")
            sb.appendLine("تاریخ: $dateStr")
            sb.appendLine("قسم: ${if (isStockIn) "سامان خریداری / بل جمع (Stock In)" else "رقم ادائیگی / کیش آؤٹ (Payment Paid)"}")
            if (entry.invoiceNumber.isNotBlank()) sb.appendLine("بل / انوائس نمبر: ${entry.invoiceNumber}")
            if (!isStockIn) sb.appendLine("ذریعہ ادائیگی: $sourceText")
            if (entry.referenceId.isNotBlank()) sb.appendLine("TID / حوالہ: ${entry.referenceId}")
            if (entry.description.isNotBlank()) sb.appendLine("تفصیل: ${entry.description}")
            sb.appendLine("----------------------------")
            sb.appendLine("رقم: ${formatPkr(entry.amount, AppLanguage.URDU)}")
            sb.appendLine("واجب الادا بقایا بیلنس: ${formatPkr(entry.balanceAfter, AppLanguage.URDU)}")
            sb.appendLine("============================")
            sb.appendLine("شکریہ — $SHOP_NAME_URDU")
        } else {
            sb.appendLine("============================")
            sb.appendLine("  $SHOP_NAME_ENGLISH")
            sb.appendLine("  Wholesaler / Supplier Ledger Receipt")
            if (shopPhone.isNotBlank()) sb.appendLine("  Phone: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("Wholesaler: ${wholesaler.name}")
            if (wholesaler.cityOrMarket.isNotBlank()) sb.appendLine("Market/City: ${wholesaler.cityOrMarket}")
            sb.appendLine("Date: $dateStr")
            sb.appendLine("Type: ${if (isStockIn) "Stock Purchased (In)" else "Payment Paid (Out)"}")
            if (entry.invoiceNumber.isNotBlank()) sb.appendLine("Invoice #: ${entry.invoiceNumber}")
            if (!isStockIn) sb.appendLine("Method: $sourceText")
            if (entry.referenceId.isNotBlank()) sb.appendLine("TID / Ref: ${entry.referenceId}")
            if (entry.description.isNotBlank()) sb.appendLine("Details: ${entry.description}")
            sb.appendLine("----------------------------")
            sb.appendLine("Amount: ${formatPkr(entry.amount, AppLanguage.ENGLISH)}")
            sb.appendLine("Remaining Payable Balance: ${formatPkr(entry.balanceAfter, AppLanguage.ENGLISH)}")
            sb.appendLine("============================")
            sb.appendLine("Thank you — $SHOP_NAME_ENGLISH")
        }
        return sb.toString()
    }

    fun buildWholesalerSummaryShareText(
        wholesaler: Wholesaler,
        language: AppLanguage,
        shopPhone: String
    ): String {
        val dateStr = formatDateTime()
        return if (language == AppLanguage.URDU) {
            """
            ============================
            $SHOP_NAME_URDU
            ہول سیلر / سپلائر کھاتہ خلاصہ
            ${if (shopPhone.isNotBlank()) "فون: $shopPhone" else ""}
            ============================
            ہول سیلر: ${wholesaler.name}
            ${if (wholesaler.cityOrMarket.isNotBlank()) "شہر / مارکیٹ: ${wholesaler.cityOrMarket}" else ""}
            تاریخ: $dateStr
            ----------------------------
            کل سامان خریداری: ${formatPkr(wholesaler.totalPurchased, AppLanguage.URDU)}
            کل ادائیگی: ${formatPkr(wholesaler.totalPaid, AppLanguage.URDU)}
            واجب الادا بقایا رقم: ${formatPkr(wholesaler.payableBalance, AppLanguage.URDU)}
            ============================
            شکریہ — $SHOP_NAME_URDU
            """.trimIndent()
        } else {
            """
            ============================
            $SHOP_NAME_ENGLISH
            Wholesaler / Supplier Account Summary
            ${if (shopPhone.isNotBlank()) "Phone: $shopPhone" else ""}
            ============================
            Wholesaler: ${wholesaler.name}
            ${if (wholesaler.cityOrMarket.isNotBlank()) "Market/City: ${wholesaler.cityOrMarket}" else ""}
            Date: $dateStr
            ----------------------------
            Total Stock Purchased: ${formatPkr(wholesaler.totalPurchased, AppLanguage.ENGLISH)}
            Total Paid: ${formatPkr(wholesaler.totalPaid, AppLanguage.ENGLISH)}
            Remaining Payable Balance: ${formatPkr(wholesaler.payableBalance, AppLanguage.ENGLISH)}
            ============================
            Thank you — $SHOP_NAME_ENGLISH
            """.trimIndent()
        }
    }

    fun buildRoznamchaStatementShareText(
        periodLabel: String,
        entries: List<DailyCashEntry>,
        totalIn: Double,
        totalOut: Double,
        netBalance: Double,
        language: AppLanguage,
        shopPhone: String,
        shopNameUrdu: String = SHOP_NAME_URDU,
        shopNameEnglish: String = SHOP_NAME_ENGLISH,
        shopAddressUrdu: String = SHOP_CITY_URDU,
        shopAddressEnglish: String = SHOP_CITY_ENGLISH
    ): String {
        val nameUr = shopNameUrdu.ifBlank { SHOP_NAME_URDU }
        val nameEn = shopNameEnglish.ifBlank { SHOP_NAME_ENGLISH }
        val addrUr = shopAddressUrdu.ifBlank { SHOP_CITY_URDU }
        val addrEn = shopAddressEnglish.ifBlank { SHOP_CITY_ENGLISH }
        val generatedStr = formatDateTime()
        val sb = StringBuilder()
        if (language == AppLanguage.URDU) {
            sb.appendLine("============================")
            sb.appendLine("  $nameUr")
            sb.appendLine("  روزنامچہ — دستی سیل اور اخراجات")
            sb.appendLine("  $addrUr")
            if (shopPhone.isNotBlank()) sb.appendLine("  فون: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("مدت: $periodLabel")
            sb.appendLine("تیاری وقت: $generatedStr")
            sb.appendLine("----------------------------")
            sb.appendLine("کل دستی کیش اِن (آمدن/سیل): +${formatPkr(totalIn, AppLanguage.URDU)}")
            sb.appendLine("کل کیش آؤٹ (اخراجات/خریداری): -${formatPkr(totalOut, AppLanguage.URDU)}")
            sb.appendLine("خالص روزنامچہ بیلنس (بچت): ${formatPkr(netBalance, AppLanguage.URDU)}")
            sb.appendLine("----------------------------")
            if (entries.isEmpty()) {
                sb.appendLine("اس مدت میں کوئی انٹری موجود نہیں۔")
            } else {
                entries.forEachIndexed { idx, entry ->
                    val isIn = entry.entryTypeEnum() == DailyCashType.CASH_IN
                    val sign = if (isIn) "+" else "-"
                    val mode = when (entry.paymentSourceEnum()) {
                        DailyPaymentSource.CASH -> "نقد"
                        DailyPaymentSource.HBL_QR -> "بینک QR"
                        DailyPaymentSource.ONLINE -> "آن لائن"
                    }
                    val dateLabel = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(entry.entryDateMs))
                    val title = entry.categoryOrTitle.ifBlank { if (isIn) "دستی سیل" else "اخراجات" }
                    sb.appendLine("${idx + 1}. [$dateLabel] $title ($mode) : $sign${formatPkr(entry.amount, AppLanguage.URDU)}")
                    if (entry.note.isNotBlank()) sb.appendLine("   نوٹ: ${entry.note}")
                }
            }
            sb.appendLine("============================")
            sb.appendLine("شکریہ — $nameUr")
        } else {
            sb.appendLine("============================")
            sb.appendLine("  $nameEn")
            sb.appendLine("  Daily Roznamcha (Manual Cashbook)")
            sb.appendLine("  $addrEn")
            if (shopPhone.isNotBlank()) sb.appendLine("  Phone: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("Period: $periodLabel")
            sb.appendLine("Generated: $generatedStr")
            sb.appendLine("----------------------------")
            sb.appendLine("Total Cash In (Sales/Income): +${formatPkr(totalIn, AppLanguage.ENGLISH)}")
            sb.appendLine("Total Cash Out (Expenses): -${formatPkr(totalOut, AppLanguage.ENGLISH)}")
            sb.appendLine("Net Cash Balance: ${formatPkr(netBalance, AppLanguage.ENGLISH)}")
            sb.appendLine("----------------------------")
            if (entries.isEmpty()) {
                sb.appendLine("No entries in this period.")
            } else {
                entries.forEachIndexed { idx, entry ->
                    val isIn = entry.entryTypeEnum() == DailyCashType.CASH_IN
                    val sign = if (isIn) "+" else "-"
                    val mode = when (entry.paymentSourceEnum()) {
                        DailyPaymentSource.CASH -> "Cash"
                        DailyPaymentSource.HBL_QR -> "Bank QR"
                        DailyPaymentSource.ONLINE -> "Online"
                    }
                    val dateLabel = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(entry.entryDateMs))
                    val title = entry.categoryOrTitle.ifBlank { if (isIn) "Manual Sale" else "Expense" }
                    sb.appendLine("${idx + 1}. [$dateLabel] $title ($mode) : $sign${formatPkr(entry.amount, AppLanguage.ENGLISH)}")
                    if (entry.note.isNotBlank()) sb.appendLine("   Note: ${entry.note}")
                }
            }
            sb.appendLine("============================")
            sb.appendLine("Thank you — $nameEn")
        }
        return sb.toString()
    }

    fun buildLowStockReportShareText(
        lowStockProducts: List<Product>,
        language: AppLanguage,
        shopPhone: String,
        shopNameUrdu: String = SHOP_NAME_URDU,
        shopNameEnglish: String = SHOP_NAME_ENGLISH,
        shopAddressUrdu: String = SHOP_CITY_URDU,
        shopAddressEnglish: String = SHOP_CITY_ENGLISH
    ): String {
        val nameUr = shopNameUrdu.ifBlank { SHOP_NAME_URDU }
        val nameEn = shopNameEnglish.ifBlank { SHOP_NAME_ENGLISH }
        val addrUr = shopAddressUrdu.ifBlank { SHOP_CITY_URDU }
        val addrEn = shopAddressEnglish.ifBlank { SHOP_CITY_ENGLISH }
        val generatedStr = formatDateTime()
        val sorted = lowStockProducts.sortedWith(
            compareBy<Product> { if (it.isOutOfStock()) 0 else 1 }
                .thenBy { if (it.lowStockThreshold > 0.0) it.stockQty / it.lowStockThreshold else 0.0 }
        )
        val outCount = sorted.count { it.isOutOfStock() }
        val totalEstCost = sorted.sumOf { it.estimatedRestockCost() }

        val sb = StringBuilder()
        if (language == AppLanguage.URDU) {
            sb.appendLine("============================")
            sb.appendLine("  $nameUr")
            sb.appendLine("  کم اسٹاک / ری اسٹاک آرڈر رپورٹ")
            sb.appendLine("  $addrUr")
            if (shopPhone.isNotBlank()) sb.appendLine("  فون: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("تاریخ: $generatedStr")
            sb.appendLine("کم اسٹاک اشیاء: ${sorted.size} (ختم شدہ: $outCount)")
            sb.appendLine("اندازاً خریداری لاگت: ${formatPkr(totalEstCost, AppLanguage.URDU)}")
            sb.appendLine("----------------------------")
            if (sorted.isEmpty()) {
                sb.appendLine("تمام اشیاء کا اسٹاک محفوظ حد سے اوپر ہے۔")
            } else {
                sorted.forEachIndexed { idx, prod ->
                    val statusTag = if (prod.isOutOfStock()) "[ختم]" else "[کم]"
                    val unitTxt = unitLabel(prod.unit, AppLanguage.URDU)
                    val reorderQty = prod.suggestedRestockQty()
                    val estCost = prod.estimatedRestockCost()
                    sb.appendLine(
                        "${idx + 1}. $statusTag ${prod.nameUrdu.ifBlank { prod.nameEnglish }}"
                    )
                    sb.appendLine(
                        "   موجودہ: ${formatQty(prod.stockQty)} $unitTxt | کم از کم حد: ${formatQty(prod.lowStockThreshold)} $unitTxt"
                    )
                    sb.appendLine(
                        "   آرڈر مقدار: +${formatQty(reorderQty)} $unitTxt | اندازاً لاگت: ${formatPkr(estCost, AppLanguage.URDU)}"
                    )
                }
            }
            sb.appendLine("============================")
            sb.appendLine("کل اندازاً ری اسٹاک لاگت: ${formatPkr(totalEstCost, AppLanguage.URDU)}")
        } else {
            sb.appendLine("============================")
            sb.appendLine("  $nameEn")
            sb.appendLine("  LOW STOCK REPLENISHMENT REPORT")
            sb.appendLine("  $addrEn")
            if (shopPhone.isNotBlank()) sb.appendLine("  Phone: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("Generated: $generatedStr")
            sb.appendLine("Low Stock Items: ${sorted.size} (Out of Stock: $outCount)")
            sb.appendLine("Est. Replenishment Cost: ${formatPkr(totalEstCost, AppLanguage.ENGLISH)}")
            sb.appendLine("----------------------------")
            if (sorted.isEmpty()) {
                sb.appendLine("All products are safely above their minimum stock threshold.")
            } else {
                sorted.forEachIndexed { idx, prod ->
                    val statusTag = if (prod.isOutOfStock()) "[OUT]" else "[LOW]"
                    val unitTxt = unitLabel(prod.unit, AppLanguage.ENGLISH)
                    val reorderQty = prod.suggestedRestockQty()
                    val estCost = prod.estimatedRestockCost()
                    sb.appendLine(
                        "${idx + 1}. $statusTag ${prod.nameEnglish.ifBlank { prod.nameUrdu }}"
                    )
                    sb.appendLine(
                        "   In Stock: ${formatQty(prod.stockQty)} $unitTxt | Min Limit: ${formatQty(prod.lowStockThreshold)} $unitTxt"
                    )
                    sb.appendLine(
                        "   Reorder Qty: +${formatQty(reorderQty)} $unitTxt | Est. Cost: ${formatPkr(estCost, AppLanguage.ENGLISH)}"
                    )
                }
            }
            sb.appendLine("============================")
            sb.appendLine("TOTAL EST. REORDER COST: ${formatPkr(totalEstCost, AppLanguage.ENGLISH)}")
        }
        return sb.toString()
    }

    fun buildLowStockReportPrintableHtml(
        lowStockProducts: List<Product>,
        language: AppLanguage,
        shopPhone: String,
        shopNameUrdu: String = SHOP_NAME_URDU,
        shopNameEnglish: String = SHOP_NAME_ENGLISH,
        shopAddressUrdu: String = SHOP_CITY_URDU,
        shopAddressEnglish: String = SHOP_CITY_ENGLISH
    ): String {
        val isUrdu = language == AppLanguage.URDU
        val dir = if (isUrdu) "rtl" else "ltr"
        val shopTitle = if (isUrdu) shopNameUrdu.ifBlank { SHOP_NAME_URDU } else shopNameEnglish.ifBlank { SHOP_NAME_ENGLISH }
        val shopAddress = if (isUrdu) shopAddressUrdu.ifBlank { SHOP_CITY_URDU } else shopAddressEnglish.ifBlank { SHOP_CITY_ENGLISH }
        val generatedStr = formatDateTime()
        val sorted = lowStockProducts.sortedWith(
            compareBy<Product> { if (it.isOutOfStock()) 0 else 1 }
                .thenBy { if (it.lowStockThreshold > 0.0) it.stockQty / it.lowStockThreshold else 0.0 }
        )
        val outCount = sorted.count { it.isOutOfStock() }
        val totalEstCost = sorted.sumOf { it.estimatedRestockCost() }

        val rows = sorted.mapIndexed { idx, prod ->
            val unitTxt = unitLabel(prod.unit, language)
            val rowBg = if (prod.isOutOfStock()) "#FEE2E2" else "#FEF3C7"
            val statusTxt = if (prod.isOutOfStock()) {
                if (isUrdu) "ختم" else "OUT"
            } else {
                if (isUrdu) "کم" else "LOW"
            }
            val titleHtml = buildString {
                append("<strong>${escapeHtml(prod.nameUrdu.ifBlank { prod.nameEnglish })}</strong>")
                if (prod.nameEnglish.isNotBlank() && prod.nameEnglish != prod.nameUrdu) {
                    append("<div style='font-size:11px;color:#555;'>${escapeHtml(prod.nameEnglish)}</div>")
                }
            }
            """
            <tr style="background:$rowBg;">
                <td style="text-align:center;">${idx + 1}</td>
                <td>$titleHtml</td>
                <td style="text-align:center;font-weight:bold;color:#B91C1C;">${escapeHtml(formatQty(prod.stockQty))} $unitTxt<div style="font-size:10px;">$statusTxt</div></td>
                <td style="text-align:center;">${escapeHtml(formatQty(prod.lowStockThreshold))} $unitTxt</td>
                <td style="text-align:center;font-weight:bold;color:#0F5132;">+${escapeHtml(formatQty(prod.suggestedRestockQty()))} $unitTxt</td>
                <td style="text-align:end;">${escapeHtml(formatPkr(prod.costPrice, language))}</td>
                <td style="text-align:end;font-weight:bold;">${escapeHtml(formatPkr(prod.estimatedRestockCost(), language))}</td>
            </tr>
            """.trimIndent()
        }.joinToString("\n")

        return """
            <!DOCTYPE html>
            <html dir="$dir">
            <head>
                <meta charset="UTF-8" />
                <style>
                    body { font-family: sans-serif; color: #1C1917; padding: 18px; }
                    .header { text-align: center; border-bottom: 2px solid #0F5132; padding-bottom: 10px; margin-bottom: 12px; }
                    .header h1 { margin: 0; font-size: 20px; color: #0F5132; }
                    .header p { margin: 3px 0; font-size: 12px; color: #57534E; }
                    .badge { display: inline-block; margin-top: 6px; padding: 4px 12px; border-radius: 999px; background: #B91C1C; color: #FFF; font-size: 12px; font-weight: bold; }
                    .summary { display: flex; justify-content: space-between; gap: 10px; margin: 12px 0; }
                    .kpi { flex: 1; border: 1px solid #D6D3D1; border-radius: 8px; padding: 8px 10px; background: #FAFAF9; text-align: center; }
                    .kpi-label { font-size: 11px; color: #57534E; }
                    .kpi-val { font-size: 16px; font-weight: bold; color: #0F5132; margin-top: 2px; }
                    table { width: 100%; border-collapse: collapse; font-size: 12px; margin-top: 8px; }
                    th { background: #0F5132; color: #FFF; padding: 7px 6px; border: 1px solid #0F5132; }
                    td { padding: 7px 6px; border: 1px solid #D6D3D1; }
                    .total-row { margin-top: 12px; padding: 10px; background: #ECFDF5; border: 1px solid #0F5132; border-radius: 8px; display: flex; justify-content: space-between; font-size: 15px; font-weight: bold; color: #0F5132; }
                    .footer { text-align: center; margin-top: 16px; font-size: 11px; color: #78716C; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>${escapeHtml(shopTitle)}</h1>
                    <p>${escapeHtml(shopAddress)}${if (shopPhone.isNotBlank()) " • ${escapeHtml(shopPhone)}" else ""}</p>
                    <p>${if (isUrdu) "رپورٹ کی تاریخ" else "Generated"}: ${escapeHtml(generatedStr)}</p>
                    <div class="badge">${if (isUrdu) "کم اسٹاک ری اسٹاک رپورٹ (Low Stock Report)" else "LOW STOCK REPLENISHMENT REPORT"}</div>
                </div>
                <div class="summary">
                    <div class="kpi"><div class="kpi-label">${if (isUrdu) "کل کم اسٹاک اشیاء" else "Low Stock Items"}</div><div class="kpi-val" style="color:#B45309;">${sorted.size}</div></div>
                    <div class="kpi"><div class="kpi-label">${if (isUrdu) "مکمل ختم اشیاء" else "Out of Stock"}</div><div class="kpi-val" style="color:#B91C1C;">$outCount</div></div>
                    <div class="kpi"><div class="kpi-label">${if (isUrdu) "اندازاً خریداری لاگت" else "Est. Replenish Cost"}</div><div class="kpi-val">${escapeHtml(formatPkr(totalEstCost, language))}</div></div>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>${if (isUrdu) "پروڈکٹ / آئٹم" else "Product"}</th>
                            <th>${if (isUrdu) "موجودہ اسٹاک" else "In Stock"}</th>
                            <th>${if (isUrdu) "کم از کم حد" else "Min Threshold"}</th>
                            <th>${if (isUrdu) "تجویز کردہ آرڈر" else "Reorder Qty"}</th>
                            <th>${if (isUrdu) "خرید ریٹ" else "Unit Cost"}</th>
                            <th>${if (isUrdu) "اندازاً لاگت" else "Est. Cost"}</th>
                        </tr>
                    </thead>
                    <tbody>$rows</tbody>
                </table>
                <div class="total-row">
                    <span>${if (isUrdu) "کل اندازاً ری اسٹاک لاگت" else "Total Estimated Replenishment Cost"}</span>
                    <span>${escapeHtml(formatPkr(totalEstCost, language))}</span>
                </div>
                <div class="footer">${escapeHtml(shopTitle)} • ${if (isUrdu) "خودکار کم اسٹاک انوینٹری رپورٹ" else "Automated Low Stock Replenishment Report"}</div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeHtml(input: String): String =
        input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
