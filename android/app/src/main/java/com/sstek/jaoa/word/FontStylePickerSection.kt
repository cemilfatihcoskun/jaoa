package com.sstek.jaoa.word

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sstek.jaoa.R

val ARIAL = FontFamily(Font(R.font.arial))
val CALIBRI = FontFamily(Font(R.font.calibri))
val TIMES_NEW_ROMAN = FontFamily(Font(R.font.times_new_roman))

val fontOptions = listOf(
    "Arial" to ARIAL,
    "Calibri" to CALIBRI,
    "Times New Roman" to TIMES_NEW_ROMAN
)

val fontSizes = mutableListOf(
    8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72, 96
)

@Composable
fun FontStylePickerSection(
    onFontSelected: (String) -> Unit,
    onSizeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFFF0F0F0))
            .padding(8.dp)
    ) {
        // Yazı Tipi Paneli
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text("Yazı Tipi", modifier = Modifier.padding(4.dp))
            androidx.compose.foundation.lazy.LazyColumn {
                items(fontOptions.size) { index ->
                    val (label, fontFamily) = fontOptions[index]
                    Text(
                        text = label,
                        fontFamily = fontFamily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                            .clickable { onFontSelected(label) }
                            .padding(8.dp)
                    )
                }
            }
        }

        // Yazı Boyutu Paneli
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text("Yazı Boyutu", modifier = Modifier.padding(4.dp))
            androidx.compose.foundation.lazy.LazyColumn {
                items(fontSizes.size) { index ->
                    val size = fontSizes[index]
                    Text(
                        text = "$size pt",
                        fontSize = size.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                            .clickable { onSizeSelected(size) }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}