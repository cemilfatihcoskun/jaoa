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
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> return DOCX
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> return XLSX
                else -> return UNKNOWN
            }
        }
    }
}
