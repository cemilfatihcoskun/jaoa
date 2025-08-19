package com.sstek.jaoa.word.utils

import org.apache.poi.xwpf.usermodel.*
import java.io.File
import java.math.BigInteger
import kotlin.math.abs
import android.content.Context
import android.net.Uri
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType
import java.io.FileOutputStream

// pt -> px map tablosu
val ptToPxMap = mapOf(
    8 to 11, 9 to 12, 10 to 13, 11 to 15, 12 to 16, 14 to 19, 16 to 22,
    18 to 24, 20 to 26, 22 to 29, 24 to 32, 26 to 35, 28 to 37, 36 to 48, 48 to 64, 72 to 96
)

// px → en yakın pt karşılığı bulan fonksiyon
fun pxToClosestPt(px: Int): Int {
    return ptToPxMap.minByOrNull { abs(it.value - px) }?.key ?: 12
}

// fontFamily → ql-font-jaoa_... formatına dönüştüren fonksiyon
fun toJaoaFontClass(fontFamily: String?): String? {
    if (fontFamily.isNullOrBlank()) return null
    return "ql-font-jaoa_" + fontFamily.trim().lowercase().replace(" ", "_")
}

fun logAllStyles(document: XWPFDocument) {
    val styles: XWPFStyles = document.styles ?: return

    println("-------- STYLES IN DOCX --------")
    for (style in styles.styles) {
        val styleType = when (style.type) {
            STStyleType.PARAGRAPH -> "Paragraph"
            STStyleType.CHARACTER -> "Character"
            STStyleType.TABLE -> "Table"
            else -> "Other"
        }

        if (styleType != "Paragraph") {
            continue;
        }

        val name = style.name ?: "Unnamed"
        val id = style.styleId ?: "No ID"

        val isDefault = false
        val isCustom = false

        val pPr = style.ctStyle?.pPr
        val rPr = style.ctStyle?.rPr

// Alignment
        val alignment = pPr?.jc?.`val`?.toString()

// Satır aralığı
        val spacing = pPr?.spacing?.line?.let { it.toString().toLong() / 240.0 }

// Left / Right indent
        val leftIndent = pPr?.ind?.left?.toString()?.toLong()
        val rightIndent = pPr?.ind?.right?.toString()?.toLong()

// Karakter formatları

        val fontFamily = rPr?.rFontsArray?.firstOrNull()?.ascii ?: "unspecified"

        // Font size (half pt)
        val fontSize = rPr?.szArray?.firstOrNull()?.`val`?.toString()?.toInt()?.div(2) ?: 12

        // Bold / Italic / Underline
        val bold = rPr?.bArray?.firstOrNull()?.`val`?.toString().toBoolean() ?: false
        val italic = rPr?.iArray?.firstOrNull()?.`val`?.toString().toBoolean() ?: false
        val underline = rPr?.uArray?.firstOrNull()?.`val`?.toString().toBoolean() ?: "none"


        println("Style ID: $id, Name: $name, Type: $styleType, Default: $isDefault, Custom: $isCustom")
        println("Alignment: $alignment, Spacing: $spacing, LeftIndent: $leftIndent, RightIndent: $rightIndent")
        println("Font: $fontFamily, Size: $fontSize, Bold: $bold, Italic: $italic, Underline: $underline")
        println("------------------------------------------------")
    }
}




fun xwpfToHtml(context: Context, document: XWPFDocument): String {
    val html = StringBuilder()
    logAllStyles(document)

    // Header
    document.headerList.forEach { header ->
        html.append("<header>")
        header.paragraphs.forEach { html.append(paragraphToHtml(context, it)) }
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
                    val listTag = if (numPr == "bullet") "ul" else "ol"

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
                    html.append(paragraphToHtml(context, p).removeSurrounding("<p>", "</p>"))
                    html.append("</li>")
                } else {
                    while (listLevelStack.isNotEmpty()) {
                        html.append("</${listLevelStack.removeAt(listLevelStack.lastIndex)}>")
                    }
                    currentListId = null
                    currentListType = null
                    html.append(paragraphToHtml(context, p))
                }
            }
            BodyElementType.TABLE -> html.append(tableToHtml(context, bodyElement as XWPFTable))
            else -> {}
        }
    }

    while (listLevelStack.isNotEmpty()) {
        html.append("</${listLevelStack.removeAt(listLevelStack.lastIndex)}>")
    }

    // Footer
    document.footerList.forEach { footer ->
        html.append("<footer>")
        footer.paragraphs.forEach { html.append(paragraphToHtml(context, it)) }
        html.append("</footer>")
    }

    return html.toString()
}

