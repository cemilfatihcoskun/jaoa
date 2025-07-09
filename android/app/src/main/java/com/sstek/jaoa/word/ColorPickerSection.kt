package com.sstek.jaoa.word

import DefaultColorRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHighlightColor

@Composable
fun ColorPickerSection(
    onTextColorSelected: (Color) -> Unit,
    onBackgroundColorSelected: (Color) -> Unit
) {
    val textColorController = rememberColorPickerController()
    val backgroundColorController = rememberColorPickerController()

    var selectedTextColor by remember { mutableStateOf(Color.Black) }
    var selectedBackgroundColor by remember { mutableStateOf(Color.Unspecified) }

    val defaultColors = listOf(
        Color.Black, Color.Red, Color.Blue, Color.Green, Color.DarkGray, Color.White,
        Color.Yellow, Color.Magenta, Color.Cyan, Color.Gray, Color.LightGray
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Yazı Rengi Paneli
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
        ) {
            Text("Yazı Rengi", modifier = Modifier.padding(4.dp))
            HsvColorPicker(
                modifier = Modifier
                    .height(180.dp)
                    .padding(8.dp),
                controller = textColorController,
                onColorChanged = { envelope ->
                    selectedTextColor = envelope.color
                    onTextColorSelected(envelope.color)
                }
            )
            // Renk Önizleme Barı
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(selectedTextColor)
                    .padding(6.dp)
            )
            DefaultColorRow(defaultColors) { color ->
                selectedTextColor = color
                textColorController.selectByColor(color, false)
                onTextColorSelected(color)
            }
        }

        // Arkaplan Rengi Paneli
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        ) {
            Text("Arkaplan Rengi", modifier = Modifier.padding(4.dp))

            DefaultColorRow(defaultColors) { color ->
                selectedBackgroundColor = color
                backgroundColorController.selectByColor(color, false)
                onBackgroundColorSelected(color)
            }
        }
    }
}
