package com.sstek.jaoa.core

import org.jsoup.nodes.TextNode

enum class FileType(val extension: String) {
    // Word
    DOCX("docx"),
    DOC("doc"),

    // Excel
    XLSX("xlsx"),
    XLS("xls"),

    // Powerpoint
    PPTX("pptx"),
    PPT("ppt"),

    UNKNOWN("");

    companion object {
        fun fromFileName(name: String): FileType {
            val ext = name.substringAfterLast('.', "").lowercase()
            return values().find { it.extension == ext } ?: FileType.UNKNOWN
        }

        fun fromMime(mime: String): FileType {
            when (mime) {
                "application/msword" -> return DOC
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> return DOCX
                "application/vnd.ms-excel" -> return XLS
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> return XLSX
                "application/vnd.ms-powerpoint" -> return PPT
                "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> return PPTX
                else -> return UNKNOWN
            }
        }
    }
}
