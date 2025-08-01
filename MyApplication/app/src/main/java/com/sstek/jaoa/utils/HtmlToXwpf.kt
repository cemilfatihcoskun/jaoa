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

val DEFAULT_WIDTH_UNITS_TO_EMU = 200.0
val DEFAULT_HEIGHT_UNITS_TO_EMU = 200.0

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
    currentIlvl: Int = 0,
    paragraph: XWPFParagraph? = null
) {
    when (element) {
        is TextNode -> {
            val text = element.text()
            if (text.isBlank()) return

            val run = (paragraph ?: document.createParagraph()).createRun().apply {
                setText(text)
                isBold = inheritedStyle.bold
                isItalic = inheritedStyle.italic
                fontSize = inheritedStyle.fontSize.coerceAtLeast(8)
                fontFamily = inheritedStyle.fontFamilyName
                if (inheritedStyle.underline) underline = UnderlinePatterns.SINGLE
                setColor(inheritedStyle.color.removePrefix("#"))
                inheritedStyle.backgroundColor?.let {
                    setTextHighlightColor(rgbIntToHighlightColorName(it.removePrefix("#")))
                }
            }

            if (paragraph == null) {
                /*
                run.parent.spacingBefore = 0
                run.parent.spacingAfter = 0
                 */

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
                val para = document.createParagraph().apply {
                    spacingBefore = 0
                    spacingAfter = 0
                    setNumID(currentNumId)
                    setNumILvl(BigInteger.valueOf(currentIlvl.toLong()))
                }

                val newStyle = updateStyleForElement(element, inheritedStyle)
                val run = para.createRun().apply {
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

                htmlInlineToSingleRun(context, element, run, newStyle)
            }



            "p", "div", "span", "b", "i", "strong", "u", "em" -> {
                val para = paragraph ?: document.createParagraph()
                if (paragraph == null) {
                    para.spacingBefore = 0
                    para.spacingAfter = 0
                }

                val styleAttr = element.attr("style")
                val textAlign = Regex("text-align\\s*:\\s*(left|right|center|justify)").find(styleAttr)?.groupValues?.get(1)
                setParagraphAlignment(para, textAlign)

                val newStyle = updateStyleForElement(element, inheritedStyle)
                for (child in element.childNodes()) {
                    htmlToXwpf(context, child, document, newStyle, currentNumId, currentIlvl, para)
                }
            }

            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                val para = document.createParagraph()
                para.spacingBefore = 0
                para.spacingAfter = 0
                para.style = "Heading${element.tagName().substring(1)}"

                val newStyle = updateStyleForElement(element, inheritedStyle).apply {
                    fontSize = when (element.tagName()) {
                        "h1" -> 32; "h2" -> 28; "h3" -> 24
                        "h4" -> 20; "h5" -> 16; "h6" -> 14
                        else -> 12
                    }
                    bold = true
                }

                for (child in element.childNodes()) {
                    htmlToXwpf(context, child, document, newStyle, currentNumId, currentIlvl, para)
                }
            }

            "img" -> {
                val src = element.attr("src")
                val para = document.createParagraph()

                try {
                    val inputStream = when {
                        src.startsWith("data:image") -> {
                            val base64Data = src.substringAfter("base64,")
                            ByteArrayInputStream(Base64.getDecoder().decode(base64Data))
                        }

                        src.startsWith("content://") -> {
                            context.contentResolver.openInputStream(Uri.parse(src))
                        }

                        else -> {
                            val file = File(src)
                            if (file.exists()) FileInputStream(file) else null
                        }
                    }

                    if (inputStream != null) {
                        val pictureType = when {
                            src.contains("png", true) -> XWPFDocument.PICTURE_TYPE_PNG
                            src.contains("jpeg", true) || src.contains("jpg", true) -> XWPFDocument.PICTURE_TYPE_JPEG
                            src.contains("gif", true) -> XWPFDocument.PICTURE_TYPE_GIF
                            else -> XWPFDocument.PICTURE_TYPE_PNG
                        }

                        inputStream.use {
                            para.createRun().addPicture(
                                it, pictureType, "image",
                                Units.toEMU(DEFAULT_WIDTH_UNITS_TO_EMU),
                                Units.toEMU(DEFAULT_HEIGHT_UNITS_TO_EMU)
                            )
                        }
                    }
                } catch (e: Exception) {
                    println("HATA: Resim eklenirken: ${e.message}")
                }
            }

            "table" -> {
                val rows = element.getElementsByTag("tr")
                val maxCols = getMaxColumns(rows)
                val table = document.createTable(rows.size, maxCols)

                for ((rowIndex, tr) in rows.withIndex()) {
                    val cells = tr.select("th, td")
                    for ((cellIndex, cellElement) in cells.withIndex()) {
                        val cell = table.getRow(rowIndex).getCell(cellIndex)
                        cell.removeParagraph(0)
                        val para = cell.addParagraph()
                        val style = updateStyleForElement(cellElement, inheritedStyle).apply {
                            if (cellElement.tagName() == "th") bold = true
                        }

                        for (child in cellElement.childNodes()) {
                            htmlToXwpf(context, child, document, style, null, 0, para)
                        }
                    }
                }
            }

            else -> {
                val para = paragraph ?: document.createParagraph()
                val newStyle = updateStyleForElement(element, inheritedStyle)

                for (child in element.childNodes()) {
                    htmlToXwpf(context, child, document, newStyle, currentNumId, currentIlvl, para)
                }
            }
        }
    }
}

fun htmlInlineToSingleRun(
    context: Context,
    element: Element,
    run: XWPFRun,
    inheritedStyle: StyleState
) {
    for (child in element.childNodes()) {
        when (child) {
            is TextNode -> {
                run.setText(child.text(), run.textPosition)
            }

            is Element -> {
                val newStyle = updateStyleForElement(child, inheritedStyle)

                // Eski stil üzerinden değiştir
                run.isBold = newStyle.bold
                run.isItalic = newStyle.italic
                run.fontSize = newStyle.fontSize
                run.fontFamily = newStyle.fontFamilyName
                if (newStyle.underline) run.underline = UnderlinePatterns.SINGLE
                run.setColor(newStyle.color.removePrefix("#"))
                newStyle.backgroundColor?.let {
                    run.setTextHighlightColor(rgbIntToHighlightColorName(it.removePrefix("#")))
                }

                htmlInlineToSingleRun(context, child, run, newStyle)
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
    lvl.addNewStart().setVal(BigInteger.ONE)

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
