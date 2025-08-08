package com.sstek.jaoa.utils

import org.apache.poi.ooxml.util.POIXMLUnits
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.*
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.Base64

// pt -> px map tablosu
val ptToPxMap = mapOf(
    8 to 11,
    9 to 12,
    10 to 13,
    11 to 15,
    12 to 16,
    14 to 19,
    16 to 22,
    18 to 24,
    20 to 26,
    22 to 29,
    24 to 32,
    26 to 35,
    28 to 37,
    36 to 48,
    48 to 64,
    72 to 96
)

// px → en yakın pt karşılığı bulan fonksiyon
fun pxToClosestPt(px: Int): Int {
    return ptToPxMap.minByOrNull { kotlin.math.abs(it.value - px) }?.key ?: 12
}

// fontFamily → ql-font-jaoa_... formatına dönüştüren fonksiyon
fun toJaoaFontClass(fontFamily: String?): String? {
    if (fontFamily.isNullOrBlank()) return null
    return "ql-font-jaoa_" + fontFamily.trim()
        .lowercase()
        .replace(" ", "_")
}

fun xwpfToHtml(document: XWPFDocument): String {
    val html = StringBuilder()
    //html.append("<meta charset=\"utf-8\">")

    // Header
    document.headerList.forEach { header ->
        html.append("<header>")
        header.paragraphs.forEach { html.append(paragraphToHtml(it)) }
        html.append("</header>")
    }

    // Body elements with list handling
    var currentListId: String? = null
    var currentListType: String? = null
    val listLevelStack = mutableListOf<String>()

    for (bodyElement in document.bodyElements) {
        when (bodyElement.elementType) {
            BodyElementType.PARAGRAPH -> {
                val p = bodyElement as XWPFParagraph
                val numPr = p.numFmt

                if (p.numID != null) {
                    val numId = p.numID.toString()
                    val ilvl = p.numIlvl?.toInt() ?: 0

                    val listTag = when (numPr) {
                        "bullet" -> "ul"
                        else -> "ol"
                    }

                    // Liste aç/kapat mantığı
                    if (currentListId == null) {
                        html.append("<$listTag>")
                        listLevelStack.add(listTag)
                        currentListId = numId
                        currentListType = listTag
                    } else if (currentListId != numId) {
                        while (listLevelStack.isNotEmpty()) {
                            html.append("</${listLevelStack.removeAt(listLevelStack.lastIndex)}>")
                        }
                        html.append("<$listTag>")
                        listLevelStack.add(listTag)
                        currentListId = numId
                        currentListType = listTag
                    }

                    html.append("<li>")
                    html.append(paragraphToHtml(p).removeSurrounding("<p>", "</p>"))
                    html.append("</li>")
                } else {
                    // Önceki açık listeyi kapat
                    while (listLevelStack.isNotEmpty()) {
                        html.append("</${listLevelStack.removeAt(listLevelStack.lastIndex)}>")
                    }
                    currentListId = null
                    currentListType = null
                    html.append(paragraphToHtml(p))
                }
            }

            BodyElementType.TABLE -> html.append(tableToHtml(bodyElement as XWPFTable))
            else -> {} // ContentControl gibi diğerleri şimdilik boş
        }
    }

    // Kalan listeleri kapat
    while (listLevelStack.isNotEmpty()) {
        html.append("</${listLevelStack.removeAt(listLevelStack.lastIndex)}>")
    }

    // Footer
    document.footerList.forEach { footer ->
        html.append("<footer>")
        footer.paragraphs.forEach { html.append(paragraphToHtml(it)) }
        html.append("</footer>")
    }

    return html.toString()
}

fun paragraphToHtml(paragraph: XWPFParagraph): String {
    val html = StringBuilder()

    val alignment = when (paragraph.alignment) {
        ParagraphAlignment.LEFT -> "left"
        ParagraphAlignment.CENTER -> "center"
        ParagraphAlignment.RIGHT -> "right"
        ParagraphAlignment.BOTH -> "justify"
        else -> "left"
    }

    val lineSpacing = try {
        paragraph.spacingBetween.takeIf { it > 0 } ?: run {
            val lineVal = paragraph.ctp.pPr?.spacing?.line
            if (lineVal != null) (lineVal as BigInteger).toDouble() / 240 else null
        }
    } catch (e: Exception) {
        null
    }

    println("xwpftohtml lineheight $lineSpacing")

    val styleParts = mutableListOf("text-align:$alignment;")
    if (lineSpacing != null && lineSpacing != 0.0 && lineSpacing != 1.0) {
        styleParts.add("line-height:${lineSpacing};")
    }

    val styleAttr = styleParts.joinToString(" ")

    // 💡 Burada heading kontrolü yap
    val styleId = paragraph.style?.replace(" ", "")?.lowercase()
    val tag = when (styleId) {
        "heading1" -> "h1"
        "heading2" -> "h2"
        "heading3" -> "h3"
        "heading4" -> "h4"
        "heading5" -> "h5"
        "heading6" -> "h6"
        else -> "p"
    }

    if (styleId != null) {
        println("xwpftohtml $styleId")
    }


    html.append("<$tag style=\"$styleAttr\">")
    for (run in paragraph.runs) {
        html.append(runToHtml(run))
    }
    html.append("</$tag>")

    return html.toString()
}



