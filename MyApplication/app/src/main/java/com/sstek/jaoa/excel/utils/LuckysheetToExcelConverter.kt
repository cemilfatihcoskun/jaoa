package com.sstek.jaoa.excel.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.*
import java.text.SimpleDateFormat
import java.util.*

class LuckysheetToExcelConverter {

    private val gson = Gson()

    companion object {
        private const val TAG = "LuckysheetToExcel"
    }

    fun convert(jsonData: String): XSSFWorkbook {
        Log.d(TAG, "Converting Luckysheet JSON to Excel workbook")
        val workbook = XSSFWorkbook()
        try {
            val type = object : TypeToken<List<LuckysheetSheet>>() {}.type
            val sheets: List<LuckysheetSheet> = gson.fromJson(jsonData, type)

            sheets.forEach { sheetData ->
                convertSheet(workbook, sheetData)
                Log.d(TAG, "Converted sheet: ${sheetData.name}")
            }

            Log.d(TAG, "Conversion completed. Created ${workbook.numberOfSheets} sheets")

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Luckysheet JSON", e)
            // Create a default sheet if parsing fails
            workbook.createSheet("Sheet1")
        }
        return workbook
    }

    private fun convertSheet(workbook: XSSFWorkbook, sheetData: LuckysheetSheet) {
        val sheet = workbook.createSheet(sheetData.name)
        val styleCache = mutableMapOf<String, XSSFCellStyle>()
        Log.d(TAG, "Processing ${sheetData.celldata?.size} cells")
        sheetData.celldata?.forEach { cellInfo ->
            convertCell(sheet, cellInfo, styleCache)
        }
        val borderProcessor = BorderProcessor()
        borderProcessor.processBorders(sheet, sheetData.config?.borderInfo, workbook)

        val mergeProcessor = MergeProcessor()
        mergeProcessor.applyMergedRanges(sheet, sheetData.config?.merge)

        val dimensionProcessor = DimensionProcessor()
        dimensionProcessor.applyDimensions(sheet, sheetData.config?.columnlen, sheetData.config?.rowlen)
    }


