package com.sstek.jaoa.utils

import android.util.Log
import androidx.compose.ui.graphics.Color
import org.apache.poi.xwpf.usermodel.*
import org.apache.poi.util.Units
import org.jsoup.nodes.*
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STColorType
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHighlightColor
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd
import java.io.ByteArrayInputStream
import java.util.Base64

val DEFAULT_WIDTH_UNITS_TO_EMU = 400.0
val DEFAULT_HEIGHT_UNITS_TO_EMU = 400.0

fun htmlToXwpf(
    element: Node,
    document: XWPFDocument,
    inheritedStyle: StyleState = StyleState()
) {
    when (element) {
        is TextNode -> {
            val paragraph = document.createParagraph()
            val run = paragraph.createRun().apply {
                setText(element.text())
                isBold = inheritedStyle.bold
                isItalic = inheritedStyle.italic
                fontSize = inheritedStyle.fontSize
                if (inheritedStyle.underline) underline = UnderlinePatterns.SINGLE
                setColor(normalizeColor(inheritedStyle.color))
                inheritedStyle.backgroundColor?.let {
                    setTextHighlightColor(normalizeColor(it))
                }
                fontFamily = inheritedStyle.fontFamilyName
            }
        }

        is Element -> {
            when (element.tagName()) {
                "p", "div" -> {
                    val paragraph = document.createParagraph()
                    val newStyle = updateStyleForElement(element, inheritedStyle)
                    for (child in element.childNodes()) {
                        htmlToXwpfChild(child, paragraph, newStyle)
                    }
                }

                "img" -> {
                    val paragraph = document.createParagraph()
                    val src = element.attr("src")
                    if (src.startsWith("data:image")) {
                        val base64Data = src.substringAfter("base64,")
                        val imageBytes = Base64.getDecoder().decode(base64Data)
                        val pictureType = when {
                            src.contains("image/png") -> XWPFDocument.PICTURE_TYPE_PNG
                            src.contains("image/jpeg") || src.contains("image/jpg") -> XWPFDocument.PICTURE_TYPE_JPEG
                            src.contains("image/gif") -> XWPFDocument.PICTURE_TYPE_GIF
                            else -> XWPFDocument.PICTURE_TYPE_PNG
                        }
                        val run = paragraph.createRun()
                        val inputStream = ByteArrayInputStream(imageBytes)
                        run.addPicture(
                            inputStream,
                            pictureType,
                            "image",
                            Units.toEMU(DEFAULT_WIDTH_UNITS_TO_EMU),
                            Units.toEMU(DEFAULT_HEIGHT_UNITS_TO_EMU)
                        )
                    }
                }

                "table" -> {
                    val rows = element.getElementsByTag("tr")
                    val maxCols = getMaxColumns(rows)
                    val table = document.createTable(rows.size, maxCols)

                    for ((rowIndex, tr) in rows.withIndex()) {
                        val ths = tr.getElementsByTag("th")
                        for ((cellIndex, th) in ths.withIndex()) {
                            val cell = table.getRow(rowIndex).getCell(cellIndex)
                            val newStyle = updateStyleForElement(th, inheritedStyle).apply {
                                bold = true
                            }
                            if (cell.paragraphs.size > 0) cell.removeParagraph(0)
                            val paragraph = cell.addParagraph()
                            for (child in th.childNodes()) {
                                htmlToXwpfChild(child, paragraph, newStyle)
                            }
                        }

                        val tds = tr.getElementsByTag("td")
                        for ((cellIndex, td) in tds.withIndex()) {
                            val cell = table.getRow(rowIndex).getCell(cellIndex)
                            val newStyle = updateStyleForElement(td, inheritedStyle)
                            if (cell.paragraphs.size > 0) cell.removeParagraph(0)
                            val paragraph = cell.addParagraph()
                            for (child in td.childNodes()) {
                                htmlToXwpfChild(child, paragraph, newStyle)
                            }
                        }
                    }
                }

                else -> {
                    val newStyle = updateStyleForElement(element, inheritedStyle)
                    val paragraph = document.createParagraph()
                    for (child in element.childNodes()) {
                        htmlToXwpfChild(child, paragraph, newStyle)
                    }
                }
            }
        }
    }
}



fun htmlToXwpfChild(element: Node, paragraph: XWPFParagraph, inheritedStyle: StyleState) {
    Log.d("HtmlToXwpf", "$element, $inheritedStyle")
    when (element) {
        is TextNode -> {
            val run = paragraph.createRun().apply {
                setText(element.text())
                isBold = inheritedStyle.bold
                isItalic = inheritedStyle.italic
                fontSize = inheritedStyle.fontSize
                fontFamily = inheritedStyle.fontFamilyName
                if (inheritedStyle.underline) underline = UnderlinePatterns.SINGLE
                setColor(inheritedStyle.color.removePrefix("#"))


                // TODO(Yalnızca STHighlightColor renkleri çalışıyor. Tüm renkler çalılşacak şekilde düzenle.
                //  Şimdilik bgColor un String olması bekleniyor.)
                inheritedStyle.backgroundColor?.let { bgColor ->
                    setTextHighlightColor(rgbIntToHighlightColorName(bgColor.removePrefix("#")))
                }

            }
        }

        is Element -> {
            // Çok önemli: yeni stil hesapla
            val newStyle = updateStyleForElement(element, inheritedStyle)
            // Bu elementin tüm çocuklarını işle
            for (child in element.childNodes()) {
                htmlToXwpfChild(child, paragraph, newStyle)
            }
        }
    }
}


