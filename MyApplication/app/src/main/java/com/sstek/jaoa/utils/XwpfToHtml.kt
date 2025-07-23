package com.sstek.jaoa.utils

import org.apache.poi.xwpf.usermodel.*
import org.apache.poi.util.Units
import java.util.Base64
import java.lang.StringBuilder

fun xwpfToHtml(document: XWPFDocument): String {
    val html = StringBuilder()
    html.append("<html><head><meta charset=\"UTF-8\"></head><body>")

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

    html.append("</body></html>")
    return html.toString()
}

fun paragraphToHtml(paragraph: XWPFParagraph): String {
    val html = StringBuilder()

    // Stil adını al ve normalize et (küçük harf + boşlukları kaldır + türkçe karakterleri dönüştür)
    val styleIdRaw = paragraph.styleID ?: paragraph.style ?: ""
    val styleId = styleIdRaw
        .lowercase()
        .replace("\\s".toRegex(), "")
        .replace("ş", "s")
        .replace("ı", "i")
        .replace("ğ", "g")
        .replace("ü", "u")
        .replace("ö", "o")
        .replace("ç", "c")
        .replace("é", "e")

    val headingLevel = when {
        styleId.contains("heading1") || styleId == "balk1" || styleId == "baslik1" -> 1
        styleId.contains("heading2") || styleId == "balk2" || styleId == "baslik2" -> 2
        styleId.contains("heading3") || styleId == "balk3" || styleId == "baslik3" -> 3
        styleId.contains("heading4") || styleId == "balk4" ||  styleId == "baslik4" -> 4
        styleId.contains("heading5") || styleId == "balk5" ||  styleId == "baslik5" -> 5
        styleId.contains("heading6") || styleId == "balk6" ||  styleId == "baslik6" -> 6
        else -> null
    }

    val tag = if (headingLevel != null) "h$headingLevel" else "p"

    val alignment = when (paragraph.alignment) {
        ParagraphAlignment.LEFT -> "left"
        ParagraphAlignment.CENTER -> "center"
        ParagraphAlignment.RIGHT -> "right"
        ParagraphAlignment.BOTH -> "justify"
        else -> "left"
    }

    val styleBuilder = StringBuilder()
    styleBuilder.append("text-align:$alignment;")

    if (headingLevel != null) {
        val headingFontSizes = mapOf(
            1 to 32,
            2 to 26,
            3 to 20,
            4 to 16,
            5 to 13,
            6 to 11
        )
        styleBuilder.append("font-weight:bold;")
        styleBuilder.append("font-size:${headingFontSizes[headingLevel]}px;")
        styleBuilder.append("font-family: 'Calibri', sans-serif;")
    } else {
        // TODO(font boyutunda problem java.awt kaynaklı)
        if (paragraph.runs.isNotEmpty()) {

            /*
            val firstRunFontSizeVal = paragraph.runs[0].fontSizeAsDouble
            val firstRunFontSize = if (firstRunFontSizeVal != null) {
                firstRunFontSizeVal.toDouble()
            } else {
                12.0
            }
             */
            val firstRunFontSize = 12.0

            if (firstRunFontSize > 0.0) {
                val px = (firstRunFontSize * 1.3333).toInt()
                styleBuilder.append("font-size:${px}px;")
            }
            val firstRunFontFamily = paragraph.runs[0].fontFamily
            if (!firstRunFontFamily.isNullOrBlank()) {
                styleBuilder.append("font-family:'$firstRunFontFamily';")
            }
        }
    }

    html.append("<$tag style=\"$styleBuilder\">")

    for (run in paragraph.runs) {
        html.append(runToHtml(run))
    }

    html.append("</$tag>")
    return html.toString()
}





fun runToHtml(run: XWPFRun): String {
    val html = StringBuilder()
    val text = run.text() ?: ""

    val styles = mutableListOf<String>()

    if (run.isBold) styles.add("font-weight:bold;")
    if (run.isItalic) styles.add("font-style:italic;")
    if (run.underline != UnderlinePatterns.NONE) styles.add("text-decoration:underline;")

    val color = run.color
    if (!color.isNullOrBlank()) styles.add("color:#${color};")

    val fontFamily = run.fontFamily
    if (!fontFamily.isNullOrBlank()) styles.add("font-family:'${fontFamily}';")

    // TODO(java.awt bağımlılığını kaldırmak için bunu yaptık ama gerçek boyutu alamadığı durumlar
    // da da alabilmek lazım)

    val fontSize = try {
        //val px = (run.fontSize * 1.3333).toInt()
        val px = (12 * 1.3333).toInt()
        styles.add("font-size:${px}px;")
    } catch (e: Exception) {
        12
    }

    if (styles.isNotEmpty() || text.isNotBlank()) {
        if (styles.isEmpty()) {
            html.append(escapeHtml(text))
        } else {
            html.append("<span style=\"${styles.joinToString("")}\">")
            html.append(escapeHtml(text))
            html.append("</span>")
        }
    }

    // Resimler varsa
    if (run.embeddedPictures.isNotEmpty()) {
        for (picture in run.embeddedPictures) {
            html.append(pictureToHtml(picture))
        }
    }

    return html.toString()
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

    return """<img src="data:$mimeType;base64,$base64" style="max-width:100%;height:auto;" />"""
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
