package com.sstek.jaoa.old

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ColorPicker(
    colors: List<Int>,
    onColorSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        colors.forEach { colorInt ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(28.dp)
                    .background(Color(colorInt))
                    .clickable { onColorSelected(colorInt) }
            )
        }
    }
}
