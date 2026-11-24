package com.example.services

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.data.DailyCashEntry
import com.example.data.DailyCashType
import com.example.data.DailyPaymentSource
import com.example.data.LedgerEntry
import com.example.data.LedgerEntryType
import com.example.data.Order
import com.example.data.Product
import com.example.data.RationCardBill
import com.example.data.ShopSettings
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.formatQty
import com.example.i18n.label
import com.example.i18n.unitLabel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptPrintHelper {

    private const val PDF_PAGE_WIDTH = 340
    private const val PDF_MARGIN = 18

    fun printOrderWithSystemPrint(
        context: Context,
        order: Order,
        language: AppLanguage,
        shopPhone: String,
        settings: ShopSettings? = null
    ) {
        val html = ReceiptFormatter.buildPrintableHtml(
            order = order,
            language = language,
            shopPhone = settings?.shopPhone?.ifBlank { shopPhone } ?: shopPhone,
            shopNameUrdu = settings?.storeNameUrdu?.ifBlank { "احمد بلال کریانہ سٹور ٹانڈہ" } ?: "احمد بلال کریانہ سٹور ٹانڈہ",
            shopNameEnglish = settings?.storeNameEnglish?.ifBlank { "Ahmad Bilal Kiryana Store, Tanda" } ?: "Ahmad Bilal Kiryana Store, Tanda",
            shopAddressUrdu = settings?.shopAddressUrdu.orEmpty(),
            shopAddressEnglish = settings?.shopAddressEnglish.orEmpty()
        )
        printHtmlDocument(context, html, "Bill_${order.billNumber}")
    }

    fun printKhataWithSystemPrint(
        context: Context,
        entry: LedgerEntry,
        language: AppLanguage,
        settings: ShopSettings? = null
    ) {
        val html = ReceiptFormatter.buildPaymentPrintableHtml(
            entry = entry,
            language = language,
            shopPhone = settings?.shopPhone.orEmpty(),
            shopNameUrdu = settings?.storeNameUrdu?.ifBlank { "احمد بلال کریانہ سٹور ٹانڈہ" } ?: "احمد بلال کریانہ سٹور ٹانڈہ",
            shopNameEnglish = settings?.storeNameEnglish?.ifBlank { "Ahmad Bilal Kiryana Store, Tanda" } ?: "Ahmad Bilal Kiryana Store, Tanda",
            shopAddressUrdu = settings?.shopAddressUrdu.orEmpty(),
            shopAddressEnglish = settings?.shopAddressEnglish.orEmpty()
        )
        printHtmlDocument(context, html, "Khata_${entry.id.take(10)}")
    }

    fun printRationCardWithSystemPrint(
        context: Context,
        bill: RationCardBill,
        language: AppLanguage,
        shopPhone: String,
        settings: ShopSettings? = null
    ) {
        val html = ReceiptFormatter.buildRationCardPrintableHtml(
            bill = bill,
            language = language,
            shopPhone = settings?.shopPhone?.ifBlank { shopPhone } ?: shopPhone,
            shopNameUrdu = settings?.storeNameUrdu?.ifBlank { "احمد بلال کریانہ سٹور ٹانڈہ" } ?: "احمد بلال کریانہ سٹور ٹانڈہ",
            shopNameEnglish = settings?.storeNameEnglish?.ifBlank { "Ahmad Bilal Kiryana Store, Tanda" } ?: "Ahmad Bilal Kiryana Store, Tanda",
            shopAddressUrdu = settings?.shopAddressUrdu.orEmpty(),
            shopAddressEnglish = settings?.shopAddressEnglish.orEmpty()
        )
        printHtmlDocument(context, html, "POS_Receipt_${bill.billNumber}")
    }

    fun printLowStockReportWithSystemPrint(
        context: Context,
        lowStockProducts: List<Product>,
        language: AppLanguage,
        settings: ShopSettings
    ) {
        val html = ReceiptFormatter.buildLowStockReportPrintableHtml(
            lowStockProducts = lowStockProducts,
            language = language,
            shopPhone = settings.shopPhone,
            shopNameUrdu = settings.storeNameUrdu,
            shopNameEnglish = settings.storeNameEnglish,
            shopAddressUrdu = settings.shopAddressUrdu,
            shopAddressEnglish = settings.shopAddressEnglish
        )
        val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        printHtmlDocument(context, html, "Low_Stock_Report_$dateTag")
    }

    private fun printHtmlDocument(context: Context, html: String, jobName: String) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
            override fun onPageFinished(view: WebView, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                printManager.print(jobName, view.createPrintDocumentAdapter(jobName), PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    }

    fun shareOrderOnWhatsAppText(
        context: Context,
        order: Order,
        language: AppLanguage,
        settings: ShopSettings,
        customerPhone: String = ""
    ) {
        val text = ReceiptFormatter.buildShareText(
            order = order,
            language = language,
            shopPhone = settings.shopPhone,
            shopNameUrdu = settings.storeNameUrdu,
            shopNameEnglish = settings.storeNameEnglish,
            shopAddressUrdu = settings.shopAddressUrdu,
            shopAddressEnglish = settings.shopAddressEnglish
        )
        sendViaWhatsApp(context, customerPhone, text)
    }

    fun shareKhataOnWhatsAppText(
        context: Context,
        entry: LedgerEntry,
        language: AppLanguage,
        settings: ShopSettings,
        customerPhone: String = ""
    ) {
        val text = ReceiptFormatter.buildPaymentShareText(
            entry = entry,
            language = language,
            shopPhone = settings.shopPhone,
            shopNameUrdu = settings.storeNameUrdu,
            shopNameEnglish = settings.storeNameEnglish,
            shopAddressUrdu = settings.shopAddressUrdu,
            shopAddressEnglish = settings.shopAddressEnglish
        )
        sendViaWhatsApp(context, customerPhone, text)
    }

    fun shareRationCardOnWhatsAppText(
        context: Context,
        bill: RationCardBill,
        language: AppLanguage,
        settings: ShopSettings
    ) {
        val text = ReceiptFormatter.buildRationCardShareText(
            bill = bill,
            language = language,
            shopPhone = settings.shopPhone,
            shopNameUrdu = settings.storeNameUrdu,
            shopNameEnglish = settings.storeNameEnglish,
            shopAddressUrdu = settings.shopAddressUrdu,
            shopAddressEnglish = settings.shopAddressEnglish
        )
        sendViaWhatsApp(context, bill.phone, text)
    }

    fun shareOrderPdfOnWhatsApp(
        context: Context,
        order: Order,
        language: AppLanguage,
        settings: ShopSettings
    ) {
        val file = generateOrderReceiptPdf(context, order, language, settings)
        val caption = ReceiptFormatter.buildShareText(
            order = order,
            language = language,
            shopPhone = settings.shopPhone,
            shopNameUrdu = settings.storeNameUrdu,
            shopNameEnglish = settings.storeNameEnglish,
            shopAddressUrdu = settings.shopAddressUrdu,
            shopAddressEnglish = settings.shopAddressEnglish
        )
        if (file != null) sharePdfFileViaWhatsApp(context, file, caption, "Bill ${order.billNumber}")
        else sendViaWhatsApp(context, "", caption)
    }

    fun shareKhataPdfOnWhatsApp(
        context: Context,
        entry: LedgerEntry,
        language: AppLanguage,
        settings: ShopSettings
    ) {
        val file = generateKhataReceiptPdf(context, entry, language, settings)
        val caption = ReceiptFormatter.buildPaymentShareText(
            entry = entry,
            language = language,
            shopPhone = settings.shopPhone,
            shopNameUrdu = settings.storeNameUrdu,
            shopNameEnglish = settings.storeNameEnglish,
            shopAddressUrdu = settings.shopAddressUrdu,
            shopAddressEnglish = settings.shopAddressEnglish
        )
        if (file != null) sharePdfFileViaWhatsApp(context, file, caption, "Khata Receipt - ${entry.customerName}")
        else sendViaWhatsApp(context, "", caption)
    }

    fun shareRationCardPdfOnWhatsApp(
        context: Context,
        bill: RationCardBill,
        language: AppLanguage,
        settings: ShopSettings
    ) {
        val file = generateRationCardReceiptPdf(context, bill, language, settings)
        val caption = ReceiptFormatter.buildRationCardShareText(
            bill = bill,
            language = language,
            shopPhone = settings.shopPhone,
            shopNameUrdu = settings.storeNameUrdu,
            shopNameEnglish = settings.storeNameEnglish,
            shopAddressUrdu = settings.shopAddressUrdu,
            shopAddressEnglish = settings.shopAddressEnglish
        )
        if (file != null) sharePdfFileViaWhatsApp(context, file, caption, "POS Receipt ${bill.billNumber}")
        else sendViaWhatsApp(context, bill.phone, caption)
    }

    fun shareRoznamchaStatementOnWhatsAppText(
        context: Context,
        periodLabel: String,
        entries: List<DailyCashEntry>,
        totalIn: Double,
        totalOut: Double,
        netBalance: Double,
        language: AppLanguage,
        settings: ShopSettings
    ) {
        val text = ReceiptFormatter.buildRoznamchaStatementShareText(
            periodLabel = periodLabel,
            entries = entries,
            totalIn = totalIn,
            totalOut = totalOut,
            netBalance = netBalance,
            language = language,
            shopPhone = settings.shopPhone,
            shopNameUrdu = settings.storeNameUrdu,
            shopNameEnglish = settings.storeNameEnglish,
            shopAddressUrdu = settings.shopAddressUrdu,
            shopAddressEnglish = settings.shopAddressEnglish
        )
        sendViaWhatsApp(context, "", text)
    }

    fun shareRoznamchaStatementPdfOnWhatsApp(
        context: Context,
        periodLabel: String,
        entries: List<DailyCashEntry>,
        totalIn: Double,
        totalOut: Double,
        netBalance: Double,
        language: AppLanguage,
        settings: ShopSettings
    ) {
        val file = generateRoznamchaStatementPdf(context, periodLabel, entries, totalIn, totalOut, netBalance, language, settings)
        val caption = ReceiptFormatter.buildRoznamchaStatementShareText(
            periodLabel = periodLabel,
            entries = entries,
            totalIn = totalIn,
            totalOut = totalOut,
            netBalance = netBalance,
            language = language,
            shopPhone = settings.shopPhone,
            shopNameUrdu = settings.storeNameUrdu,
            shopNameEnglish = settings.storeNameEnglish,
            shopAddressUrdu = settings.shopAddressUrdu,
            shopAddressEnglish = settings.shopAddressEnglish
        )
        if (file != null) sharePdfFileViaWhatsApp(context, file, caption, "Roznamcha Statement - $periodLabel")
        else sendViaWhatsApp(context, "", caption)
    }

    fun shareLowStockReportOnWhatsAppText(
        context: Context,
        lowStockProducts: List<Product>,
        language: AppLanguage,
        settings: ShopSettings
    ) {
        val text = ReceiptFormatter.buildLowStockReportShareText(
            lowStockProducts = lowStockProducts,
            language = language,
            shopPhone = settings.shopPhone,
            shopNameUrdu = settings.storeNameUrdu,
            shopNameEnglish = settings.storeNameEnglish,
            shopAddressUrdu = settings.shopAddressUrdu,
            shopAddressEnglish = settings.shopAddressEnglish
        )
        sendViaWhatsApp(context, "", text)
    }

    fun shareLowStockReportPdf(
        context: Context,
        lowStockProducts: List<Product>,
        language: AppLanguage,
        settings: ShopSettings,
        preferWhatsApp: Boolean = true
    ): File? {
        val file = generateLowStockReportPdf(context, lowStockProducts, language, settings)
        val caption = ReceiptFormatter.buildLowStockReportShareText(
            lowStockProducts = lowStockProducts,
            language = language,
            shopPhone = settings.shopPhone,
            shopNameUrdu = settings.storeNameUrdu,
            shopNameEnglish = settings.storeNameEnglish,
            shopAddressUrdu = settings.shopAddressUrdu,
            shopAddressEnglish = settings.shopAddressEnglish
        )
        val subject = if (language == AppLanguage.URDU) "کم اسٹاک رپورٹ" else "Low Stock Report"
        if (file != null) {
            if (preferWhatsApp) sharePdfFileViaWhatsApp(context, file, caption, subject)
            else sharePdfFileSystemChooser(context, file, caption, subject)
        } else {
            sendViaWhatsApp(context, "", caption)
        }
        return file
    }

    fun openOrPrintLowStockReportPdf(
        context: Context,
        lowStockProducts: List<Product>,
        language: AppLanguage,
        settings: ShopSettings
    ) {
        val file = generateLowStockReportPdf(context, lowStockProducts, language, settings)
        if (file != null) {
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return
            } catch (_: Exception) {}
        }
        printLowStockReportWithSystemPrint(context, lowStockProducts, language, settings)
    }

    fun generateLowStockReportPdf(
        context: Context,
        lowStockProducts: List<Product>,
        language: AppLanguage,
        settings: ShopSettings
    ): File? {
        val isUrdu = language == AppLanguage.URDU
        val title = if (isUrdu) settings.storeNameUrdu else settings.storeNameEnglish
        val address = if (isUrdu) settings.shopAddressUrdu else settings.shopAddressEnglish
        val lines = mutableListOf<PdfReceiptLine>()
        lines += PdfReceiptLine(if (isUrdu) "تاریخ: ${ReceiptFormatter.formatDateTime()}" else "Date: ${ReceiptFormatter.formatDateTime()}", bold = true)
        lines += PdfReceiptLine.divider()
        lowStockProducts.forEachIndexed { idx, prod ->
            val name = if (isUrdu) prod.nameUrdu.ifBlank { prod.nameEnglish } else prod.nameEnglish.ifBlank { prod.nameUrdu }
            lines += PdfReceiptLine("${idx + 1}. $name — ${formatQty(prod.stockQty)} ${unitLabel(prod.unit, language)}")
        }
        val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return renderReceiptPdf(
            context = context,
            fileName = "Low_Stock_Report_$dateTag.pdf",
            shopTitle = title,
            shopSubtitle = address,
            shopPhone = settings.shopPhone,
            badgeText = if (isUrdu) "کم اسٹاک خریداری لسٹ (${lowStockProducts.size})" else "LOW STOCK LIST (${lowStockProducts.size})",
            badgeColor = Color.rgb(185, 28, 28),
            lines = lines,
            footer = title
        )
    }

    private fun sharePdfFileSystemChooser(context: Context, pdfFile: File, caption: String, subject: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(shareIntent, subject).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (_: Exception) {
            shareText(context, caption, subject)
        }
    }

    fun generateRoznamchaStatementPdf(
        context: Context,
        periodLabel: String,
        entries: List<DailyCashEntry>,
        totalIn: Double,
        totalOut: Double,
        netBalance: Double,
        language: AppLanguage,
        settings: ShopSettings
    ): File? {
        val isUrdu = language == AppLanguage.URDU
        val title = if (isUrdu) settings.storeNameUrdu else settings.storeNameEnglish
        val address = if (isUrdu) settings.shopAddressUrdu else settings.shopAddressEnglish
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.US)

        val lines = mutableListOf<PdfReceiptLine>()
        lines += PdfReceiptLine(if (isUrdu) "مدت: $periodLabel" else "Period: $periodLabel", bold = true)
        lines += PdfReceiptLine.divider()
        lines += PdfReceiptLine(if (isUrdu) "کل کیش اِن: +${formatPkr(totalIn, language)}" else "Cash In: +${formatPkr(totalIn, language)}", bold = true, accentColor = Color.rgb(15, 81, 50))
        lines += PdfReceiptLine(if (isUrdu) "کل کیش آؤٹ: -${formatPkr(totalOut, language)}" else "Cash Out: -${formatPkr(totalOut, language)}", bold = true, accentColor = Color.rgb(198, 30, 30))
        lines += PdfReceiptLine(if (isUrdu) "خالص بیلنس: ${formatPkr(netBalance, language)}" else "Net Balance: ${formatPkr(netBalance, language)}", bold = true)
        lines += PdfReceiptLine.divider()
        entries.take(80).forEachIndexed { idx, entry ->
            val isIn = entry.entryTypeEnum() == DailyCashType.CASH_IN
            val sign = if (isIn) "+" else "-"
            val mode = when (entry.paymentSourceEnum()) {
                DailyPaymentSource.CASH -> if (isUrdu) "نقد" else "Cash"
                DailyPaymentSource.HBL_QR -> "QR"
                DailyPaymentSource.ONLINE -> if (isUrdu) "آن لائن" else "Online"
            }
            lines += PdfReceiptLine("${idx + 1}. [${dateFmt.format(Date(entry.entryDateMs))}] ${entry.categoryOrTitle} ($mode) $sign${formatPkr(entry.amount, language)}")
        }
        val safePeriod = periodLabel.replace(Regex("[^A-Za-z0-9_-]"), "_").take(20).ifBlank { "Statement" }
        return renderReceiptPdf(
            context = context,
            fileName = "Roznamcha_${safePeriod}_${System.currentTimeMillis() % 10000}.pdf",
            shopTitle = title,
            shopSubtitle = address,
            shopPhone = settings.shopPhone,
            badgeText = if (isUrdu) "روزنامچہ اسٹیٹمنٹ" else "ROZNAMCHA STATEMENT",
            badgeColor = Color.rgb(15, 81, 50),
            lines = lines,
            footer = title
        )
    }

    fun generateOrderReceiptPdf(
        context: Context,
        order: Order,
        language: AppLanguage,
        settings: ShopSettings
    ): File? {
        return try {
            val isUrdu = language == AppLanguage.URDU
            val shopNameUr = settings.storeNameUrdu.ifBlank { "احمد بلال کریانہ سٹور ٹانڈہ" }
            val shopNameEn = settings.storeNameEnglish.ifBlank { "Ahmad Bilal Kiryana Store, Tanda" }
            val shopAddr = if (isUrdu) settings.shopAddressUrdu else settings.shopAddressEnglish
            val dateStr = ReceiptFormatter.formatDateTime(order.createdAt?.toDate()?.time ?: System.currentTimeMillis())

            val pageWidth = 420
            val margin = 18f
            val contentWidth = pageWidth - (margin * 2f)
            val minRows = 15
            val totalRows = maxOf(order.items.size, minRows)
            val rowHeight = 20f
            val headerHeight = 76f
            val metaHeight = 54f
            val tableHeaderHeight = 24f
            val footerTotalsHeight = 78f
            val pageHeight = (margin * 2f + headerHeight + metaHeight + tableHeaderHeight + (totalRows * rowHeight) + footerTotalsHeight + 16f).toInt().coerceAtLeast(605)

            val document = PdfDocument()
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            val emeraldDark = Color.rgb(6, 78, 59)
            val emeraldMid = Color.rgb(6, 95, 70)
            val emeraldLight = Color.rgb(236, 253, 245)
            val emeraldZebra = Color.rgb(244, 249, 246)
            val goldBar = Color.rgb(212, 175, 55)
            val goldText = Color.rgb(253, 230, 138)
            val crimsonRed = Color.rgb(185, 28, 28)
            val borderSlate = Color.rgb(148, 163, 184)
            val darkInk = Color.rgb(17, 24, 39)

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f; color = emeraldDark }
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.8f; color = borderSlate }

            fun drawAlignedText(text: String, paint: TextPaint, x: Float, y: Float, w: Float, align: Layout.Alignment) {
                val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, w.toInt().coerceAtLeast(10))
                    .setAlignment(align).setLineSpacing(0f, 1.0f).setIncludePad(false).build()
                canvas.save(); canvas.translate(x, y); layout.draw(canvas); canvas.restore()
            }

            canvas.drawRoundRect(margin, margin, pageWidth - margin, pageHeight - margin, 10f, 10f, strokePaint)

            val bannerTop = margin
            val bannerBottom = bannerTop + headerHeight
            fillPaint.color = emeraldMid
            canvas.drawRect(margin + 1f, bannerTop + 1f, pageWidth - margin - 1f, bannerBottom - 18f, fillPaint)
            fillPaint.color = goldBar
            canvas.drawRect(margin + 1f, bannerBottom - 18f, pageWidth - margin - 1f, bannerBottom, fillPaint)

            val enHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 12.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val urHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = goldText; textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = emeraldDark; textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

            drawAlignedText(shopNameEn, enHeaderPaint, margin + 8f, bannerTop + 12f, contentWidth * 0.45f, Layout.Alignment.ALIGN_NORMAL)
            drawAlignedText(shopNameUr, urHeaderPaint, margin + (contentWidth * 0.43f), bannerTop + 8f, (contentWidth * 0.55f) - 8f, Layout.Alignment.ALIGN_OPPOSITE)

            val addressAndPhone = buildString {
                if (shopAddr.isNotBlank()) append(shopAddr)
                if (settings.shopPhone.isNotBlank()) { if (isNotEmpty()) append(" • "); append(settings.shopPhone) }
            }
            if (addressAndPhone.isNotBlank()) drawAlignedText(addressAndPhone, addrPaint, margin + 6f, bannerBottom - 14f, contentWidth - 12f, Layout.Alignment.ALIGN_CENTER)

            var y = bannerBottom + 8f
            val metaValPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = darkInk; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = goldText; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

            val pillW = 100f
            val pillX = margin + (contentWidth - pillW) / 2f
            fillPaint.color = emeraldMid
            canvas.drawRoundRect(pillX, y - 2f, pillX + pillW, y + 16f, 8f, 8f, fillPaint)
            drawAlignedText(if (isUrdu) "کیش میمو" else "CASH MEMO", badgePaint, pillX, y + 1f, pillW, Layout.Alignment.ALIGN_CENTER)

            drawAlignedText(if (isUrdu) "تاریخ: $dateStr" else "No: ${order.billNumber}", metaValPaint, margin + 8f, y + 1f, pillX - margin - 12f, Layout.Alignment.ALIGN_NORMAL)
            drawAlignedText(if (isUrdu) "نمبر: ${order.billNumber}" else "Date: $dateStr", metaValPaint, pillX + pillW + 4f, y + 1f, (pageWidth - margin) - (pillX + pillW) - 12f, Layout.Alignment.ALIGN_OPPOSITE)

            y += 24f
            val buyerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = emeraldDark; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val cleanCust = order.customerName.trim()
            val isWalkIn = cleanCust.isBlank() || cleanCust == "نقد کسٹمر" || cleanCust.equals("Walk-in Customer", ignoreCase = true)
            val custLineText = if (isUrdu) {
                if (isWalkIn) "نام خریدار:  ____________________________"
                else "نام خریدار:  $cleanCust  (${order.paymentMethodEnum().label(AppLanguage.URDU)})"
            } else {
                if (isWalkIn) "Buyer Name:  ____________________________"
                else "Buyer Name:  $cleanCust  (${order.paymentMethodEnum().label(AppLanguage.ENGLISH)})"
            }
            drawAlignedText(custLineText, buyerPaint, margin + 8f, y, contentWidth - 16f, if (isUrdu) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
            y += 16f
            canvas.drawLine(margin + 6f, y, pageWidth - margin - 6f, y, gridPaint)
            y += 6f

            val tableLeft = margin + 6f
            val tableRight = pageWidth - margin - 6f
            val tableWidth = tableRight - tableLeft
            val wSr = 34f
            val wQty = 52f
            val wRate = 50f
            val wAmount = 66f
            val wDesc = tableWidth - wSr - wQty - wRate - wAmount

            val xCol0 = tableLeft
            val xCol1 = if (isUrdu) xCol0 + wAmount else xCol0 + wSr
            val xCol2 = if (isUrdu) xCol1 + wRate else xCol1 + wQty
            val xCol3 = if (isUrdu) xCol2 + wDesc else xCol2 + wDesc
            val xCol4 = if (isUrdu) xCol3 + wQty else xCol3 + wRate

            fillPaint.color = emeraldMid
            canvas.drawRect(tableLeft, y, tableRight, y + tableHeaderHeight, fillPaint)
            canvas.drawRect(tableLeft, y, tableRight, y + tableHeaderHeight, strokePaint)

            val tblHdrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = goldText; textSize = 9.8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            if (isUrdu) {
                drawAlignedText("روپے", tblHdrPaint, xCol0, y + 6f, wAmount, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("نرخ", tblHdrPaint, xCol1, y + 6f, wRate, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("تفصیل اشیاء", tblHdrPaint, xCol2, y + 6f, wDesc, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("تعداد", tblHdrPaint, xCol3, y + 6f, wQty, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("نمبر شمار", tblHdrPaint, xCol4, y + 6f, wSr, Layout.Alignment.ALIGN_CENTER)
            } else {
                drawAlignedText("Sr#", tblHdrPaint, xCol0, y + 6f, wSr, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("Qty", tblHdrPaint, xCol1, y + 6f, wQty, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("Description", tblHdrPaint, xCol2, y + 6f, wDesc, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("Rate", tblHdrPaint, xCol3, y + 6f, wRate, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("Amount", tblHdrPaint, xCol4, y + 6f, wAmount, Layout.Alignment.ALIGN_CENTER)
            }
            listOf(xCol1, xCol2, xCol3, xCol4).forEach { canvas.drawLine(it, y, it, y + tableHeaderHeight, strokePaint) }

            y += tableHeaderHeight
            val rowsTopY = y
            val cellTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = darkInk; textSize = 9.8f }
            val cellBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = darkInk; textSize = 9.8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val srPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = emeraldDark; textSize = 9.8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

            for (r in 0 until totalRows) {
                val rowTop = rowsTopY + (r * rowHeight)
                val rowBottom = rowTop + rowHeight
                if (r % 2 == 1) {
                    fillPaint.color = emeraldZebra
                    canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, fillPaint)
                }
                canvas.drawLine(tableLeft, rowBottom, tableRight, rowBottom, gridPaint)

                val srStr = "${r + 1}"
                if (isUrdu) drawAlignedText(srStr, srPaint, xCol4, rowTop + 4f, wSr, Layout.Alignment.ALIGN_CENTER)
                else drawAlignedText(srStr, srPaint, xCol0, rowTop + 4f, wSr, Layout.Alignment.ALIGN_CENTER)

                if (r < order.items.size) {
                    val item = order.items[r]
                    val itemName = if (isUrdu) item.nameUrdu.ifBlank { item.nameEnglish } else item.nameEnglish.ifBlank { item.nameUrdu }
                    val qtyText = "${formatQty(item.quantity)} ${unitLabel(item.unit, language)}"
                    val rateText = "${item.unitPrice.toLong()}"
                    val amtText = "${item.lineTotal.toLong()}/-"

                    if (isUrdu) {
                        drawAlignedText(amtText, cellBoldPaint, xCol0 + 2f, rowTop + 4f, wAmount - 4f, Layout.Alignment.ALIGN_CENTER)
                        drawAlignedText(rateText, cellTextPaint, xCol1 + 2f, rowTop + 4f, wRate - 4f, Layout.Alignment.ALIGN_CENTER)
                        drawAlignedText(itemName, cellBoldPaint, xCol2 + 4f, rowTop + 4f, wDesc - 8f, Layout.Alignment.ALIGN_OPPOSITE)
                        drawAlignedText(qtyText, cellTextPaint, xCol3 + 2f, rowTop + 4f, wQty - 4f, Layout.Alignment.ALIGN_CENTER)
                    } else {
                        drawAlignedText(qtyText, cellTextPaint, xCol1 + 2f, rowTop + 4f, wQty - 4f, Layout.Alignment.ALIGN_CENTER)
                        drawAlignedText(itemName, cellBoldPaint, xCol2 + 4f, rowTop + 4f, wDesc - 8f, Layout.Alignment.ALIGN_NORMAL)
                        drawAlignedText(rateText, cellTextPaint, xCol3 + 2f, rowTop + 4f, wRate - 4f, Layout.Alignment.ALIGN_CENTER)
                        drawAlignedText(amtText, cellBoldPaint, xCol4 + 2f, rowTop + 4f, wAmount - 4f, Layout.Alignment.ALIGN_CENTER)
                    }
                }
            }

            val rowsBottomY = rowsTopY + (totalRows * rowHeight)
            canvas.drawRect(tableLeft, rowsTopY, tableRight, rowsBottomY, strokePaint)
            listOf(xCol1, xCol2, xCol3, xCol4).forEach { canvas.drawLine(it, rowsTopY, it, rowsBottomY, gridPaint) }

            y = rowsBottomY + 8f
            val totalBoxW = wAmount + wRate + 34f
            val totalBoxX = if (isUrdu) tableLeft else (tableRight - totalBoxW)
            val noteX = if (isUrdu) (totalBoxX + totalBoxW + 8f) else tableLeft
            val noteW = tableWidth - totalBoxW - 8f

            fillPaint.color = emeraldLight
            canvas.drawRoundRect(totalBoxX, y, totalBoxX + totalBoxW, y + 60f, 8f, 8f, fillPaint)
            strokePaint.color = emeraldDark
            canvas.drawRoundRect(totalBoxX, y, totalBoxX + totalBoxW, y + 60f, 8f, 8f, strokePaint)

            val totalBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = emeraldDark; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val subTotalPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = darkInk; textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val udharPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = crimsonRed; textSize = 9.8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

            drawAlignedText(if (isUrdu) "میزان کل:  ${formatPkr(order.totalAmount, language)}" else "TOTAL:  ${formatPkr(order.totalAmount, language)}", totalBoldPaint, totalBoxX + 4f, y + 6f, totalBoxW - 8f, Layout.Alignment.ALIGN_CENTER)
            drawAlignedText(if (isUrdu) "وصول شدہ:  ${formatPkr(order.paidAmount, language)}" else "Paid:  ${formatPkr(order.paidAmount, language)}", subTotalPaint, totalBoxX + 4f, y + 24f, totalBoxW - 8f, Layout.Alignment.ALIGN_CENTER)

            if (order.remainingUdhar > 0.0) {
                drawAlignedText(if (isUrdu) "بقایا ادھار:  ${formatPkr(order.remainingUdhar, language)}" else "Due:  ${formatPkr(order.remainingUdhar, language)}", udharPaint, totalBoxX + 4f, y + 40f, totalBoxW - 8f, Layout.Alignment.ALIGN_CENTER)
            } else if (order.discount > 0.0) {
                drawAlignedText(if (isUrdu) "رعایت:  -${formatPkr(order.discount, language)}" else "Disc:  -${formatPkr(order.discount, language)}", subTotalPaint, totalBoxX + 4f, y + 40f, totalBoxW - 8f, Layout.Alignment.ALIGN_CENTER)
            }

            val bhoolChookPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = emeraldDark; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val thanksPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = darkInk; textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            drawAlignedText(if (isUrdu) "بھول چوک لین دین معاف" else "Errors & Omissions Excepted", bhoolChookPaint, noteX, y + 10f, noteW, if (isUrdu) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
            drawAlignedText(if (isUrdu) "خریداری کا شکریہ! دوبارہ تشریف لائیں" else "Thank you for shopping! Visit again", thanksPaint, noteX, y + 32f, noteW, if (isUrdu) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)

            document.finishPage(page)
            val safeName = order.billNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val pdfFile = File(File(context.cacheDir, "receipts").apply { mkdirs() }, "CashMemo_${safeName}.pdf")
            FileOutputStream(pdfFile).use { document.writeTo(it) }
            document.close()
            pdfFile
        } catch (_: Throwable) { null }
    }

    fun generateKhataReceiptPdf(
        context: Context,
        entry: LedgerEntry,
        language: AppLanguage,
        settings: ShopSettings
    ): File? {
        val isUrdu = language == AppLanguage.URDU
        val isCredit = entry.entryTypeEnum() == LedgerEntryType.CREDIT
        val title = if (isUrdu) settings.storeNameUrdu else settings.storeNameEnglish
        val address = if (isUrdu) settings.shopAddressUrdu else settings.shopAddressEnglish
        val dateStr = ReceiptFormatter.formatDateTime(entry.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val accent = if (isCredit) Color.rgb(15, 81, 50) else Color.rgb(198, 30, 30)

        val lines = mutableListOf<PdfReceiptLine>()
        lines += PdfReceiptLine(if (isUrdu) "کسٹمر: ${entry.customerName}" else "Customer: ${entry.customerName}", bold = true)
        lines += PdfReceiptLine(if (isUrdu) "تاریخ: $dateStr" else "Date: $dateStr")
        lines += PdfReceiptLine(if (isCredit) (if (isUrdu) "قسم: نقد وصولی (+)" else "Type: Cash Payment") else (if (isUrdu) "قسم: ادھار اندراج" else "Type: Udhar Entry"))
        if (entry.note.isNotBlank()) lines += PdfReceiptLine(if (isUrdu) "تفصیل: ${entry.note}" else "Note: ${entry.note}")
        lines += PdfReceiptLine.divider()
        lines += PdfReceiptLine(if (isUrdu) "رقم: ${formatPkr(entry.amount, language)}" else "AMOUNT: ${formatPkr(entry.amount, language)}", bold = true, accentColor = accent)
        lines += PdfReceiptLine(if (isUrdu) "نیا بقایا ادھار: ${formatPkr(entry.balanceAfter, language)}" else "Updated Balance: ${formatPkr(entry.balanceAfter, language)}", bold = true)

        val safeId = entry.id.take(12).replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { System.currentTimeMillis().toString() }
        return renderReceiptPdf(
            context = context,
            fileName = "Khata_${safeId}.pdf",
            shopTitle = title,
            shopSubtitle = address,
            shopPhone = settings.shopPhone,
            badgeText = if (isCredit) (if (isUrdu) "کھاتہ وصولی رسید" else "KHATA RECEIPT") else (if (isUrdu) "ادھار کھاتہ رسید" else "UDHAR SLIP"),
            badgeColor = accent,
            lines = lines,
            footer = title
        )
    }

    fun generateRationCardReceiptPdf(
        context: Context,
        bill: RationCardBill,
        language: AppLanguage,
        settings: ShopSettings
    ): File? {
        val isUrdu = language == AppLanguage.URDU
        val title = if (isUrdu) settings.storeNameUrdu else settings.storeNameEnglish
        val address = if (isUrdu) settings.shopAddressUrdu else settings.shopAddressEnglish
        val dateStr = ReceiptFormatter.formatDateTime(bill.createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val netLimit = if (bill.netSubsidyLimit > 0.0) bill.netSubsidyLimit else (bill.subsidyLimit - bill.feeDeduction).coerceAtLeast(0.0)

        val lines = mutableListOf<PdfReceiptLine>()
        lines += PdfReceiptLine(if (isUrdu) "رسید نمبر: ${bill.billNumber} (${bill.monthKey})" else "Receipt #: ${bill.billNumber} (${bill.monthKey})", bold = true)
        lines += PdfReceiptLine(if (isUrdu) "تاریخ: $dateStr" else "Date: $dateStr")
        lines += PdfReceiptLine(if (isUrdu) "صارف: ${bill.beneficiaryName}" else "Customer: ${bill.beneficiaryName}")
        if (bill.phone.isNotBlank()) lines += PdfReceiptLine(if (isUrdu) "موبائل: ${bill.phone}" else "Phone: ${bill.phone}")
        if (bill.cnic.isNotBlank()) lines += PdfReceiptLine("CNIC: ${bill.cnic}")
        if (bill.cardNumber.isNotBlank()) lines += PdfReceiptLine(if (isUrdu) "کارڈ نمبر: ${bill.cardNumber}" else "Card #: ${bill.cardNumber}")
        lines += PdfReceiptLine.divider()
        bill.items.forEachIndexed { idx, item ->
            val name = if (isUrdu) item.nameUrdu else item.nameEnglish
            val qty = "${formatQty(item.quantity)} ${unitLabel(item.unit, language)}"
            lines += PdfReceiptLine("${idx + 1}. $name ($qty x ${item.unitPrice.toLong()}) = ${formatPkr(item.lineTotal, language)}")
        }
        lines += PdfReceiptLine.divider()
        lines += PdfReceiptLine(if (isUrdu) "کل کارڈ کوٹہ: ${formatPkr(bill.subsidyLimit, language)}" else "Total Quota: ${formatPkr(bill.subsidyLimit, language)}")
        lines += PdfReceiptLine(if (isUrdu) "سروس فیس کٹوتی: -${formatPkr(bill.feeDeduction, language)}" else "Fee Deduction: -${formatPkr(bill.feeDeduction, language)}")
        lines += PdfReceiptLine(if (isUrdu) "خالص راشن: ${formatPkr(netLimit, language)}" else "Net Value: ${formatPkr(netLimit, language)}", bold = true)
        lines += PdfReceiptLine(if (isUrdu) "کل POS بل: ${formatPkr(bill.totalAmount, language)}" else "TOTAL POS BILL: ${formatPkr(bill.totalAmount, language)}", bold = true, accentColor = Color.rgb(8, 76, 54))
        lines += PdfReceiptLine(if (isUrdu) "کارڈ سے کٹوتی: ${formatPkr(bill.subsidyUsed, language)}" else "Card Used: ${formatPkr(bill.subsidyUsed, language)}")
        if (bill.cashTopUp > 0.0) lines += PdfReceiptLine(if (isUrdu) "اضافی نقد: ${formatPkr(bill.cashTopUp, language)}" else "Cash Paid: ${formatPkr(bill.cashTopUp, language)}", bold = true)
        lines += PdfReceiptLine(if (isUrdu) "بقایا بیلنس: ${formatPkr(bill.remainingSubsidy, language)}" else "Remaining: ${formatPkr(bill.remainingSubsidy, language)}")

        val safeName = bill.billNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return renderReceiptPdf(
            context = context,
            fileName = "POS_${safeName}.pdf",
            shopTitle = title,
            shopSubtitle = address,
            shopPhone = settings.shopPhone,
            badgeText = if (isUrdu) "POS / پی او ایس کارڈ رسید" else "POS CARD RECEIPT",
            badgeColor = Color.rgb(200, 138, 20),
            lines = lines,
            footer = title
        )
    }

    private data class PdfReceiptLine(
        val text: String,
        val bold: Boolean = false,
        val accentColor: Int = Color.rgb(24, 24, 24),
        val isDivider: Boolean = false
    ) {
        companion object {
            fun divider() = PdfReceiptLine("", isDivider = true)
        }
    }

    private fun renderReceiptPdf(
        context: Context,
        fileName: String,
        shopTitle: String,
        shopSubtitle: String,
        shopPhone: String,
        badgeText: String,
        badgeColor: Int,
        lines: List<PdfReceiptLine>,
        footer: String
    ): File? {
        return try {
            val contentWidth = PDF_PAGE_WIDTH - (PDF_MARGIN * 2)
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15, 81, 50); textSize = 15f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(80, 80, 80); textSize = 10f }
            val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val bodyRegular = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(28, 28, 28); textSize = 11f }
            val bodyBold = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(18, 18, 18); textSize = 11.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(190, 190, 190); strokeWidth = 1f }

            fun makeLayout(text: String, paint: TextPaint, align: Layout.Alignment): StaticLayout =
                StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth).setAlignment(align).setLineSpacing(2f, 1.15f).setIncludePad(false).build()

            val titleLayout = makeLayout(shopTitle, titlePaint, Layout.Alignment.ALIGN_CENTER)
            val subLayout = if (shopSubtitle.isNotBlank()) makeLayout(shopSubtitle, subPaint, Layout.Alignment.ALIGN_CENTER) else null
            val phoneLayout = if (shopPhone.isNotBlank()) makeLayout(shopPhone, subPaint, Layout.Alignment.ALIGN_CENTER) else null
            val badgeLayout = makeLayout(badgeText, badgePaint, Layout.Alignment.ALIGN_CENTER)
            val footerLayout = makeLayout(footer, subPaint, Layout.Alignment.ALIGN_CENTER)

            val bodyLayouts = lines.map { line ->
                if (line.isDivider) null else makeLayout(line.text, TextPaint(if (line.bold) bodyBold else bodyRegular).apply { color = line.accentColor }, Layout.Alignment.ALIGN_NORMAL)
            }

            var totalHeight = PDF_MARGIN + titleLayout.height + 6 + (subLayout?.height?.plus(3) ?: 0) + (phoneLayout?.height?.plus(3) ?: 0) + badgeLayout.height + 20
            bodyLayouts.forEach { totalHeight += if (it == null) 14 else (it.height + 6) }
            totalHeight += 18 + footerLayout.height + PDF_MARGIN

            val document = PdfDocument()
            val page = document.startPage(PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, totalHeight.coerceAtLeast(280), 1).create())
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            var y = PDF_MARGIN.toFloat()
            canvas.save(); canvas.translate(PDF_MARGIN.toFloat(), y); titleLayout.draw(canvas); canvas.restore(); y += titleLayout.height + 4f
            if (subLayout != null) { canvas.save(); canvas.translate(PDF_MARGIN.toFloat(), y); subLayout.draw(canvas); canvas.restore(); y += subLayout.height + 3f }
            if (phoneLayout != null) { canvas.save(); canvas.translate(PDF_MARGIN.toFloat(), y); phoneLayout.draw(canvas); canvas.restore(); y += phoneLayout.height + 4f }

            val pillTop = y + 2f
            val pillBottom = pillTop + badgeLayout.height + 10f
            canvas.drawRoundRect(PDF_MARGIN + 18f, pillTop, (PDF_PAGE_WIDTH - PDF_MARGIN - 18f), pillBottom, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeColor })
            canvas.save(); canvas.translate(PDF_MARGIN.toFloat(), pillTop + 5f); badgeLayout.draw(canvas); canvas.restore()
            y = pillBottom + 12f

            bodyLayouts.forEach { layout ->
                if (layout == null) { canvas.drawLine(PDF_MARGIN.toFloat(), y + 6f, (PDF_PAGE_WIDTH - PDF_MARGIN).toFloat(), y + 6f, dividerPaint); y += 14f }
                else { canvas.save(); canvas.translate(PDF_MARGIN.toFloat(), y); layout.draw(canvas); canvas.restore(); y += layout.height + 6f }
            }

            y += 6f; canvas.drawLine(PDF_MARGIN.toFloat(), y, (PDF_PAGE_WIDTH - PDF_MARGIN).toFloat(), y, dividerPaint); y += 8f
            canvas.save(); canvas.translate(PDF_MARGIN.toFloat(), y); footerLayout.draw(canvas); canvas.restore()
            document.finishPage(page)

            val pdfFile = File(File(context.cacheDir, "receipts").apply { mkdirs() }, fileName)
            FileOutputStream(pdfFile).use { document.writeTo(it) }
            document.close()
            pdfFile
        } catch (_: Throwable) { null }
    }

    private fun sharePdfFileViaWhatsApp(context: Context, pdfFile: File, caption: String, subject: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val whatsappPackage = findInstalledWhatsAppPackage(context)
        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_SUBJECT, subject); putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (whatsappPackage != null) { baseIntent.setPackage(whatsappPackage); context.startActivity(baseIntent) }
            else context.startActivity(Intent.createChooser(baseIntent, subject).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (_: Exception) { shareText(context, caption, subject) }
    }

    private fun findInstalledWhatsAppPackage(context: Context): String? {
        val pm = context.packageManager
        return listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
            try { pm.getPackageInfo(pkg, 0); true } catch (_: Throwable) { false }
        }
    }

    fun formatPakistaniWhatsAppNumber(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.startsWith("0092") && digits.length >= 13 -> digits.removePrefix("00")
            digits.startsWith("92") && digits.length >= 11 -> digits
            digits.startsWith("0") && digits.length >= 10 -> "92" + digits.drop(1)
            digits.startsWith("3") && digits.length == 10 -> "92$digits"
            else -> digits
        }
    }

    fun formatPakistaniSmsNumber(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.startsWith("0092") && digits.length >= 13 -> "0" + digits.removePrefix("0092")
            digits.startsWith("92") && digits.length >= 12 -> "0" + digits.removePrefix("92")
            digits.startsWith("3") && digits.length == 10 -> "0$digits"
            else -> digits
        }
    }

    fun sendViaWhatsApp(context: Context, phone: String, message: String) {
        val waNumber = formatPakistaniWhatsAppNumber(phone)
        if (waNumber.isNotBlank()) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNumber?text=${Uri.encode(message)}")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                return
            } catch (_: Exception) {}
        }
        val whatsappPackage = findInstalledWhatsAppPackage(context)
        if (whatsappPackage != null) {
            try {
                context.startActivity(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; setPackage(whatsappPackage); putExtra(Intent.EXTRA_TEXT, message); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                return
            } catch (_: Exception) {}
        }
        shareText(context, message, "WhatsApp Receipt")
    }

    fun sendViaSimSms(context: Context, phone: String, message: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${formatPakistaniSmsNumber(phone)}")).apply { putExtra("sms_body", message); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (_: Exception) { shareText(context, message, "SMS Receipt") }
    }

    fun shareText(context: Context, text: String, title: String) {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, title); putExtra(Intent.EXTRA_TEXT, text) }, title).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
}
