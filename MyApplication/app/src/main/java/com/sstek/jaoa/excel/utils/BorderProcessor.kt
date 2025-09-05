package com.sstek.jaoa.excel.utils

import android.util.Log
import com.google.gson.Gson
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class BorderProcessor {

    private val gson = Gson()

    companion object {
        private const val TAG = "BorderProcessor"
    }

    // LuckysheetToExcelConverter

    fun processBorders(sheet: XSSFSheet, borderInfo: List<Any>?, workbook: XSSFWorkbook) {
        if (borderInfo.isNullOrEmpty()) {
            Log.d(TAG, "No border info to process")
            return
        }

        Log.d(TAG, "Processing ${borderInfo.size} border entries...")

        borderInfo.forEach { borderItem ->
            try {
                val borderJson = gson.toJsonTree(borderItem).asJsonObject

                val borderType = borderJson.get("borderType")?.asString ?: "border-all"
                val color = borderJson.get("color")?.asString ?: "#000000"
                val style = borderJson.get("style")?.asInt ?: 1

                Log.d(TAG, "Processing border: type=$borderType, color=$color, style=$style (int)")

                val rangeArray = borderJson.get("range")?.asJsonArray
                rangeArray?.forEach { rangeElement ->
                    val rangeObj = rangeElement.asJsonObject
                    val rowArray = rangeObj.get("row")?.asJsonArray
                    val columnArray = rangeObj.get("column")?.asJsonArray
                        ?: rangeObj.get("col")?.asJsonArray

                    if (rowArray != null && columnArray != null) {
                        val startRow = rowArray[0].asInt
                        val endRow = rowArray[1].asInt
                        val startCol = columnArray[0].asInt
                        val endCol = columnArray[1].asInt

                        Log.d(TAG, "Applying range border to ($startRow,$startCol) to ($endRow,$endCol)")

                        when (borderType) {
                            "border-all" -> {
                                for (r in startRow..endRow) {
                                    for (c in startCol..endCol) {
                                        applyCellBorder(sheet, r, c, workbook, color, style)
                                    }
                                }
                            }
                            "border-none" -> {
                                for (r in startRow..endRow) {
                                    for (c in startCol..endCol) {
                                        removeCellBorder(sheet, r, c, workbook)
                                    }
                                }
                            }
                            "border-top" -> {
                                for (c in startCol..endCol) {
                                    applySpecificBorderSide(sheet, startRow, c, workbook, color, style, "top")
                                }
                            }
                            "border-bottom" -> {
                                for (c in startCol..endCol) {
                                    applySpecificBorderSide(sheet, endRow, c, workbook, color, style, "bottom")
                                }
                            }
                            "border-left" -> {
                                for (r in startRow..endRow) {
                                    applySpecificBorderSide(sheet, r, startCol, workbook, color, style, "left")
                                }
                            }
                            "border-right" -> {
                                for (r in startRow..endRow) {
                                    applySpecificBorderSide(sheet, r, endCol, workbook, color, style, "right")
                                }
                            }
                            "border-outside" -> {
                                applyOutsideBorder(sheet, startRow, endRow, startCol, endCol, workbook, color, style)
                            }
                            "border-inside" -> {
                                applyInsideBorder(sheet, startRow, endRow, startCol, endCol, workbook, color, style)
                            }
                            "border-horizontal" -> {
                                for (r in (startRow + 1)..endRow) {
                                    for (c in startCol..endCol) {
                                        applySpecificBorderSide(sheet, r, c, workbook, color, style, "top")
                                    }
                                }
                            }
                            "border-vertical" -> {
                                for (c in (startCol + 1)..endCol) {
                                    for (r in startRow..endRow) {
                                        applySpecificBorderSide(sheet, r, c, workbook, color, style, "left")
                                    }
                                }
                            }
                            else -> {
                                Log.w(TAG, "Unknown border type: $borderType")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing border item: ${e.message}")
            }
        }
    }

    private fun applySpecificBorderSide(sheet: XSSFSheet, row: Int, col: Int, workbook: XSSFWorkbook,
                                        color: String, style: Int, borderSide: String) {
        val excelRow = sheet.getRow(row) ?: sheet.createRow(row)
        val cell = excelRow.getCell(col) ?: excelRow.createCell(col)

        val cellStyle = workbook.createCellStyle()
        cellStyle.cloneStyleFrom(cell.cellStyle)

        val borderStyle = when (style) {
            0 -> BorderStyle.NONE
            1 -> BorderStyle.THIN
            2 -> BorderStyle.HAIR
            3 -> BorderStyle.DOTTED
            4 -> BorderStyle.DASHED
            5 -> BorderStyle.DASH_DOT
            6 -> BorderStyle.DASH_DOT_DOT
            7 -> BorderStyle.DOUBLE
            8 -> BorderStyle.MEDIUM
            9 -> BorderStyle.MEDIUM_DASHED
            10 -> BorderStyle.MEDIUM_DASH_DOT
            11 -> BorderStyle.MEDIUM_DASH_DOT_DOT
            12 -> BorderStyle.SLANTED_DASH_DOT
            13 -> BorderStyle.THICK
            else -> {
                Log.w(TAG, "Unknown Luckysheet border style: $style for $borderSide, using THIN")
                BorderStyle.THIN
            }
        }

        Log.d(TAG, "✅ Applying $borderSide border - Luckysheet style $style → POI $borderStyle")

        when (borderSide) {
            "top" -> cellStyle.borderTop = borderStyle
            "bottom" -> cellStyle.borderBottom = borderStyle
            "left" -> cellStyle.borderLeft = borderStyle
            "right" -> cellStyle.borderRight = borderStyle
        }

        // ✅ Border color using utils
        BackgroundAndBorderColorUtils.applyBorderColor(cellStyle, color, borderSide)

        cell.cellStyle = cellStyle
    }

    private fun removeCellBorder(sheet: XSSFSheet, row: Int, col: Int, workbook: XSSFWorkbook) {
        val excelRow = sheet.getRow(row) ?: sheet.createRow(row)
        val cell = excelRow.getCell(col) ?: excelRow.createCell(col)

        val cellStyle = workbook.createCellStyle()
        cellStyle.cloneStyleFrom(cell.cellStyle)

        cellStyle.borderLeft = BorderStyle.NONE
        cellStyle.borderRight = BorderStyle.NONE
        cellStyle.borderTop = BorderStyle.NONE
        cellStyle.borderBottom = BorderStyle.NONE

        cell.cellStyle = cellStyle
    }

    private fun applyOutsideBorder(sheet: XSSFSheet, startRow: Int, endRow: Int, startCol: Int, endCol: Int,
                                   workbook: XSSFWorkbook, color: String, style: Int) {
        // Üst border
        for (c in startCol..endCol) {
            applySpecificBorderSide(sheet, startRow, c, workbook, color, style, "top")
        }
        // Alt border
        for (c in startCol..endCol) {
            applySpecificBorderSide(sheet, endRow, c, workbook, color, style, "bottom")
        }
        // Sol border
        for (r in startRow..endRow) {
            applySpecificBorderSide(sheet, r, startCol, workbook, color, style, "left")
        }
        // Sağ border
        for (r in startRow..endRow) {
            applySpecificBorderSide(sheet, r, endCol, workbook, color, style, "right")
        }
    }

    private fun applyInsideBorder(sheet: XSSFSheet, startRow: Int, endRow: Int, startCol: Int, endCol: Int,
                                  workbook: XSSFWorkbook, color: String, style: Int) {
        for (r in (startRow + 1)..endRow) {
            for (c in startCol..endCol) {
                applySpecificBorderSide(sheet, r, c, workbook, color, style, "top")
            }
        }

        for (c in (startCol + 1)..endCol) {
            for (r in startRow..endRow) {
                applySpecificBorderSide(sheet, r, c, workbook, color, style, "left")
            }
        }
    }

    private fun applyCellBorder(sheet: XSSFSheet, row: Int, col: Int, workbook: XSSFWorkbook, color: String, style: Int) {
        val excelRow = sheet.getRow(row) ?: sheet.createRow(row)
        val cell = excelRow.getCell(col) ?: excelRow.createCell(col)

        val cellStyle = workbook.createCellStyle()
        cellStyle.cloneStyleFrom(cell.cellStyle)

        val borderStyle = when (style) {
            0 -> BorderStyle.NONE                    // None
            1 -> BorderStyle.THIN                    // Thin (default)
            2 -> BorderStyle.HAIR                    // Hair (very thin)
            3 -> BorderStyle.DOTTED                  // Dotted
            4 -> BorderStyle.DASHED                  // Dashed
            5 -> BorderStyle.DASH_DOT                // DashDot
            6 -> BorderStyle.DASH_DOT_DOT           // DashDotDot
            7 -> BorderStyle.DOUBLE                  // Double
            8 -> BorderStyle.MEDIUM                  // Medium
            9 -> BorderStyle.MEDIUM_DASHED          // MediumDashed
            10 -> BorderStyle.MEDIUM_DASH_DOT       // MediumDashDot
            11 -> BorderStyle.MEDIUM_DASH_DOT_DOT   // MediumDashDotDot
            12 -> BorderStyle.SLANTED_DASH_DOT      // SlantedDashDot
            13 -> BorderStyle.THICK                 // Thick
            else -> {
                Log.w(TAG, "Unknown Luckysheet border style: $style, using THIN as fallback")
                BorderStyle.THIN
            }
        }

        Log.d(TAG, "✅ Luckysheet style $style → POI $borderStyle for cell ($row,$col)")

        if (borderStyle == BorderStyle.NONE) {
            cellStyle.borderLeft = BorderStyle.NONE
            cellStyle.borderRight = BorderStyle.NONE
            cellStyle.borderTop = BorderStyle.NONE
            cellStyle.borderBottom = BorderStyle.NONE
        } else {
            cellStyle.borderLeft = borderStyle
            cellStyle.borderRight = borderStyle
            cellStyle.borderTop = borderStyle
            cellStyle.borderBottom = borderStyle

            // ✅ Border color using utils
            BackgroundAndBorderColorUtils.applyBorderColor(cellStyle, color, "all")
        }

        cell.cellStyle = cellStyle
    }

    // ExcelToLuckysheetConverter

    fun createBorderInfo(sheet: XSSFSheet, maxRow: Int, maxCol: Int): List<Map<String, Any>> {
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
}