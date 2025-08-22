package com.sstek.jaoa.excel.utils

import android.util.Log
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFFont

/**
 * Simple Font Color Utils - AWT-Free Final Version
 * CTColor ile güvenli RGB extraction + kendi color mapping
 */
object SimpleFontColorUtils {

    private const val TAG = "SimpleFontColor"

    // ===== EXCEL'DEN RENK OKUMA =====

    fun extractFontColorSimple(font: Font?): String? {
        if (font == null) return null

        try {
            Log.d(TAG, "🎨 Extracting font color...")

            val xssfFont = font as? XSSFFont ?: return null
            val xssfColor = xssfFont.xssfColor

            if (xssfColor != null) {
                // CTColor ile güvenli RGB extraction
                try {
                    val ctColor = xssfColor.ctColor
                    if (ctColor != null && ctColor.isSetRgb()) {
                        val rgbBytes = ctColor.rgb
                        if (rgbBytes != null) {
                            Log.d(TAG, "🔍 CTColor bytes length: ${rgbBytes.size}")
                            Log.d(TAG, "🔍 CTColor bytes: [${rgbBytes.map { it.toInt() and 0xFF }}]")

                            if (rgbBytes.size == 3) {
                                // RGB format
                                val r = rgbBytes[0].toInt() and 0xFF
                                val g = rgbBytes[1].toInt() and 0xFF
                                val b = rgbBytes[2].toInt() and 0xFF
                                val hex = String.format("#%02X%02X%02X", r, g, b)
                                Log.d(TAG, "✅ Font color from RGB: $hex")
                                return hex
                            } else if (rgbBytes.size == 4) {
                                // ARGB format - son 3 byte'ı al (RGB kısmı)
                                val r = rgbBytes[1].toInt() and 0xFF
                                val g = rgbBytes[2].toInt() and 0xFF
                                val b = rgbBytes[3].toInt() and 0xFF
                                val hex = String.format("#%02X%02X%02X", r, g, b)
                                Log.d(TAG, "✅ Font color from ARGB (skipped alpha): $hex")
                                return hex
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "CTColor access failed: ${e.message}")
                }
            }

            // Fallback: Indexed color mapping
            val colorIndex = xssfFont.color.toInt()
            if (colorIndex > 0 && colorIndex != 8) {
                val hex = indexToHex(colorIndex)
                Log.d(TAG, "✅ Font color from index $colorIndex: $hex")
                return hex
            }

            Log.d(TAG, "❌ No font color found (index: $colorIndex)")
            return null

        } catch (e: Exception) {
            Log.e(TAG, "❌ Font color extraction failed: ${e.message}")
            return null
        }
    }

    // ===== EXCEL'E RENK YAZMA =====

    fun applyFontColorSimple(font: XSSFFont, hexColor: String?) {
        if (hexColor.isNullOrBlank()) return

        try {
            Log.d(TAG, "🎨 Applying font color: $hexColor")

            // Method 1: Custom color via RGB bytes
            val rgbBytes = hexToBytes(hexColor)
            if (rgbBytes != null) {
                try {
                    val xssfColor = XSSFColor(rgbBytes, null)
                    font.setColor(xssfColor)
                    Log.d(TAG, "✅ Font color applied as custom RGB")
                    return
                } catch (e: Exception) {
                    Log.d(TAG, "Custom RGB failed: ${e.message}")
                }
            }

            // Method 2: Fallback to indexed color
            val colorIndex = hexToIndex(hexColor)
            font.color = colorIndex.toShort()
            Log.d(TAG, "✅ Font color applied as indexed color: $colorIndex")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Font color application failed: ${e.message}")
        }
    }

    // ===== UTILITY METHODS =====

    /**
     * Hex string'i byte array'e çevirir
     */
    private fun hexToBytes(hexColor: String): ByteArray? {
        return try {
            val cleanHex = hexColor.removePrefix("#").uppercase()
            if (cleanHex.length != 6) return null

            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)

            byteArrayOf(r.toByte(), g.toByte(), b.toByte())
        } catch (e: Exception) {
            Log.e(TAG, "Hex to bytes conversion failed: ${e.message}")
            null
        }
    }

    /**
     * Kendi color index mapping tablomuz - POI'nin HSSFColor'ına dokunmaz
     * Excel'in gerçek color index'leri
     */
    private fun indexToHex(colorIndex: Int): String {
        return when (colorIndex) {
            // Built-in Excel colors (gerçek index'ler)
            8 -> "#000000"    // Black (Auto)
            9 -> "#FFFFFF"    // White
            10 -> "#FF0000"   // Red
            11 -> "#00FF00"   // Bright Green
            12 -> "#0000FF"   // Blue
            13 -> "#FFFF00"   // Yellow
            14 -> "#FF00FF"   // Magenta
            15 -> "#00FFFF"   // Cyan
            16 -> "#800000"   // Dark Red
            17 -> "#008000"   // Green
            18 -> "#000080"   // Dark Blue
            19 -> "#808000"   // Dark Yellow (Olive)
            20 -> "#800080"   // Purple
            21 -> "#008080"   // Teal
            22 -> "#C0C0C0"   // Silver
            23 -> "#808080"   // Grey

            // Theme colors (Modern Excel)
            24 -> "#99CCFF"   // Light Blue
            25 -> "#993366"   // Dark Pink
            26 -> "#FFFFCC"   // Light Yellow
            27 -> "#CCFFFF"   // Light Cyan
            28 -> "#660066"   // Dark Purple
            29 -> "#FF8080"   // Light Red
            30 -> "#0066CC"   // Medium Blue
            31 -> "#CCCCFF"   // Very Light Blue

            // Office theme colors
            32 -> "#000080"   // Navy Blue
            33 -> "#FF0000"   // Red Accent
            34 -> "#00B050"   // Green Accent
            35 -> "#0070C0"   // Blue Accent
            36 -> "#FFC000"   // Orange Accent
            37 -> "#7030A0"   // Purple Accent
            38 -> "#C5504B"   // Dark Red Accent
            39 -> "#4BACC6"   // Light Blue Accent
            40 -> "#9BBB59"   // Light Green Accent

            // Additional palette
            41 -> "#F79646"   // Orange
            42 -> "#8064A2"   // Lavender
            43 -> "#4F81BD"   // Steel Blue
            44 -> "#B2DF8A"   // Pale Green
            45 -> "#FFCCCC"   // Light Pink
            46 -> "#D9D9D9"   // Light Gray
            47 -> "#A6A6A6"   // Medium Gray
            48 -> "#FFFF99"   // Pale Yellow
            49 -> "#99CCFF"   // Sky Blue
            50 -> "#FF9999"   // Rose

            // More theme variants
            51 -> "#99FF99"   // Light Green
            52 -> "#FFCC99"   // Peach
            53 -> "#CC99FF"   // Light Purple
            54 -> "#FF6666"   // Salmon
            55 -> "#66CCFF"   // Light Sky Blue
            56 -> "#66FF66"   // Bright Green
            57 -> "#FFFF66"   // Bright Yellow
            58 -> "#FF66FF"   // Bright Magenta
            59 -> "#66FFFF"   // Bright Cyan
            60 -> "#FFE066"   // Gold

            // Default colors for unknown indices
            1, 2, 3, 4, 5, 6, 7 -> "#000000"  // Early indices - black
            64 -> "#000000"   // System foreground
            65 -> "#FFFFFF"   // System background

            else -> {
                Log.d(TAG, "⚠️ Unknown color index: $colorIndex - using default")
                "#000000"     // Default to black for unknown indices
            }
        }
    }

    /**
     * Hex'i en yakın color index'e çevirir (yazma için)
     */
    private fun hexToIndex(hexColor: String): Int {
        val cleanHex = hexColor.uppercase()

        // Direct matches
        return when (cleanHex) {
            "#000000" -> 8    // Black
            "#FFFFFF" -> 9    // White
            "#FF0000" -> 10   // Red
            "#00FF00" -> 11   // Bright Green
            "#0000FF" -> 12   // Blue
            "#FFFF00" -> 13   // Yellow
            "#FF00FF" -> 14   // Magenta
            "#00FFFF" -> 15   // Cyan
            "#800000" -> 16   // Dark Red
            "#008000" -> 17   // Green
            "#000080" -> 18   // Dark Blue
            "#808000" -> 19   // Olive
            "#800080" -> 20   // Purple
            "#008080" -> 21   // Teal
            "#C0C0C0" -> 22   // Silver
            "#808080" -> 23   // Grey
            else -> {
                // Find closest match
                val closestIndex = findClosestColorIndex(cleanHex)
                Log.d(TAG, "🎯 Closest match for $cleanHex: index $closestIndex")
                closestIndex
            }
        }
    }

    /**
     * RGB distance ile en yakın rengi bulur
     */
    private fun findClosestColorIndex(targetHex: String): Int {
        val targetRgb = hexToRgb(targetHex)
        var minDistance = Double.MAX_VALUE
        var closestIndex = 8 // Default: black

        // All known indices'leri test et
        val knownIndices = listOf(8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40)

        knownIndices.forEach { index ->
            val indexHex = indexToHex(index)
            val indexRgb = hexToRgb(indexHex)
            val distance = colorDistance(targetRgb, indexRgb)

            if (distance < minDistance) {
                minDistance = distance
                closestIndex = index
            }
        }

        return closestIndex
    }

    /**
     * Hex'i RGB'ye çevirir
     */
    private fun hexToRgb(hex: String): Triple<Int, Int, Int> {
        val cleanHex = hex.removePrefix("#")
        val r = cleanHex.substring(0, 2).toInt(16)
        val g = cleanHex.substring(2, 4).toInt(16)
        val b = cleanHex.substring(4, 6).toInt(16)
        return Triple(r, g, b)
    }

    /**
     * İki RGB değeri arasındaki mesafeyi hesaplar
     */
    private fun colorDistance(rgb1: Triple<Int, Int, Int>, rgb2: Triple<Int, Int, Int>): Double {
        val dr = rgb1.first - rgb2.first
        val dg = rgb1.second - rgb2.second
        val db = rgb1.third - rgb2.third
        return kotlin.math.sqrt((dr * dr + dg * dg + db * db).toDouble())
    }

    /**
     * Debug için renk bilgilerini loglar
     */
    fun debugFontColor(font: XSSFFont, label: String = "") {
        Log.d(TAG, "=== FONT COLOR DEBUG: $label ===")
        try {
            Log.d(TAG, "Font.color index: ${font.color}")
            Log.d(TAG, "Font name: ${font.fontName}")
            Log.d(TAG, "Font size: ${font.fontHeightInPoints}")
        } catch (e: Exception) {
            Log.e(TAG, "Debug failed: ${e.message}")
        }
        Log.d(TAG, "================================")
    }
}