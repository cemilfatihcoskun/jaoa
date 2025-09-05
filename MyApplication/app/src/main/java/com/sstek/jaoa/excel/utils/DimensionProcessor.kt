package com.sstek.jaoa.excel.utils

import android.util.Log
import org.apache.poi.xssf.usermodel.XSSFSheet

class DimensionProcessor {

    companion object {
        private const val TAG = "DimensionProcessor"
    }

    data class SheetDimensions(
        val columnWidths: Map<String, Double>?,
        val rowHeights: Map<String, Double>?
    )

    // ExcelToLuckysheetConverter
    fun extractDimensions(sheet: XSSFSheet, maxRow: Int, maxCol: Int): SheetDimensions {
        val columnWidths = extractColumnWidths(sheet, maxCol)
        val rowHeights = extractRowHeights(sheet, maxRow)

        return SheetDimensions(
            columnWidths = if (columnWidths.isNotEmpty()) columnWidths else null,
            rowHeights = if (rowHeights.isNotEmpty()) rowHeights else null
        )
    }

    private fun extractColumnWidths(sheet: XSSFSheet, maxCol: Int): Map<String, Double> {
        val columnWidths = mutableMapOf<String, Double>()

        for (col in 0..maxCol) {
            val widthUnits = sheet.getColumnWidth(col)
            val defaultUnits = (sheet.defaultColumnWidth * 256).toInt()

            Log.d(TAG, "Column $col: widthUnits=$widthUnits, defaultUnits=$defaultUnits")

            if (widthUnits != defaultUnits) {
                val pixels = ConversionUtils.excelColumnWidthToPx(widthUnits)
                columnWidths[col.toString()] = pixels.toDouble()
                Log.d(TAG, "Column $col: ${widthUnits} units → ${pixels} px")
            } else {
                Log.d(TAG, "Column $col: Using default width, skipping")
            }
        }

        return columnWidths
    }

    private fun extractRowHeights(sheet: XSSFSheet, maxRow: Int): Map<String, Double> {
        val rowHeights = mutableMapOf<String, Double>()

        for (rowNum in 0..maxRow) {
            val row = sheet.getRow(rowNum) ?: continue
            val heightTwips = row.height
            val defaultTwips = sheet.defaultRowHeight

            if (heightTwips != defaultTwips) {
                val pixels = ConversionUtils.excelRowHeightToPx(heightTwips)
                rowHeights[rowNum.toString()] = pixels.toDouble()
                Log.d(TAG, "Row $rowNum: ${heightTwips} twips → ${pixels} px")
            }
        }

        return rowHeights
    }

    // LuckysheetToExcelConverter
    fun applyDimensions(sheet: XSSFSheet, columnWidths: Map<String, Double>?, rowHeights: Map<String, Double>?) {
        // Column widths
        columnWidths?.forEach { (colIndex, width) ->
            try {
                val col = colIndex.toIntOrNull()
                if (col != null && width > 0) {
                    val excelWidth = ConversionUtils.pxToExcelColumnWidthUnits(width)
                    sheet.setColumnWidth(col, excelWidth)
                    Log.d(TAG, "Column $col: ${width} px → ${excelWidth} units")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set column width for column $colIndex: ${e.message}")
            }
        }

        // Row heights
        rowHeights?.forEach { (rowIndex, height) ->
            try {
                val rowNum = rowIndex.toIntOrNull()
                if (rowNum != null && height > 0) {
                    val row = sheet.getRow(rowNum) ?: sheet.createRow(rowNum)
                    val excelHeight = ConversionUtils.pxToTwips(height)
                    row.height = excelHeight
                    Log.d(TAG, "Row $rowNum: ${height} px → ${excelHeight} twips")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set row height for row $rowIndex: ${e.message}")
            }
        }
    }
}