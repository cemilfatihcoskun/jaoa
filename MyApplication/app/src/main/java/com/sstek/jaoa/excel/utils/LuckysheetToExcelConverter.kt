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

        // Create cell styles cache to avoid creating duplicates
        val styleCache = mutableMapOf<String, XSSFCellStyle>()

        // Process cell data
        sheetData.celldata?.forEach { cellInfo ->
            convertCell(sheet, cellInfo, styleCache)
        }

        // Process merged regions
        sheetData.config?.merge?.forEach { mergeInfo ->
            try {
                val cellRangeAddress = CellRangeAddress(
                    mergeInfo.r,
                    mergeInfo.r + mergeInfo.rs - 1,
                    mergeInfo.c,
                    mergeInfo.c + mergeInfo.cs - 1
                )
                sheet.addMergedRegion(cellRangeAddress)
            } catch (e: Exception) {
                Log.w(TAG, "Could not add merged region: $mergeInfo", e)
            }
        }

        // Set column widths
        sheetData.config?.columnlen?.forEach { (colIndex, width) ->
            try {
                val col = colIndex.toIntOrNull()
                if (col != null && width > 0) {
                    sheet.setColumnWidth(col, width * 256)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set column width for column $colIndex", e)
            }
        }

        // Set row heights
        sheetData.config?.rowlen?.forEach { (rowIndex, height) ->
            try {
                val rowNum = rowIndex.toIntOrNull()
                if (rowNum != null && height > 0) {
                    val row = sheet.getRow(rowNum) ?: sheet.createRow(rowNum)
                    row.height = (height * 20).toShort()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set row height for row $rowIndex", e)
            }
        }
    }

    private fun convertCell(
        sheet: XSSFSheet,
        cellInfo: LuckysheetCell,
        styleCache: MutableMap<String, XSSFCellStyle>
    ) {
        try {
            val row = sheet.getRow(cellInfo.r) ?: sheet.createRow(cellInfo.r)
            val cell = row.createCell(cellInfo.c)

            // Set cell value
            setCellValue(cell, cellInfo.v)

            // Set cell style
            val style = createCellStyle(sheet.workbook, cellInfo.v, styleCache)
            if (style != null) {
                cell.cellStyle = style
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error converting cell at ${cellInfo.r},${cellInfo.c}", e)
        }
    }

    private fun setCellValue(cell: XSSFCell, cellValue: LuckysheetCellValue) {
        // First check if there's a formula
        if (!cellValue.f.isNullOrBlank()) {
            try {
                cell.cellFormula = cellValue.f
                return
            } catch (e: Exception) {
                Log.w(TAG, "Could not set formula: ${cellValue.f}", e)
                // Fall back to setting the value directly
            }
        }

        // Set the raw value
        when (val value = cellValue.v) {
            is String -> {
                if (value.isNotBlank()) {
                    // Check if it's a date string
                    if (isDateString(value)) {
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
                // For null or other types, use the display value
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
        // Create a style key based on the cell properties
        val styleKey = createStyleKey(cellValue)

        // Return cached style if exists
        styleCache[styleKey]?.let { return it }

        // Check if any styling is needed
        if (!hasStyleProperties(cellValue)) {
            return null
        }

        val style = workbook.createCellStyle()
        val font = workbook.createFont()

        // Font properties
        cellValue.ff?.let { font.fontName = it }
        cellValue.fs?.let { fontSize ->
            if (fontSize > 0) {
                font.fontHeightInPoints = fontSize.toShort()
            }
        }
        cellValue.fc?.let { color ->
            try {
                val xssfColor = parseColor(color)
                xssfColor?.let { validColor ->
                    font.setColor(validColor)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not parse font color: $color", e)
            }
        }

        // Font styling
        if (cellValue.bl == 1) font.bold = true
        if (cellValue.it == 1) font.italic = true
        if (cellValue.cl == 1) font.underline = Font.U_SINGLE

        style.setFont(font)

        // Background color
        cellValue.bg?.let { backgroundColor ->
            try {
                val xssfColor = parseColor(backgroundColor)
                xssfColor?.let { validColor ->
                    style.setFillForegroundColor(validColor)
                    style.fillPattern = FillPatternType.SOLID_FOREGROUND
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not parse background color: $backgroundColor", e)
            }
        }

        // Alignment
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

        // Number format
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
        return listOfNotNull(
            cellValue.ff,
            cellValue.fs?.toString(),
            cellValue.fc,
            cellValue.bg,
            cellValue.bl?.toString(),
            cellValue.it?.toString(),
            cellValue.cl?.toString(),
            cellValue.ht?.toString(),
            cellValue.vt?.toString(),
            cellValue.ct?.fa
        ).joinToString("|")
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
                cellValue.ct?.fa != "General"
    }

    private fun parseColor(colorString: String): XSSFColor? {
        return try {
            val cleanColor = colorString.removePrefix("#")
            if (cleanColor.length == 6) {
                val r = cleanColor.substring(0, 2).toInt(16).toByte()
                val g = cleanColor.substring(2, 4).toInt(16).toByte()
                val b = cleanColor.substring(4, 6).toInt(16).toByte()
                XSSFColor(byteArrayOf(r, g, b), null)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse color: $colorString", e)
            null
        }
    }

    private fun isDateString(value: String): Boolean {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(value) != null
        } catch (e: Exception) {
            false
        }
    }
}