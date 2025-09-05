package com.sstek.jaoa.excel.utils

import android.util.Log
import com.google.gson.Gson
import com.sstek.jaoa.excel.utils.LuckysheetConfig
import com.sstek.jaoa.excel.utils.LuckysheetMerge
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFSheet

class MergeProcessor {

    private val gson = Gson()

    companion object {
        private const val TAG = "MergeProcessor"
    }

    // ExcelToLuckysheetConverter
    fun extractMergedRanges(sheet: XSSFSheet): Map<String, Map<String, Int>>? {
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

        return if (mergeObject.isNotEmpty()) mergeObject else null
    }

    // LuckysheetToExcelConverter
    fun applyMergedRanges(sheet: XSSFSheet, mergeConfig: Any?) {
        if (mergeConfig == null) return

        try {
            val mergedRanges = parseMergeFromConfig(mergeConfig)
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
    }

    private fun parseMergeFromConfig(config: Any?): List<LuckysheetMerge> {
        if (config == null) return emptyList()

        val mergeList = mutableListOf<LuckysheetMerge>()

        try {
            val mergeJson = gson.toJsonTree(config)

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
}