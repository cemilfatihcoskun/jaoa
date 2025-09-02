package com.sstek.jaoa.excel.utils

data class LuckysheetWorkbook(
    val sheets: List<LuckysheetSheet>,
    val title: String = "Excel Workbook"
)

data class LuckysheetSheet(
    val name: String,
    val index: Int,
    val celldata: List<LuckysheetCell>? = null,
    val row: Int = 100,
    val column: Int = 26,
    val config: LuckysheetConfig? = null,
    val scrollLeft: Double = 0.0,
    val scrollTop: Double = 0.0,
    val selection: List<LuckysheetSelection>? = null,
    val luckysheet_select_save: List<LuckysheetSelection>? = null,
    val calcChain: List<LuckysheetCalcChain>? = null,
    val status: Int = 1,
    val order: Int = index,
    val hide: Int = 0,
    val zoomRatio: Double = 1.0
)

data class LuckysheetCell(
    val r: Int,  // row index
    val c: Int,  // column index
    val v: LuckysheetCellValue
)

data class LuckysheetCellValue(
    val v: Any?,                    // raw value (string, number, boolean)
    val ct: LuckysheetCellType? = null,    // cell type info
    val m: String? = null,          // display text
    val bg: String? = null,         // background color
    val ff: String? = null,         // font family
    val fc: String? = null,         // font color
    val fs: Int? = null,            // font size
    val bl: Int? = null,            // bold (0/1)
    val it: Int? = null,            // italic (0/1)
    val cl: Int? = null,            // underline (0/1)
    val un: Int? = null,            // strikethrough (0/1)
    val vt: Int? = null,            // vertical align (0=top, 1=middle, 2=bottom)
    val ht: Int? = null,            // horizontal align (0=left, 1=center, 2=right)
    val tb: String? = null,         // text break
    val f: String? = null,          // formula
    val spl: Any? = null,           // split info
    val rt: List<LuckysheetRichText>? = null,  // rich text
)

data class LuckysheetCellType(
    val fa: String = "General",     // format type
    val t: String = "g"             // data type (g=general, n=number, d=date, s=string)
)

data class LuckysheetRichText(
    val v: String,                  // text content
    val ff: String? = null,         // font family
    val fc: String? = null,         // font color
    val fs: Int? = null,            // font size
    val bl: Int? = null,            // bold
    val it: Int? = null,            // italic
    val cl: Int? = null,            // underline
    val un: Int? = null             // strikethrough
)

data class LuckysheetConfig(
    val merge: Any? = null,  // ✅ List yerine Any - hem object hem array kabul eder
    val borderInfo: List<Any>? = null,
    val rowlen: Map<String, Double>? = null,
    val columnlen: Map<String, Double>? = null,
    val rowhidden: Map<String, Int>? = null,
    val colhidden: Map<String, Int>? = null,
    val customHeight: Map<String, Double>? = null,
    val customWidth: Map<String, Double>? = null
)

data class LuckysheetMerge(
    val r: Int,    // start row
    val c: Int,    // start column
    val rs: Int,   // row span
    val cs: Int    // column span
)

data class LuckysheetSelection(
    val row: List<Int>,        // [startRow, endRow]
    val column: List<Int>      // [startCol, endCol]
)

data class LuckysheetBorder(
    val rangeType: String,     // "range" | "cell"
    val value: LuckysheetBorderValue
)

data class LuckysheetBorderValue(
    val style: String,         // border style
    val color: String,         // border color
    val range: List<LuckysheetBorderRange>
)

data class LuckysheetBorderRange(
    val row: List<Int>,
    val column: List<Int>
)

data class LuckysheetCalcChain(
    val r: Int,                // row
    val c: Int,                // column
    val index: String,         // sheet index
    val func: List<Any>,       // [true, cachedValue, "=FORMULA"]
    val color: String = "w",   // default color
    val parent: Any? = null,   // parent reference
    val chidren: Map<String, Any> = emptyMap(),
    val times: Int = 0         // calculation times
)