    private fun convertCell(
        sheet: XSSFSheet,
        cellInfo: LuckysheetCell,
        styleCache: MutableMap<String, XSSFCellStyle>
    ) {
        Log.d(TAG, "Converting cell (${cellInfo.r},${cellInfo.c}): fc=${cellInfo.v.fc}, bg=${cellInfo.v.bg}")
        try {
            val row = sheet.getRow(cellInfo.r) ?: sheet.createRow(cellInfo.r)
            val cell = row.createCell(cellInfo.c)
            setCellValue(cell, cellInfo.v)
            val style = createCellStyle(sheet.workbook, cellInfo.v, styleCache)
            if (style != null) {
                cell.cellStyle = style
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting cell at ${cellInfo.r},${cellInfo.c}", e)
        }
    }

    private fun setCellValue(cell: XSSFCell, cellValue: LuckysheetCellValue) {

        if (!cellValue.f.isNullOrBlank()) {
            val formulaProcessor = FormulaProcessor()
            try {
                formulaProcessor.setFormulaToCell(cell, cellValue.f, cellValue.v)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Formula setting failed, falling back to regular value")
            }
        }

        when (val value = cellValue.v) {
            is String -> {
                if (value.isNotBlank()) {

                    if (ConversionUtils.isDateString(value)) {
                        try {
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(value)
                            date?.let { validDate ->
                                cell.setCellValue(validDate)
                                return
                            }
                        } catch (e: Exception) {
                            // Not a valid date, treat as string
                        }
                    }
                    cell.setCellValue(value)
                }
            }
            is Number -> {
                cell.setCellValue(value.toDouble())
            }
            is Boolean -> {
                cell.setCellValue(value)
            }
            else -> {
                val displayValue = cellValue.m
                if (!displayValue.isNullOrBlank()) {
                    cell.setCellValue(displayValue)
                }
            }
        }
    }

    private fun createCellStyle(
        workbook: XSSFWorkbook,
        cellValue: LuckysheetCellValue,
        styleCache: MutableMap<String, XSSFCellStyle>
    ): XSSFCellStyle? {
        Log.d(TAG, "Creating style: fc=${cellValue.fc}, bg=${cellValue.bg}")
        val styleKey = createStyleKey(cellValue)
        styleCache[styleKey]?.let { return it }
        if (!hasStyleProperties(cellValue)) {
            Log.d(TAG, "!hasStyleProperties(cellValue) null")
            return null
        }
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        cellValue.ff?.let { fontFamily ->
            if (fontFamily.isNotBlank()) {
                font.fontName = fontFamily
            }
        }
        cellValue.fs?.let { fontSize ->
            val validatedSize = when {
                fontSize <= 0 -> 11
                fontSize > 72 -> 72
                fontSize < 6 -> 6
                else -> fontSize
            }

            Log.d(TAG, "Setting font size: original=$fontSize, validated=$validatedSize")
            font.fontHeightInPoints = validatedSize.toShort()
        }
        Log.d(TAG, "About to apply font color: ${cellValue.fc}")
        // ✅ Font color using utils
        FontColorUtils.applyFontColorSimple(font, cellValue.fc)
        Log.d(TAG, "Font color apply completed")
        // ✅ Font styles
        if (cellValue.bl == 1) font.bold = true
        if (cellValue.it == 1) font.italic = true
        if (cellValue.cl == 1) font.underline = Font.U_SINGLE

        style.setFont(font)

        // ✅ Background color using utils
        BackgroundAndBorderColorUtils.applyBackgroundColor(style, cellValue.bg)

        // ✅ Alignment
        cellValue.ht?.let { horizontalAlign ->
            style.alignment = when (horizontalAlign) {
                LuckysheetConstants.ALIGN_LEFT -> HorizontalAlignment.LEFT
                LuckysheetConstants.ALIGN_CENTER -> HorizontalAlignment.CENTER
                LuckysheetConstants.ALIGN_RIGHT -> HorizontalAlignment.RIGHT
                else -> HorizontalAlignment.LEFT
            }
        }

        cellValue.vt?.let { verticalAlign ->
            style.verticalAlignment = when (verticalAlign) {
                LuckysheetConstants.ALIGN_TOP -> VerticalAlignment.TOP
                LuckysheetConstants.ALIGN_MIDDLE -> VerticalAlignment.CENTER
                LuckysheetConstants.ALIGN_BOTTOM -> VerticalAlignment.BOTTOM
                else -> VerticalAlignment.CENTER
            }
        }

        Log.d(TAG, "Processing cell tb value: ${cellValue.tb}")
        cellValue.tb?.let { textBreak ->
            Log.d(TAG, "Applying text break: $textBreak")
            applyTextBreak(style, textBreak)
        }

        //  Number format
        cellValue.ct?.let { cellType ->
            when (cellType.fa) {
                "0.00%" -> {
                    val format = workbook.createDataFormat()
                    style.dataFormat = format.getFormat("0.00%")
                }
                "$#,##0.00" -> {
                    val format = workbook.createDataFormat()
                    style.dataFormat = format.getFormat("$#,##0.00")
                }
                "yyyy-mm-dd" -> {
                    val format = workbook.createDataFormat()
                    style.dataFormat = format.getFormat("yyyy-mm-dd")
                }
                "0.00" -> {
                    val format = workbook.createDataFormat()
                    style.dataFormat = format.getFormat("0.00")
                }
            }
        }

        styleCache[styleKey] = style
        return style
    }

    private fun createStyleKey(cellValue: LuckysheetCellValue): String {
        val key = listOf(
            cellValue.ff ?: "null",
            cellValue.fs?.toString() ?: "null",
            cellValue.fc ?: "null",
            cellValue.bg ?: "null",
            cellValue.bl?.toString() ?: "null",
            cellValue.it?.toString() ?: "null",
            cellValue.cl?.toString() ?: "null",
            cellValue.ht?.toString() ?: "null",
            cellValue.vt?.toString() ?: "null",
            cellValue.ct?.fa ?: "null",
            cellValue.tb ?: "null"
        ).joinToString("|")

        Log.d(TAG, "Style key for fc=${cellValue.fc}, bg=${cellValue.bg}: $key")
        return key
    }

    private fun hasStyleProperties(cellValue: LuckysheetCellValue): Boolean {
        return cellValue.ff != null ||
                cellValue.fs != null ||
                cellValue.fc != null ||
                cellValue.bg != null ||
                cellValue.bl == 1 ||
                cellValue.it == 1 ||
                cellValue.cl == 1 ||
                cellValue.ht != null ||
                cellValue.vt != null ||
                cellValue.ct?.fa != "General"||
                cellValue.tb != null
    }

    private fun applyTextBreak(cellStyle: XSSFCellStyle, tbValue: String?) {
        when (tbValue) {
            "2" -> {
                cellStyle.wrapText = true
                cellStyle.shrinkToFit = false
                Log.d(TAG, "Applied text wrap")
            }
            "0", null  -> {
                cellStyle.wrapText = false
                cellStyle.shrinkToFit = true
                Log.d(TAG, "Applied shrink to fit (clip)")
            }
            "1" -> {
                cellStyle.wrapText = false
                cellStyle.shrinkToFit = false
                Log.d(TAG, "Applied overflow")
            }
        }
    }

}