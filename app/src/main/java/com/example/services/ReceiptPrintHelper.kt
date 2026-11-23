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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.formatQty
import com.example.i18n.label
import com.example.i18n.unitLabel
import java.io.File
import java.io.FileOutputStream

object ReceiptPrintHelper {

    private const val PDF_PAGE_WIDTH = 340
    private const val PDF_MARGIN = 18

    /**
     * Renders an authentic RTL Urdu / LTR English receipt via WebView into Android's native
     * PrintManager (supports Wi-Fi printers, USB printers, and Save as PDF).
     */
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
            shopNameUrdu = settings?.storeNameUrdu ?: ReceiptFormatter.SHOP_NAME_URDU,
            shopNameEnglish = settings?.storeNameEnglish ?: ReceiptFormatter.SHOP_NAME_ENGLISH,
            shopAddressUrdu = settings?.shopAddressUrdu ?: ReceiptFormatter.SHOP_CITY_URDU,
            shopAddressEnglish = settings?.shopAddressEnglish ?: ReceiptFormatter.SHOP_CITY_ENGLISH
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
            shopNameUrdu = settings?.storeNameUrdu ?: ReceiptFormatter.SHOP_NAME_URDU,
            shopNameEnglish = settings?.storeNameEnglish ?: ReceiptFormatter.SHOP_NAME_ENGLISH,
            shopAddressUrdu = settings?.shopAddressUrdu ?: ReceiptFormatter.SHOP_CITY_URDU,
            shopAddressEnglish = settings?.shopAddressEnglish ?: ReceiptFormatter.SHOP_CITY_ENGLISH
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
            shopNameUrdu = settings?.storeNameUrdu ?: ReceiptFormatter.SHOP_NAME_URDU,
            shopNameEnglish = settings?.storeNameEnglish ?: ReceiptFormatter.SHOP_NAME_ENGLISH,
            shopAddressUrdu = settings?.shopAddressUrdu ?: ReceiptFormatter.SHOP_CITY_URDU,
            shopAddressEnglish = settings?.shopAddressEnglish ?: ReceiptFormatter.SHOP_CITY_ENGLISH
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
                // No PDF viewer installed -> fall back to Android native Print / Save as PDF
            }
        }
        printLowStockReportWithSystemPrint(context, lowStockProducts, language, settings)
    }

    /**
     * Generates a clean, multi-page A4 Low Stock Replenishment Report PDF using Android PdfDocument.
     * Sorted by urgency (Out of Stock first, then lowest stock-to-threshold ratio).
     */
    fun generateLowStockReportPdf(
        context: Context,
        lowStockProducts: List<Product>,
        language: AppLanguage,
        settings: ShopSettings
    ): File? {
        return try {
            val isUrdu = language == AppLanguage.URDU
            val pageWidth = 595 // A4 width in PostScript points
            val pageHeight = 842 // A4 height in PostScript points
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

            // Column X coordinates & widths (sum = contentWidth = 539pt)
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

            // 1. Top Emerald Header Banner (Page 1)
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

            // Crimson Alert Badge inside header
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

            // 2. Summary KPI Strip (3 cards)
            val kpiGap = 10f
            val kpiW = (contentWidth - (kpiGap * 2f)) / 3f
            val kpiH = 48f
            val kpiCards = listOf(
                Triple(
                    if (isUrdu) "کم اسٹاک اشیاء" else "Low Stock Products",
                    "${sorted.size}",
                    amberColor
                ),
                Triple(
                    if (isUrdu) "مکمل ختم اشیاء" else "Out of Stock (Critical)",
                    "$outCount",
                    crimsonColor
                ),
                Triple(
                    if (isUrdu) "اندازاً خریداری لاگت" else "Est. Replenish Cost",
                    formatPkr(totalEstCost, language),
                    emeraldColor
                )
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

            // 3. Replenishment Table
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

                    // Row background tint
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

                    // Col 0: Index
                    drawStaticText(canvas, "${idx + 1}", cellRegularPaint, colStarts[0] + 2f, y + 6f, (colWidths[0] - 4f).toInt(), Layout.Alignment.ALIGN_CENTER)
                    // Col 1: Product Name
                    drawStaticText(canvas, primaryName.take(36), cellBoldPaint, colStarts[1] + 4f, y + 4f, (colWidths[1] - 8f).toInt(), Layout.Alignment.ALIGN_NORMAL)
                    if (secondaryName.isNotBlank()) {
                        drawStaticText(canvas, secondaryName.take(36), cellSubPaint, colStarts[1] + 4f, y + 17f, (colWidths[1] - 8f).toInt(), Layout.Alignment.ALIGN_NORMAL)
                    }
                    // Col 2: In Stock + Status
                    val stockTxt = "${formatQty(prod.stockQty)} $unitTxt"
                    drawStaticText(canvas, stockTxt, stockPaint, colStarts[2] + 2f, y + 4f, (colWidths[2] - 4f).toInt(), Layout.Alignment.ALIGN_CENTER)
                    val badgeStatus = if (prod.isOutOfStock()) (if (isUrdu) "ختم" else "OUT") else (if (isUrdu) "کم" else "LOW")
                    drawStaticText(canvas, badgeStatus, stockPaint, colStarts[2] + 2f, y + 16f, (colWidths[2] - 4f).toInt(), Layout.Alignment.ALIGN_CENTER)

                    // Col 3: Min Threshold
                    drawStaticText(canvas, "${formatQty(prod.lowStockThreshold)} $unitTxt", cellRegularPaint, colStarts[3] + 2f, y + 6f, (colWidths[3] - 4f).toInt(), Layout.Alignment.ALIGN_CENTER)
                    // Col 4: Suggested Reorder Qty
                    drawStaticText(canvas, "+${formatQty(prod.suggestedRestockQty())} $unitTxt", reorderPaint, colStarts[4] + 2f, y + 6f, (colWidths[4] - 4f).toInt(), Layout.Alignment.ALIGN_CENTER)
                    // Col 5: Unit Cost
                    drawStaticText(canvas, "Rs.${prod.costPrice.toLong()}", cellRegularPaint, colStarts[5] + 2f, y + 6f, (colWidths[5] - 6f).toInt(), Layout.Alignment.ALIGN_OPPOSITE)
                    // Col 6: Est Cost
                    drawStaticText(canvas, "Rs.${prod.estimatedRestockCost().toLong()}", cellBoldPaint, colStarts[6] + 2f, y + 6f, (colWidths[6] - 6f).toInt(), Layout.Alignment.ALIGN_OPPOSITE)

                    y += rowHeight
                }
            }

            // 4. Grand Total Replenishment Box
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
        lines += PdfReceiptLine(
            if (isUrdu) "مدت: $periodLabel" else "Period: $periodLabel",
            bold = true
        )
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
                    lines += PdfReceiptLine(
                        text = "   ${entry.note}",
                        accentColor = Color.rgb(90, 90, 90)
                    )
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
        val isUrdu = language == AppLanguage.URDU
        val title = if (isUrdu) settings.storeNameUrdu else settings.storeNameEnglish
        val address = if (isUrdu) settings.shopAddressUrdu else settings.shopAddressEnglish
        val dateStr = ReceiptFormatter.formatDateTime(order.createdAt?.toDate()?.time ?: System.currentTimeMillis())

        val lines = mutableListOf<PdfReceiptLine>()
        lines += PdfReceiptLine(
            if (isUrdu) "بل نمبر: ${order.billNumber}" else "Bill #: ${order.billNumber}",
            bold = true
        )
        lines += PdfReceiptLine(if (isUrdu) "تاریخ: $dateStr" else "Date: $dateStr")
        lines += PdfReceiptLine(if (isUrdu) "کسٹمر: ${order.customerName}" else "Customer: ${order.customerName}")
        lines += PdfReceiptLine(
            if (isUrdu) "ادائیگی: ${order.paymentMethodEnum().label(AppLanguage.URDU)}"
            else "Payment: ${order.paymentMethodEnum().label(AppLanguage.ENGLISH)}"
        )
        lines += PdfReceiptLine.divider()
        order.items.forEachIndexed { idx, item ->
            val name = if (isUrdu) item.nameUrdu else item.nameEnglish
            val qty = "${formatQty(item.quantity)} ${unitLabel(item.unit, language)}"
            lines += PdfReceiptLine(
                "${idx + 1}. $name  ($qty x ${item.unitPrice.toLong()})  =  ${formatPkr(item.lineTotal, language)}"
            )
        }
        lines += PdfReceiptLine.divider()
        if (order.discount > 0.0) {
            lines += PdfReceiptLine(
                if (isUrdu) "کل رقم: ${formatPkr(order.subtotal, language)}"
                else "Subtotal: ${formatPkr(order.subtotal, language)}"
            )
            lines += PdfReceiptLine(
                if (isUrdu) "رعایت: -${formatPkr(order.discount, language)}"
                else "Discount: -${formatPkr(order.discount, language)}"
            )
        }
        lines += PdfReceiptLine(
            if (isUrdu) "واجب الادا کل: ${formatPkr(order.totalAmount, language)}"
            else "GRAND TOTAL: ${formatPkr(order.totalAmount, language)}",
            bold = true,
            accentColor = Color.rgb(15, 81, 50)
        )
        lines += PdfReceiptLine(
            if (isUrdu) "وصول شدہ: ${formatPkr(order.paidAmount, language)}"
            else "Paid: ${formatPkr(order.paidAmount, language)}"
        )
        if (order.remainingUdhar > 0.0) {
            lines += PdfReceiptLine(
                if (isUrdu) "بقایا ادھار: ${formatPkr(order.remainingUdhar, language)}"
                else "Remaining Udhar: ${formatPkr(order.remainingUdhar, language)}",
                bold = true,
                accentColor = Color.rgb(198, 30, 30)
            )
        }

        val safeName = order.billNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return renderReceiptPdf(
            context = context,
            fileName = "Bill_${safeName}.pdf",
            shopTitle = title,
            shopSubtitle = address,
            shopPhone = settings.shopPhone,
            badgeText = if (isUrdu) "سیلز بل رسید" else "SALES BILL RECEIPT",
            badgeColor = Color.rgb(15, 81, 50),
            lines = lines,
            footer = if (isUrdu) "خریداری کا شکریہ! $title" else "Thank you for shopping at $title"
        )
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
        lines += PdfReceiptLine(
            if (isUrdu) "رسید نمبر: ${bill.billNumber} (${bill.monthKey})" else "Receipt #: ${bill.billNumber} (${bill.monthKey})",
            bold = true
        )
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
            lines += PdfReceiptLine(
                "${idx + 1}. $name  ($qty x ${item.unitPrice.toLong()})  =  ${formatPkr(item.lineTotal, language)}"
            )
        }
        lines += PdfReceiptLine.divider()
        lines += PdfReceiptLine(
            if (isUrdu) "کل کارڈ کوٹہ: ${formatPkr(bill.subsidyLimit, language)}"
            else "Total Card Quota: ${formatPkr(bill.subsidyLimit, language)}"
        )
        lines += PdfReceiptLine(
            if (isUrdu) "سروس فیس کٹوتی: -${formatPkr(bill.feeDeduction, language)}"
            else "Service Fee Deduction: -${formatPkr(bill.feeDeduction, language)}"
        )
        lines += PdfReceiptLine(
            if (isUrdu) "خالص قابلِ استعمال راشن: ${formatPkr(netLimit, language)}"
            else "Net Ration Value: ${formatPkr(netLimit, language)}",
            bold = true
        )
        lines += PdfReceiptLine(
            if (isUrdu) "کل POS بل: ${formatPkr(bill.totalAmount, language)}"
            else "TOTAL POS BILL: ${formatPkr(bill.totalAmount, language)}",
            bold = true,
            accentColor = Color.rgb(8, 76, 54)
        )
        lines += PdfReceiptLine(
            if (isUrdu) "کارڈ سے کٹوتی: ${formatPkr(bill.subsidyUsed, language)}"
            else "POS Card Used: ${formatPkr(bill.subsidyUsed, language)}"
        )
        if (bill.cashTopUp > 0.0) {
            lines += PdfReceiptLine(
                if (isUrdu) "اضافی نقد ادائیگی: ${formatPkr(bill.cashTopUp, language)}"
                else "Extra Cash Paid: ${formatPkr(bill.cashTopUp, language)}",
                bold = true,
                accentColor = Color.rgb(180, 100, 12)
            )
        }
        lines += PdfReceiptLine(
            if (isUrdu) "بقایا خالص بیلنس: ${formatPkr(bill.remainingSubsidy, language)}"
            else "Remaining Net Balance: ${formatPkr(bill.remainingSubsidy, language)}"
        )

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
                if (line.isDivider) {
                    null
                } else {
                    val paint = TextPaint(if (line.bold) bodyBold else bodyRegular).apply {
                        color = line.accentColor
                    }
                    makeLayout(line.text, paint, Layout.Alignment.ALIGN_NORMAL)
                }
            }

            var totalHeight = PDF_MARGIN + titleLayout.height + 6
            if (subLayout != null) totalHeight += subLayout.height + 3
            if (phoneLayout != null) totalHeight += phoneLayout.height + 3
            totalHeight += badgeLayout.height + 20 // pill + spacing
            bodyLayouts.forEach { layout ->
                totalHeight += if (layout == null) 14 else (layout.height + 6)
            }
            totalHeight += 18 + footerLayout.height + PDF_MARGIN

            val pageHeight = totalHeight.coerceAtLeast(280)
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // White background
            canvas.drawColor(Color.WHITE)

            var y = PDF_MARGIN.toFloat()

            // Header title
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

            // Badge pill
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

            // Body lines
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

            // Footer
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

    /**
     * Converts Pakistani phone numbers (e.g. 0300-1234567, +92 300 1234567, 3001234567)
     * into WhatsApp international digits without '+' (923001234567).
     */
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

    /**
     * Formats Pakistani phone numbers for native SIM SMS (03XXXXXXXXX or international digits).
     */
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

    /**
     * Opens WhatsApp directly with pre-filled receipt text via https://wa.me/ link
     * (works with both WhatsApp and WhatsApp Business). Falls back gracefully to system share.
     */
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

    /**
     * Opens Android's native SIM SMS messaging app pre-filled with the customer's phone number
     * and full receipt body using smsto: URI (zero SMS permission required).
     */
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
