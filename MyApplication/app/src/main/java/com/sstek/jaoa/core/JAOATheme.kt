package com.sstek.jaoa.core

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Açık tema renkleri (mavi ağırlıklı)
private val LightColors = lightColorScheme(
    primary = Color(0xFF1E88E5),       // ana mavi
    onPrimary = Color.White,           // primary üzerine yazı
    secondary = Color(0xFF42A5F5),     // açık mavi
    onSecondary = Color.White,
    background = Color(0xFFF0F4FF),    // açık tonlu arka plan
    onBackground = Color(0xFF0D47A1),  // koyu mavi yazı
    surface = Color.White,             // kart, listItem yüzeyi
    onSurface = Color(0xFF0D47A1),    // yüzey üzerine yazı
    error = Color(0xFFD32F2F),        // hata rengi
    onError = Color.White,
    outline = Color(0xFF90CAF9)        // sınır/outline
)

// Karanlık tema renkleri (mavi ağırlıklı)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),       // açık mavi
    onPrimary = Color.Black,
    secondary = Color(0xFF64B5F6),     // biraz daha açık ton
    onSecondary = Color.Black,
    background = Color(0xFF0D1B2A),    // koyu mavi arka plan
    onBackground = Color(0xFFE3F2FD),  // açık yazı
    surface = Color(0xFF1B2A44),       // kart/listItem yüzeyi
    onSurface = Color(0xFFE3F2FD),     // yüzey üzeri yazı
    error = Color(0xFFEF5350),
    onError = Color.Black,
    outline = Color(0xFF64B5F6)
)

@Composable
fun JAOATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    //val colors = if (darkTheme) DarkColors else LightColors
    val colors = if (darkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(), // isteğe bağlı kendi font
        shapes = Shapes(),         // köşe yuvarlama, button shape vs
        content = content
    )
}
