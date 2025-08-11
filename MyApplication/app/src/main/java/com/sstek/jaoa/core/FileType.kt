package com.sstek.jaoa.core

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
    }
}
