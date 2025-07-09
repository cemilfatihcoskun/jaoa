package com.sstek.jaoa.word

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.sstek.jaoa.main.MainViewModel
import jp.wasabeef.richeditor.RichEditor



@Composable
fun EditorScreen(viewModel: MainViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val htmlContent by viewModel.htmlContent.collectAsState()
    var html by remember { mutableStateOf(htmlContent) }
    var activePanel by remember { mutableStateOf(PanelType.NONE) }

    var richEditor by remember { mutableStateOf<RichEditor?>(null) }

    var isBoldActive by remember { mutableStateOf(false) }
    var isItalicActive by remember { mutableStateOf(false) }
    var isUnderlineActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // jp.wasabeef RichEditor Compose sarmalayıcı
        AndroidView(
            factory = { ctx ->
                RichEditor(ctx).apply {
                    setEditorHeight(200)
                    setEditorFontSize(16)
                    setPadding(10, 10, 10, 10)
                    setHtml(html)

                    setOnTextChangeListener { updatedHtml ->
                        html = updatedHtml
                    }

                    richEditor = this
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.Gray)
                .background(Color.White)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Stil araç çubuğu (Bold, Italic, Underline, Renk seçici, Font seçici gibi)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            // Bold
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBoldActive) Color.Gray else Color.LightGray
                ),
                onClick = {
                    richEditor?.setBold()
                    isBoldActive = !isBoldActive
                }
            ) {
                Icon(Icons.Default.FormatBold, contentDescription = "Kalın")
            }

            // Italic
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isItalicActive) Color.Gray else Color.LightGray
                ),
                onClick = {
                    richEditor?.setItalic()
                    isItalicActive = !isItalicActive
                }
            ) {
                Icon(Icons.Default.FormatItalic, contentDescription = "İtalik")
            }

            // Underline
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUnderlineActive) Color.Gray else Color.LightGray
                ),
                onClick = {
                    richEditor?.setUnderline()
                    isUnderlineActive = !isUnderlineActive
                }
            ) {
                Icon(Icons.Default.FormatUnderlined, contentDescription = "Altı çizili")
            }

            // Renk seçici panelini aç/kapa
            Button(onClick = {
                activePanel = if (activePanel == PanelType.COLOR) PanelType.NONE else PanelType.COLOR
            }) {
                Icon(Icons.Default.ColorLens, contentDescription = "Font")
            }

            // Font seçici panelini aç/kapa
            Button(onClick = {
                activePanel = if (activePanel == PanelType.FONT) PanelType.NONE else PanelType.FONT
            }) {
                Icon(Icons.Default.FontDownload, contentDescription = "Font")
            }
        }

        // Renk veya Font paneli göster
        when (activePanel) {
            PanelType.COLOR -> {
                ColorPickerSection(
                    onTextColorSelected = { color ->
                        richEditor?.setTextColor(color.toArgb())
                    },
                    onBackgroundColorSelected = { color ->
                        richEditor?.setTextBackgroundColor(color.toArgb())
                    }
                )
            }
            PanelType.FONT -> {
                FontStylePickerSection(
                    onFontSelected = { fontFamily ->

                        val js = "javascript:document.execCommand('fontName', false, '$fontFamily');"
                        Log.d("EditorScreen", "$js, fontFamily")
                        richEditor?.loadUrl(js)
                    },
                    onSizeSelected = { size ->
                        richEditor?.setFontSize(size)
                    }
                )
            }
            else -> {

            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            onClick = {
                Log.d("EditorScreen", "Kaydedilen HTML: $html")
                viewModel.saveDocxFromHtml(html, "notlar.docx")
                Toast.makeText(context, "Document is saved", Toast.LENGTH_SHORT).show()
            }
        ) {
            Icon(Icons.Default.Save, contentDescription = "Font")
        }
    }
}
