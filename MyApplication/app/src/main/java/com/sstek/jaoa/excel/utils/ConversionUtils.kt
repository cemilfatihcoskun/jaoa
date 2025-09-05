package com.sstek.jaoa.excel.utils

import java.text.SimpleDateFormat
import java.util.*

object ConversionUtils {

    // Column width conversions
    fun pxToExcelColumnWidthUnits(pixels: Double): Int {
        val charCount = (pixels - 5) / 7.0
        val units = kotlin.math.round(charCount * 256).toInt()
        return units.coerceAtMost(65280).coerceAtLeast(0) // Max 255 char = 65280 units
    }

    fun excelColumnWidthToPx(widthUnits: Int): Int {
        val charCount = widthUnits / 256.0
        return kotlin.math.floor(charCount * 7 + 5).toInt()
    }

    // Row height conversions
    fun pxToTwips(pixels: Double): Short {
        val twips = kotlin.math.round(pixels * 15.0).toInt()
        return twips.coerceIn(0, Short.MAX_VALUE.toInt()).toShort()
    }

    fun excelRowHeightToPx(heightTwips: Short): Int {
        return kotlin.math.round(heightTwips / 15.0).toInt()
    }

    // Text utilities
    fun cleanControlCharacters(text: String): String {
        return text
            .replace("\u0000", "")      // NULL
            .replace("\u0001", "")      // SOH
            .replace("\u0002", "")      // STX
            .replace("\u0003", "")      // ETX
            .replace("\u0004", "")      // EOT
            .replace("\u0005", "")      // ENQ
            .replace("\u0006", "")      // ACK
            .replace("\u0007", "")      // BEL
            .replace("\u0008", "")      // BS
            .replace("\u000B", "")      // VT
            .replace("\u000C", "")      // FF
            .replace("\u000E", "")      // SO
            .replace("\u000F", "")      // SI
            .replace("\u0010", "")      // DLE
            .replace("\u0011", "")      // DC1
            .replace("\u0012", "")      // DC2
            .replace("\u0013", "")      // DC3
            .replace("\u0014", "")      // DC4
            .replace("\u0015", "")      // NAK
            .replace("\u0016", "")      // SYN
            .replace("\u0017", "")      // ETB
            .replace("\u0018", "")      // CAN
            .replace("\u0019", "")      // EM
            .replace("\u001A", "")      // SUB
            .replace("\u001B", "")      // ESC
            .replace("\u001C", "")      // FS
            .replace("\u001D", "")      // GS
            .replace("\u001E", "")      // RS
            .replace("\u001F", "")      // US
            .replace("\u007F", "")      // DEL
    }

    fun isDateString(value: String): Boolean {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(value) != null
        } catch (e: Exception) {
            false
        }
    }
}