data class ExcelCellInfo(
    val row: Int,
    val column: Int,
    val value: Any?,
    val formula: String? = null,
    val cellType: String = "general",
    val format: String? = null,
    val style: ExcelCellStyle? = null
)

data class ExcelCellStyle(
    val fontFamily: String? = null,
    val fontSize: Int? = null,
    val fontColor: String? = null,
    val backgroundColor: String? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val horizontalAlignment: String? = null,
    val verticalAlignment: String? = null,
    val wrapText: Boolean = false
)

data class ExcelSheetInfo(
    val name: String,
    val index: Int,
    val cells: List<ExcelCellInfo> = emptyList(),
    val mergedRanges: List<ExcelMergedRange> = emptyList(),
    val columnWidths: Map<Int, Double> = emptyMap(),
    val rowHeights: Map<Int, Double> = emptyMap(),
    val hiddenRows: Set<Int> = emptySet(),
    val hiddenColumns: Set<Int> = emptySet()
)

data class ExcelMergedRange(
    val startRow: Int,
    val endRow: Int,
    val startColumn: Int,
    val endColumn: Int
)


object LuckysheetConstants {
    const val DEFAULT_ROW_COUNT = 100
    const val DEFAULT_COLUMN_COUNT = 26
    const val DEFAULT_ROW_HEIGHT = 19.0      // ✅ Double
    const val DEFAULT_COLUMN_WIDTH = 73.0    // ✅ Double


    const val ALIGN_LEFT = 1
    const val ALIGN_CENTER = 0
    const val ALIGN_RIGHT = 2
    const val ALIGN_TOP = 1
    const val ALIGN_MIDDLE = 0
    const val ALIGN_BOTTOM = 2


    const val TYPE_GENERAL = "g"
    const val TYPE_NUMBER = "n"
    const val TYPE_STRING = "s"
    const val TYPE_DATE = "d"
    const val TYPE_BOOLEAN = "b"

    const val FORMAT_GENERAL = "General"
    const val FORMAT_NUMBER = "0.00"
    const val FORMAT_PERCENTAGE = "0.00%"
    const val FORMAT_CURRENCY = "$#,##0.00"
    const val FORMAT_DATE = "yyyy-mm-dd"
    const val FORMAT_TIME = "hh:mm:ss"
    const val FORMAT_DATETIME = "yyyy-mm-dd hh:mm:ss"
}

fun LuckysheetSheet.getCellAt(row: Int, column: Int): LuckysheetCell? {
    return celldata?.find { it.r == row && it.c == column }
}

fun LuckysheetSheet.getMaxRow(): Int {
    return celldata?.maxOfOrNull { it.r } ?: 0
}

fun LuckysheetSheet.getMaxColumn(): Int {
    return celldata?.maxOfOrNull { it.c } ?: 0
}

fun ExcelCellInfo.toLuckysheetCell(): LuckysheetCell {
    return LuckysheetCell(
        r = row,
        c = column,
        v = LuckysheetCellValue(
            v = value,
            f = formula,
            m = value?.toString(),
            ct = LuckysheetCellType(
                fa = format ?: LuckysheetConstants.FORMAT_GENERAL,
                t = cellType
            ),

            ff = style?.fontFamily,
            fs = style?.fontSize,
            fc = style?.fontColor,
            bg = style?.backgroundColor,
            bl = if (style?.bold == true) 1 else 0,
            it = if (style?.italic == true) 1 else 0,
            cl = if (style?.underline == true) 1 else 0,
            un = if (style?.strikethrough == true) 1 else 0,
            ht = when (style?.horizontalAlignment?.lowercase()) {
                "left" -> LuckysheetConstants.ALIGN_LEFT
                "center" -> LuckysheetConstants.ALIGN_CENTER
                "right" -> LuckysheetConstants.ALIGN_RIGHT
                else -> null
            },
            vt = when (style?.verticalAlignment?.lowercase()) {
                "top" -> LuckysheetConstants.ALIGN_TOP
                "middle" -> LuckysheetConstants.ALIGN_MIDDLE
                "bottom" -> LuckysheetConstants.ALIGN_BOTTOM
                else -> null
            }
        )
    )
}