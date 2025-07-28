package com.sstek.jaoa.utils

import android.content.Context
import android.net.Uri
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.*
import org.jsoup.Jsoup
import org.jsoup.nodes.*
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.util.Base64

val DEFAULT_WIDTH_UNITS_TO_EMU = 100.0
val DEFAULT_HEIGHT_UNITS_TO_EMU = 100.0

fun convertHtmlToXwpf(context: Context, html: String): XWPFDocument {
    val document = XWPFDocument()
    val body = Jsoup.parseBodyFragment(html).body()

    println("HtmlToXwpf, $html")

    for (element in body.childNodes()) {
        htmlToXwpf(context, element, document)
    }

    return document
}

fun htmlToXwpf(
    context: Context,
    element: Node,
    document: XWPFDocument,
    inheritedStyle: StyleState = StyleState(),
    currentNumId: BigInteger? = null,
    currentIlvl: Int = 0
) {
    when (element) {
        is TextNode -> {
            val text = element.text().trim()
            if (text.isEmpty()) return

            val paragraph = document.createParagraph()
            paragraph.spacingBefore = 100
            paragraph.spacingAfter = 100

            if (currentNumId != null) {
                paragraph.setNumID(currentNumId)
                paragraph.setNumILvl(BigInteger.valueOf(currentIlvl.toLong()))
            }

            val run = paragraph.createRun().apply {
                setText(text)
                isBold = inheritedStyle.bold
                isItalic = inheritedStyle.italic
                fontSize = inheritedStyle.fontSize.coerceAtLeast(8) // min 8pt
                fontFamily = inheritedStyle.fontFamilyName
                if (inheritedStyle.underline) underline = UnderlinePatterns.SINGLE
                setColor(inheritedStyle.color.removePrefix("#"))
                inheritedStyle.backgroundColor?.let {
                    setTextHighlightColor(rgbIntToHighlightColorName(it.removePrefix("#")))
                }
            }
        }

        is Element -> when (element.tagName()) {
            "ul", "ol" -> {
                val isOrdered = element.tagName() == "ol"
                val numId = if (isOrdered) createDecimalNumbering(document) else createBulletNumbering(document)

                for (li in element.children().filter { it.tagName() == "li" }) {
                    htmlToXwpf(context, li, document, inheritedStyle, numId, currentIlvl)
                }
            }

            "li" -> {
                val paragraph = document.createParagraph()
                paragraph.spacingBefore = 100
                paragraph.spacingAfter = 100
                if (currentNumId != null) {
                    paragraph.setNumID(currentNumId)
                    paragraph.setNumILvl(BigInteger.valueOf(currentIlvl.toLong()))
                }

                val newStyle = updateStyleForElement(element, inheritedStyle)

                for (child in element.childNodes()) {
                    htmlToXwpf(context, child, document, newStyle, currentNumId, currentIlvl)
                }
            }

            "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "span", "b", "strong", "i", "em", "u" -> {
                val paragraph = document.createParagraph()
                paragraph.spacingBefore = if (element.tagName().startsWith("h")) 300 else 100
                paragraph.spacingAfter = if (element.tagName().startsWith("h")) 300 else 100

                val styleAttr = element.attr("style")
                val textAlign = Regex("text-align\\s*:\\s*(left|right|center|justify)").find(styleAttr)?.groupValues?.get(1)
                setParagraphAlignment(paragraph, textAlign)

                if (element.tagName().startsWith("h")) {
                    paragraph.style = "Heading${element.tagName().substring(1)}"
                }

                val newStyle = updateStyleForElement(element, inheritedStyle).apply {
                    if (element.tagName().startsWith("h")) {
                        fontSize = when (element.tagName()) {
                            "h1" -> 32
                            "h2" -> 28
                            "h3" -> 24
                            "h4" -> 20
                            "h5" -> 16
                            "h6" -> 14
                            else -> 12
                        }
                        bold = true
                    }
                }

                for (child in element.childNodes()) {
                    if (child is TextNode) {
                        val text = child.text().trim()
                        if (text.isNotEmpty()) {
                            val run = paragraph.createRun().apply {
                                setText(text)
                                isBold = newStyle.bold
                                isItalic = newStyle.italic
                                fontSize = newStyle.fontSize.coerceAtLeast(8)
                                fontFamily = newStyle.fontFamilyName
                                if (newStyle.underline) underline = UnderlinePatterns.SINGLE
                                setColor(newStyle.color.removePrefix("#"))
                                newStyle.backgroundColor?.let {
                                    setTextHighlightColor(rgbIntToHighlightColorName(it.removePrefix("#")))
                                }
                            }
                        }
                    } else {
                        htmlToXwpf(context, child, document, newStyle, currentNumId, currentIlvl)
                    }
                }
            }

            "img" -> {
                val src = element.attr("src")
                val paragraph = document.createParagraph()

                try {
                    val inputStream = when {
                        src.startsWith("data:image") && src.contains("base64,") -> {
                            val base64Data = src.substringAfter("base64,")
                            val imageBytes = Base64.getDecoder().decode(base64Data)
                            ByteArrayInputStream(imageBytes)
                        }

                        src.startsWith("content://") -> {
                            context.contentResolver.openInputStream(Uri.parse(src))
                        }

                        else -> {
                            val file = File(src)
                            if (file.exists()) {
                                FileInputStream(file)
                            } else {
                                println("HtmlToXwpf → dosya bulunamadı: ${file.absolutePath}")
                                null
                            }
                        }
                    }

                    if (inputStream != null) {
                        val pictureType = when {
                            src.contains("png", ignoreCase = true) -> XWPFDocument.PICTURE_TYPE_PNG
                            src.contains("jpeg", ignoreCase = true) || src.contains("jpg", ignoreCase = true) -> XWPFDocument.PICTURE_TYPE_JPEG
                            src.contains("gif", ignoreCase = true) -> XWPFDocument.PICTURE_TYPE_GIF
                            else -> XWPFDocument.PICTURE_TYPE_PNG
                        }

                        val run = paragraph.createRun()

                        inputStream.use {
                            run.addPicture(
                                it,
                                pictureType,
                                "image",
                                Units.toEMU(DEFAULT_WIDTH_UNITS_TO_EMU),
                                Units.toEMU(DEFAULT_HEIGHT_UNITS_TO_EMU)
                            )
                        }

                    } else {
                        println("HtmlToXwpf → inputStream null, resim eklenemedi")
                    }
                } catch (e: Exception) {
                    println("HtmlToXwpf → HATA: Resim eklenirken hata oluştu → ${e.message}")
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
                        val newStyle = updateStyleForElement(th, inheritedStyle).apply { bold = true }
                        if (cell.paragraphs.size > 0) cell.removeParagraph(0)
                        val paragraph = cell.addParagraph()
                        for (child in th.childNodes()) {
                            htmlToXwpf(context, child, document, newStyle)
                        }
                    }

                    val tds = tr.getElementsByTag("td")
                    for ((cellIndex, td) in tds.withIndex()) {
                        val cell = table.getRow(rowIndex).getCell(cellIndex)
                        val newStyle = updateStyleForElement(td, inheritedStyle)
                        if (cell.paragraphs.size > 0) cell.removeParagraph(0)
                        val paragraph = cell.addParagraph()
                        for (child in td.childNodes()) {
                            htmlToXwpf(context, child, document, newStyle)
                        }
                    }
                }
            }

            else -> {
                val paragraph = document.createParagraph()
                paragraph.spacingBefore = 100
                paragraph.spacingAfter = 100

                val styleAttr = element.attr("style")
                val textAlign = Regex("text-align\\s*:\\s*(left|right|center|justify)").find(styleAttr)?.groupValues?.get(1)
                setParagraphAlignment(paragraph, textAlign)

                val newStyle = updateStyleForElement(element, inheritedStyle)
                for (child in element.childNodes()) {
                    htmlToXwpf(context, child, document, newStyle, currentNumId, currentIlvl)
                }
            }
        }
    }
}

