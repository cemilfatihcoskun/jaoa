package com.sstek.jaoa.word

import android.webkit.WebView
import android.webkit.WebViewClient

class QuillWebViewClient(
    private val onLoaded: () -> Unit
) : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onLoaded()
    }
}
