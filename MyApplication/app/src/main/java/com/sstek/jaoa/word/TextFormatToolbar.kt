package com.sstek.jaoa.word

import IconButtonWithTooltip
import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.vector.ImageVector
import com.sstek.jaoa.core.JAOATheme
import com.sstek.jaoa.R

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun TextFormatToolbar(webView: WebView?, viewModel: WordViewModel) {
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val jsCode = """
                (function() {
                    const editor = superdoc.activeEditor;
                    editor.commands.setImage({
                        src: '${selectedUri}'
                    });
                })();
            """.trimIndent()
            webView?.evaluateJavascript(jsCode, null)
        }
    }

    var showHighlightColors by remember { mutableStateOf(false) }
    var showTextColors by remember { mutableStateOf(false) }
    var showFontFamily by remember { mutableStateOf(false) }
    var showFontSize by remember { mutableStateOf(false) }
    var showTablePanel by remember { mutableStateOf(false) }
    var showTextFormat by remember { mutableStateOf(false) }

    var tableRows by remember { mutableStateOf(2) }
    var tableCols by remember { mutableStateOf(2) }

    val highlightColors = listOf(
        "#ffff00","#00ff00","#ff00ff","#aaaaaa",
        "#ff8800","#00ffff","#ff0000","#0088ff",
        "#880088","#88ff88","#cccccc","#444444",
        "#ffcc00","#cc00ff","#00ccff","#ff4444"
    )
    val textColors = listOf(
        "#000000","#ff0000","#0000ff","#dddddd",
        "#008800","#880088","#444444","#00ffff",
        "#ff8800","#88ff88","#cc00ff","#ffcc00",
        "#880000","#000088","#0088ff","#ff4444"
    )
    val fontFamilies = listOf("Arial", "Georgia", "Courier New", "Times New Roman")
    val fontSizes = listOf("8","9","10","11","12","14","16","18","20","22","24","26","28","36","48","72")

    val textFormats = listOf("Heading1", "Heading2", "Heading3", "Heading4", "Heading5", "Heading6", "Heading7", "Heading8", "Heading9", "IntenseQuote", "ListParagraph", "Normal", "Quote", "Subtitle", "Title")

    val isBoldSelected by viewModel.isBold.collectAsState()
    val isItalicSelected by viewModel.isItalic.collectAsState()
    val isUnderlineSelected by viewModel.isUnderline.collectAsState()

    JAOATheme {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {

            // Toolbar: horizontal scrollable
            val toolbarScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(toolbarScrollState)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tema Renkleri Tanımlamaları
                val colorScheme = MaterialTheme.colorScheme
                val selectedTint = colorScheme.primary // Bold aktifken ikon rengi (Açık veya Koyu Temanın Primary'si)
                val defaultTint = colorScheme.onSurface // Varsayılan ikon rengi
                val selectedBackground = colorScheme.surfaceVariant // Bold aktifken arka plan rengi

                IconButtonWithTooltip(
                    icon = Icons.Default.FormatBold,
                    contentDescriptionResId = R.string.editorscreen_tooltipBold,
                    tint = if (isBoldSelected) selectedTint else defaultTint,
                    backgroundColor = if (isBoldSelected) selectedBackground else Color.Transparent
                ) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.toggleBold();", null)
                    viewModel.toggleBold()
                }

                IconButtonWithTooltip(
                    icon = Icons.Default.FormatItalic,
                    contentDescriptionResId = R.string.editorscreen_tooltipItalic,
                    tint = if (isItalicSelected) selectedTint else defaultTint,
                    backgroundColor = if (isItalicSelected) selectedBackground else Color.Transparent
                ) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.toggleItalic();", null)
                    viewModel.toggleItalic()
                }

                IconButtonWithTooltip(
                    icon = Icons.Default.FormatUnderlined,
                    contentDescriptionResId = R.string.editorscreen_tooltipUnderline,
                    tint = if (isUnderlineSelected) selectedTint else defaultTint,
                    backgroundColor = if (isUnderlineSelected) selectedBackground else Color.Transparent
                ) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.toggleUnderlined();", null)
                    viewModel.toggleUnderline()
                }

                // Renk ve font
                IconButtonWithTooltip(Icons.Default.BorderColor, R.string.editorscreen_tooltipHighlight) {
                    showHighlightColors = !showHighlightColors
                    showTextColors = false
                    showFontFamily = false
                    showFontSize = false
                    showTablePanel = false
                    showTextFormat = false
                }
                IconButtonWithTooltip(Icons.Default.FormatColorText, R.string.editorscreen_tooltipColor) {
                    showTextColors = !showTextColors
                    showHighlightColors = false
                    showFontFamily = false
                    showFontSize = false
                    showTablePanel = false
                    showTextFormat = false
                }
                IconButtonWithTooltip(Icons.Default.FormatSize, R.string.editorscreen_tooltipFontSize) {
                    showFontSize = !showFontSize
                    showFontFamily = false
                    showHighlightColors = false
                    showTextColors = false
                    showTablePanel = false
                    showTextFormat = false
                }
                IconButtonWithTooltip(Icons.Default.FontDownload, R.string.editorscreen_tooltipFontFamily) {
                    showFontFamily = !showFontFamily
                    showFontSize = false
                    showHighlightColors = false
                    showTextColors = false
                    showTablePanel = false
                    showTextFormat = false
                }

                // Hizalama
                IconButtonWithTooltip(Icons.Default.FormatAlignLeft, R.string.editorscreen_tooltipAlignLeft) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.setTextAlign('left');", null)
                }
                IconButtonWithTooltip(Icons.Default.FormatAlignCenter, R.string.editorscreen_tooltipAlignCenter) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.setTextAlign('center');", null)
                }
                IconButtonWithTooltip(Icons.Default.FormatAlignRight, R.string.editorscreen_tooltipAlignRight) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.setTextAlign('right');", null)
                }
                IconButtonWithTooltip(Icons.Default.FormatAlignJustify, R.string.editorscreen_tooltipAlignJustify) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.setTextAlign('justify');", null)
                }

                // Listeler ve indent
                IconButtonWithTooltip(Icons.Default.FormatListBulleted, R.string.editorscreen_tooltipToggleList) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.toggleList();", null)
                }
                IconButtonWithTooltip(Icons.Default.FormatListNumbered, R.string.editorscreen_tooltipToggleOrderedList) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.toggleOrderedList();", null)
                }
                IconButtonWithTooltip(Icons.Default.FormatIndentIncrease, R.string.editorscreen_tooltipIncreaseIndent) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.increaseTextIndent();", null)
                }
                IconButtonWithTooltip(Icons.Default.FormatIndentDecrease, R.string.editorscreen_tooltipDecreaseIndent) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.decreaseTextIndent();", null)
                }

                // Resim
                IconButtonWithTooltip(Icons.Default.Image, R.string.editorscreen_tooltipUploadImage) {
                    filePickerLauncher.launch("image/*")
                }

                // Tablo
                IconButtonWithTooltip(Icons.Default.TableRows, R.string.editorscreen_tooltipInsertTable) {
                    showTablePanel = !showTablePanel
                    showHighlightColors = false
                    showTextColors = false
                    showFontFamily = false
                    showFontSize = false
                    showTextFormat = false
                }

                IconButtonWithTooltip(Icons.Default.FontDownloadOff, contentDescriptionResId = R.string.editorscreen_tooltipTextFormat) {
                    showTextFormat = !showTextFormat
                    showTablePanel = false
                    showHighlightColors = false
                    showTextColors = false
                    showFontFamily = false
                    showFontSize = false
                }
            }

            // Alt panel
            AnimatedVisibility(
                visible = showHighlightColors || showTextColors || showFontFamily || showFontSize || showTablePanel || showTextFormat,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (showHighlightColors) ColorPaletteGrid(highlightColors) { color ->
                            webView?.evaluateJavascript("superdoc.activeEditor.commands.setHighlight('$color');", null)
                            showHighlightColors = false
                        }
                        if (showTextColors) ColorPaletteGrid(textColors) { color ->
                            webView?.evaluateJavascript("superdoc.activeEditor.commands.setColor('$color');", null)
                            showTextColors = false
                        }
                        if (showFontFamily) {
                            ScrollableButtonColumn(
                                fontFamilies,
                                onItemClick = { family ->
                                    webView?.evaluateJavascript("superdoc.activeEditor.commands.setFontFamily('$family');", null)
                                    showFontFamily = false
                                }
                            )
                        }
                        if (showFontSize) {
                            ScrollableButtonColumn(
                                fontSizes,
                                onItemClick = { size ->
                                    webView?.evaluateJavascript("superdoc.activeEditor.commands.setFontSize($size);", null)
                                    showFontSize = false
                                }
                            )
                        }
                        if (showTextFormat) {
                            TextFormatButton(
                                textFormats,
                                onItemClick = { size ->
                                    webView?.evaluateJavascript("superdoc.activeEditor.commands.setLinkedStyle('$size');", null)
                                    showTextFormat = false
                                }
                            )
                        }

                        if (showTablePanel) {
                            TablePanel(
                                maxRows = 5,
                                maxCols = 5,
                                onInsert = { rows, cols ->
                                    val jsCode = """
                                        (function() {
                                            superdoc.activeEditor.commands.insertTable({cols: $cols, rows: $rows});
                                        })();
                                    """.trimIndent()
                                    webView?.evaluateJavascript(jsCode, null)
                                    showTablePanel = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPaletteGrid(colors: List<String>, onColorSelected: (String) -> Unit) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(android.graphics.Color.parseColor(color)), RoundedCornerShape(6.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

@Composable
fun ScrollableButtonColumn(items: List<String>, onItemClick: (String) -> Unit, maxHeight: Dp = 200.dp) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            Button(
                onClick = { onItemClick(item) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(item, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun TextFormatButton(items: List<String>, onItemClick: (String) -> Unit, maxHeight: Dp = 200.dp) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            Button(
                onClick = { onItemClick(item) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val resId = context.resources.getIdentifier(
                    "editorscreen_tooltipTextFormat$item",
                    "string", // resource type
                    context.packageName
                )
                if (resId != 0) {
                    Text(context.getString(resId), fontSize = 14.sp)
                } else {
                    Text("Unknown", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun TablePanel(
    onInsert: (rows: Int, cols: Int) -> Unit,
    maxRows: Int = 5,
    maxCols: Int = 5
) {
    val context = LocalContext.current

    var manualRows by remember { mutableStateOf(2) }
    var manualCols by remember { mutableStateOf(2) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // 1️⃣ Grid seçimi (hızlı seçim)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..maxRows).forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..maxCols).forEach { c ->
                        Button(
                            onClick = { onInsert(r, c) }, // Grid tıklayınca direkt tablo
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                        ) { }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2️⃣ Manuel Row / Col girişi
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(context.getString(R.string.editorscreen_tableRows))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { if (manualRows > 1) manualRows-- }) { Text("-") }
                    Text("$manualRows", modifier = Modifier.width(24.dp).padding(4.dp), color = Color.Black)
                    Button(onClick = { manualRows++ }) { Text("+") }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(context.getString(R.string.editorscreen_tableCols))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { if (manualCols > 1) manualCols-- }) { Text("-") }
                    Text("$manualCols", modifier = Modifier.width(24.dp).padding(4.dp), color = Color.Black)
                    Button(onClick = { manualCols++ }) { Text("+") }
                }
            }
        }

        // Insert butonu (manuel değerleri alır)
        Button(
            onClick = { onInsert(manualRows, manualCols) },
            modifier = Modifier.align(Alignment.End),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(context.getString(R.string.editorscreen_insertTable))
        }
    }
}

