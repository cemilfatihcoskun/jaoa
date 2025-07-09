package com.sstek.jaoa.word

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun RichEditorCompose(
    html: String,
    onHtmlChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(factory = { context ->
        jp.wasabeef.richeditor.RichEditor(context).apply {
            setEditorHeight(200)
            setEditorFontSize(16)
            setPadding(10, 10, 10, 10)
            setHtml(html)

            setOnTextChangeListener { updatedHtml ->
                onHtmlChange(updatedHtml)
            }
        }
    }, modifier = modifier)
}
