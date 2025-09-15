package com.sstek.jaoa.word

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPalette(
    colors: List<String>,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    boxSize: Dp = 36.dp,
    spacing: Dp = 8.dp
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(boxSize)
                            .background(
                                color = Color(android.graphics.Color.parseColor(color)),
                                shape = CircleShape
                            )
                            .border(1.dp, Color.Black, CircleShape)
                            .clickable {
                                onColorSelected(color)
                                onDismiss()
                            }
                    )
                }
            }
        }
    }
}
