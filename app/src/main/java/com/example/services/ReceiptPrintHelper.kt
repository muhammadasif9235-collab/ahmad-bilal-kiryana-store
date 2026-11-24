package com.example.services

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
            shopNameUrdu = settings?.storeNameUrdu?.ifBlank { "کریانہ اینڈ جنرل سٹور" } ?: "کریانہ اینڈ جنرل سٹور",
            shopNameEnglish = settings?.storeNameEnglish?.ifBlank { "Kiryana & General Store" } ?: "Kiryana & General Store",
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
            shopNameUrdu = settings?.storeNameUrdu?.ifBlank { "کریانہ اینڈ جنرل سٹور" } ?: "کریانہ اینڈ جنرل سٹور",
            shopNameEnglish = settings?.storeNameEnglish?.ifBlank { "Kiryana & General Store" } ?: "Kiryana & General Store",
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
            shopNameUrdu = settings?.storeNameUrdu?.ifBlank { "کریانہ اینڈ جنرل سٹور" } ?: "کریانہ اینڈ جنرل سٹور",
            shopNameEnglish = settings?.storeNameEnglish?.ifBlank { "Kiryana & General Store" } ?: "Kiryana & General Store",
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
                val printAdapter = view.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    }

    // --- ONE-TAP WHATSAPP TEXT SHARING (BILLING, KHATA, POS) ---

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

    // --- ONE-TAP REAL PDF RECEIPT GENERATION & WHATSAPP SHARING ---

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
        if (file != null) {
            sharePdfFileViaWhatsApp(context, file, caption, "Bill ${order.billNumber}")
        } else {
            sendViaWhatsApp(context, "", caption)
        }
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
        if (file != null) {
            sharePdfFileViaWhatsApp(context, file, caption, "Khata Receipt - ${entry.customerName}")
        } else {
            sendViaWhatsApp(context, "", caption)
        }
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
        if (file != null) {
            sharePdfFileViaWhatsApp(context, file, caption, "POS Receipt ${bill.billNumber}")
        } else {
            sendViaWhatsApp(context, bill.phone, caption)
        }
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
        val file = generateRoznamchaStatementPdf(
            context = context,
            periodLabel = periodLabel,
            entries = entries,
            totalIn = totalIn,
            totalOut = totalOut,
            netBalance = netBalance,
            language = language,
            settings = settings
        )
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
        if (file != null) {
            sharePdfFileViaWhatsApp(context, file, caption, "Roznamcha Statement - $periodLabel")
        } else {
            sendViaWhatsApp(context, "", caption)
        }
    }

    // --- LOW STOCK REPLENISHMENT REPORT (A4 PDF + WHATSAPP + VIEW/PRINT) ---

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
        val subject = if (language == AppLanguage.URDU) {
            "کم اسٹاک ری اسٹاک رپورٹ (Low Stock Report)"
        } else {
            "Low Stock Replenishment Report"
        }
        if (file != null) {
            if (preferWhatsApp) {
                sharePdfFileViaWhatsApp(context, file, caption, subject)
            } else {
                sharePdfFileSystemChooser(context, file, caption, subject)
            }
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
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewIntent)
                return
            } catch (_: Exception) {
                // Fall back to Android native Print / Save as PDF
            }
        }
        printLowStockReportWithSystemPrint(context, lowStockProducts, language, settings)
    }
        fun generateLowStockReportPdf(
        context: Context,
        lowStockProducts: List<Product>,
        language: AppLanguage,
        settings: ShopSettings
    ): File? {
        return try {
            val isUrdu = language == AppLanguage.URDU
            val pageWidth = 595
            val pageHeight = 842
            val margin = 28f
            val contentWidth = pageWidth - (margin * 2f)

            val sorted = lowStockProducts.sortedWith(
                compareBy<Product> { if (it.isOutOfStock()) 0 else 1 }
                    .thenBy { if (it.lowStockThreshold > 0.0) it.stockQty / it.lowStockThreshold else 0.0 }
            )
            val outCount = sorted.count { it.isOutOfStock() }
            val totalEstCost = sorted.sumOf { it.estimatedRestockCost() }
            val generatedStr = ReceiptFormatter.formatDateTime()
            val shopTitle = if (isUrdu) settings.storeNameUrdu else settings.storeNameEnglish
            val shopAddress = if (isUrdu) settings.shopAddressUrdu else settings.shopAddressEnglish

            val emeraldColor = Color.rgb(15, 81, 50)
            val crimsonColor = Color.rgb(185, 28, 28)
            val amberColor = Color.rgb(180, 83, 9)
            val darkInk = Color.rgb(28, 25, 23)
            val mutedInk = Color.rgb(92, 88, 84)

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subWhitePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(220, 252, 231)
                textSize = 10f
            }
            val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val kpiLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedInk
                textSize = 9.5f
            }
            val kpiValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emeraldColor
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val tableHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellRegularPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkInk
                textSize = 9.5f
            }
            val cellBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkInk
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedInk
                textSize = 8.2f
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(214, 211, 209)
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

            val colWidths = floatArrayOf(26f, 188f, 68f, 65f, 68f, 58f, 66f)
            val colStarts = FloatArray(colWidths.size)
            var accX = margin
            for (i in colWidths.indices) {
                colStarts[i] = accX
                accX += colWidths[i]
            }

            fun drawStaticText(
                canvas: android.graphics.Canvas,
                text: String,
                paint: TextPaint,
                x: Float,
                y: Float,
                width: Int,
                align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
            ): Int {
                val safeW = width.coerceAtLeast(10)
                val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, safeW)
                    .setAlignment(align)
                    .setLineSpacing(1.5f, 1.08f)
                    .setIncludePad(false)
                    .build()
                canvas.save()
                canvas.translate(x, y)
                layout.draw(canvas)
                canvas.restore()
                return layout.height
            }

            val document = PdfDocument()
            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            fun drawTableHeader(startY: Float): Float {
                val hdrH = 24f
                fillPaint.color = emeraldColor
                canvas.drawRoundRect(margin, startY, pageWidth - margin, startY + hdrH, 4f, 4f, fillPaint)
                val headers = if (isUrdu) {
                    listOf("#", "پروڈکٹ / آئٹم", "موجودہ", "کم حد", "آرڈر مقدار", "خرید ریٹ", "لاگت")
                } else {
                    listOf("#", "Product Name", "In Stock", "Min Limit", "Reorder", "Unit Cost", "Est. Cost")
                }
                headers.forEachIndexed { idx, label ->
                    val align = when (idx) {
                        1 -> Layout.Alignment.ALIGN_NORMAL
                        5, 6 -> Layout.Alignment.ALIGN_OPPOSITE
                        else -> Layout.Alignment.ALIGN_CENTER
                    }
                    drawStaticText(
                        canvas,
                        label,
                        tableHeaderPaint,
                        colStarts[idx] + 3f,
                        startY + 6f,
                        (colWidths[idx] - 6f).toInt(),
                        align
                    )
                }
                return startY + hdrH + 2f
            }

            fun drawPageFooter(pageIdx: Int) {
                val footerY = pageHeight - margin - 14f
                canvas.drawLine(margin, footerY - 5f, pageWidth - margin, footerY - 5f, borderPaint)
                val footerText = if (isUrdu) {
                    "$shopTitle • کم اسٹاک ری اسٹاک رپورٹ • صفحہ $pageIdx"
                } else {
                    "$shopTitle • Low Stock Replenishment Report • Page $pageIdx"
                }
                drawStaticText(canvas, footerText, cellSubPaint, margin, footerY, contentWidth.toInt(), Layout.Alignment.ALIGN_CENTER)
            }

            val bannerTop = margin
            val bannerHeight = 82f
            fillPaint.color = emeraldColor
            canvas.drawRoundRect(margin, bannerTop, pageWidth - margin, bannerTop + bannerHeight, 10f, 10f, fillPaint)

            var y = bannerTop + 10f
            y += drawStaticText(canvas, shopTitle, titlePaint, margin + 12f, y, (contentWidth - 24f).toInt(), Layout.Alignment.ALIGN_CENTER) + 3f
            val subLine = buildString {
                if (shopAddress.isNotBlank()) append(shopAddress)
                if (settings.shopPhone.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(settings.shopPhone)
                }
            }
            if (subLine.isNotBlank()) {
                y += drawStaticText(canvas, subLine, subWhitePaint, margin + 12f, y, (contentWidth - 24f).toInt(), Layout.Alignment.ALIGN_CENTER) + 5f
            }

            val badgeText = if (isUrdu) {
                "کم اسٹاک ری اسٹاک رپورٹ — LOW STOCK REPLENISHMENT REPORT ($generatedStr)"
            } else {
                "LOW STOCK REPLENISHMENT REPORT • $generatedStr"
            }
            val badgeW = (contentWidth - 60f)
            val badgeX = margin + 30f
            fillPaint.color = crimsonColor
            canvas.drawRoundRect(badgeX, y, badgeX + badgeW, y + 20f, 10f, 10f, fillPaint)
            drawStaticText(canvas, badgeText, badgePaint, badgeX + 6f, y + 4f, (badgeW - 12f).toInt(), Layout.Alignment.ALIGN_CENTER)

            y = bannerTop + bannerHeight + 12f

            val kpiGap = 10f
            val kpiW = (contentWidth - (kpiGap * 2f)) / 3f
            val kpiH = 48f
            val kpiCards = listOf(
                Triple(if (isUrdu) "کم اسٹاک اشیاء" else "Low Stock Products", "${sorted.size}", amberColor),
                Triple(if (isUrdu) "مکمل ختم اشیاء" else "Out of Stock (Critical)", "$outCount", crimsonColor),
                Triple(if (isUrdu) "اندازاً خریداری لاگت" else "Est. Replenish Cost", formatPkr(totalEstCost, language), emeraldColor)
            )
            kpiCards.forEachIndexed { idx, (label, value, accent) ->
                val kx = margin + idx * (kpiW + kpiGap)
                fillPaint.color = Color.rgb(250, 250, 249)
                canvas.drawRoundRect(kx, y, kx + kpiW, y + kpiH, 8f, 8f, fillPaint)
                canvas.drawRoundRect(kx, y, kx + kpiW, y + kpiH, 8f, 8f, borderPaint)
                drawStaticText(canvas, label, kpiLabelPaint, kx + 6f, y + 7f, (kpiW - 12f).toInt(), Layout.Alignment.ALIGN_CENTER)
                val valPaint = TextPaint(kpiValuePaint).apply { color = accent }
                drawStaticText(canvas, value, valPaint, kx + 6f, y + 22f, (kpiW - 12f).toInt(), Layout.Alignment.ALIGN_CENTER)
            }
            y += kpiH + 14f

            y = drawTableHeader(y)

            val maxBodyY = pageHeight - margin - 52f
            if (sorted.isEmpty()) {
                val emptyMsg = if (isUrdu) {
                    "تمام اشیاء کا اسٹاک مقررہ کم از کم حد سے اوپر ہے۔ ری اسٹاک کی ضرورت نہیں۔"
                } else {
                    "All products are safely above their minimum stock threshold. No replenishment needed."
                }
                y += 18f
                drawStaticText(canvas, emptyMsg, cellBoldPaint, margin, y, contentWidth.toInt(), Layout.Alignment.ALIGN_CENTER)
                y += 30f
            } else {
                sorted.forEachIndexed { idx, prod ->
                    val unitTxt = unitLabel(prod.unit, language)
                    val primaryName = prod.nameUrdu.ifBlank { prod.nameEnglish }
                    val secondaryName = if (prod.nameEnglish.isNotBlank() && prod.nameEnglish != primaryName) prod.nameEnglish else ""
                    val rowHeight = if (secondaryName.isNotBlank()) 32f else 24f

                    if (y + rowHeight > maxBodyY) {
                        drawPageFooter(pageNumber)
                        document.finishPage(page)
                        pageNumber += 1
                        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                        canvas = page.canvas
                        canvas.drawColor(Color.WHITE)
                        y = margin
                        val contHeader = if (isUrdu) "$shopTitle — کم اسٹاک رپورٹ (جاری)" else "$shopTitle — Low Stock Report (Continued)"
                        y += drawStaticText(canvas, contHeader, cellBoldPaint, margin, y, contentWidth.toInt(), Layout.Alignment.ALIGN_NORMAL) + 8f
                        y = drawTableHeader(y)
                    }

                    fillPaint.color = when {
                        prod.isOutOfStock() -> Color.rgb(254, 226, 226)
                        idx % 2 == 0 -> Color.rgb(255, 251, 235)
                        else -> Color.rgb(255, 255, 255)
                    }
                    canvas.drawRect(margin, y, pageWidth - margin, y + rowHeight, fillPaint)
                    canvas.drawRect(margin, y, pageWidth - margin, y + rowHeight, borderPaint)

                    val stockPaint = TextPaint(cellBoldPaint).apply {
                        color = if (prod.isOutOfStock()) crimsonColor else amberColor
                    }
                    val reorderPaint = TextPaint(cellBoldPaint).apply { color = emeraldColor }

                    drawStaticText(canvas, "${idx + 1}", cellRegularPaint, colStarts[0] + 2f, y + 6f, (colWidths[0] - 4f).toInt(), Layout.Alignment.ALIGN_CENTER)
                    drawStaticText(canvas, primaryName.take(36), cellBoldPaint, colStarts[1] + 4f, y + 4f, (colWidths[1] - 8f).toInt(), Layout.Alignment.ALIGN_NORMAL)
                    if (secondaryName.isNotBlank()) {
                        drawStaticText(canvas, secondaryName.take(36), cellSubPaint, colStarts[1] + 4f, y + 17f, (colWidths[1] - 8f).toInt(), Layout.Alignment.ALIGN_NORMAL)
                    }
                    val stockTxt = "${formatQty(prod.stockQty)} $unitTxt"
                    drawStaticText(canvas, stockTxt, stockPaint, colStarts[2] + 2f, y + 4f, (colWidths[2] - 4f).toInt(), Layout.Alignment.ALIGN_CENTER)
                    val badgeStatus = if (prod.isOutOfStock()) (if (isUrdu) "ختم" else "OUT") else (if (isUrdu) "کم" else "LOW")
                    drawStaticText(canvas, badgeStatus, stockPaint, colStarts[2] + 2f, y + 16f, (colWidths[2] - 4f).toInt(), Layout.Alignment.ALIGN_CENTER)

                    drawStaticText(canvas, "${formatQty(prod.lowStockThreshold)} $unitTxt", cellRegularPaint, colStarts[3] + 2f, y + 6f, (colWidths[3] - 4f).toInt(), Layout.Alignment.ALIGN_CENTER)
                    drawStaticText(canvas, "+${formatQty(prod.suggestedRestockQty())} $unitTxt", reorderPaint, colStarts[4] + 2f, y + 6f, (colWidths[4] - 4f).toInt(), Layout.Alignment.ALIGN_CENTER)
                    drawStaticText(canvas, "Rs.${prod.costPrice.toLong()}", cellRegularPaint, colStarts[5] + 2f, y + 6f, (colWidths[5] - 6f).toInt(), Layout.Alignment.ALIGN_OPPOSITE)
                    drawStaticText(canvas, "Rs.${prod.estimatedRestockCost().toLong()}", cellBoldPaint, colStarts[6] + 2f, y + 6f, (colWidths[6] - 6f).toInt(), Layout.Alignment.ALIGN_OPPOSITE)

                    y += rowHeight
                }
            }

            if (y + 44f > pageHeight - margin - 20f) {
                drawPageFooter(pageNumber)
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                y = margin
            }
            y += 10f
            fillPaint.color = Color.rgb(236, 253, 245)
            canvas.drawRoundRect(margin, y, pageWidth - margin, y + 32f, 8f, 8f, fillPaint)
            val greenStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emeraldColor
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }
            canvas.drawRoundRect(margin, y, pageWidth - margin, y + 32f, 8f, 8f, greenStroke)
            val totalLabel = if (isUrdu) {
                "کل اندازاً ری اسٹاک لاگت (${sorted.size} اشیاء):"
            } else {
                "TOTAL ESTIMATED REPLENISHMENT COST (${sorted.size} items):"
            }
            val totalPaint = TextPaint(cellBoldPaint).apply {
                color = emeraldColor
                textSize = 11.5f
            }
            drawStaticText(canvas, totalLabel, totalPaint, margin + 10f, y + 9f, (contentWidth * 0.65f).toInt(), Layout.Alignment.ALIGN_NORMAL)
            drawStaticText(canvas, formatPkr(totalEstCost, language), totalPaint, margin + (contentWidth * 0.65f), y + 9f, (contentWidth * 0.35f - 10f).toInt(), Layout.Alignment.ALIGN_OPPOSITE)

            drawPageFooter(pageNumber)
            document.finishPage(page)

            val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val pdfFile = File(receiptsDir, "Low_Stock_Report_$dateTag.pdf")
            FileOutputStream(pdfFile).use { fos ->
                document.writeTo(fos)
            }
            document.close()
            pdfFile
        } catch (_: Throwable) {
            null
        }
    }

    private fun sharePdfFileSystemChooser(
        context: Context,
        pdfFile: File,
        caption: String,
        subject: String
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(
                Intent.createChooser(shareIntent, subject).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
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
        val generatedStr = ReceiptFormatter.formatDateTime()
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.US)

        val lines = mutableListOf<PdfReceiptLine>()
        lines += PdfReceiptLine(if (isUrdu) "مدت: $periodLabel" else "Period: $periodLabel", bold = true)
        lines += PdfReceiptLine(if (isUrdu) "تاریخ: $generatedStr" else "Generated: $generatedStr")
        lines += PdfReceiptLine.divider()
        lines += PdfReceiptLine(
            if (isUrdu) "کل دستی کیش اِن (آمدن/سیل): +${formatPkr(totalIn, language)}"
            else "Total Cash In: +${formatPkr(totalIn, language)}",
            bold = true,
            accentColor = Color.rgb(15, 81, 50)
        )
        lines += PdfReceiptLine(
            if (isUrdu) "کل کیش آؤٹ (اخراجات/خریداری): -${formatPkr(totalOut, language)}"
            else "Total Cash Out: -${formatPkr(totalOut, language)}",
            bold = true,
            accentColor = Color.rgb(198, 30, 30)
        )
        lines += PdfReceiptLine(
            if (isUrdu) "خالص روزنامچہ بیلنس (بچت): ${formatPkr(netBalance, language)}"
            else "NET CASH BALANCE: ${formatPkr(netBalance, language)}",
            bold = true,
            accentColor = if (netBalance >= 0.0) Color.rgb(15, 81, 50) else Color.rgb(198, 30, 30)
        )
        lines += PdfReceiptLine.divider()

        if (entries.isEmpty()) {
            lines += PdfReceiptLine(if (isUrdu) "کوئی انٹری موجود نہیں" else "No entries recorded")
        } else {
            entries.take(80).forEachIndexed { idx, entry ->
                val isIn = entry.entryTypeEnum() == DailyCashType.CASH_IN
                val sign = if (isIn) "+" else "-"
                val mode = when (entry.paymentSourceEnum()) {
                    DailyPaymentSource.CASH -> if (isUrdu) "نقد" else "Cash"
                    DailyPaymentSource.HBL_QR -> if (isUrdu) "بینک QR" else "Bank QR"
                    DailyPaymentSource.ONLINE -> if (isUrdu) "آن لائن" else "Online"
                }
                val dateTxt = dateFmt.format(Date(entry.entryDateMs))
                val label = entry.categoryOrTitle.ifBlank { if (isIn) "Cash In" else "Cash Out" }
                lines += PdfReceiptLine(
                    text = "${idx + 1}. [$dateTxt] $label ($mode)  $sign${formatPkr(entry.amount, language)}",
                    bold = false,
                    accentColor = if (isIn) Color.rgb(15, 81, 50) else Color.rgb(185, 28, 28)
                )
             if (entry.note.isNotBlank()) {
                    lines += PdfReceiptLine(text = "   ${entry.note}", accentColor = Color.rgb(90, 90, 90))
                }
            }
        }

        val safePeriod = periodLabel.replace(Regex("[^A-Za-z0-9_-]"), "_").take(24).ifBlank { "Statement" }
        return renderReceiptPdf(
            context = context,
            fileName = "Roznamcha_${safePeriod}_${System.currentTimeMillis() % 10000}.pdf",
            shopTitle = title,
            shopSubtitle = address,
            shopPhone = settings.shopPhone,
            badgeText = if (isUrdu) "روزنامچہ — دستی کیش اسٹیٹمنٹ" else "DAILY ROZNAMCHA STATEMENT",
            badgeColor = Color.rgb(15, 81, 50),
            lines = lines,
            footer = if (isUrdu) "شکریہ — $title" else "Thank you — $title"
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
            val shopNameUr = settings.storeNameUrdu.ifBlank { "کریانہ اینڈ جنرل سٹور" }
            val shopNameEn = settings.storeNameEnglish.ifBlank { "Kiryana & General Store" }
            val shopAddr = if (isUrdu) settings.shopAddressUrdu else settings.shopAddressEnglish
            val dateStr = ReceiptFormatter.formatDateTime(order.createdAt?.toDate()?.time ?: System.currentTimeMillis())

            // Traditional Pakistani A5 Cash Memo Pad Design
            val pageWidth = 420
            val margin = 20f
            val contentWidth = pageWidth - (margin * 2f)

            val minRows = 14
            val totalRows = maxOf(order.items.size, minRows)
            val rowHeight = 20f
            val headerHeight = 76f
            val metaHeight = 52f
            val tableHeaderHeight = 24f
            val footerTotalsHeight = 76f
            val pageHeight = (margin * 2f + headerHeight + metaHeight + tableHeaderHeight + (totalRows * rowHeight) + footerTotalsHeight + 16f)
                .toInt()
                .coerceAtLeast(595)

            val document = PdfDocument()
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            val emeraldDark = Color.rgb(6, 78, 59)
            val goldAmber = Color.rgb(245, 158, 11)
            val goldLight = Color.rgb(254, 243, 199)
            val memoBlue = Color.rgb(30, 64, 175)
            val crimsonRed = Color.rgb(185, 28, 28)
            val ruleBlue = Color.rgb(147, 197, 253)
            val darkInk = Color.rgb(24, 24, 27)

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                color = emeraldDark
            }
            val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 0.85f
                color = ruleBlue
            }

            fun drawAlignedText(
                text: String,
                paint: TextPaint,
                x: Float,
                y: Float,
                w: Float,
                align: Layout.Alignment
            ) {
                val safeW = w.toInt().coerceAtLeast(10)
                val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, safeW)
                    .setAlignment(align)
                    .setLineSpacing(0f, 1.0f)
                    .setIncludePad(false)
                    .build()
                canvas.save()
                canvas.translate(x, y)
                layout.draw(canvas)
                canvas.restore()
            }

            // 1. Outer Cash Memo Border
            canvas.drawRoundRect(margin, margin, pageWidth - margin, pageHeight - margin, 8f, 8f, strokePaint)

            // 2. Top Shop Banner (Dynamic Shop Name from User's Settings)
            val bannerTop = margin
            val bannerBottom = bannerTop + headerHeight
            fillPaint.color = goldLight
            canvas.drawRect(margin + 1f, bannerTop + 1f, pageWidth - margin - 1f, bannerBottom - 18f, fillPaint)

            fillPaint.color = goldAmber
            canvas.drawRect(margin + 1f, bannerBottom - 18f, pageWidth - margin - 1f, bannerBottom, fillPaint)

            val enHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = memoBlue
                textSize = 13.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val urHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = crimsonRed
                textSize = 16.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkInk
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            drawAlignedText(shopNameEn, enHeaderPaint, margin + 8f, bannerTop + 10f, (contentWidth * 0.48f), Layout.Alignment.ALIGN_NORMAL)
            drawAlignedText(shopNameUr, urHeaderPaint, margin + (contentWidth * 0.46f), bannerTop + 8f, (contentWidth * 0.52f) - 8f, Layout.Alignment.ALIGN_OPPOSITE)

            val addressAndPhone = buildString {
                if (shopAddr.isNotBlank()) append(shopAddr)
                if (settings.shopPhone.isNotBlank()) {
                    if (isNotEmpty()) append("  •  ")
                    append(settings.shopPhone)
                }
            }
            if (addressAndPhone.isNotBlank()) {
                drawAlignedText(addressAndPhone, addrPaint, margin + 6f, bannerBottom - 14f, contentWidth - 12f, Layout.Alignment.ALIGN_CENTER)
            }

            // 3. Meta Section: Bill #, Cash Memo Badge, Date, Customer Name
            var y = bannerBottom + 8f
            val metaLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = memoBlue
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val metaValPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkInk
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val pillW = 104f
            val pillX = margin + (contentWidth - pillW) / 2f
            fillPaint.color = emeraldDark
            canvas.drawRoundRect(pillX, y - 2f, pillX + pillW, y + 16f, 6f, 6f, fillPaint)
            drawAlignedText(if (isUrdu) "کیش میمو" else "CASH MEMO", badgePaint, pillX, y + 1f, pillW, Layout.Alignment.ALIGN_CENTER)

            val leftMeta = if (isUrdu) "تاریخ: $dateStr" else "No: ${order.billNumber}"
            val rightMeta = if (isUrdu) "نمبر: ${order.billNumber}" else "Date: $dateStr"
            drawAlignedText(leftMeta, metaValPaint, margin + 8f, y + 1f, pillX - margin - 12f, Layout.Alignment.ALIGN_NORMAL)
            drawAlignedText(rightMeta, metaValPaint, pillX + pillW + 4f, y + 1f, (pageWidth - margin) - (pillX + pillW) - 12f, Layout.Alignment.ALIGN_OPPOSITE)

            y += 24f
            val custLineText = if (isUrdu) {
                "نام خریدار:   ${order.customerName}   (${order.paymentMethodEnum().label(AppLanguage.URDU)})"
            } else {
                "Customer Name:   ${order.customerName}   (${order.paymentMethodEnum().label(AppLanguage.ENGLISH)})"
            }
            drawAlignedText(
                custLineText,
                metaLabelPaint,
                margin + 10f,
                y,
                contentWidth - 20f,
                if (isUrdu) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
            )
            y += 15f
            canvas.drawLine(margin + 8f, y, pageWidth - margin - 8f, y, rulePaint)
            y += 6f

            // 4. Ruled Cash Memo Table (Urdu RTL vs English LTR)
            val tableLeft = margin + 6f
            val tableRight = pageWidth - margin - 6f
            val tableWidth = tableRight - tableLeft

            val wQty = 54f
            val wRate = 58f
            val wAmount = 74f
            val wDesc = tableWidth - wQty - wRate - wAmount

            val xCol0: Float
            val xCol1: Float
            val xCol2: Float
            val xCol3: Float
            if (isUrdu) {
                xCol0 = tableLeft
                xCol1 = xCol0 + wAmount
                xCol2 = xCol1 + wRate
                xCol3 = xCol2 + wDesc
            } else {
                xCol0 = tableLeft
                xCol1 = xCol0 + wQty
                xCol2 = xCol1 + wDesc
                xCol3 = xCol2 + wRate
            }

            fillPaint.color = goldAmber
            canvas.drawRoundRect(tableLeft, y, tableRight, y + tableHeaderHeight, 6f, 6f, fillPaint)
            strokePaint.color = emeraldDark
            canvas.drawRoundRect(tableLeft, y, tableRight, y + tableHeaderHeight, 6f, 6f, strokePaint)

            val tblHdrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkInk
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            if (isUrdu) {
                drawAlignedText("روپے", tblHdrPaint, xCol0, y + 6f, wAmount, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("نرخ", tblHdrPaint, xCol1, y + 6f, wRate, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("تفصیل اشیاء", tblHdrPaint, xCol2, y + 6f, wDesc, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("تعداد", tblHdrPaint, xCol3, y + 6f, wQty, Layout.Alignment.ALIGN_CENTER)
            } else {
                drawAlignedText("Qty", tblHdrPaint, xCol0, y + 6f, wQty, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("Description", tblHdrPaint, xCol1, y + 6f, wDesc, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("Rate", tblHdrPaint, xCol2, y + 6f, wRate, Layout.Alignment.ALIGN_CENTER)
                drawAlignedText("Amount", tblHdrPaint, xCol3, y + 6f, wAmount, Layout.Alignment.ALIGN_CENTER)
            }

            canvas.drawLine(xCol1, y, xCol1, y + tableHeaderHeight, strokePaint)
            canvas.drawLine(xCol2, y, xCol2, y + tableHeaderHeight, strokePaint)
            canvas.drawLine(xCol3, y, xCol3, y + tableHeaderHeight, strokePaint)

            y += tableHeaderHeight
            val rowsTopY = y

            val cellTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkInk
                textSize = 10f
            }
            val cellBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkInk
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            for (r in 0 until totalRows) {
                val rowTop = rowsTopY + (r * rowHeight)
                val rowBottom = rowTop + rowHeight
                if (r % 2 == 1) {
                    fillPaint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, fillPaint)
                }
                canvas.drawLine(tableLeft, rowBottom, tableRight, rowBottom, rulePaint)

                if (r < order.items.size) {
                    val item = order.items[r]
                    val itemName = if (isUrdu) item.nameUrdu else item.nameEnglish
                    val qtyText = "${formatQty(item.quantity)} ${unitLabel(item.unit, language)}"
                    val rateText = "${item.unitPrice.toLong()}"
                    val amtText = "${item.lineTotal.toLong()}/-"

                    if (isUrdu) {
                        drawAlignedText(amtText, cellBoldPaint, xCol0 + 4f, rowTop + 4f, wAmount - 8f, Layout.Alignment.ALIGN_CENTER)
                        drawAlignedText(rateText, cellTextPaint, xCol1 + 2f, rowTop + 4f, wRate - 4f, Layout.Alignment.ALIGN_CENTER)
                        drawAlignedText(itemName, cellBoldPaint, xCol2 + 4f, rowTop + 4f, wDesc - 10f, Layout.Alignment.ALIGN_OPPOSITE)
                        drawAlignedText(qtyText, cellTextPaint, xCol3 + 2f, rowTop + 4f, wQty - 4f, Layout.Alignment.ALIGN_CENTER)
                    } else {
                        drawAlignedText(qtyText, cellTextPaint, xCol0 + 2f, rowTop + 4f, wQty - 4f, Layout.Alignment.ALIGN_CENTER)
                        drawAlignedText(itemName, cellBoldPaint, xCol1 + 6f, rowTop + 4f, wDesc - 10f, Layout.Alignment.ALIGN_NORMAL)
                        drawAlignedText(rateText, cellTextPaint, xCol2 + 2f, rowTop + 4f, wRate - 4f, Layout.Alignment.ALIGN_CENTER)
                        drawAlignedText(amtText, cellBoldPaint, xCol3 + 4f, rowTop + 4f, wAmount - 8f, Layout.Alignment.ALIGN_CENTER)
                    }
                }
            }

            val rowsBottomY = rowsTopY + (totalRows * rowHeight)
            canvas.drawRect(tableLeft, rowsTopY, tableRight, rowsBottomY, strokePaint)
            canvas.drawLine(xCol1, rowsTopY, xCol1, rowsBottomY, rulePaint)
            canvas.drawLine(xCol2, rowsTopY, xCol2, rowsBottomY, rulePaint)
            canvas.drawLine(xCol3, rowsTopY, xCol3, rowsBottomY, rulePaint)

            // 5. Bottom Meezan / Total Box + "بھول چوک لین دین"
            y = rowsBottomY + 6f
            val totalBoxW = wAmount + wRate + 36f
            val totalBoxX = if (isUrdu) tableLeft else (tableRight - totalBoxW)
            val noteX = if (isUrdu) (totalBoxX + totalBoxW + 8f) else tableLeft
            val noteW = tableWidth - totalBoxW - 8f

            fillPaint.color = goldLight
            canvas.drawRoundRect(totalBoxX, y, totalBoxX + totalBoxW, y + 60f, 6f, 6f, fillPaint)
            strokePaint.color = emeraldDark
            canvas.drawRoundRect(totalBoxX, y, totalBoxX + totalBoxW, y + 60f, 6f, 6f, strokePaint)

            val totalBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emeraldDark
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subTotalPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkInk
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val udharPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = crimsonRed
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val meezanStr = if (isUrdu) "میزان:  ${formatPkr(order.totalAmount, language)}" else "TOTAL:  ${formatPkr(order.totalAmount, language)}"
            val paidStr = if (isUrdu) "وصول شدہ:  ${formatPkr(order.paidAmount, language)}" else "Paid:  ${formatPkr(order.paidAmount, language)}"
            drawAlignedText(meezanStr, totalBoldPaint, totalBoxX + 6f, y + 6f, totalBoxW - 12f, Layout.Alignment.ALIGN_CENTER)
            drawAlignedText(paidStr, subTotalPaint, totalBoxX + 6f, y + 24f, totalBoxW - 12f, Layout.Alignment.ALIGN_CENTER)

            if (order.remainingUdhar > 0.0) {
                val remStr = if (isUrdu) "بقایا ادھار:  ${formatPkr(order.remainingUdhar, language)}" else "Balance Due:  ${formatPkr(order.remainingUdhar, language)}"
                drawAlignedText(remStr, udharPaint, totalBoxX + 6f, y + 40f, totalBoxW - 12f, Layout.Alignment.ALIGN_CENTER)
            } else if (order.discount > 0.0) {
                val discStr = if (isUrdu) "رعایت:  -${formatPkr(order.discount, language)}" else "Discount:  -${formatPkr(order.discount, language)}"
                drawAlignedText(discStr, subTotalPaint, totalBoxX + 6f, y + 40f, totalBoxW - 12f, Layout.Alignment.ALIGN_CENTER)
            }

            val bhoolChookPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = crimsonRed
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val thanksPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emeraldDark
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            drawAlignedText(
                if (isUrdu) "بھول چوک لین دین" else "Errors & Omissions Excepted",
                bhoolChookPaint,
                noteX,
                y + 10f,
                noteW,
                if (isUrdu) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
            )
            drawAlignedText(
                if (isUrdu) "خریداری کا شکریہ! دوبارہ تشریف لائیں" else "Thank you for shopping! Visit again",
                thanksPaint,
                noteX,
                y + 32f,
                noteW,
                if (isUrdu) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
            )

            document.finishPage(page)

            val safeName = order.billNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val pdfFile = File(receiptsDir, "CashMemo_${safeName}.pdf")
            FileOutputStream(pdfFile).use { fos ->
                document.writeTo(fos)
            }
            document.close()
            pdfFile
        } catch (_: Throwable) {
            null
        }
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
        lines += PdfReceiptLine(
            if (isCredit) {
                if (isUrdu) "قسم: نقد وصولی (+)" else "Type: Cash Payment Received"
            } else {
                if (isUrdu) "قسم: ادھار اندراج" else "Type: Udhar Credit Entry"
            }
        )
        if (entry.note.isNotBlank()) {
            lines += PdfReceiptLine(if (isUrdu) "تفصیل: ${entry.note}" else "Note: ${entry.note}")
        }
        lines += PdfReceiptLine.divider()
        lines += PdfReceiptLine(
            if (isUrdu) "رقم: ${formatPkr(entry.amount, language)}" else "AMOUNT: ${formatPkr(entry.amount, language)}",
            bold = true,
            accentColor = accent
        )
        lines += PdfReceiptLine(
            if (isUrdu) "نیا بقایا ادھار: ${formatPkr(entry.balanceAfter, language)}"
            else "Updated Balance: ${formatPkr(entry.balanceAfter, language)}",
            bold = true
        )

        val safeId = entry.id.take(12).replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { System.currentTimeMillis().toString() }
        return renderReceiptPdf(
            context = context,
            fileName = "Khata_${safeId}.pdf",
            shopTitle = title,
            shopSubtitle = address,
            shopPhone = settings.shopPhone,
            badgeText = if (isCredit) {
                if (isUrdu) "کھاتہ وصولی رسید" else "KHATA PAYMENT RECEIPT"
            } else {
                if (isUrdu) "ادھار کھاتہ رسید" else "KHATA UDHAR SLIP"
            },
            badgeColor = accent,
            lines = lines,
            footer = if (isUrdu) "شکریہ — $title" else "Thank you — $title"
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
        if (bill.cardUid.isNotBlank()) lines += PdfReceiptLine("NFC UID: ${bill.cardUid}")
        lines += PdfReceiptLine.divider()
        bill.items.forEachIndexed { idx, item ->
            val name = if (isUrdu) item.nameUrdu else item.nameEnglish
            val qty = "${formatQty(item.quantity)} ${unitLabel(item.unit, language)}"
            lines += PdfReceiptLine("${idx + 1}. $name  ($qty x ${item.unitPrice.toLong()})  =  ${formatPkr(item.lineTotal, language)}")
        }
        lines += PdfReceiptLine.divider()
        lines += PdfReceiptLine(if (isUrdu) "کل کارڈ کوٹہ: ${formatPkr(bill.subsidyLimit, language)}" else "Total Card Quota: ${formatPkr(bill.subsidyLimit, language)}")
        lines += PdfReceiptLine(if (isUrdu) "سروس فیس کٹوتی: -${formatPkr(bill.feeDeduction, language)}" else "Service Fee Deduction: -${formatPkr(bill.feeDeduction, language)}")
        lines += PdfReceiptLine(if (isUrdu) "خالص قابلِ استعمال راشن: ${formatPkr(netLimit, language)}" else "Net Ration Value: ${formatPkr(netLimit, language)}", bold = true)
        lines += PdfReceiptLine(if (isUrdu) "کل POS بل: ${formatPkr(bill.totalAmount, language)}" else "TOTAL POS BILL: ${formatPkr(bill.totalAmount, language)}", bold = true, accentColor = Color.rgb(8, 76, 54))
        lines += PdfReceiptLine(if (isUrdu) "کارڈ سے کٹوتی: ${formatPkr(bill.subsidyUsed, language)}" else "POS Card Used: ${formatPkr(bill.subsidyUsed, language)}")
        if (bill.cashTopUp > 0.0) {
            lines += PdfReceiptLine(if (isUrdu) "اضافی نقد ادائیگی: ${formatPkr(bill.cashTopUp, language)}" else "Extra Cash Paid: ${formatPkr(bill.cashTopUp, language)}", bold = true, accentColor = Color.rgb(180, 100, 12))
        }
        lines += PdfReceiptLine(if (isUrdu) "بقایا خالص بیلنس: ${formatPkr(bill.remainingSubsidy, language)}" else "Remaining Net Balance: ${formatPkr(bill.remainingSubsidy, language)}")

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
            footer = if (isUrdu) "شکریہ — $title" else "Thank you — $title"
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

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 81, 50)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(80, 80, 80)
                textSize = 10f
            }
            val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyRegular = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(28, 28, 28)
                textSize = 11f
            }
            val bodyBold = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(18, 18, 18)
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(190, 190, 190)
                strokeWidth = 1f
            }

            fun makeLayout(text: String, paint: TextPaint, align: Layout.Alignment): StaticLayout {
                return StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
                    .setAlignment(align)
                    .setLineSpacing(2f, 1.15f)
                    .setIncludePad(false)
                    .build()
            }

            val titleLayout = makeLayout(shopTitle, titlePaint, Layout.Alignment.ALIGN_CENTER)
            val subLayout = if (shopSubtitle.isNotBlank()) makeLayout(shopSubtitle, subPaint, Layout.Alignment.ALIGN_CENTER) else null
            val phoneLayout = if (shopPhone.isNotBlank()) makeLayout(shopPhone, subPaint, Layout.Alignment.ALIGN_CENTER) else null
            val badgeLayout = makeLayout(badgeText, badgePaint, Layout.Alignment.ALIGN_CENTER)
            val footerLayout = makeLayout(footer, subPaint, Layout.Alignment.ALIGN_CENTER)

            val bodyLayouts = lines.map { line ->
                if (line.isDivider) null else {
                    val paint = TextPaint(if (line.bold) bodyBold else bodyRegular).apply { color = line.accentColor }
                    makeLayout(line.text, paint, Layout.Alignment.ALIGN_NORMAL)
                }
            }

            var totalHeight = PDF_MARGIN + titleLayout.height + 6
            if (subLayout != null) totalHeight += subLayout.height + 3
            if (phoneLayout != null) totalHeight += phoneLayout.height + 3
            totalHeight += badgeLayout.height + 20
            bodyLayouts.forEach { layout ->
                totalHeight += if (layout == null) 14 else (layout.height + 6)
            }
            totalHeight += 18 + footerLayout.height + PDF_MARGIN

            val pageHeight = totalHeight.coerceAtLeast(280)
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)
            var y = PDF_MARGIN.toFloat()
            canvas.save()
            canvas.translate(PDF_MARGIN.toFloat(), y)
            titleLayout.draw(canvas)
            canvas.restore()
            y += titleLayout.height + 4f

            if (subLayout != null) {
                canvas.save()
                canvas.translate(PDF_MARGIN.toFloat(), y)
                subLayout.draw(canvas)
                canvas.restore()
                y += subLayout.height + 3f
            }

            if (phoneLayout != null) {
                canvas.save()
                canvas.translate(PDF_MARGIN.toFloat(), y)
                phoneLayout.draw(canvas)
                canvas.restore()
                y += phoneLayout.height + 4f
            }

            val pillTop = y + 2f
            val pillBottom = pillTop + badgeLayout.height + 10f
            val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeColor }
            canvas.drawRoundRect(
                PDF_MARGIN.toFloat() + 18f,
                pillTop,
                (PDF_PAGE_WIDTH - PDF_MARGIN).toFloat() - 18f,
                pillBottom,
                10f,
                10f,
                badgeBgPaint
            )
            canvas.save()
            canvas.translate(PDF_MARGIN.toFloat(), pillTop + 5f)
            badgeLayout.draw(canvas)
            canvas.restore()
            y = pillBottom + 12f

            bodyLayouts.forEach { layout ->
                if (layout == null) {
                    val midY = y + 6f
                    canvas.drawLine(PDF_MARGIN.toFloat(), midY, (PDF_PAGE_WIDTH - PDF_MARGIN).toFloat(), midY, dividerPaint)
                    y += 14f
                } else {
                    canvas.save()
                    canvas.translate(PDF_MARGIN.toFloat(), y)
                    layout.draw(canvas)
                    canvas.restore()
                    y += layout.height + 6f
                }
            }

            y += 6f
            canvas.drawLine(PDF_MARGIN.toFloat(), y, (PDF_PAGE_WIDTH - PDF_MARGIN).toFloat(), y, dividerPaint)
            y += 8f
            canvas.save()
            canvas.translate(PDF_MARGIN.toFloat(), y)
            footerLayout.draw(canvas)
            canvas.restore()

            document.finishPage(page)

            val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val pdfFile = File(receiptsDir, fileName)
            FileOutputStream(pdfFile).use { fos ->
                document.writeTo(fos)
            }
            document.close()
            pdfFile
        } catch (_: Throwable) {
            null
        }
    }

    private fun sharePdfFileViaWhatsApp(
        context: Context,
        pdfFile: File,
        caption: String,
        subject: String
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val whatsappPackage = findInstalledWhatsAppPackage(context)
        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (whatsappPackage != null) {
                baseIntent.setPackage(whatsappPackage)
                context.startActivity(baseIntent)
            } else {
                context.startActivity(
                    Intent.createChooser(baseIntent, subject).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        } catch (_: Exception) {
            try {
                baseIntent.setPackage(null)
                context.startActivity(
                    Intent.createChooser(baseIntent, subject).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Exception) {
                shareText(context, caption, subject)
            }
        }
    }

    private fun findInstalledWhatsAppPackage(context: Context): String? {
        val pm = context.packageManager
        val candidates = listOf("com.whatsapp", "com.whatsapp.w4b")
        return candidates.firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                true
            } catch (_: Throwable) {
                try {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                    true
                } catch (_: Throwable) {
                    false
                }
            }
        }
    }

    fun formatPakistaniWhatsAppNumber(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        if (digits.isBlank()) return ""
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
        if (digits.isBlank()) return ""
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
            val encodedText = Uri.encode(message)
            val uriStr = "https://wa.me/$waNumber?text=$encodedText"
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Fall through to direct WhatsApp text intent
            }
              }
        val whatsappPackage = findInstalledWhatsAppPackage(context)
        if (whatsappPackage != null) {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage(whatsappPackage)
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Fall through to system share
            }
        }
        shareText(context, message, "WhatsApp Receipt")
    }

    fun sendViaSimSms(context: Context, phone: String, message: String) {
        val smsNumber = formatPakistaniSmsNumber(phone)
        val uri = Uri.parse("smsto:$smsNumber")
        try {
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            shareText(context, message, "SMS Receipt")
        }
    }

    fun shareText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(
            Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
