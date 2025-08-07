package com.sstek.jaoa.old

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FontPicker(
    fonts: List<String>,
    fontSizes: List<Int>,
    selectedFont: String,
    selectedFontSize: Int,
    onFontSelected: (String) -> Unit,
    onFontSizeSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        shadowElevation = 8.dp,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var fontExpanded by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { fontExpanded = true }) {
                    Text("Font: $selectedFont")
                }
                DropdownMenu(
                    expanded = fontExpanded,
                    onDismissRequest = { fontExpanded = false }
                ) {
                    fonts.forEach { font ->
                        DropdownMenuItem(
                            text = { Text(font) },
                            onClick = {
                                onFontSelected(font)
                                fontExpanded = false
                            }
                        )
                    }
                }
            }

            var sizeExpanded by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { sizeExpanded = true }) {
                    Text("Boyut: $selectedFontSize")
                }
                DropdownMenu(
                    expanded = sizeExpanded,
                    onDismissRequest = { sizeExpanded = false }
                ) {
                    fontSizes.forEach { size ->
                        DropdownMenuItem(
                            text = { Text("$size") },
                            onClick = {
                                onFontSizeSelected(size)
                                sizeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    }
}
