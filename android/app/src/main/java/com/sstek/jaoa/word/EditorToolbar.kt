package com.sstek.jaoa.word

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import com.sstek.jaoa.main.MainViewModel

@Composable
fun EditorToolbar(state: RichTextState, onToggleColorPicker: () -> Unit, onToggleFontPicker: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MainViewModel = hiltViewModel()

    val pickDocxFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                viewModel.loadDocxFile(uri, context.contentResolver)
            }
        }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Bold
        IconButton(onClick = {
            state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        }) {
            Icon(Icons.Default.FormatBold, contentDescription = "Bold")
        }

        // Italic
        IconButton(onClick = {
            state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
        }) {
            Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
        }

        // Underline
        IconButton(onClick = {
            state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        }) {
            Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
        }

        // Color
        IconButton(onClick = {
            onToggleColorPicker()
        }) {
            Icon(Icons.Default.ColorLens, contentDescription = "Color")
        }

        // Font
        IconButton(onClick = {
            onToggleFontPicker()
        }) {
            Icon(Icons.Default.FontDownload, contentDescription = "Font")
        }

        // Open
        Button(onClick = {
            pickDocxFileLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        }) {
            Icon(Icons.Default.FileOpen, contentDescription = "Ac")
        }

        // Save
        IconButton(
            onClick = {
                val html = state.toHtml()
                Log.d("WordScreen", html)
                viewModel.saveDocxFromHtml(html, "notlar.docx")
                Toast.makeText(context, "Belge kaydedildi", Toast.LENGTH_SHORT).show()
            }
        ) {
            Icon(Icons.Default.Save, contentDescription = "Kaydet")
        }

    }
}