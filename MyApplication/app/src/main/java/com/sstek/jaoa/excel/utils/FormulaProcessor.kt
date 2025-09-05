package com.sstek.jaoa.excel.utils

import android.util.Log
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFSheet

class FormulaProcessor {

    companion object {
        private const val TAG = "FormulaProcessor"
    }

    // ExcelToLuckysheetConverter
    fun createCalculationChain(sheet: XSSFSheet, sheetIndex: Int): List<LuckysheetCalcChain> {
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

    // Formula extraction utilities
    fun extractFormula(cell: XSSFCell): String? {
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

    fun hasFormula(cell: XSSFCell): Boolean {
        return try {
            cell.cellType == CellType.FORMULA
        } catch (e: Exception) {
            false
        }
    }

    // Formula cached value extraction
    fun extractFormulaCachedValue(cell: Cell): Any? {
        return try {
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

    // LuckysheetToExcelConverter
    fun setFormulaToCell(cell: XSSFCell, formula: String, cachedValue: Any?) {
        try {
            val cleanFormula = formula.removePrefix("=")
            cell.cellFormula = cleanFormula

            // Set cached value if available
            when (cachedValue) {
                is Number -> {
                    cell.setCellValue(cachedValue.toDouble())
                    Log.d(TAG, "Set formula: $cleanFormula with cached numeric value: $cachedValue")
                }
                is String -> {
                    cell.setCellValue(cachedValue)
                    Log.d(TAG, "Set formula: $cleanFormula with cached string value: $cachedValue")
                }
                is Boolean -> {
                    cell.setCellValue(cachedValue)
                    Log.d(TAG, "Set formula: $cleanFormula with cached boolean value: $cachedValue")
                }
                else -> {
                    Log.d(TAG, "Set formula: $cleanFormula without cached value")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set formula: $formula", e)
            throw e // Re-throw so caller can handle fallback
        }
    }
}