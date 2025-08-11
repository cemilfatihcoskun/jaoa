package com.sstek.jaoa.word

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class MyWebChromeClient(
    private val activity: Activity,
    private val openFileChooser: () -> Unit
) : WebChromeClient() {

    var uploadMessage: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_REQUEST_CODE = 1001

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        uploadMessage?.onReceiveValue(null) // Önce varsa eskiyi temizle
        uploadMessage = filePathCallback
        openFileChooser.invoke()
        return true
    }

    // Activity onActivityResult'ta çağrılacak
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (uploadMessage == null) return

            val results = if (resultCode == Activity.RESULT_OK) {
                data?.data?.let { arrayOf(it) } ?: arrayOf()
            } else null

            uploadMessage?.onReceiveValue(results)
            uploadMessage = null
        }
    }

    fun getRequestCode() = FILE_CHOOSER_REQUEST_CODE
}
