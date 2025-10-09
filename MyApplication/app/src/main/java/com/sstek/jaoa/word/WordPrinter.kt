package com.sstek.jaoa.word

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import com.sstek.jaoa.R

fun printHtml(webView: WebView, context: Context, filename: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
    if (printManager == null) {
        Toast.makeText(context, context.getString(R.string.printingNotReadyMessage), Toast.LENGTH_SHORT).show()
        return
    }

    webView.evaluateJavascript("""
        if (window.superdoc && window.superdoc.activeEditor) {
            window.superdoc.activeEditor.view.dom.blur();
        }
    """.trimIndent(), null)

    webView.evaluateJavascript("""
        if (window.superdoc && window.superdoc.activeEditor) {
            window.superdoc.togglePagination();
        }
    """.trimIndent(), null)

    val printAdapter = webView.createPrintDocumentAdapter(filename)
    printManager.print(
        filename,
        printAdapter,
        android.print.PrintAttributes.Builder().build()
    )

    webView.evaluateJavascript("""
        if (window.superdoc && window.superdoc.activeEditor) {
            window.superdoc.togglePagination();
        }
    """.trimIndent(), null)

}
