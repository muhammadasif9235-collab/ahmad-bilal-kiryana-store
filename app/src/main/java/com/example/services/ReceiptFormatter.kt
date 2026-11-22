package com.example.services

import com.example.data.LedgerEntry
import com.example.data.LedgerEntryType
import com.example.data.Order
import com.example.data.PaymentMethod
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
    const val SHOP_CITY_URDU = "ٹانڈہ، ضلع گجرات، پنجاب، پاکستان"
    const val SHOP_CITY_ENGLISH = "Tanda, District Gujrat, Punjab, Pakistan"

    fun formatDateTime(epochMillis: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(Date(epochMillis))
    }

    /**
     * Plain text receipt for WhatsApp / SMS sharing.
     */
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
    fun buildPrintableHtml(order: Order, language: AppLanguage, shopPhone: String): String {
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

        val shopTitle = if (isUrdu) SHOP_NAME_URDU else SHOP_NAME_ENGLISH
        val shopSub = if (isUrdu) SHOP_CITY_URDU else SHOP_CITY_ENGLISH
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
        val footerMsg = if (isUrdu) "خریداری کا شکریہ — احمد بلال کریانہ سٹور، گجرات" else "Thank you for visiting Ahmad Bilal Kiryana Store, Gujrat"

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
     * Builds ESC/POS command byte buffer for 58mm / 80mm Bluetooth thermal receipt printers.
     */
    fun buildEscPosBytes(order: Order, shopPhone: String): ByteArray {
        val out = ByteArrayOutputStream()
        fun cmd(vararg bytes: Int) {
            bytes.forEach { out.write(it) }
        }
        fun line(text: String) {
            out.write("$text\n".toByteArray(Charsets.UTF_8))
        }

        // Initialize printer
        cmd(0x1B, 0x40)
        // Center align + Bold
        cmd(0x1B, 0x61, 0x01)
        cmd(0x1B, 0x45, 0x01)
        line("AHMAD BILAL KIRYANA STORE")
        line("TANDA, DISTRICT GUJRAT")
        cmd(0x1B, 0x45, 0x00)
        if (shopPhone.isNotBlank()) line("Tel: $shopPhone")
        line("--------------------------------")

        // Left align
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

    fun buildPaymentShareText(entry: LedgerEntry, language: AppLanguage): String {
        val dateStr = formatDateTime(entry.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val isCredit = entry.entryTypeEnum() == LedgerEntryType.CREDIT
        return if (language == AppLanguage.URDU) {
            val typeText = if (isCredit) "وصولی رسید (نقد)" else "ادھار اندراج"
            """
            $SHOP_NAME_URDU ($SHOP_CITY_URDU)
            $typeText
            کسٹمر: ${entry.customerName}
            رقم: ${formatPkr(entry.amount, AppLanguage.URDU)}
            بقایا ادھار: ${formatPkr(entry.balanceAfter, AppLanguage.URDU)}
            تاریخ: $dateStr
            ${if (entry.note.isNotBlank()) "نوٹ: ${entry.note}" else ""}
            """.trimIndent()
        } else {
            val typeText = if (isCredit) "Payment Receipt (Cash)" else "Udhar Debit Entry"
            """
            $SHOP_NAME_ENGLISH ($SHOP_CITY_ENGLISH)
            $typeText
            Customer: ${entry.customerName}
            Amount: ${formatPkr(entry.amount, AppLanguage.ENGLISH)}
            Remaining Balance: ${formatPkr(entry.balanceAfter, AppLanguage.ENGLISH)}
            Date: $dateStr
            ${if (entry.note.isNotBlank()) "Note: ${entry.note}" else ""}
            """.trimIndent()
        }
    }

    fun buildRationCardShareText(bill: RationCardBill, language: AppLanguage, shopPhone: String): String {
        val dateStr = formatDateTime(bill.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val sb = StringBuilder()
        if (language == AppLanguage.URDU) {
            sb.appendLine("============================")
            sb.appendLine("  $SHOP_NAME_URDU")
            sb.appendLine("  مریم نواز راشن کارڈ رسید (حکومت پنجاب)")
            sb.appendLine("  $SHOP_CITY_URDU")
            if (shopPhone.isNotBlank()) sb.appendLine("  فون: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("رسید نمبر: ${bill.billNumber}")
            sb.appendLine("تاریخ: $dateStr")
            sb.appendLine("صارف کا نام: ${bill.beneficiaryName}")
            if (bill.cnic.isNotBlank()) sb.appendLine("شناختی کارڈ: ${bill.cnic}")
            if (bill.cardNumber.isNotBlank()) sb.appendLine("کارڈ نمبر: ${bill.cardNumber}")
            if (bill.cardUid.isNotBlank()) sb.appendLine("NFC UID: ${bill.cardUid}")
            sb.appendLine("ماہانہ کوٹہ: ${formatPkr(bill.subsidyLimit, AppLanguage.URDU)} (${bill.monthKey})")
            sb.appendLine("----------------------------")
            bill.items.forEachIndexed { idx, item ->
                sb.appendLine(
                    "${idx + 1}. ${item.nameUrdu} (${formatQty(item.quantity)} ${unitLabel(item.unit, AppLanguage.URDU)}) x ${item.unitPrice.toLong()} = ${formatPkr(item.lineTotal, AppLanguage.URDU)}"
                )
            }
            sb.appendLine("----------------------------")
            sb.appendLine("کل بل: ${formatPkr(bill.totalAmount, AppLanguage.URDU)}")
            sb.appendLine("راشن کارڈ سے کٹوتی: ${formatPkr(bill.subsidyUsed, AppLanguage.URDU)}")
            if (bill.cashTopUp > 0) sb.appendLine("اضافی نقد ادائیگی: ${formatPkr(bill.cashTopUp, AppLanguage.URDU)}")
            sb.appendLine("کارڈ بقایا بیلنس: ${formatPkr(bill.remainingSubsidy, AppLanguage.URDU)}")
            sb.appendLine("============================")
        } else {
            sb.appendLine("============================")
            sb.appendLine("  $SHOP_NAME_ENGLISH")
            sb.appendLine("  Maryam Nawaz Ration Card Receipt")
            sb.appendLine("  $SHOP_CITY_ENGLISH")
            if (shopPhone.isNotBlank()) sb.appendLine("  Phone: $shopPhone")
            sb.appendLine("============================")
            sb.appendLine("Receipt #: ${bill.billNumber}")
            sb.appendLine("Date: $dateStr")
            sb.appendLine("Beneficiary: ${bill.beneficiaryName}")
            if (bill.cnic.isNotBlank()) sb.appendLine("CNIC: ${bill.cnic}")
            if (bill.cardNumber.isNotBlank()) sb.appendLine("Card #: ${bill.cardNumber}")
            if (bill.cardUid.isNotBlank()) sb.appendLine("NFC UID: ${bill.cardUid}")
            sb.appendLine("Monthly Quota: ${formatPkr(bill.subsidyLimit, AppLanguage.ENGLISH)} (${bill.monthKey})")
            sb.appendLine("----------------------------")
            bill.items.forEachIndexed { idx, item ->
                sb.appendLine(
                    "${idx + 1}. ${item.nameEnglish} (${formatQty(item.quantity)} ${unitLabel(item.unit, AppLanguage.ENGLISH)}) x ${item.unitPrice.toLong()} = ${formatPkr(item.lineTotal, AppLanguage.ENGLISH)}"
                )
            }
            sb.appendLine("----------------------------")
            sb.appendLine("Total Bill: ${formatPkr(bill.totalAmount, AppLanguage.ENGLISH)}")
            sb.appendLine("Card Subsidy Used: ${formatPkr(bill.subsidyUsed, AppLanguage.ENGLISH)}")
            if (bill.cashTopUp > 0) sb.appendLine("Extra Cash Paid: ${formatPkr(bill.cashTopUp, AppLanguage.ENGLISH)}")
            sb.appendLine("Remaining Subsidy: ${formatPkr(bill.remainingSubsidy, AppLanguage.ENGLISH)}")
            sb.appendLine("============================")
        }
        return sb.toString()
    }

    fun buildRationCardPrintableHtml(bill: RationCardBill, language: AppLanguage, shopPhone: String): String {
        val isUrdu = language == AppLanguage.URDU
        val dir = if (isUrdu) "rtl" else "ltr"
        val dateStr = formatDateTime(bill.createdAt?.toDate()?.time ?: System.currentTimeMillis())
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
        val shopTitle = if (isUrdu) SHOP_NAME_URDU else SHOP_NAME_ENGLISH
        val programTitle = if (isUrdu) "مریم نواز راشن کارڈ — حکومت پنجاب (Rs. 3,000 سبسڈی)" else "Maryam Nawaz Ration Card — Govt of Punjab (Rs. 3,000 Subsidy)"
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
                    <p class="sub">${escapeHtml(if (isUrdu) SHOP_CITY_URDU else SHOP_CITY_ENGLISH)}</p>
                    ${if (shopPhone.isNotBlank()) "<p class='sub'>${escapeHtml(shopPhone)}</p>" else ""}
                </div>
                <div class="meta">
                    <div><strong>${if (isUrdu) "رسید نمبر" else "Receipt #"}:</strong> ${escapeHtml(bill.billNumber)} (${escapeHtml(bill.monthKey)})</div>
                    <div><strong>${if (isUrdu) "تاریخ" else "Date"}:</strong> ${escapeHtml(dateStr)}</div>
                    <div><strong>${if (isUrdu) "صارف" else "Beneficiary"}:</strong> ${escapeHtml(bill.beneficiaryName)}</div>
                    ${if (bill.cnic.isNotBlank()) "<div><strong>CNIC:</strong> ${escapeHtml(bill.cnic)}</div>" else ""}
                    ${if (bill.cardNumber.isNotBlank()) "<div><strong>${if (isUrdu) "کارڈ نمبر" else "Card #"}:</strong> ${escapeHtml(bill.cardNumber)}</div>" else ""}
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
                    <div class="row grand"><span>${if (isUrdu) "کل راشن بل" else "Total Ration Bill"}</span><span>${escapeHtml(formatPkr(bill.totalAmount, language))}</span></div>
                    <div class="row"><span>${if (isUrdu) "راشن کارڈ سبسڈی کٹوتی" else "Ration Card Subsidy"}</span><span>${escapeHtml(formatPkr(bill.subsidyUsed, language))}</span></div>
                    ${if (bill.cashTopUp > 0) "<div class='row'><span>${if (isUrdu) "اضافی نقد ادائیگی" else "Extra Cash Paid"}</span><span>${escapeHtml(formatPkr(bill.cashTopUp, language))}</span></div>" else ""}
                    <div class="row"><span>${if (isUrdu) "بقایا ماہانہ بیلنس" else "Remaining Quota"}</span><span>${escapeHtml(formatPkr(bill.remainingSubsidy, language))}</span></div>
                </div>
                <div class="footer">${escapeHtml(shopTitle)} • ${if (isUrdu) "رجسٹرڈ کریانہ مرچنٹ" else "Registered Kiryana Merchant"}</div>
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
        line("Govt of Punjab Rs.3000 Subsidy")
        if (shopPhone.isNotBlank()) line("Tel: $shopPhone")
        line("--------------------------------")
        cmd(0x1B, 0x61, 0x00)
        line("Bill # : ${bill.billNumber}")
        line("Month  : ${bill.monthKey}")
        line("Date   : ${formatDateTime(bill.createdAt?.toDate()?.time ?: System.currentTimeMillis())}")
        line("Name   : ${bill.beneficiaryName}")
        if (bill.cnic.isNotBlank()) line("CNIC   : ${bill.cnic}")
        if (bill.cardNumber.isNotBlank()) line("Card # : ${bill.cardNumber}")
        if (bill.cardUid.isNotBlank()) line("NFC ID : ${bill.cardUid}")
        line("--------------------------------")
        line("Item               Qty    Total")
        line("--------------------------------")
        for (item in bill.items) {
            val label = item.nameEnglish.ifBlank { item.nameUrdu }.take(18).padEnd(18)
            val qty = formatQty(item.quantity).padStart(4)
            val amt = item.lineTotal.toLong().toString().padStart(8)
            line("$label $qty $amt")
        }
        line("--------------------------------")
        cmd(0x1B, 0x45, 0x01)
        line("TOTAL BILL : Rs. ${bill.totalAmount.toLong()}")
        line("CARD USED  : Rs. ${bill.subsidyUsed.toLong()}")
        cmd(0x1B, 0x45, 0x00)
        if (bill.cashTopUp > 0) line("EXTRA CASH : Rs. ${bill.cashTopUp.toLong()}")
        line("REMAINING  : Rs. ${bill.remainingSubsidy.toLong()}")
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
