package com.sstek.jaoa.excel.utils

import android.util.Log
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.*

/**
 * EXCEL TO LUCKYSHEET
 * Excel color utilities for converting between Excel and Luckysheet formats
 * Supports both XSSF (modern) and indexed (legacy) color formats
 * AWT-free implementation for Android compatibility
 */
object ExcelColorUtils {

    private const val TAG = "ExcelColorUtils"

    // ===== BORDER COLOR EXTRACTION =====

    /**
     * Extract border color for specific side of a cell
     */
    fun extractBorderColor(cellStyle: CellStyle, side: String): String {
        Log.d(TAG, "🎨 Extracting border color for side: $side")

        try {
            // Check if border exists on this side
            val hasBorder = when (side) {
                "top" -> cellStyle.borderTop != BorderStyle.NONE
                "bottom" -> cellStyle.borderBottom != BorderStyle.NONE
                "left" -> cellStyle.borderLeft != BorderStyle.NONE
                "right" -> cellStyle.borderRight != BorderStyle.NONE
                else -> false
            }

            if (!hasBorder) {
                Log.d(TAG, "🎨 No border on $side, returning default black")
                return "#000000"
            }

            val xssfStyle = cellStyle as? XSSFCellStyle
            return if (xssfStyle != null) {
                Log.d(TAG, "🎨 Using XSSF color extraction")
                extractXSSFBorderColor(xssfStyle, side)
            } else {
                Log.d(TAG, "🎨 Using indexed color extraction")
                extractIndexedBorderColor(cellStyle, side)
            }

        } catch (e: Exception) {
            Log.e(TAG, "🎨 Error extracting border color for $side: ${e.message}")
            return "#000000"
        }
    }
    // ===== BACKGROUND COLOR EXTRACTION =====

