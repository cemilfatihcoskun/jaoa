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

        val mergeProcessor = MergeProcessor()
        val mergeObject = mergeProcessor.extractMergedRanges(sheet)

        val dimensionProcessor = DimensionProcessor()
        val dimensions = dimensionProcessor.extractDimensions(sheet, maxRow, maxCol)

        val borderProcessor = BorderProcessor()
        val rangeBorderInfo = borderProcessor.createBorderInfo(sheet, maxRow, maxCol)

        val config = LuckysheetConfig(
            merge = mergeObject,
            columnlen = dimensions.columnWidths,
            rowlen = dimensions.rowHeights,
            borderInfo = if (rangeBorderInfo.isNotEmpty()) rangeBorderInfo else null
        )
        val formulaProcessor = FormulaProcessor()
        val calcChain = formulaProcessor.createCalculationChain(sheet, sheetIndex)
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

    private fun convertCell(cell: Cell): LuckysheetCell? {
        try {
            val formulaProcessor = FormulaProcessor()
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
            if (cellValue == null && !formulaProcessor.hasFormula(xssfCell) && !hasBackground && !hasBorder) {
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
                f = formulaProcessor.extractFormula(xssfCell),
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
                val formulaProcessor = FormulaProcessor()
                formulaProcessor.extractFormulaCachedValue(cell)
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
            displayValue.takeIf { it.isNotBlank() }?.let { ConversionUtils.cleanControlCharacters(it) }
        } catch (e: Exception) {
            extractCellValue(cell)?.toString()?.let { ConversionUtils.cleanControlCharacters(it) }
        }
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

    private fun getTextBreakValue(cellStyle: CellStyle): String? {
        return when {
            cellStyle.wrapText -> "2"           // wrap
            cellStyle.shrinkToFit -> "0"      // clip (shrink to fit)
            else -> "0"                        // overflow (default)
        }
    }
}