package com.sstek.jaoa

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import jp.wasabeef.richeditor.RichEditor

@Composable
fun RichEditorView(
    modifier: Modifier = Modifier,
    onInstanceReady: (RichEditor) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RichEditor(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setPadding(16, 16, 16, 16)
                setEditorFontSize(16)
                //setPlaceholder("Buraya yaz...")
                //setEditorHeight(5000) // ⚠️ bu çok kritik, WebView içerisine yükseklik vermeli
                setFocusable(true)
                setFocusableInTouchMode(true)
                requestFocus()
                onInstanceReady(this)
                clearCache(false)
                clearHistory()
                settings.javaScriptEnabled = true
            }
        },
        update = {
            // Ekstra güncelleme yapılabilir.
        }
    )
}



