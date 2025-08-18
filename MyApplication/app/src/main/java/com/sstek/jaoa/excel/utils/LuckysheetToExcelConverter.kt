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

        sheetData.celldata?.forEach { cellInfo ->
            convertCell(sheet, cellInfo, styleCache)
        }

        // ✅ Merge işlemi - hem object hem array formatını handle et
        try {
            val mergedRanges = parseMergeFromConfig(sheetData.config)
            mergedRanges.forEach { mergeInfo ->
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
        } catch (e: Exception) {
            Log.w(TAG, "Error processing merge data", e)
        }

        // Column widths
        sheetData.config?.columnlen?.forEach { (colIndex, width) ->
            try {
                val col = colIndex.toIntOrNull()
                if (col != null && width > 0) {
                    val excelWidth = (width * 256).toInt()
                    sheet.setColumnWidth(col, excelWidth)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set column width for column $colIndex", e)
            }
        }

        // Row heights
        sheetData.config?.rowlen?.forEach { (rowIndex, height) ->
            try {
                val rowNum = rowIndex.toIntOrNull()
                if (rowNum != null && height > 0) {
                    val row = sheet.getRow(rowNum) ?: sheet.createRow(rowNum)
                    val excelHeight = (height * 20).toInt().toShort()
                    row.height = excelHeight
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set row height for row $rowIndex", e)
            }
        }
    }

    private fun parseMergeFromConfig(config: LuckysheetConfig?): List<LuckysheetMerge> {
        if (config?.merge == null) return emptyList()

        val mergeList = mutableListOf<LuckysheetMerge>()

        try {
            val mergeJson = gson.toJsonTree(config.merge)

            when {
                mergeJson.isJsonArray -> {
                    // Array format: [{"r":0,"c":0,"rs":2,"cs":2}]
                    mergeJson.asJsonArray.forEach { element ->
                        val obj = element.asJsonObject
                        mergeList.add(LuckysheetMerge(
                            r = obj.get("r")?.asInt ?: 0,
                            c = obj.get("c")?.asInt ?: 0,
                            rs = obj.get("rs")?.asInt ?: 1,
                            cs = obj.get("cs")?.asInt ?: 1
                        ))
                    }
                }
                mergeJson.isJsonObject -> {
                    // Object format: {"0_0":{"r":0,"c":0,"rs":2,"cs":2}}
                    mergeJson.asJsonObject.entrySet().forEach { entry ->
                        val obj = entry.value.asJsonObject
                        mergeList.add(LuckysheetMerge(
                            r = obj.get("r")?.asInt ?: 0,
                            c = obj.get("c")?.asInt ?: 0,
                            rs = obj.get("rs")?.asInt ?: 1,
                            cs = obj.get("cs")?.asInt ?: 1
                        ))
                    }
                }
            }

            Log.d(TAG, "Parsed ${mergeList.size} merge ranges from ${if (mergeJson.isJsonArray) "array" else "object"} format")

        } catch (e: Exception) {
            Log.w(TAG, "Could not parse merge data: ${e.message}")
        }

        return mergeList
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


        when (val value = cellValue.v) {
            is String -> {
                if (value.isNotBlank()) {

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

        val styleKey = createStyleKey(cellValue)
        styleCache[styleKey]?.let { return it }
        if (!hasStyleProperties(cellValue)) {
            return null
        }

        val style = workbook.createCellStyle()
        val font = workbook.createFont()
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