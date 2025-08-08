package com.sstek.jaoa.utils

import android.content.Context
import android.net.Uri
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.*
import org.jsoup.Jsoup
import org.jsoup.nodes.*
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHighlightColor
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.util.Base64

val DEFAULT_WIDTH_UNITS_TO_EMU = 200.0
val DEFAULT_HEIGHT_UNITS_TO_EMU = 200.0

fun loadTemplateDocument(context: Context): XWPFDocument {
    // assets'den InputStream al
    val inputStream = context.assets.open("template.docx")
    // XWPFDocument'i InputStream'den oluştur
    return XWPFDocument(inputStream).also {
        inputStream.close()
    }
}


fun convertHtmlToXwpf(context: Context, html: String): XWPFDocument {
    val document = loadTemplateDocument(context)
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

            val para = paragraph ?: document.createParagraph()
            para.spacingBefore = 0
            para.spacingAfter = 0
            inheritedStyle.lineHeight?.let { applyLineHeight(para, it) }

            val run = para.createRun().apply {
                setText(text)
                isBold = inheritedStyle.bold
                isItalic = inheritedStyle.italic
                fontSize = inheritedStyle.fontSize.coerceAtLeast(8)
                fontFamily = inheritedStyle.fontFamilyName
                if (inheritedStyle.underline) underline = UnderlinePatterns.SINGLE
                setColor(inheritedStyle.color.removePrefix("#"))
                inheritedStyle.backgroundColor?.let {
                    try {
                        setTextHighlightColor(backgroundColorNameNormalizationQuillToXwpf(it))
                    } catch (e: Exception) {
                        println("htmltoxwpf, ${e.message}")
                    }
                }
            }
        }


        is Element -> when (element.tagName().lowercase()) {
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
                        println("htmltoxwpf, $it")
                        try {
                            setTextHighlightColor(backgroundColorNameNormalizationQuillToXwpf(it))
                        } catch (e: Exception) {
                            println("htmltoxwpf, ${e.message}")
                        }
                    }
                }

                htmlInlineToSingleRun(context, element, run, newStyle)
            }

            // **Header tag'ları için burada stil ata**
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                val para = document.createParagraph().apply {
                    spacingBefore = 0
                    spacingAfter = 0
                }

                para.style = when (element.tagName().lowercase()) {
                    "h1" -> "Heading1"
                    "h2" -> "Heading2"
                    "h3" -> "Heading3"
                    "h4" -> "Heading4"
                    "h5" -> "Heading5"
                    "h6" -> "Heading6"
                    else -> null
                }

                val newStyle = updateStyleForElement(element, inheritedStyle)

                // Satır aralığı uygula
                newStyle.lineHeight?.let { lh ->
                    applyLineHeight(para, lh)
                }

                newStyle.bold = true

                newStyle.fontSize = when(element.tagName().lowercase()) {
                    "h1" -> 18
                    "h2" -> 16
                    "h3" -> 14
                    "h4" -> 13
                    "h5" -> 12
                    "h6" -> 11
                    else -> newStyle.fontSize
                }


                newStyle.lineHeight = newStyle.lineHeight.takeIf { it in 0.5..5.0 } ?: 1.0
                val pPr = para.ctp.pPr ?: para.ctp.addNewPPr()
                val spacing = pPr.spacing ?: pPr.addNewSpacing()
                spacing.line = BigInteger.valueOf((newStyle.lineHeight * 240).toLong())
                spacing.lineRule = org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.AUTO

                for (child in element.childNodes()) {
                    htmlToXwpf(context, child, document, newStyle, currentNumId, currentIlvl, para)
                }
            }

            "p" -> {
                // Normal paragraf
                val para = document.createParagraph().apply {
                    spacingBefore = 0
                    spacingAfter = 0
                }

                val newStyle = updateStyleForElement(element, inheritedStyle)

                // Satır aralığı vb. stil ayarları (isteğe bağlı)
                newStyle.lineHeight?.let { lh ->
                    applyLineHeight(para, lh)

                }

                for (child in element.childNodes()) {
                    htmlToXwpf(context, child, document, newStyle, currentNumId, currentIlvl, para)
                }
            }

            "div", "span", "b", "i", "strong", "u", "em" -> {
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

                        val style = element.attr("style")
                        val widthPx = Regex("width\\s*:\\s*(\\d+)px").find(style)?.groupValues?.get(1)?.toIntOrNull()
                        val heightPx = Regex("height\\s*:\\s*(\\d+)px").find(style)?.groupValues?.get(1)?.toIntOrNull()
                        val widthAttr = element.attr("width").toIntOrNull()
                        val heightAttr = element.attr("height").toIntOrNull()

                        val finalWidthPx = widthPx ?: widthAttr ?: DEFAULT_WIDTH_UNITS_TO_EMU.toInt()
                        val finalHeightPx = heightPx ?: heightAttr ?: DEFAULT_HEIGHT_UNITS_TO_EMU.toInt()

                        val widthEmu = pxToEmu(finalWidthPx.toInt())
                        val heightEmu = pxToEmu(finalHeightPx.toInt())

                        inputStream.use {
                            para.createRun().addPicture(
                                it, pictureType, "image",
                                widthEmu,
                                heightEmu
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
                    println("htmltoxwpf, $it")
                    try {
                        run.setTextHighlightColor(backgroundColorNameNormalizationQuillToXwpf(it))
                    } catch (e: Exception) {
                        println("htmltoxwpf, ${e.message}")
                    }
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
        styleAttr.extractBackgroundColor()?.let { newStyle.backgroundColor = it }
        styleAttr.extractFontSize()?.let { newStyle.fontSize = it }
        styleAttr.extractFontFamily()?.let { newStyle.fontFamilyName = it }
    }

    // Quill sınıflarından font-size ve font-family kontrolü
    val classAttr = element.className()
    val sizeMatch = Regex("""ql-size-(\d+)pt""").find(classAttr)
    if (sizeMatch != null) {
        val sizePt = sizeMatch.groupValues[1].toIntOrNull()
        if (sizePt != null) newStyle.fontSize = sizePt
    }

    val fontMatch = Regex("""ql-font-([\w_]+)""").find(classAttr)
    val fontNameFormatted = fontMatch?.groupValues?.get(1)
        ?.removePrefix("jaoa_")  // "jaoa_" kısmını çıkar
        ?.split('_')             // '_' ile parçala
        ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } // Her kelimenin ilk harfini büyük yap
        ?: "Calibri" // Bulamazsa default font
    newStyle.fontFamilyName = fontNameFormatted

    styleAttr.extractLineHeight()?.let {
        println("htmltoxwpf lineheight $it")
        newStyle.lineHeight = it
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
    var backgroundColor: String? = null,
    var lineHeight: Double = 1.0
)

fun String.extractLineHeight(): Double? =
    Regex("line-height\\s*:\\s*([\\d.]+)").find(this)
        ?.groupValues?.get(1)
        ?.toDoubleOrNull()

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

fun backgroundColorNameNormalizationQuillToXwpf(name: String): String {
    return when (name) {
        "darkblue" -> "darkBlue"
        "darkcyan" -> "darkCyan"
        "darkgreen" -> "darkGreen"
        "darkmagenta" -> "darkMagenta"
        "darkred" -> "darkRed"
        "rgb(139, 128, 0)" -> "darkYellow"
        "darkgray" -> "darkGray"
        "lightgray" -> "lightGray"
        else -> name
    }
}

fun applyLineHeight(para: XWPFParagraph, lineHeight: Double) {
    val pPr = para.ctp.pPr ?: para.ctp.addNewPPr()
    val spacing = pPr.spacing ?: pPr.addNewSpacing()
    spacing.line = BigInteger.valueOf((lineHeight * 240).toLong())
    spacing.lineRule = STLineSpacingRule.AUTO  // veya AUTO, dene hangisi uygunsa
}
