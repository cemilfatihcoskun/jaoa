package com.sstek.jaoa.excel.utils
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.*
import java.text.SimpleDateFormat
import java.util.*

class ExcelToLuckysheetConverter {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    companion object {
        private const val TAG = "ExcelToLuckysheet"
    }

    fun convert(workbook: XSSFWorkbook): String {
        Log.d(TAG, "Converting Excel workbook to Luckysheet format")

        val sheets = mutableListOf<LuckysheetSheet>()

        for (i in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(i)
            val luckysheetSheet = convertSheet(sheet, i)
            sheets.add(luckysheetSheet)
            Log.d(TAG, "Converted sheet: ${sheet.sheetName}")
        }

        val result = gson.toJson(sheets)
        Log.d(TAG, "Conversion completed. Generated JSON size: ${result.length} characters")

        return result
    }

    private fun convertSheet(sheet: XSSFSheet, sheetIndex: Int): LuckysheetSheet {
        Log.d(TAG, "Converting sheet: ${sheet.sheetName}")

        val cellData = mutableListOf<LuckysheetCell>()
        var maxRow = 0
        var maxCol = 0

        for (row in sheet) {
            maxRow = maxOf(maxRow, row.rowNum)
            for (cell in row) {
                maxCol = maxOf(maxCol, cell.columnIndex)
                val luckysheetCell = convertCell(cell)
                if (luckysheetCell != null) {
                    cellData.add(luckysheetCell)
                }
            }
        }

        // Merge, column/row sizes
        val mergeObject = mutableMapOf<String, Map<String, Int>>()
        for (mergedRegion in sheet.mergedRegions) {
            val key = "${mergedRegion.firstRow}_${mergedRegion.firstColumn}"
            mergeObject[key] = mapOf(
                "r" to mergedRegion.firstRow,
                "c" to mergedRegion.firstColumn,
                "rs" to (mergedRegion.lastRow - mergedRegion.firstRow + 1),
                "cs" to (mergedRegion.lastColumn - mergedRegion.firstColumn + 1)
            )
        }

        val columnWidths = mutableMapOf<String, Double>()
        val rowHeights = mutableMapOf<String, Double>()

        for (col in 0..maxCol) {
            val width = sheet.getColumnWidth(col)
            if (width != sheet.defaultColumnWidth) {
                val luckysheetWidth = (width / 256.0)
                columnWidths[col.toString()] = maxOf(luckysheetWidth, 73.0)
            }
        }

        for (rowNum in 0..maxRow) {
            val row = sheet.getRow(rowNum)
            if (row != null && row.height != sheet.defaultRowHeight) {
                val luckysheetHeight = (row.height / 20.0)
                rowHeights[rowNum.toString()] = luckysheetHeight
            }
        }

        val rangeBorderInfo = createBorderInfo(sheet, maxRow, maxCol)

        val config = LuckysheetConfig(
            merge = if (mergeObject.isNotEmpty()) mergeObject else null,
            columnlen = if (columnWidths.isNotEmpty()) columnWidths else null,
            rowlen = if (rowHeights.isNotEmpty()) rowHeights else null,
            borderInfo = if (rangeBorderInfo.isNotEmpty()) rangeBorderInfo else null
        )

        return LuckysheetSheet(
            name = sheet.sheetName ?: "Sheet${sheetIndex + 1}",
            index = sheetIndex,
            celldata = if (cellData.isNotEmpty()) cellData else null,
            row = maxOf(maxRow + 1, LuckysheetConstants.DEFAULT_ROW_COUNT),
            column = maxOf(maxCol + 1, LuckysheetConstants.DEFAULT_COLUMN_COUNT),
            config = config
        )
    }
    private fun createBorderInfo(sheet: XSSFSheet, maxRow: Int, maxCol: Int): List<Map<String, Any>> {
        val borderInfo = mutableListOf<Map<String, Any>>()

        try {
            Log.d(TAG, "🔍 Reading borders directly from Excel...")

            for (r in 0..maxRow) {
                val row = sheet.getRow(r) ?: continue
                for (c in 0..maxCol) {
                    val cell = row.getCell(c) ?: continue
                    val style = cell.cellStyle

                    val hasTop = style.borderTop != BorderStyle.NONE
                    val hasBottom = style.borderBottom != BorderStyle.NONE
                    val hasLeft = style.borderLeft != BorderStyle.NONE
                    val hasRight = style.borderRight != BorderStyle.NONE

                    if (hasTop || hasBottom || hasLeft || hasRight) {

                        if (hasTop && hasBottom && hasLeft && hasRight &&
                            isSameStyleAndColor(style)) {

                            borderInfo.add(mapOf(
                                "rangeType" to "range",
                                "borderType" to "border-all",
                                "style" to extractBorderStyleForSide(style, "top"),
                                "color" to (extractBorderColorForSide(style, "top") ?: "#000000"),
                                "range" to listOf(mapOf(
                                    "row" to listOf(r, r),
                                    "column" to listOf(c, c)
                                ))
                            ))
                        } else {
                            addIndividualBorders(borderInfo, r, c, style)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Border reading failed: ${e.message}")
        }

        Log.d(TAG, "🔍 Border reading completed: ${borderInfo.size} entries")
        return borderInfo
    }

    private fun isSameStyleAndColor(style: CellStyle): Boolean {
        return try {
            val topStyle = extractBorderStyleForSide(style, "top")
            val bottomStyle = extractBorderStyleForSide(style, "bottom")
            val leftStyle = extractBorderStyleForSide(style, "left")
            val rightStyle = extractBorderStyleForSide(style, "right")

            val topColor = extractBorderColorForSide(style, "top")
            val bottomColor = extractBorderColorForSide(style, "bottom")
            val leftColor = extractBorderColorForSide(style, "left")
            val rightColor = extractBorderColorForSide(style, "right")

            topStyle == bottomStyle && bottomStyle == leftStyle && leftStyle == rightStyle &&
                    topColor == bottomColor && bottomColor == leftColor && leftColor == rightColor
        } catch (e: Exception) {
            false
        }
    }

    private fun addIndividualBorders(
        borderInfo: MutableList<Map<String, Any>>,
        r: Int,
        c: Int,
        style: CellStyle
    ) {

        if (style.borderTop != BorderStyle.NONE) {
            borderInfo.add(mapOf(
                "rangeType" to "range",
                "borderType" to "border-top",
                "style" to extractBorderStyleForSide(style, "top"),
                "color" to (extractBorderColorForSide(style, "top") ?: "#000000"),
                "range" to listOf(mapOf(
                    "row" to listOf(r, r),
                    "column" to listOf(c, c)
                ))
            ))
        }


        if (style.borderBottom != BorderStyle.NONE) {
            borderInfo.add(mapOf(
                "rangeType" to "range",
                "borderType" to "border-bottom",
                "style" to extractBorderStyleForSide(style, "bottom"),
                "color" to (extractBorderColorForSide(style, "bottom") ?: "#000000"),
                "range" to listOf(mapOf(
                    "row" to listOf(r, r),
                    "column" to listOf(c, c)
                ))
            ))
        }

        if (style.borderLeft != BorderStyle.NONE) {
            borderInfo.add(mapOf(
                "rangeType" to "range",
                "borderType" to "border-left",
                "style" to extractBorderStyleForSide(style, "left"),
                "color" to (extractBorderColorForSide(style, "left") ?: "#000000"),
                "range" to listOf(mapOf(
                    "row" to listOf(r, r),
                    "column" to listOf(c, c)
                ))
            ))
        }

        if (style.borderRight != BorderStyle.NONE) {
            borderInfo.add(mapOf(
                "rangeType" to "range",
                "borderType" to "border-right",
                "style" to extractBorderStyleForSide(style, "right"),
                "color" to (extractBorderColorForSide(style, "right") ?: "#000000"),
                "range" to listOf(mapOf(
                    "row" to listOf(r, r),
                    "column" to listOf(c, c)
                ))
            ))
        }
    }

    private fun convertCell(cell: Cell): LuckysheetCell? {
        try {
            val xssfCell = cell as? XSSFCell ?: return null

            val cellValue = extractCellValue(xssfCell)
            val displayValue = extractDisplayValue(xssfCell)
            val cellStyle = xssfCell.cellStyle

            val hasBackground = try {
                cellStyle.fillPattern == FillPatternType.SOLID_FOREGROUND &&
                        cellStyle.fillForegroundColor != IndexedColors.AUTOMATIC.index
            } catch (e: Exception) {
                false
            }

            val hasBorderTop = cellStyle.borderTop != BorderStyle.NONE
            val hasBorderBottom = cellStyle.borderBottom != BorderStyle.NONE
            val hasBorderLeft = cellStyle.borderLeft != BorderStyle.NONE
            val hasBorderRight = cellStyle.borderRight != BorderStyle.NONE
            val hasBorder = hasBorderTop || hasBorderBottom || hasBorderLeft || hasBorderRight

            // Skip empty cells
            if (cellValue == null && !hasFormula(xssfCell) && !hasBackground && !hasBorder) {
                return null
            }

            val font = xssfCell.sheet.workbook.getFontAt(cellStyle.fontIndexAsInt)

            val luckysheetValue = LuckysheetCellValue(
                v = cellValue,
                m = displayValue,
                f = getFormulaSafely(xssfCell),
                ct = getLuckysheetCellType(xssfCell),


                ff = font.fontName ?: "Arial",
                fs = font.fontHeightInPoints.toInt().takeIf { it > 0 } ?: 11,
                fc = extractFontColor(font as? XSSFFont),
                bl = if (font.bold) 1 else 0,
                it = if (font.italic) 1 else 0,
                cl = if (font.underline != Font.U_NONE) 1 else 0,

                bg = extractBackgroundColor(cellStyle as? XSSFCellStyle),
                ht = getHorizontalAlignment(cellStyle.alignment),
                vt = getVerticalAlignment(cellStyle.verticalAlignment)
            )

            return LuckysheetCell(
                r = xssfCell.rowIndex,
                c = xssfCell.columnIndex,
                v = luckysheetValue
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error converting cell at ${cell.rowIndex},${cell.columnIndex}", e)
            return null
        }
    }

    private fun extractBorderColorForSide(cellStyle: CellStyle, side: String): String {
        Log.d(TAG, "🎨 Extracting border color for side: $side")

        try {
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
            if (xssfStyle != null) {
                Log.d(TAG, "🎨 Using XSSF color extraction")
                return extractXSSFBorderColor(xssfStyle, side)
            } else {
                Log.d(TAG, "🎨 Using indexed color extraction")
                return extractIndexedBorderColor(cellStyle, side)
            }

        } catch (e: Exception) {
            Log.e(TAG, "🎨 Error extracting border color for $side: ${e.message}")
            return "#000000"
        }
    }
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
                Log.d(TAG, "🎨 XSSF color for $side: $hexColor")
                return hexColor
            }

            Log.d(TAG, "🎨 No XSSF color found for $side, trying indexed")
            return extractIndexedBorderColor(xssfStyle, side)

        } catch (e: Exception) {
            Log.e(TAG, "🎨 XSSF color extraction failed for $side: ${e.message}")
            return extractIndexedBorderColor(xssfStyle, side)
        }
    }

    // ✅ INDEXED COLOR EXTRACTION (Legacy Excel format)
    private fun extractIndexedBorderColor(cellStyle: CellStyle, side: String): String {
        try {
            val colorIndex = when (side) {
                "top" -> cellStyle.topBorderColor
                "bottom" -> cellStyle.bottomBorderColor
                "left" -> cellStyle.leftBorderColor
                "right" -> cellStyle.rightBorderColor
                else -> 8.toShort() // Default black
            }

            Log.d(TAG, "🎨 Color index for $side: $colorIndex")
            val hexColor = convertIndexToHex(colorIndex.toInt())
            Log.d(TAG, "🎨 Indexed color for $side: $hexColor")
            return hexColor

        } catch (e: Exception) {
            Log.e(TAG, "🎨 Indexed color extraction failed for $side: ${e.message}")
            return "#000000"
        }
    }
    private fun convertXSSFColorToHex(xssfColor: XSSFColor): String {
        try {
            try {
                val rgbField = xssfColor.javaClass.getDeclaredField("rgb")
                rgbField.isAccessible = true
                val rgbBytes = rgbField.get(xssfColor) as? ByteArray

                if (rgbBytes != null && rgbBytes.size >= 3) {
                    val r = rgbBytes[0].toInt() and 0xFF
                    val g = rgbBytes[1].toInt() and 0xFF
                    val b = rgbBytes[2].toInt() and 0xFF
                    val hexColor = String.format("#%02X%02X%02X", r, g, b)
                    Log.d(TAG, "🎨 RGB extraction successful: $hexColor (R:$r, G:$g, B:$b)")
                    return hexColor
                }
            } catch (e: Exception) {
                Log.d(TAG, "🎨 RGB field access failed, trying alternative methods")
            }


            try {
                val argbHex = xssfColor.argbHex
                if (argbHex != null && argbHex.length >= 8) {
                    val hexColor = "#" + argbHex.substring(2) // Remove alpha channel
                    Log.d(TAG, "🎨 ARGB extraction successful: $hexColor")
                    return hexColor
                }
            } catch (e: Exception) {
                Log.d(TAG, "🎨 ARGB access failed")
            }

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
            // Basic colors
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

            // Extended colors
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

            // System colors
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
    private fun extractBorderStyleForSide(cellStyle: CellStyle, side: String): Int {
        return try {
            val borderStyle = when (side) {
                "top" -> if (cellStyle.borderTop != BorderStyle.NONE) cellStyle.borderTop else null
                "bottom" -> if (cellStyle.borderBottom != BorderStyle.NONE) cellStyle.borderBottom else null
                "left" -> if (cellStyle.borderLeft != BorderStyle.NONE) cellStyle.borderLeft else null
                "right" -> if (cellStyle.borderRight != BorderStyle.NONE) cellStyle.borderRight else null
                else -> null
            }

            borderStyle?.let {
                when (it) {
                    BorderStyle.NONE -> 0
                    BorderStyle.THIN, BorderStyle.HAIR -> 1
                    BorderStyle.THICK, BorderStyle.MEDIUM -> 2
                    BorderStyle.DASHED, BorderStyle.MEDIUM_DASHED -> 3
                    BorderStyle.DOTTED -> 4
                    BorderStyle.DOUBLE -> 5
                    else -> 1
                }
            } ?: 1
        } catch (e: Exception) {
            1
        }
    }

    private fun extractXSSFColorToHexSafe(xssfColor: XSSFColor): String {
        Log.d("deneme","2")
        return try {
            try {
                val rgbField = xssfColor.javaClass.getDeclaredField("rgb")
                rgbField.isAccessible = true
                val rgbBytes = rgbField.get(xssfColor) as? ByteArray

                if (rgbBytes != null && rgbBytes.size >= 3) {
                    val r = rgbBytes[0].toInt() and 0xFF
                    val g = rgbBytes[1].toInt() and 0xFF
                    val b = rgbBytes[2].toInt() and 0xFF
                    Log.d("deneme",String.format("#%02X%02X%02X", r, g, b))
                    return String.format("#%02X%02X%02X", r, g, b)
                }
            } catch (e: Exception) {

            }
            // Method 2: Indexed color fallback
            try {
                Log.d("deneme","3")
                val indexed = xssfColor.indexed
                if (indexed >= 0) {
                    return getColorFromIndex(indexed.toInt()) ?: "#000000"
                }
            } catch (e: Exception) {
                // Indexed access failed
            }
            "#000000"
        } catch (e: Exception) {
            "#000000"
        }
    }
    private fun hasFormula(cell: XSSFCell): Boolean {
        return try {
            cell.cellType == CellType.FORMULA
        } catch (e: Exception) {
            false
        }
    }

    private fun getFormulaSafely(cell: XSSFCell): String? {
        return try {
            if (cell.cellType == CellType.FORMULA) {
                cell.cellFormula
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractCellValue(cell: Cell): Any? {
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {

                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cell.dateCellValue)
                } else {
                    val numValue = cell.numericCellValue

                    if (numValue == numValue.toLong().toDouble()) {
                        numValue.toLong()
                    } else {
                        numValue
                    }
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue
            CellType.FORMULA -> {
                try {
                    when (cell.cachedFormulaResultType) {
                        CellType.NUMERIC -> {
                            val numValue = cell.numericCellValue
                            if (numValue == numValue.toLong().toDouble()) {
                                numValue.toLong()
                            } else {
                                numValue
                            }
                        }
                        CellType.STRING -> cell.stringCellValue
                        CellType.BOOLEAN -> cell.booleanCellValue
                        else -> null
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not evaluate formula in cell ${cell.rowIndex},${cell.columnIndex}")
                    null
                }
            }
            CellType.BLANK -> null
            CellType.ERROR -> "#ERROR"
            else -> null
        }
    }

    private fun extractDisplayValue(cell: Cell): String? {
        return try {
            val dataFormatter = DataFormatter()
            val displayValue = dataFormatter.formatCellValue(cell)

            displayValue.takeIf { it.isNotBlank() }?.let { cleanControlCharacters(it) }
        } catch (e: Exception) {
            extractCellValue(cell)?.toString()?.let { cleanControlCharacters(it) }
        }
    }

    private fun cleanControlCharacters(text: String): String {
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

    private fun getLuckysheetCellType(cell: Cell): LuckysheetCellType {
        val format = cell.cellStyle.dataFormatString ?: "General"

        return when {
            format.contains("%") -> LuckysheetCellType("0.00%", LuckysheetConstants.TYPE_NUMBER)
            format.contains("$") || format.contains("€") || format.contains("£") ->
                LuckysheetCellType("$#,##0.00", LuckysheetConstants.TYPE_NUMBER)
            format.contains("date", true) || format.contains("yyyy") || format.contains("mm") ->
                LuckysheetCellType("yyyy-mm-dd", LuckysheetConstants.TYPE_DATE)
            cell.cellType == CellType.NUMERIC ->
                LuckysheetCellType("0.00", LuckysheetConstants.TYPE_NUMBER)
            cell.cellType == CellType.BOOLEAN ->
                LuckysheetCellType("General", LuckysheetConstants.TYPE_BOOLEAN)
            else ->
                LuckysheetCellType("General", LuckysheetConstants.TYPE_GENERAL)
        }
    }

    private fun extractFontColor(font: XSSFFont?): String? {
        return try {
            if (font == null) return "#000000"
            try {
                val xssfColor = font.xssfColor
                if (xssfColor != null) {
                    return extractXSSFColorToHexSafe(xssfColor)
                }
            } catch (e: Exception) {
            }
            val colorIndex = try {
                font.color.toInt()
            } catch (e: Exception) {
                0
            }
            return getColorFromIndex(colorIndex) ?: "#000000"
        } catch (e: Exception) {
            "#000000"
        }
    }

    private fun extractBackgroundColor(cellStyle: XSSFCellStyle?): String? {
        return try {
            if (cellStyle == null) return null

            val fillPattern = cellStyle.fillPattern
            if (fillPattern == FillPatternType.SOLID_FOREGROUND) {


                try {
                    val xssfColor = cellStyle.fillForegroundColorColor as? XSSFColor
                    if (xssfColor != null) {
                        val colorHex = extractXSSFColorToHexSafe(xssfColor)

                        if (colorHex != "#FFFFFF") {
                            return colorHex
                        }
                    }
                } catch (e: Exception) {
                }
                val colorIndex = try {
                    cellStyle.fillForegroundColor.toInt()
                } catch (e: Exception) {
                    null
                }

                if (colorIndex != null) {
                    val indexColor = getColorFromIndex(colorIndex)
                    if (indexColor != null && indexColor != "#FFFFFF") {
                        return indexColor
                    }
                }
            }

            return null
        } catch (e: Exception) {
            return null
        }
    }


    private fun getColorFromIndex(colorIndex: Int): String? {
        return when (colorIndex) {
            0 -> "#000000"   // Auto/Black
            1 -> "#000000"   // Black
            2 -> "#FFFFFF"   // White
            3 -> "#FF0000"   // Red
            4 -> "#00FF00"   // Green
            5 -> "#0000FF"   // Blue
            6 -> "#FFFF00"   // Yellow
            7 -> "#FF00FF"   // Magenta
            8 -> "#000000"   // Cyan
            9 -> "#800000"   // Dark Red
            10 -> "#008000"  // Dark Green
            11 -> "#000080"  // Dark Blue
            12 -> "#808000"  // Olive
            13 -> "#800080"  // Purple
            14 -> "#008080"  // Teal
            15 -> "#C0C0C0"  // Silver
            16 -> "#808080"  // Gray
            17 -> "#9999FF"  // Light Blue
            18 -> "#993366"  // Dark Pink
            19 -> "#FFFFCC"  // Light Yellow
            20 -> "#CCFFFF"  // Light Cyan
            21 -> "#660066"  // Dark Purple
            22 -> "#FF8080"  // Light Red
            23 -> "#0066CC"  // Medium Blue
            24 -> "#CCCCFF"  // Very Light Blue
            25 -> "#000080"  // Navy Blue
            26 -> "#FF0000"  // Red Accent
            27 -> "#00B050"  // Green Accent
            28 -> "#0070C0"  // Blue Accent
            29 -> "#FFC000"  // Orange Accent
            30 -> "#7030A0"  // Purple Accent
            31 -> "#C5504B"  // Dark Red Accent
            32 -> "#4BACC6"  // Light Blue Accent
            33 -> "#9BBB59"  // Light Green Accent
            34 -> "#F79646"  // Orange
            35 -> "#8064A2"  // Lavender
            36 -> "#4F81BD"  // Steel Blue
            37 -> "#B2DF8A"  // Pale Green
            38 -> "#FFCCCC"  // Light Pink
            39 -> "#D9D9D9"  // Light Gray
            40 -> "#A6A6A6"  // Medium Gray
            41 -> "#FFFF99"  // Pale Yellow
            42 -> "#99CCFF"  // Sky Blue
            43 -> "#FF9999"  // Rose
            44 -> "#99FF99"  // Light Green
            45 -> "#FFCC99"  // Peach
            46 -> "#CC99FF"  // Light Purple
            47 -> "#FF6666"  // Salmon
            48 -> "#66CCFF"  // Light Sky Blue
            49 -> "#66FF66"  // Bright Green
            50 -> "#FFFF66"  // Bright Yellow
            64 -> "#000000"  // System foreground
            65 -> "#FFFFFF"  // System background
            else -> null
        }
    }

    private fun getHorizontalAlignment(alignment: HorizontalAlignment): Int? {
        return when (alignment) {
            HorizontalAlignment.LEFT -> LuckysheetConstants.ALIGN_LEFT
            HorizontalAlignment.CENTER -> LuckysheetConstants.ALIGN_CENTER
            HorizontalAlignment.RIGHT -> LuckysheetConstants.ALIGN_RIGHT
            else -> null
        }
    }

    private fun getVerticalAlignment(alignment: VerticalAlignment): Int? {
        return when (alignment) {
            VerticalAlignment.TOP -> LuckysheetConstants.ALIGN_TOP
            VerticalAlignment.CENTER -> LuckysheetConstants.ALIGN_MIDDLE
            VerticalAlignment.BOTTOM -> LuckysheetConstants.ALIGN_BOTTOM
            else -> null
        }
    }

}