    /**
     * Extract background/fill color from cell style
     */
    fun extractBackgroundColor(cellStyle: CellStyle?): String? {
        if (cellStyle == null) return null

        Log.d(TAG, "🎨 Extracting background color")

        try {
            val fillPattern = cellStyle.fillPattern
            if (fillPattern == FillPatternType.SOLID_FOREGROUND) {

                val xssfStyle = cellStyle as? XSSFCellStyle
                return if (xssfStyle != null) {
                    Log.d(TAG, "🎨 Using XSSF background color extraction")
                    extractXSSFBackgroundColor(xssfStyle)
                } else {
                    Log.d(TAG, "🎨 Using indexed background color extraction")
                    val colorIndex = cellStyle.fillForegroundColor.toInt()
                    val hexColor = convertIndexToHex(colorIndex)
                    if (hexColor != "#FFFFFF") hexColor else null
                }
            }
Log.d("deneme","null dondu")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "🎨 Error extracting background color: ${e.message}")
            return null
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private fun extractXSSFBorderColor(xssfStyle: XSSFCellStyle, side: String): String {
        try {
            val xssfColor = when (side) {
                "top" -> xssfStyle.topBorderXSSFColor
                "bottom" -> xssfStyle.bottomBorderXSSFColor
                "left" -> xssfStyle.leftBorderXSSFColor
                "right" -> xssfStyle.rightBorderXSSFColor
                else -> null
            }

            if (xssfColor != null) {
                val hexColor = convertXSSFColorToHex(xssfColor)
                Log.d(TAG, "🎨 XSSF border color for $side: $hexColor")
                return hexColor
            }

            Log.d(TAG, "🎨 No XSSF border color found for $side, trying indexed")
            return extractIndexedBorderColor(xssfStyle, side)

        } catch (e: Exception) {
            Log.e(TAG, "🎨 XSSF border color extraction failed for $side: ${e.message}")
            return extractIndexedBorderColor(xssfStyle, side)
        }
    }

    private fun extractIndexedBorderColor(cellStyle: CellStyle, side: String): String {
        try {
            val colorIndex = when (side) {
                "top" -> cellStyle.topBorderColor
                "bottom" -> cellStyle.bottomBorderColor
                "left" -> cellStyle.leftBorderColor
                "right" -> cellStyle.rightBorderColor
                else -> 8.toShort() // Default black
            }

            Log.d(TAG, "🎨 Border color index for $side: $colorIndex")
            val hexColor = convertIndexToHex(colorIndex.toInt())
            Log.d(TAG, "🎨 Indexed border color for $side: $hexColor")
            return hexColor

        } catch (e: Exception) {
            Log.e(TAG, "🎨 Indexed border color extraction failed for $side: ${e.message}")
            return "#000000"
        }
    }

    private fun extractXSSFBackgroundColor(xssfStyle: XSSFCellStyle): String? {
        try {
            val xssfColor = xssfStyle.fillForegroundColorColor as? XSSFColor
            if (xssfColor != null) {
                val hexColor = convertXSSFColorToHex(xssfColor)
                Log.d(TAG, "🎨 XSSF background color: $hexColor")
                return if (hexColor != "#FFFFFF") hexColor else null
            }
            Log.d(TAG, "🎨 No XSSF background color, trying indexed")
            val colorIndex = xssfStyle.fillForegroundColor.toInt()
            val hexColor = convertIndexToHex(colorIndex)
            return if (hexColor != "#FFFFFF") hexColor else null

        } catch (e: Exception) {
            Log.e(TAG, "🎨 XSSF background color extraction failed: ${e.message}")
            return null
        }
    }

    private fun convertXSSFColorToHex(xssfColor: XSSFColor): String {
        try {
            try {
                val ctColor = xssfColor.ctColor
                if (ctColor != null && ctColor.isSetRgb()) {
                    val rgbBytes = ctColor.rgb
                    if (rgbBytes != null && rgbBytes.size >= 3) {
                        val r = rgbBytes[0].toInt() and 0xFF
                        val g = rgbBytes[1].toInt() and 0xFF
                        val b = rgbBytes[2].toInt() and 0xFF
                        val hexColor = String.format("#%02X%02X%02X", r, g, b)
                        Log.d(TAG, "🎨 CTColor extraction successful: $hexColor")
                        return hexColor
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "🎨 CTColor access failed: ${e.message}")
            }

            // Method 3: Indexed color fallback
            try {
                val indexed = xssfColor.indexed
                if (indexed >= 0) {
                    val hexColor = convertIndexToHex(indexed.toInt())
                    Log.d(TAG, "🎨 Indexed fallback successful: $hexColor")
                    return hexColor
                }
            } catch (e: Exception) {
                Log.d(TAG, "🎨 Indexed fallback failed")
            }

            Log.w(TAG, "🎨 All XSSFColor extraction methods failed, using default")
            return "#000000"

        } catch (e: Exception) {
            Log.e(TAG, "🎨 XSSFColor conversion completely failed: ${e.message}")
            return "#000000"
        }
    }

    private fun convertIndexToHex(colorIndex: Int): String {
        Log.d(TAG, "🎨 Converting color index: $colorIndex")

        val hexColor = when (colorIndex) {
            0, 8 -> "#000000"   // Auto/Black
            1 -> "#000000"      // Black
            2 -> "#FFFFFF"      // White
            3 -> "#FF0000"      // Red
            4 -> "#00FF00"      // Green
            5 -> "#0000FF"      // Blue
            6 -> "#FFFF00"      // Yellow
            7 -> "#FF00FF"      // Magenta
            9 -> "#800000"      // Dark Red
            10 -> "#008000"     // Dark Green
            11 -> "#000080"     // Dark Blue
            12 -> "#808000"     // Olive
            13 -> "#800080"     // Purple
            14 -> "#008080"     // Teal
            15 -> "#C0C0C0"     // Silver
            16 -> "#808080"     // Gray
            17 -> "#9999FF"     // Light Blue
            18 -> "#993366"     // Dark Pink
            19 -> "#FFFFCC"     // Light Yellow
            20 -> "#CCFFFF"     // Light Cyan
            21 -> "#660066"     // Dark Purple
            22 -> "#FF8080"     // Light Red
            23 -> "#0066CC"     // Medium Blue
            24 -> "#CCCCFF"     // Very Light Blue
            25 -> "#000080"     // Navy Blue
            26 -> "#FF0000"     // Red Accent
            27 -> "#00B050"     // Green Accent
            28 -> "#0070C0"     // Blue Accent
            29 -> "#FFC000"     // Orange Accent
            30 -> "#7030A0"     // Purple Accent
            31 -> "#C5504B"     // Dark Red Accent
            32 -> "#4BACC6"     // Light Blue Accent
            33 -> "#9BBB59"     // Light Green Accent
            34 -> "#F79646"     // Orange
            35 -> "#8064A2"     // Lavender
            36 -> "#4F81BD"     // Steel Blue
            37 -> "#B2DF8A"     // Pale Green
            38 -> "#FFCCCC"     // Light Pink
            39 -> "#D9D9D9"     // Light Gray
            40 -> "#A6A6A6"     // Medium Gray
            41 -> "#FFFF99"     // Pale Yellow
            42 -> "#99CCFF"     // Sky Blue
            43 -> "#FF9999"     // Rose
            44 -> "#99FF99"     // Light Green
            45 -> "#FFCC99"     // Peach
            46 -> "#CC99FF"     // Light Purple
            47 -> "#FF6666"     // Salmon
            48 -> "#66CCFF"     // Light Sky Blue
            49 -> "#66FF66"     // Bright Green
            50 -> "#FFFF66"     // Bright Yellow
            64 -> "#000000"     // System foreground
            65 -> "#FFFFFF"     // System background
            else -> {
                Log.w(TAG, "🎨 Unknown color index: $colorIndex, using black")
                "#000000"
            }
        }
        Log.d(TAG, "🎨 Index $colorIndex → $hexColor")
        return hexColor
    }

    // ===== HEX TO EXCEL COLOR CONVERSION =====

    /**
     * lUCKYSHEET TO EXCEL
     * Convert hex color string to XSSFColor for Excel
     * Used when writing colors from Luckysheet to Excel
     */
    fun hexToXSSFColor(hex: String): XSSFColor? {
        return try {
            Log.d(TAG, "🎨 Converting hex to XSSFColor: $hex")

            val cleanHex = hex.removePrefix("#").uppercase()
            if (cleanHex.length != 6) {
                Log.w(TAG, "🎨 Invalid hex color length: $cleanHex")
                return null
            }

            val rgb = ByteArray(3)
            rgb[0] = cleanHex.substring(0, 2).toInt(16).toByte()
            rgb[1] = cleanHex.substring(2, 4).toInt(16).toByte()
            rgb[2] = cleanHex.substring(4, 6).toInt(16).toByte()

            val xssfColor = XSSFColor(rgb, null) // AWT-free!
            Log.d(TAG, "🎨 Successfully converted $hex to XSSFColor")
            return xssfColor

        } catch (e: Exception) {
            Log.e(TAG, "🎨 Failed to convert hex color: $hex", e)
            null
        }
    }

    /**
     * Apply background color to Excel cell style from hex string
     */
    fun applyBackgroundColor(cellStyle: XSSFCellStyle, hexColor: String?) {

        if (hexColor.isNullOrBlank())
        {
            Log.d("deneme", "hexcolorisnullorblank")
            return }

        try {
            Log.d(TAG, "🎨 Applying background color: $hexColor")
            val xssfColor = hexToXSSFColor(hexColor)
            xssfColor?.let {
                cellStyle.setFillForegroundColor(it)
                cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
                Log.d(TAG, "🎨 ✅ Background color applied successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "🎨 Failed to apply background color: $hexColor", e)
        }
    }

    /**
     * Apply border color to Excel cell style for specific side
     */
    fun applyBorderColor(cellStyle: XSSFCellStyle, hexColor: String, side: String) {
        try {
            Log.d(TAG, "🎨 Applying border color $hexColor to $side")
            val xssfColor = hexToXSSFColor(hexColor)
            xssfColor?.let { color ->
                when (side) {
                    "top" -> cellStyle.setTopBorderColor(color)
                    "bottom" -> cellStyle.setBottomBorderColor(color)
                    "left" -> cellStyle.setLeftBorderColor(color)
                    "right" -> cellStyle.setRightBorderColor(color)
                    "all" -> {
                        cellStyle.setTopBorderColor(color)
                        cellStyle.setBottomBorderColor(color)
                        cellStyle.setLeftBorderColor(color)
                        cellStyle.setRightBorderColor(color)
                    }
                }
                Log.d(TAG, "🎨 ✅ Border color applied to $side")
            }
        } catch (e: Exception) {
            Log.e(TAG, "🎨 Failed to apply border color: $hexColor to $side", e)
        }
    }
}