package com.example.services

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.Order
import com.example.data.RationCardBill
import com.example.i18n.AppLanguage

object ReceiptPrintHelper {

    /**
     * Renders an authentic RTL Urdu / LTR English receipt via WebView into Android's native
     * PrintManager (supports Wi-Fi printers, USB printers, and Save as PDF).
     */
    fun printOrderWithSystemPrint(
        context: Context,
        order: Order,
        language: AppLanguage,
        shopPhone: String
    ) {
        val html = ReceiptFormatter.buildPrintableHtml(order, language, shopPhone)
        printHtmlDocument(context, html, "AhmadBilal_${order.billNumber}")
    }

    fun printRationCardWithSystemPrint(
        context: Context,
        bill: RationCardBill,
        language: AppLanguage,
        shopPhone: String
    ) {
        val html = ReceiptFormatter.buildRationCardPrintableHtml(bill, language, shopPhone)
        printHtmlDocument(context, html, "MaryamRation_${bill.billNumber}")
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

    fun shareText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
