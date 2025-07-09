package com.sstek.jaoa.word

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sstek.jaoa.main.MainViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

@Composable
fun WordScreen(viewModel: MainViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val createResult by viewModel.createResult.collectAsState()
    val readResult by viewModel.readResult.collectAsState()
    val htmlContent by viewModel.htmlContent.collectAsState()

    LaunchedEffect(createResult) {
        when (createResult) {
            true -> Toast.makeText(context, "Belge oluşturuldu", Toast.LENGTH_SHORT).show()
            false -> Toast.makeText(context, "Belge oluşturulamadı", Toast.LENGTH_SHORT).show()
            null -> Unit
        }
        viewModel.resetCreateResult()
    }

    val richTextState = rememberRichTextState()

    var activePanel by remember { mutableStateOf(PanelType.NONE) }
    fun panelChooseAndDisableIfTheSame(panelType: PanelType) {
        if (activePanel == panelType) {
            activePanel = PanelType.NONE
        } else {
            activePanel = panelType
        }
    }
    var showColorPicker by remember { mutableStateOf(false) }
    var fontMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(htmlContent) {
        if (htmlContent.isNotBlank()) {
            richTextState.setHtml(htmlContent)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        RichTextEditor(
            state = richTextState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .background(Color.White)
        )

        if (activePanel == PanelType.COLOR) {
            ColorPickerSection(
                onTextColorSelected = { color ->
                    richTextState.toggleSpanStyle(SpanStyle(color = color))
                },
                onBackgroundColorSelected = { color ->
                    richTextState.toggleSpanStyle(SpanStyle(background = color))
                }
            )
        }

        if (activePanel == PanelType.FONT) {
            FontStylePickerSection(
                onFontSelected = { fontFamily ->
                    richTextState.toggleSpanStyle(SpanStyle(fontFamily = FontFamily.Cursive))
                },
                onSizeSelected = { size ->
                    richTextState.toggleSpanStyle(SpanStyle(fontSize = size.sp))
                }
            )
        }

        EditorToolbar(
            richTextState,
            onToggleColorPicker = { panelChooseAndDisableIfTheSame(PanelType.COLOR) },
            onToggleFontPicker = { panelChooseAndDisableIfTheSame(PanelType.FONT) }
        )
    }
}