fun createDecimalNumbering(document: XWPFDocument): BigInteger {
    if (document.numbering == null) {
        document.createNumbering()
    }
    val numbering = document.numbering ?: throw IllegalStateException("Numbering should be initialized")

    val ctAbstractNum = CTAbstractNum.Factory.newInstance()
    ctAbstractNum.abstractNumId = BigInteger.valueOf(0)

    val lvl = ctAbstractNum.addNewLvl()
    lvl.ilvl = BigInteger.ZERO

    val numFmt = lvl.addNewNumFmt()
    numFmt.setVal(STNumberFormat.DECIMAL)

    val lvlText = lvl.addNewLvlText()
    lvlText.setVal("%1.")

    val lvlJc = lvl.addNewLvlJc()
    lvlJc.setVal(STJc.LEFT)

    val pPr = lvl.addNewPPr()
    val ind = pPr.addNewInd()
    ind.left = BigInteger.valueOf(720)

    val abstractNum = XWPFAbstractNum(ctAbstractNum)
    val abstractNumID = numbering.addAbstractNum(abstractNum)
    return numbering.addNum(abstractNumID)
}

fun createBulletNumbering(document: XWPFDocument): BigInteger {
    if (document.numbering == null) {
        document.createNumbering()
    }
    val numbering = document.numbering ?: throw IllegalStateException("Numbering should be initialized")

    val ctAbstractNum = CTAbstractNum.Factory.newInstance()
    ctAbstractNum.abstractNumId = BigInteger.valueOf(1)

    val lvl = ctAbstractNum.addNewLvl()
    lvl.ilvl = BigInteger.ZERO

    val numFmt = lvl.addNewNumFmt()
    numFmt.setVal(STNumberFormat.BULLET)

    val lvlText = lvl.addNewLvlText()
    lvlText.setVal("•") // bullet sembolü

    val lvlJc = lvl.addNewLvlJc()
    lvlJc.setVal(STJc.LEFT)

    val pPr = lvl.addNewPPr()
    val ind = pPr.addNewInd()
    ind.left = BigInteger.valueOf(720)

    val abstractNum = XWPFAbstractNum(ctAbstractNum)
    val abstractNumID = numbering.addAbstractNum(abstractNum)
    return numbering.addNum(abstractNumID)
}