fun updateStyleForElement(element: Element, inheritedStyle: StyleState): StyleState {
    val newStyle = inheritedStyle.copy()

    when (element.tagName()) {
        "b", "strong" -> newStyle.bold = true
        "i", "em" -> newStyle.italic = true
        "u" -> newStyle.underline = true
        "font" -> {
            val color = element.attr("color")
            if (color.isNotBlank()) newStyle.color = normalizeColor(color)


            val font = element.attr("face")
            if (font.isNotBlank()) newStyle.fontFamilyName = font

            val sizeStr = element.attr("size")
            if (sizeStr.isNotBlank()) {
                val sizeNum = sizeStr.toIntOrNull()
                if (sizeNum != null) {
                    newStyle.fontSize = 8 + (sizeNum - 1) * 3
                }
            }
        }
    }

    val styleAttr = element.attr("style")
    if (styleAttr.isNotBlank()) {
        styleAttr.extractCssColor()?.let { newStyle.color = normalizeColor(it) }
        styleAttr.extractFontSize()?.let { newStyle.fontSize = it }
        styleAttr.extractFontFamily()?.let { newStyle.fontFamilyName = it }
        styleAttr.extractBackgroundColor()?.let { newStyle.backgroundColor = normalizeColor(it) }
    }

    return newStyle
}

data class StyleState(
    var bold: Boolean = false,
    var italic: Boolean = false,
    var underline: Boolean = false,
    var color: String = "#000000",
    var fontFamilyName: String = "Calibri",
    var fontSize: Int = 12,
    var backgroundColor: String? = null
)

// Yardımcı Fonksiyonlar

fun String.extractCssColor(): String? =
    Regex("color\\s*:\\s*([^;]+);?").find(this)?.groupValues?.get(1)?.trim()

fun String.extractBackgroundColor(): String? =
    Regex("background-color\\s*:\\s*([^;]+);?").find(this)?.groupValues?.get(1)?.trim()

fun String.extractFontSize(): Int? =
    Regex("font-size\\s*:\\s*(\\d+)\\s*px").find(this)?.groupValues?.get(1)?.toIntOrNull()?.let { px -> (px * 0.75).toInt() }

fun String.extractFontFamily(): String? =
    Regex("font-family\\s*:\\s*([^;]+);?").find(this)?.groupValues?.get(1)?.split(",")?.firstOrNull()
        ?.replace("\"", "")?.replace("'", "")?.trim()

fun normalizeColor(color: String): String {
    // color parametresi örnek: "rgb(0, 0, 255)", "#FF00FF", "blue"
    return when {
        color.startsWith("rgb") -> rgbToHex(color)
        color.startsWith("#") -> color.removePrefix("#").uppercase()
        else -> cssColorNameToHex(color)?.removePrefix("#")?.uppercase() ?: "000000"
    }
}

fun rgbToHex(rgb: String): String {
    // "rgb(0, 0, 255)" -> "0000FF"
    val nums = rgb.removePrefix("rgb(").removeSuffix(")").split(",").map { it.trim().toInt() }
    return nums.joinToString("") { it.coerceIn(0, 255).toString(16).padStart(2, '0') }.uppercase()
}

fun cssColorNameToHex(name: String): String? = mapOf(
    "black" to "#000000", "white" to "#FFFFFF", "red" to "#FF0000",
    "green" to "#008000", "blue" to "#0000FF", "yellow" to "#FFFF00",
    "gray" to "#808080", "cyan" to "#00FFFF", "magenta" to "#FF00FF",
    "orange" to "#FFA500", "purple" to "#800080", "pink" to "#FFC0CB"
)[name.lowercase()]

fun getMaxColumns(rows: List<Element>): Int {
    var max = 0
    for (row in rows) {
        val count = row.getElementsByTag("td").size + row.getElementsByTag("th").size
        if (count > max) max = count
    }
    return max
}

fun rgbIntToHighlightColorName(rgb: String): String {
    return when (rgb.uppercase()) {
        "FFFF00" -> "yellow"
        "00FF00" -> "green"
        "00FFFF" -> "turquoise"
        "FF00FF" -> "pink"
        "0000FF" -> "blue"
        "FF0000" -> "red"
        "808080" -> "gray"      // gri 25%
        "000000" -> "black"
        "FFFFFF" -> "white"
        "000080" -> "darkBlue"
        "800000" -> "darkRed"
        "808000" -> "darkYellow"
        "008000" -> "darkGreen"
        "008080" -> "darkTeal"     // koyu camgöbeği
        "800080" -> "darkPurple"
        "666666" -> "gray50"       // gri 50%
        else -> "none"
    }
}
