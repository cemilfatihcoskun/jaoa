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

        // Process all rows and cells
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

        // Process merged regions
        val mergedRanges = mutableListOf<LuckysheetMerge>()
        for (mergedRegion in sheet.mergedRegions) {
            mergedRanges.add(
                LuckysheetMerge(
                    r = mergedRegion.firstRow,
                    c = mergedRegion.firstColumn,
                    rs = mergedRegion.lastRow - mergedRegion.firstRow + 1,
                    cs = mergedRegion.lastColumn - mergedRegion.firstColumn + 1
                )
            )
        }

        // Process column widths and row heights
        val columnWidths = mutableMapOf<String, Int>()
        val rowHeights = mutableMapOf<String, Int>()

        // Get custom column widths
        for (col in 0..maxCol) {
            val width = sheet.getColumnWidth(col)
            if (width != sheet.defaultColumnWidth) {
                columnWidths[col.toString()] = (width / 256.0 * 8).toInt()
            }
        }

        // Get custom row heights
        for (rowNum in 0..maxRow) {
            val row = sheet.getRow(rowNum)
            if (row != null && row.height != sheet.defaultRowHeight) {
                rowHeights[rowNum.toString()] = (row.height / 20).toInt()
            }
        }

        val config = LuckysheetConfig(
            merge = if (mergedRanges.isNotEmpty()) mergedRanges else null,
            columnlen = if (columnWidths.isNotEmpty()) columnWidths else null,
            rowlen = if (rowHeights.isNotEmpty()) rowHeights else null
        )

        return LuckysheetSheet(
            name = sheet.sheetName ?: "Sheet${sheetIndex + 1}",
            index = sheetIndex,
            celldata = if (cellData.isNotEmpty()) cellData else null,
            row = maxOf(maxRow + 1, LuckysheetConstants.DEFAULT_ROW_COUNT),
            column = maxOf(maxCol + 1, LuckysheetConstants.DEFAULT_COLUMN_COUNT),
            config = if (mergedRanges.isNotEmpty() || columnWidths.isNotEmpty() || rowHeights.isNotEmpty()) config else null
        )
    }

    private fun convertCell(cell: Cell): LuckysheetCell? {
        try {
            val xssfCell = cell as? XSSFCell ?: return null

            val cellValue = extractCellValue(xssfCell)
            val displayValue = extractDisplayValue(xssfCell)

            // Skip empty cells
            if (cellValue == null && !hasFormula(xssfCell)) {
                return null
            }

            val cellStyle = xssfCell.cellStyle
            val font = xssfCell.sheet.workbook.getFontAt(cellStyle.fontIndexAsInt)

            val luckysheetValue = LuckysheetCellValue(
                v = cellValue,
                m = displayValue,
                f = getFormulaSafely(xssfCell),
                ct = getLuckysheetCellType(xssfCell),

                // Font properties
                ff = font.fontName ?: "Arial",
                fs = font.fontHeightInPoints.toInt().takeIf { it > 0 } ?: 11,
                fc = extractFontColor(font as? XSSFFont),
                bl = if (font.bold) 1 else 0,
                it = if (font.italic) 1 else 0,
                cl = if (font.underline != Font.U_NONE) 1 else 0,

                // Cell properties
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
                    // Format date as string for Luckysheet
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cell.dateCellValue)
                } else {
                    val numValue = cell.numericCellValue
                    // Return integer if it's a whole number
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
            // Control karakterleri temizle
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
            val color = font?.xssfColor
            if (color != null && color.rgb != null) {
                "#" + color.rgb.joinToString("") {
                    String.format("%02x", it.toInt() and 0xFF)
                }.uppercase()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractBackgroundColor(cellStyle: XSSFCellStyle?): String? {
        return try {
            val fillPattern = cellStyle?.fillPattern
            if (fillPattern == FillPatternType.SOLID_FOREGROUND) {
                val color = cellStyle.fillForegroundColorColor as? XSSFColor
                if (color?.rgb != null) {
                    "#" + color.rgb.joinToString("") {
                        String.format("%02x", it.toInt() and 0xFF)
                    }.uppercase()
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
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