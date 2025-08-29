package com.sstek.jaoa.word.utils

import android.content.Context
import android.net.Uri
import android.print.PrintManager
import android.webkit.WebView
import com.sstek.jaoa.R

fun htmlPrint(webView: WebView, context: Context) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val printAdapter = webView.createPrintDocumentAdapter(context.resources.getString(R.string.printer_defaultDocumentName)) // API < 21 için createPrintDocumentAdapter()
    printManager.print(context.resources.getString(R.string.printer_title), printAdapter, null)
}