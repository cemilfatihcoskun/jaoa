package com.sstek.jaoa.utils

import org.apache.poi.xwpf.usermodel.UnderlinePatterns
import org.apache.poi.xwpf.usermodel.XWPFDocument

fun xwpfToHtml(doc: XWPFDocument): String {
    val sb = StringBuilder()

    for (paragraph in doc.paragraphs) {
        sb.append("<p>")
        for (run in paragraph.runs) {
            var openTags = ""
            var closeTags = ""

            if (run.isBold) {
                openTags += "<b>"
                closeTags = "</b>" + closeTags
            }
            if (run.isItalic) {
                openTags += "<i>"
                closeTags = "</i>" + closeTags
            }
            if (run.underline != UnderlinePatterns.NONE) {
                openTags += "<u>"
                closeTags = "</u>" + closeTags
            }
            run.color?.let { color ->
                openTags += "<span style=\"color:#$color\">"
                closeTags = "</span>" + closeTags
            }

            sb.append(openTags)
            sb.append(run.text())
            sb.append(closeTags)
        }
        sb.append("</p>")
    }

    return sb.toString()
}

