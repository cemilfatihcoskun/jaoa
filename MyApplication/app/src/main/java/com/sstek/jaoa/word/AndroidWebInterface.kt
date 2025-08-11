package com.sstek.jaoa.word

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView

class AndroidWebInterface(private val webView: WebView, private val htmlContent: String) {
    @JavascriptInterface
    fun onEditorReady() {
        val escaped = htmlContent.replace("\"", "\\\"").replace("\n", "\\n")
        webView.post {
            webView.evaluateJavascript("window.setHtmlContent(\"$escaped\")", null)
        }
    }

    @JavascriptInterface
    fun receiveHtml(html: String) {
        Log.d("AndroidInterface", "HTML received from JS: $html")
    }
}
