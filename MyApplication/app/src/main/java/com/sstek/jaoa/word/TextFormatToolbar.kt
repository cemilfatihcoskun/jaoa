package com.sstek.jaoa.word

import IconButtonWithTooltip
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.webkit.WebView
import com.sstek.jaoa.core.JAOATheme
import com.sstek.jaoa.word.ColorPalette
import com.sstek.jaoa.R

@Composable
fun TextFormatToolbar(webView: WebView?) {
    val scrollState = rememberScrollState()

    var showHighlightColors by remember { mutableStateOf(false) }
    var showTextColors by remember { mutableStateOf(false) }

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

    var showFontFamily by remember { mutableStateOf(false) }
    var showFontSize by remember { mutableStateOf(false) }

    val fontFamilies = listOf(
        "Arial", "Georgia", "Courier New", "Times New Roman"
    )

    val fontSizes = listOf(
        "8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "26", "28", "36", "48", "72"
    )

    JAOATheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Ana Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonWithTooltip(Icons.Default.FormatBold, R.string.editorscreen_tooltipBold) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.toggleBold();", null)
                }
                IconButtonWithTooltip(Icons.Default.FormatItalic, R.string.editorscreen_tooltipItalic) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.toggleItalic();", null)
                }
                IconButtonWithTooltip(Icons.Default.FormatUnderlined, R.string.editorscreen_tooltipUnderline) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.toggleUnderline();", null)
                }

                // Vurgu
                IconButtonWithTooltip(Icons.Default.BorderColor, R.string.editorscreen_tooltipHighlight) {
                    showHighlightColors = !showHighlightColors
                    showTextColors = false
                    showFontFamily = false
                    showFontSize = false
                }

                // Renk
                IconButtonWithTooltip(Icons.Default.FormatColorText, R.string.editorscreen_tooltipColor) {
                    showTextColors = !showTextColors
                    showHighlightColors = false
                    showFontFamily = false
                    showFontSize = false
                }

                // Font Size
                IconButtonWithTooltip(Icons.Default.FormatSize, R.string.editorscreen_tooltipFontSize) {
                    showFontSize = !showFontSize
                    showFontFamily = false
                    showHighlightColors = false
                    showTextColors = false
                }

                // Font Family
                IconButtonWithTooltip(Icons.Default.FontDownload, R.string.editorscreen_tooltipFontFamily) {
                    showFontFamily = !showFontFamily
                    showFontSize = false
                    showHighlightColors = false
                    showTextColors = false
                }

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
                IconButtonWithTooltip(Icons.Default.Image, R.string.editorscreen_tooltipUploadImage) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.startImageUpload();", null)
                }
                IconButtonWithTooltip(Icons.Default.TableRows, R.string.editorscreen_tooltipInsertTable) {
                    webView?.evaluateJavascript("superdoc.activeEditor.commands.insertTable({cols: 5, rows: 5});", null)
                }
            }

                // Alt panel (gizli row, kaydırılabilir)
                AnimatedVisibility(
                    visible = showHighlightColors || showTextColors || showFontFamily || showFontSize,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showHighlightColors) {
                            ColorPalette(
                                colors = highlightColors,
                                onColorSelected = { color ->
                                    webView?.evaluateJavascript("superdoc.activeEditor.commands.setHighlight('$color');", null)
                                    showHighlightColors = false
                                },
                                onDismiss = { showHighlightColors = false }
                            )
                        }

                        if (showTextColors) {
                            ColorPalette(
                                colors = textColors,
                                onColorSelected = { color ->
                                    webView?.evaluateJavascript("superdoc.activeEditor.commands.setColor('$color');", null)
                                    showTextColors = false
                                },
                                onDismiss = { showTextColors = false }
                            )
                        }

                        if (showFontFamily) {
                            fontFamilies.forEach { family ->
                                Button(
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .height(36.dp),
                                    onClick = {
                                        webView?.evaluateJavascript("superdoc.activeEditor.commands.setFontFamily('$family');", null)
                                        showFontFamily = false
                                    }
                                ) {
                                    Text(family)
                                }
                            }
                        }

                        if (showFontSize) {
                            fontSizes.forEach { size ->
                                Button(
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .height(36.dp),
                                    onClick = {
                                        webView?.evaluateJavascript("superdoc.activeEditor.commands.setFontSize($size);", null)
                                        showFontSize = false
                                    }
                                ) {
                                    Text(size)
                                }
                            }
                        }
                    }
                }


        }
    }
}