fun paragraphToHtml(context: Context, paragraph: XWPFParagraph): String {
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
    } catch (e: Exception) { null }

    val styleParts = mutableListOf("text-align:$alignment;")
    if (lineSpacing != null && lineSpacing != 0.0 && lineSpacing != 1.0) styleParts.add("line-height:${lineSpacing};")
    val styleAttr = styleParts.joinToString(" ")

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

    html.append("<$tag style=\"$styleAttr\">")
    for (run in paragraph.runs) html.append(runToHtml(context, run))
    html.append("</$tag>")

    return html.toString()
}

fun runToHtml(context: Context, run: XWPFRun): String {
    val html = StringBuilder()
    val text = run.text() ?: ""

    val classes = mutableListOf<String>()
    toJaoaFontClass(run.fontFamily)?.let { classes.add(it) }

    val fontSizePt = try { (run.ctr.rPr.szArray?.firstOrNull()?.`val` as? BigInteger)?.toInt()?.div(2) } catch (e: Exception) { null } ?: 12
    classes.add("ql-size-${fontSizePt}pt")

    val styles = mutableListOf<String>()
    if (run.isBold) styles.add("font-weight:bold;")
    if (run.isItalic) styles.add("font-style:italic;")
    if (run.underline != UnderlinePatterns.NONE) styles.add("text-decoration:underline;")
    val highlight = backgroundColorNameNormalizationXwpfToQuill(run.textHighlightColor.toString())
    if (!highlight.isNullOrBlank() && highlight.lowercase() != "none") styles.add("background-color:$highlight;")
    run.color?.let { styles.add("color:#$it;") }

    val styleAttr = styles.joinToString("")

    if (classes.isNotEmpty() || styleAttr.isNotBlank()) {
        html.append("<span class=\"${classes.joinToString(" ")}\" style=\"$styleAttr\">")
        html.append(escapeHtml(text))
        html.append("</span>")
    } else html.append(escapeHtml(text))

    // Resimler temp klasöre kaydedilir ve src oradan yüklenir
    if (run.embeddedPictures.isNotEmpty()) {
        for (pic in run.embeddedPictures) html.append(pictureToHtmlTemp(context, pic))
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


fun pictureToHtmlTemp(context: Context, picture: XWPFPicture): String {
    val pictureData = picture.pictureData ?: return ""

    // pictureType üzerinden uzantı belirleme
    val ext = when (pictureData.pictureType) {
        Document.PICTURE_TYPE_BMP -> "bmp"
        Document.PICTURE_TYPE_EMF -> "emf"
        Document.PICTURE_TYPE_WMF -> "wmf"
        Document.PICTURE_TYPE_PICT -> "pict"
        Document.PICTURE_TYPE_JPEG -> "jpg"
        Document.PICTURE_TYPE_PNG -> "png"
        Document.PICTURE_TYPE_DIB -> "dib"
        Document.PICTURE_TYPE_GIF -> "gif"
        Document.PICTURE_TYPE_TIFF -> "tiff"
        Document.PICTURE_TYPE_EPS -> "eps"
        Document.PICTURE_TYPE_WPG -> "wpg"
        else -> "png"
    }

    // Temp dosya oluştur
    val tempFile = File.createTempFile("jaoa_image_", ".$ext", context.cacheDir)
    FileOutputStream(tempFile).use { fos ->
        fos.write(pictureData.data)
    }

    // Resim boyutu
    val (widthPx, heightPx) = getPictureSizeInPx(picture)

    // Eğer boyut alınamadıysa style kısmına max-width ve auto koyabiliriz
    val style = if (widthPx > 0 && heightPx > 0) {
        "width:${widthPx}px; height:${heightPx}px;"
    } else {
        "max-width:100%; height:auto;"
    }

    println("xwpftohtml, imgsize: $widthPx, $heightPx")

    val base64 = android.util.Base64.encodeToString(pictureData.data, android.util.Base64.NO_WRAP)
    return """<img style="$style" src="data:image/$ext;base64,$base64"/>"""
}

fun tableToHtml(context: Context, table: XWPFTable): String {
    val html = StringBuilder()
    html.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"5\" style=\"border-collapse:collapse;\">")
    for (row in table.rows) {
        html.append("<tr>")
        for (cell in row.tableCells) {
            val isHeader = cell.paragraphs.any { p -> p.runs.any { it.isBold } }
            val tag = if (isHeader) "th" else "td"
            html.append("<$tag>")
            for (p in cell.paragraphs) html.append(paragraphToHtml(context, p))
            html.append("</$tag>")
        }
        html.append("</tr>")
    }
    html.append("</table>")
    return html.toString()
}

fun escapeHtml(text: String): String {
    return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
        .replace("\"","&quot;").replace("'","&#39;")
}

fun backgroundColorNameNormalizationXwpfToQuill(name:String): String {
    return when(name){"darkYellow"->"rgb(139,128,0)";else->name}
}