fun runToHtml(run: XWPFRun): String {
    val html = StringBuilder()
    val text = run.text() ?: ""

    val classes = mutableListOf<String>()
    val fontClass = toJaoaFontClass(run.fontFamily)
    if (fontClass != null) classes.add(fontClass)

    val fontSizePt = try {
        (run.ctr.rPr.szArray?.firstOrNull()?.`val` as? BigInteger)?.toInt()?.div(2)
    } catch (e: Exception) {
        null
    } ?: 12
    classes.add("ql-size-${fontSizePt}pt")

    val styles = mutableListOf<String>()
    if (run.isBold) styles.add("font-weight:bold;")
    if (run.isItalic) styles.add("font-style:italic;")
    if (run.underline != UnderlinePatterns.NONE) styles.add("text-decoration:underline;")
    val highlight = backgroundColorNameNormalizationXwpfToQuill(run.textHighlightColor.toString())
    if (!highlight.isNullOrBlank() && highlight.lowercase() != "none") styles.add("background-color:$highlight;")
    val color = run.color
    if (!color.isNullOrBlank()) styles.add("color:#$color;")

    val styleAttr = styles.joinToString("")

    if (classes.isNotEmpty() || styleAttr.isNotBlank()) {
        html.append("<span class=\"${classes.joinToString(" ")}\" style=\"$styleAttr\">")
        html.append(escapeHtml(text))
        html.append("</span>")
    } else {
        html.append(escapeHtml(text))
    }

    // Resimler varsa
    if (run.embeddedPictures.isNotEmpty()) {
        for (picture in run.embeddedPictures) {
            html.append(pictureToHtml(picture))
        }
    }

    return html.toString()
}



fun getPictureSizeInPx(picture: XWPFPicture): Pair<Int, Int> {
    val ctExtent = picture.ctPicture.spPr?.xfrm?.ext ?: return Pair(0, 0)

    val widthEmu = ctExtent.cx.toLong()
    val heightEmu = ctExtent.cy.toLong()

    val widthPx = emuToPx(widthEmu)
    val heightPx = emuToPx(heightEmu)

    return Pair(widthPx, heightPx)
}



fun pictureToHtml(picture: XWPFPicture): String {
    val pictureData = picture.pictureData ?: return ""
    val base64 = Base64.getEncoder().encodeToString(pictureData.data)
    val ext = pictureData.suggestFileExtension()
    val mimeType = when (ext.lowercase()) {
        "emf" -> "image/emf"
        "wmf" -> "image/wmf"
        "pict" -> "image/pict"
        "jpeg", "jpg" -> "image/jpeg"
        "png" -> "image/png"
        "dib" -> "image/bmp"
        "gif" -> "image/gif"
        "tiff" -> "image/tiff"
        "eps" -> "image/eps"
        "bmp" -> "image/bmp"
        "wpg" -> "image/wpg"
        else -> "image/png"
    }

    val (widthPx, heightPx) = getPictureSizeInPx(picture)

    // Eğer boyut alınamadıysa style kısmına max-width ve auto koyabiliriz
    val style = if (widthPx > 0 && heightPx > 0) {
        "width:${widthPx}px; height:${heightPx}px;"
    } else {
        "max-width:100%; height:auto;"
    }

    println("xwpftohtml, imgsize: $widthPx, $heightPx")

    return """<img width="$widthPx" height="$heightPx" src="data:$mimeType;base64,$base64" />"""
}

fun tableToHtml(table: XWPFTable): String {
    val html = StringBuilder()
    html.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"5\" style=\"border-collapse:collapse;\">")

    for (row in table.rows) {
        html.append("<tr>")
        for (cell in row.tableCells) {
            val isHeader = cell.paragraphs.any { p -> p.runs.any { r -> r.isBold } }
            val tag = if (isHeader) "th" else "td"
            html.append("<$tag>")
            for (p in cell.paragraphs) {
                html.append(paragraphToHtml(p))
            }
            html.append("</$tag>")
        }
        html.append("</tr>")
    }

    html.append("</table>")
    return html.toString()
}

fun escapeHtml(text: String): String {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}

fun backgroundColorNameNormalizationXwpfToQuill(name: String): String {
    return when (name) {
        "darkYellow" -> "rgb(139, 128, 0)"
        else -> name
    }
}