fun setParagraphAlignment(paragraph: XWPFParagraph, align: String?) {
    when (align?.lowercase()) {
        "left" -> paragraph.alignment = ParagraphAlignment.LEFT
        "right" -> paragraph.alignment = ParagraphAlignment.RIGHT
        "center" -> paragraph.alignment = ParagraphAlignment.CENTER
        "justify" -> paragraph.alignment = ParagraphAlignment.BOTH
        else -> paragraph.alignment = ParagraphAlignment.LEFT
    }
}

fun updateStyleForElement(element: Element, inheritedStyle: StyleState): StyleState {
    val newStyle = inheritedStyle.copy()

    when (element.tagName()) {
        "b", "strong" -> newStyle.bold = true
        "i", "em" -> newStyle.italic = true
        "u" -> newStyle.underline = true
    }

    val styleAttr = element.attr("style")
    if (styleAttr.isNotBlank()) {
        if (Regex("font-weight\\s*:\\s*bold", RegexOption.IGNORE_CASE).containsMatchIn(styleAttr)) {
            newStyle.bold = true
        }
        if (Regex("font-style\\s*:\\s*italic", RegexOption.IGNORE_CASE).containsMatchIn(styleAttr)) {
            newStyle.italic = true
        }
        if (Regex("text-decoration\\s*:\\s*underline", RegexOption.IGNORE_CASE).containsMatchIn(styleAttr)) {
            newStyle.underline = true
        }
        styleAttr.extractCssColor()?.let { newStyle.color = normalizeColor(it) }
        styleAttr.extractBackgroundColor()?.let { newStyle.backgroundColor = normalizeColor(it) }
        styleAttr.extractFontSize()?.let { newStyle.fontSize = it }
        styleAttr.extractFontFamily()?.let { newStyle.fontFamilyName = it }
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

fun String.extractCssColor(): String? =
    Regex("""(?<!background-)color\s*:\s*([^;]+)""").find(this)?.groupValues?.get(1)?.trim()

fun String.extractBackgroundColor(): String? =
    Regex("""background-color\s*:\s*([^;]+)""").find(this)?.groupValues?.get(1)?.trim()

fun String.extractFontSize(): Int? =
    Regex("font-size\\s*:\\s*(\\d+)\\s*px").find(this)?.groupValues?.get(1)?.toIntOrNull()?.let { px -> (px * 0.75).toInt() }

fun String.extractFontFamily(): String? =
    Regex("font-family\\s*:\\s*([^;]+);?").find(this)?.groupValues?.get(1)?.split(",")?.firstOrNull()
        ?.replace("\"", "")?.replace("'", "")?.trim()

fun normalizeColor(color: String): String {
    return when {
        color.startsWith("rgb") -> rgbToHex(color)
        color.startsWith("#") -> color.removePrefix("#").uppercase()
        else -> cssColorNameToHex(color)?.removePrefix("#")?.uppercase() ?: "000000"
    }
}

fun rgbToHex(rgb: String): String {
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
        "808080" -> "gray"
        "000000" -> "black"
        "FFFFFF" -> "white"
        "000080" -> "darkBlue"
        "800000" -> "darkRed"
        "808000" -> "darkYellow"
        "008000" -> "darkGreen"
        "008080" -> "darkTeal"
        "800080" -> "darkPurple"
        "666666" -> "gray50"
        else -> "none"
    }
}
