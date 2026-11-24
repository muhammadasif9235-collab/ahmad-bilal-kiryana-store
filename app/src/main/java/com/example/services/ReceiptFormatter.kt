package com.example.services

import com.example.data.LedgerEntry
import com.example.data.LedgerEntryType
import com.example.data.Order
import com.example.data.RationCardBill
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
    const val SHOP_CITY_URDU = "محلہ محمد نگر، ٹانڈہ، ضلع گجرات"
    const val SHOP_CITY_ENGLISH = "Mohalla Muhammad Nagar, Tanda, District Gujrat"

    fun formatDateTime(epochMillis: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(Date(epochMillis))
    }

    fun buildShareText(order: Order, language: AppLanguage, shopPhone: String): String {
        val dateStr = formatDateTime(order.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val sb = StringBuilder()
        if (language == AppLanguage.URDU) {
            sb.appendLine("============================")
            sb.appendLine("    $SHOP_NAME_URDU")
            sb.appendLine("    $SHOP_CITY_URDU")
            if (shopPhone.isNotBlank()) sb.appendLine("    فون: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("بل نمبر: ${order.billNumber}")
            sb.appendLine("تاریخ: $dateStr")
            sb.appendLine("خریدار: ${order.customerName}")
            sb.appendLine("ادائیگی: ${order.paymentMethodEnum().label(AppLanguage.URDU)}")
            sb.appendLine("----------------------------")
            order.items.forEachIndexed { idx, item ->
                sb.appendLine("${idx + 1}. ${item.nameUrdu} (${formatQty(item.quantity)} ${unitLabel(item.unit, AppLanguage.URDU)}) x ${item.unitPrice.toLong()} = ${formatPkr(item.lineTotal, AppLanguage.URDU)}")
            }
            sb.appendLine("----------------------------")
            if (order.discount > 0) {
                sb.appendLine("کل رقم: ${formatPkr(order.subtotal, AppLanguage.URDU)}")
                sb.appendLine("رعایت: -${formatPkr(order.discount, AppLanguage.URDU)}")
            }
            sb.appendLine("میزان / کل رقم: ${formatPkr(order.totalAmount, AppLanguage.URDU)}")
            sb.appendLine("وصول شدہ: ${formatPkr(order.paidAmount, AppLanguage.URDU)}")
            if (order.remainingUdhar > 0) {
                sb.appendLine("بقایا ادھار: ${formatPkr(order.remainingUdhar, AppLanguage.URDU)}")
            }
            sb.appendLine("============================")
            sb.appendLine("خریداری کا شکریہ! دوبارہ تشریف لائیں")
        } else {
            sb.appendLine("============================")
            sb.appendLine("  $SHOP_NAME_ENGLISH")
            sb.appendLine("  $SHOP_CITY_ENGLISH")
            if (shopPhone.isNotBlank()) sb.appendLine("  Phone: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("Bill #: ${order.billNumber}")
            sb.appendLine("Date: $dateStr")
            sb.appendLine("Customer: ${order.customerName}")
            sb.appendLine("Payment: ${order.paymentMethodEnum().label(AppLanguage.ENGLISH)}")
            sb.appendLine("----------------------------")
            order.items.forEachIndexed { idx, item ->
                sb.appendLine("${idx + 1}. ${item.nameEnglish} (${formatQty(item.quantity)} ${unitLabel(item.unit, AppLanguage.ENGLISH)}) x ${item.unitPrice.toLong()} = ${formatPkr(item.lineTotal, AppLanguage.ENGLISH)}")
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
     * Royal Emerald & Gold Cash Memo HTML (True RTL Urdu: Sr# | Qty | Item | Rate | Amount)
     */
    fun buildPrintableHtml(order: Order, language: AppLanguage, shopPhone: String): String {
        val isUrdu = language == AppLanguage.URDU
        val dir = if (isUrdu) "rtl" else "ltr"
        val dateStr = formatDateTime(order.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val totalRows = maxOf(15, order.items.size)
        val rowsHtml = (0 until totalRows).joinToString("\n") { idx ->
            val item = order.items.getOrNull(idx)
            val bg = if (idx % 2 == 1) "#F4F9F6" else "#FFFFFF"
            if (item != null) {
                val name = if (isUrdu) item.nameUrdu.ifBlank { item.nameEnglish } else item.nameEnglish.ifBlank { item.nameUrdu }
                val qtyStr = "${formatQty(item.quantity)} ${unitLabel(item.unit, language)}"
                """
                <tr style="background:$bg;">
                    <td class="c-sr">${idx + 1}</td>
                    <td class="c-qty">${escapeHtml(qtyStr)}</td>
                    <td class="c-item">${escapeHtml(name)}</td>
                    <td class="c-rate">${item.unitPrice.toLong()}</td>
                    <td class="c-amt">${item.lineTotal.toLong()}/-</td>
                </tr>
                """.trimIndent()
            } else {
                """
                <tr style="background:$bg;">
                    <td class="c-sr">${idx + 1}</td>
                    <td class="c-qty">&nbsp;</td>
                    <td class="c-item">&nbsp;</td>
                    <td class="c-rate">&nbsp;</td>
                    <td class="c-amt">&nbsp;</td>
                </tr>
                """.trimIndent()
            }
        }

        val cleanCust = order.customerName.trim()
        val isWalkIn = cleanCust.isBlank() || cleanCust == "نقد کسٹمر" || cleanCust.equals("Walk-in Customer", ignoreCase = true)
        val buyerDisplay = if (isWalkIn) {
            if (isUrdu) "____________________________" else "____________________________"
        } else {
            val payTag = if (order.remainingUdhar > 0) (if (isUrdu) " (ادھار)" else " (Credit)") else ""
            escapeHtml("$cleanCust$payTag")
        }

        return """
            <!DOCTYPE html>
            <html dir="$dir" lang="${language.code}">
            <head>
            <meta charset="UTF-8" />
            <style>
                body { font-family: sans-serif; color: #111827; margin: 14px; background: #fff; }
                .memo-box { border: 2px solid #064E3B; border-radius: 14px; overflow: hidden; max-width: 680px; margin: auto; }
                .hero { background: linear-gradient(135deg, #043927, #065F46); color: #fff; padding: 14px 18px; border-bottom: 3px solid #D4AF37; }
                .hero-top { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
                .shop-ur { font-size: 21px; font-weight: bold; color: #FDE68A; margin: 0; }
                .shop-en { font-size: 13px; font-weight: bold; color: #E5E7EB; margin: 0; }
                .sub-bar { background: #D4AF37; color: #043927; text-align: center; font-size: 12px; font-weight: bold; padding: 5px 10px; }
                .meta-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px 6px 16px; font-size: 13px; }
                .memo-pill { background: #065F46; color: #FDE68A; padding: 4px 18px; border-radius: 20px; font-weight: bold; border: 1px solid #D4AF37; }
                .buyer-bar { padding: 8px 16px 10px 16px; font-size: 14px; font-weight: bold; color: #064E3B; text-align: start; border-bottom: 1.5px solid #D1FAE5; }
                table { width: 100%; border-collapse: collapse; font-size: 13px; }
                th { background: #065F46; color: #FDE68A; padding: 8px 6px; border: 1px solid #043927; font-weight: bold; }
                td { padding: 7px 6px; border: 1px solid #CBD5E1; }
                .c-sr { width: 9%; text-align: center; font-weight: bold; color: #065F46; }
                .c-qty { width: 16%; text-align: start; }
                .c-item { width: 45%; text-align: start; font-weight: 600; }
                .c-rate { width: 14%; text-align: center; }
                .c-amt { width: 16%; text-align: center; font-weight: bold; }
                .bottom-sec { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: #F8FAFC; border-top: 2px solid #065F46; gap: 12px; }
                .tot-card { border: 1.5px solid #065F46; background: #ECFDF5; border-radius: 10px; padding: 8px 14px; min-width: 210px; font-size: 13px; }
                .tot-line { display: flex; justify-content: space-between; margin: 3px 0; }
                .grand-line { font-size: 16px; font-weight: bold; color: #064E3B; border-bottom: 1px dashed #6EE7B7; padding-bottom: 4px; }
                .udhar-line { color: #B91C1C; font-weight: bold; }
                .thanks { font-size: 12px; color: #374151; line-height: 1.8; text-align: start; }
            </style>
            </head>
            <body>
            <div class="memo-box">
                <div class="hero">
                    <div class="hero-top">
                        <p class="shop-ur">$SHOP_NAME_URDU</p>
                        <p class="shop-en">$SHOP_NAME_ENGLISH</p>
                    </div>
                </div>
                <div class="sub-bar">$SHOP_CITY_URDU ${if (shopPhone.isNotBlank()) " • $shopPhone" else ""}</div>
                <div class="meta-row">
                    <div><strong>${if (isUrdu) "نمبر" else "Bill #"}:</strong> ${escapeHtml(order.billNumber)}</div>
                    <div class="memo-pill">${if (isUrdu) "کیش میمو" else "CASH MEMO"}</div>
                    <div><strong>${if (isUrdu) "تاریخ" else "Date"}:</strong> ${escapeHtml(dateStr)}</div>
                </div>
                <div class="buyer-bar">${if (isUrdu) "نام خریدار:" else "Buyer Name:"} $buyerDisplay</div>
                <table>
                    <thead>
                        <tr>
                            <th style="width:9%;text-align:center;">${if (isUrdu) "نمبر شمار" else "Sr#"}</th>
                            <th style="width:16%;text-align:start;">${if (isUrdu) "تعداد" else "Qty"}</th>
                            <th style="width:45%;text-align:start;">${if (isUrdu) "تفصیل اشیاء" else "Item Description"}</th>
                            <th style="width:14%;text-align:center;">${if (isUrdu) "نرخ" else "Rate"}</th>
                            <th style="width:16%;text-align:center;">${if (isUrdu) "روپے" else "Amount"}</th>
                        </tr>
                    </thead>
                    <tbody>$rowsHtml</tbody>
                </table>
                <div class="bottom-sec">
                    <div class="thanks">
                        <div><strong>${if (isUrdu) "بھول چوک لین دین معاف" else "Errors & Omissions Excepted"}</strong></div>
                        <div>${if (isUrdu) "خریداری کا شکریہ! دوبارہ تشریف لائیں" else "Thank you for shopping! Visit again"}</div>
                    </div>
                    <div class="tot-card">
                        <div class="tot-line grand-line"><span>${if (isUrdu) "میزان کل:" else "Grand Total:"}</span><span>${escapeHtml(formatPkr(order.totalAmount, language))}</span></div>
                        <div class="tot-line"><span>${if (isUrdu) "وصول شدہ:" else "Paid:"}</span><span>${escapeHtml(formatPkr(order.paidAmount, language))}</span></div>
                        ${if (order.remainingUdhar > 0) "<div class='tot-line udhar-line'><span>${if (isUrdu) "بقایا ادھار:" else "Udhar:"}</span><span>${escapeHtml(formatPkr(order.remainingUdhar, language))}</span></div>" else ""}
                    </div>
                </div>
            </div>
            </body>
            </html>
        """.trimIndent()
    }
        fun buildEscPosBytes(order: Order, shopPhone: String): ByteArray {
        val out = ByteArrayOutputStream()
        fun cmd(vararg bytes: Int) { bytes.forEach { out.write(it) } }
        fun line(text: String) { out.write("$text\n".toByteArray(Charsets.UTF_8)) }

        cmd(0x1B, 0x40)
        cmd(0x1B, 0x61, 0x01)
        cmd(0x1B, 0x45, 0x01)
        line("AHMAD BILAL KIRYANA STORE")
        line("TANDA, DISTRICT GUJRAT")
        cmd(0x1B, 0x45, 0x00)
        if (shopPhone.isNotBlank()) line("Tel: $shopPhone")
        line("--------------------------------")
        cmd(0x1B, 0x61, 0x00)
        line("Bill #: ${order.billNumber}")
        line("Date  : ${formatDateTime(order.createdAt?.toDate()?.time ?: System.currentTimeMillis())}")
        line("Cust  : ${order.customerName}")
        line("Pay   : ${order.paymentMethod}")
        line("--------------------------------")
        line("Item               Qty    Total")
        line("--------------------------------")
        for (item in order.items) {
            val label = item.nameEnglish.ifBlank { item.nameUrdu }.take(18).padEnd(18)
            val qty = formatQty(item.quantity).padStart(4)
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
        if (order.remainingUdhar > 0) line("UDHAR    : Rs. ${order.remainingUdhar.toLong()}")
        line("--------------------------------")
        cmd(0x1B, 0x61, 0x01)
        line("Shukriya! Visit Again")
        line("\n\n\n")
        cmd(0x1D, 0x56, 0x41, 0x03)
        return out.toByteArray()
    }

    fun buildPaymentShareText(entry: LedgerEntry, language: AppLanguage): String {
        val dateStr = formatDateTime(entry.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val isCredit = entry.entryTypeEnum() == LedgerEntryType.CREDIT
        return if (language == AppLanguage.URDU) {
            val typeText = if (isCredit) "وصولی رسید (نقد)" else "ادھار اندراج"
            "$SHOP_NAME_URDU ($SHOP_CITY_URDU)\n$typeText\nکسٹمر: ${entry.customerName}\nرقم: ${formatPkr(entry.amount, AppLanguage.URDU)}\nبقایا ادھار: ${formatPkr(entry.balanceAfter, AppLanguage.URDU)}\nتاریخ: $dateStr"
        } else {
            val typeText = if (isCredit) "Payment Receipt (Cash)" else "Udhar Debit Entry"
            "$SHOP_NAME_ENGLISH ($SHOP_CITY_ENGLISH)\n$typeText\nCustomer: ${entry.customerName}\nAmount: ${formatPkr(entry.amount, AppLanguage.ENGLISH)}\nRemaining Balance: ${formatPkr(entry.balanceAfter, AppLanguage.ENGLISH)}\nDate: $dateStr"
        }
    }

    fun buildRationCardShareText(bill: RationCardBill, language: AppLanguage, shopPhone: String): String {
        val dateStr = formatDateTime(bill.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val sb = StringBuilder()
        if (language == AppLanguage.URDU) {
            sb.appendLine("============================")
            sb.appendLine("  $SHOP_NAME_URDU")
            sb.appendLine("  مریم نواز راشن کارڈ رسید")
            if (shopPhone.isNotBlank()) sb.appendLine("  فون: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("رسید نمبر: ${bill.billNumber}")
            sb.appendLine("تاریخ: $dateStr")
            sb.appendLine("صارف: ${bill.beneficiaryName}")
            if (bill.cardNumber.isNotBlank()) sb.appendLine("کارڈ نمبر: ${bill.cardNumber}")
            sb.appendLine("----------------------------")
            bill.items.forEachIndexed { idx, item ->
                sb.appendLine("${idx + 1}. ${item.nameUrdu} (${formatQty(item.quantity)} ${unitLabel(item.unit, AppLanguage.URDU)}) x ${item.unitPrice.toLong()} = ${formatPkr(item.lineTotal, AppLanguage.URDU)}")
            }
            sb.appendLine("----------------------------")
            sb.appendLine("کل بل: ${formatPkr(bill.totalAmount, AppLanguage.URDU)}")
            sb.appendLine("راشن کارڈ سے کٹوتی: ${formatPkr(bill.subsidyUsed, AppLanguage.URDU)}")
            sb.appendLine("============================")
        } else {
            sb.appendLine("============================")
            sb.appendLine("  $SHOP_NAME_ENGLISH")
            sb.appendLine("  Maryam Nawaz Ration Card Receipt")
            sb.appendLine("============================")
            sb.appendLine("Receipt #: ${bill.billNumber}")
            sb.appendLine("Date: $dateStr")
            sb.appendLine("Beneficiary: ${bill.beneficiaryName}")
            bill.items.forEachIndexed { idx, item ->
                sb.appendLine("${idx + 1}. ${item.nameEnglish} (${formatQty(item.quantity)}) = ${formatPkr(item.lineTotal, AppLanguage.ENGLISH)}")
            }
            sb.appendLine("Total Bill: ${formatPkr(bill.totalAmount, AppLanguage.ENGLISH)}")
        }
        return sb.toString()
    }

    fun buildRationCardPrintableHtml(bill: RationCardBill, language: AppLanguage, shopPhone: String): String {
        val isUrdu = language == AppLanguage.URDU
        val dir = if (isUrdu) "rtl" else "ltr"
        val dateStr = formatDateTime(bill.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val rows = bill.items.mapIndexed { idx, item ->
            val name = if (isUrdu) item.nameUrdu else item.nameEnglish
            val qtyStr = "${formatQty(item.quantity)} ${unitLabel(item.unit, language)}"
            """
            <tr>
                <td style="padding:6px;border:1px solid #CBD5E1;text-align:center;font-weight:bold;">${idx + 1}</td>
                <td style="padding:6px;border:1px solid #CBD5E1;text-align:start;">${escapeHtml(qtyStr)}</td>
                <td style="padding:6px;border:1px solid #CBD5E1;text-align:start;font-weight:600;">${escapeHtml(name)}</td>
                <td style="padding:6px;border:1px solid #CBD5E1;text-align:center;">${item.unitPrice.toLong()}</td>
                <td style="padding:6px;border:1px solid #CBD5E1;text-align:center;font-weight:bold;">${item.lineTotal.toLong()}/-</td>
            </tr>
            """.trimIndent()
        }.joinToString("\n")
        return """
            <!DOCTYPE html>
            <html dir="$dir" lang="${language.code}">
            <head><meta charset="UTF-8" /></head>
            <body style="font-family:sans-serif;margin:14px;">
                <div style="border:2px solid #064E3B;border-radius:14px;overflow:hidden;max-width:650px;margin:auto;">
                    <div style="background:#065F46;color:#FDE68A;padding:12px 16px;text-align:center;border-bottom:3px solid #D4AF37;">
                        <h2 style="margin:0;">$SHOP_NAME_URDU</h2>
                        <div style="font-size:12px;color:#fff;margin-top:4px;">${if (isUrdu) "مریم نواز راشن کارڈ رسید" else "Maryam Nawaz Ration Card Receipt"}</div>
                    </div>
                    <div style="padding:10px 14px;font-size:13px;background:#ECFDF5;">
                        <div><strong>${if (isUrdu) "رسید نمبر" else "Receipt #"}:</strong> ${escapeHtml(bill.billNumber)} | <strong>${if (isUrdu) "تاریخ" else "Date"}:</strong> ${escapeHtml(dateStr)}</div>
                        <div style="margin-top:4px;"><strong>${if (isUrdu) "نام خریدار / صارف" else "Beneficiary"}:</strong> ${escapeHtml(bill.beneficiaryName)}</div>
                    </div>
                    <table style="width:100%;border-collapse:collapse;font-size:13px;">
                        <thead>
                            <tr style="background:#065F46;color:#FDE68A;">
                                <th style="padding:7px;border:1px solid #043927;">${if (isUrdu) "نمبر شمار" else "Sr#"}</th>
                                <th style="padding:7px;border:1px solid #043927;text-align:start;">${if (isUrdu) "تعداد" else "Qty"}</th>
                                <th style="padding:7px;border:1px solid #043927;text-align:start;">${if (isUrdu) "تفصیل اشیاء" else "Items"}</th>
                                <th style="padding:7px;border:1px solid #043927;">${if (isUrdu) "نرخ" else "Rate"}</th>
                                <th style="padding:7px;border:1px solid #043927;">${if (isUrdu) "روپے" else "Amount"}</th>
                            </tr>
                        </thead>
                        <tbody>$rows</tbody>
                    </table>
                    <div style="padding:10px 14px;background:#F8FAFC;font-weight:bold;color:#064E3B;display:flex;justify-content:space-between;">
                        <span>${if (isUrdu) "کل راشن بل:" else "Total Bill:"} ${escapeHtml(formatPkr(bill.totalAmount, language))}</span>
                        <span>${if (isUrdu) "کارڈ کٹوتی:" else "Card Used:"} ${escapeHtml(formatPkr(bill.subsidyUsed, language))}</span>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun buildRationCardEscPosBytes(bill: RationCardBill, shopPhone: String): ByteArray {
        val out = ByteArrayOutputStream()
        fun cmd(vararg bytes: Int) { bytes.forEach { out.write(it) } }
        fun line(text: String) { out.write("$text\n".toByteArray(Charsets.UTF_8)) }

        cmd(0x1B, 0x40)
        cmd(0x1B, 0x61, 0x01)
        cmd(0x1B, 0x45, 0x01)
        line("AHMAD BILAL KIRYANA STORE")
        line("TANDA, DISTRICT GUJRAT")
        cmd(0x1B, 0x45, 0x00)
        line("MARYAM NAWAZ RATION CARD")
        if (shopPhone.isNotBlank()) line("Tel: $shopPhone")
        line("--------------------------------")
        cmd(0x1B, 0x61, 0x00)
        line("Bill # : ${bill.billNumber}")
        line("Date   : ${formatDateTime(bill.createdAt?.toDate()?.time ?: System.currentTimeMillis())}")
        line("Name   : ${bill.beneficiaryName}")
        line("--------------------------------")
        for (item in bill.items) {
            val label = item.nameEnglish.ifBlank { item.nameUrdu }.take(18).padEnd(18)
            val qty = formatQty(item.quantity).padStart(4)
            val amt = item.lineTotal.toLong().toString().padStart(8)
            line("$label $qty $amt")
        }
        line("--------------------------------")
        line("TOTAL BILL : Rs. ${bill.totalAmount.toLong()}")
        line("CARD USED  : Rs. ${bill.subsidyUsed.toLong()}")
        line("--------------------------------")
        cmd(0x1B, 0x61, 0x01)
        line("Shukriya! Ahmad Bilal Tanda")
        line("\n\n\n")
        cmd(0x1D, 0x56, 0x41, 0x03)
        return out.toByteArray()
    }

    private fun escapeHtml(input: String): String =
        input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
