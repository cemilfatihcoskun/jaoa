package com.sstek.jaoa.excel.utils
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.apache.poi.ss.usermodel.*
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
            val widthUnits = sheet.getColumnWidth(col)
            val defaultUnits = (sheet.defaultColumnWidth * 256).toInt()

            Log.d(TAG, "Column $col: widthUnits=$widthUnits, defaultUnits=$defaultUnits")

            if (widthUnits != defaultUnits) {
                val pixels = excelColumnWidthToPx(widthUnits)
                columnWidths[col.toString()] = pixels.toDouble()
                Log.d(TAG, "Column $col: ${widthUnits} units → ${pixels} px")
            } else {
                Log.d(TAG, "Column $col: Using default width, skipping")
            }
        }

        for (rowNum in 0..maxRow) {
            val row = sheet.getRow(rowNum) ?: continue
            val heightTwips = row.height
            val defaultTwips = sheet.defaultRowHeight

            if (heightTwips != defaultTwips) {
                val pixels = excelRowHeightToPx(heightTwips)
                rowHeights[rowNum.toString()] = pixels.toDouble()
                Log.d(TAG, "Row $rowNum: ${heightTwips} twips → ${pixels} px")
            }
        }

        val rangeBorderInfo = createBorderInfo(sheet, maxRow, maxCol)

        val config = LuckysheetConfig(
            merge = if (mergeObject.isNotEmpty()) mergeObject else null,
            columnlen = if (columnWidths.isNotEmpty()) columnWidths else null,
            rowlen = if (rowHeights.isNotEmpty()) rowHeights else null,
            borderInfo = if (rangeBorderInfo.isNotEmpty()) rangeBorderInfo else null
        )
        val calcChain = createCalculationChain(sheet, sheetIndex)
        return LuckysheetSheet(
            name = sheet.sheetName ?: "Sheet${sheetIndex + 1}",
            index = sheetIndex,
            celldata = if (cellData.isNotEmpty()) cellData else null,
            row = maxOf(maxRow + 1, LuckysheetConstants.DEFAULT_ROW_COUNT),
            column = maxOf(maxCol + 1, LuckysheetConstants.DEFAULT_COLUMN_COUNT),
            config = config,
            calcChain = if (calcChain.isNotEmpty()) calcChain else null
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
                                "color" to (BackgroundAndBorderColorUtils.extractBorderColor(style, "top")),
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

            val topColor = BackgroundAndBorderColorUtils.extractBorderColor(style, "top")
            val bottomColor = BackgroundAndBorderColorUtils.extractBorderColor(style, "bottom")
            val leftColor = BackgroundAndBorderColorUtils.extractBorderColor(style, "left")
            val rightColor = BackgroundAndBorderColorUtils.extractBorderColor(style, "right")

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
                "color" to (BackgroundAndBorderColorUtils.extractBorderColor(style, "top")),
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
                "color" to (BackgroundAndBorderColorUtils.extractBorderColor(style, "bottom")),
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
                "color" to (BackgroundAndBorderColorUtils.extractBorderColor(style, "left")),
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
                "color" to (BackgroundAndBorderColorUtils.extractBorderColor(style, "right")),
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
            val fontSize = try {
                val fontPoints = font.fontHeightInPoints.toInt()
                when {
                    fontPoints <= 0 -> 11
                    fontPoints > 72 -> 72
                    else -> fontPoints
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not get font size, using default: ${e.message}")
                11
            }

            val textBreak = getTextBreakValue(cellStyle)
            Log.d(TAG, "Cell (${cell.rowIndex},${cell.columnIndex}): text break = $textBreak " + "(wrapText=${cellStyle.wrapText}, shrinkToFit=${cellStyle.shrinkToFit})")

            val luckysheetValue = LuckysheetCellValue(
                v = cellValue,
                m = displayValue,
                f = getFormulaSafely(xssfCell),
                ct = getLuckysheetCellType(xssfCell),


                ff = font.fontName?.takeIf { it.isNotBlank() } ?: "Arial",
                fs = fontSize,
                fc = FontColorUtils.extractFontColorSimple(font),
                bl = if (font.bold) 1 else 0,
                it = if (font.italic) 1 else 0,
                cl = if (font.underline != Font.U_NONE) 1 else 0,

                bg = BackgroundAndBorderColorUtils.extractBackgroundColor(cellStyle as? XSSFCellStyle),
                ht = getHorizontalAlignment(cellStyle.alignment),
                vt = getVerticalAlignment(cellStyle.verticalAlignment),
                tb = getTextBreakValue(cellStyle)

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


    private fun extractBorderStyleForSide(cellStyle: CellStyle, side: String): Int {
        return try {
            val borderStyle = when (side) {
                "top" -> if (cellStyle.borderTop != BorderStyle.NONE) cellStyle.borderTop else null
                "bottom" -> if (cellStyle.borderBottom != BorderStyle.NONE) cellStyle.borderBottom else null
                "left" -> if (cellStyle.borderLeft != BorderStyle.NONE) cellStyle.borderLeft else null
                "right" -> if (cellStyle.borderRight != BorderStyle.NONE) cellStyle.borderRight else null
                else -> null
            }

            borderStyle?.let { poiBorderStyle ->
                when (poiBorderStyle) {
                    BorderStyle.NONE -> 0                          // None
                    BorderStyle.THIN -> 1                          // Thin
                    BorderStyle.HAIR -> 2                          // Hair
                    BorderStyle.DOTTED -> 3                        // Dotted
                    BorderStyle.DASHED -> 4                        // Dashed
                    BorderStyle.DASH_DOT -> 5                      // DashDot
                    BorderStyle.DASH_DOT_DOT -> 6                  // DashDotDot
                    BorderStyle.DOUBLE -> 7                        // Double
                    BorderStyle.MEDIUM -> 8                        // Medium
                    BorderStyle.MEDIUM_DASHED -> 9                 // MediumDashed
                    BorderStyle.MEDIUM_DASH_DOT -> 10              // MediumDashDot
                    BorderStyle.MEDIUM_DASH_DOT_DOT -> 11          // MediumDashDotDot
                    BorderStyle.SLANTED_DASH_DOT -> 12             // SlantedDashDot
                    BorderStyle.THICK -> 13                        // Thick
                    else -> {
                        Log.d(TAG, "Unknown POI border style: $poiBorderStyle, using Thin (1)")
                        1
                    }
                }
            } ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting border style for $side: ${e.message}")
            0
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
                "=" + cell.cellFormula
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

            if (cell.cellType == CellType.FORMULA) {

                return when (cell.cachedFormulaResultType) {
                    CellType.NUMERIC -> {
                        val numValue = cell.numericCellValue
                        if (numValue == numValue.toLong().toDouble()) {
                            numValue.toLong().toString()
                        } else {
                            numValue.toString()
                        }
                    }
                    CellType.STRING -> cell.stringCellValue
                    CellType.BOOLEAN -> cell.booleanCellValue.toString()
                    else -> null
                }
            }

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

        if (cell.cellType == CellType.FORMULA) {
            return when (cell.cachedFormulaResultType) {
                CellType.NUMERIC -> LuckysheetCellType("General", LuckysheetConstants.TYPE_NUMBER) // "n"
                CellType.STRING -> LuckysheetCellType("General", LuckysheetConstants.TYPE_STRING)  // "s"
                CellType.BOOLEAN -> LuckysheetCellType("General", LuckysheetConstants.TYPE_BOOLEAN) // "b"
                else -> LuckysheetCellType("General", LuckysheetConstants.TYPE_NUMBER) // Default numeric
            }
        }
        val format = cell.cellStyle.dataFormatString ?: "General"
        return when {
            format.contains("%") -> LuckysheetCellType("0.00%", LuckysheetConstants.TYPE_NUMBER)
            format.contains("$") || format.contains("€") || format.contains("£") ->
                LuckysheetCellType("$#,##0.00", LuckysheetConstants.TYPE_NUMBER)
            format.contains("date", true) || format.contains("yyyy") || format.contains("mm") ->
                LuckysheetCellType("yyyy-mm-dd", LuckysheetConstants.TYPE_DATE)
            cell.cellType == CellType.NUMERIC -> {
                if (format == "General") {
                    LuckysheetCellType("General", LuckysheetConstants.TYPE_NUMBER)
                } else {
                    LuckysheetCellType(format, LuckysheetConstants.TYPE_NUMBER)
                }
            }
            cell.cellType == CellType.BOOLEAN ->
                LuckysheetCellType("General", LuckysheetConstants.TYPE_BOOLEAN)
            else ->
                LuckysheetCellType("General", LuckysheetConstants.TYPE_GENERAL)
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
    // Utility functions
    private fun excelColumnWidthToPx(widthUnits: Int): Int {
        val charCount = widthUnits / 256.0
        return kotlin.math.floor(charCount * 7 + 5).toInt()
    }

    private fun excelRowHeightToPx(heightTwips: Short): Int {
        return kotlin.math.round(heightTwips / 15.0).toInt()
    }

    private fun getTextBreakValue(cellStyle: CellStyle): String? {
        return when {
            cellStyle.wrapText -> "2"           // wrap
            cellStyle.shrinkToFit -> "0"      // clip (shrink to fit)
            else -> "0"                        // overflow (default)
        }
    }
    private fun createCalculationChain(sheet: XSSFSheet, sheetIndex: Int): List<LuckysheetCalcChain> {
        val calcChain = mutableListOf<LuckysheetCalcChain>()

        for (row in sheet) {
            for (cell in row) {
                if (cell.cellType == CellType.FORMULA) {
                    try {
                        val cachedValue = when (cell.cachedFormulaResultType) {
                            CellType.NUMERIC -> cell.numericCellValue
                            CellType.STRING -> cell.stringCellValue
                            CellType.BOOLEAN -> cell.booleanCellValue
                            else -> 0.0
                        }

                        calcChain.add(
                            LuckysheetCalcChain(
                                r = cell.rowIndex,
                                c = cell.columnIndex,
                                index = sheetIndex.toString(),
                                func = listOf(true, cachedValue, "=" + cell.cellFormula)
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not create calc chain for cell ${cell.rowIndex},${cell.columnIndex}")
                    }
                }
            }
        }

        return calcChain
    }
}