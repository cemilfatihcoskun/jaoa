package com.sstek.jaoa.word.utils

import android.content.Context
import android.net.Uri
import android.print.PrintManager
import android.webkit.WebView

fun htmlPrint(webView: WebView, context: Context) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val printAdapter = webView.createPrintDocumentAdapter("Belge") // API < 21 için createPrintDocumentAdapter()
    printManager.print("Belge", printAdapter, null)
}