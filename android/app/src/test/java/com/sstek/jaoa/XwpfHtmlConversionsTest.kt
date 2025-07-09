package com.sstek.jaoa

import com.sstek.jaoa.utils.htmlToXwpf
import com.sstek.jaoa.utils.xwpfToHtml
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.jsoup.Jsoup
import org.junit.Test

import org.junit.Assert.*
import java.io.File
import java.io.FileOutputStream

class XwpfHtmlConversionsTest {
    @Test
    fun testHtmlToXwpf() {
        val filePath = "src/test/resources/sample.html"
        val file = File(filePath)
        if (!file.exists()) {
            println("Sample docx dosyası bulunamadı!")
        }

        val document = XWPFDocument()
        val body = Jsoup.parse(file).body()

        for (element in body.children()) {
            val paragraph = document.createParagraph()
            htmlToXwpf(element, document)
        }

        val outFile = File("build/test-output/test_output.docx")
        outFile.parentFile.mkdirs()
        FileOutputStream(outFile).use { document.write(it) }

        assertTrue(outFile.exists())
        println("Word file is created: ${outFile.absolutePath}")
    }

    @Test
    fun testXwpfToHtml() {
        val filePath = "src/test/resources/sample.docx"
        val file = File(filePath)
        if (!file.exists()) {
            println("Sample docx dosyası bulunamadı!")
        }

        val document = XWPFDocument(file.inputStream())
        val html = xwpfToHtml(document)

        if (!html.contains("<p>")) {
            println("HTML does not contain <p>.")
        }

        if (html.isBlank()) {
            println("HTML is blank.")
        }

        val outFile = File("build/test-output/test_output.html")
        outFile.parentFile.mkdirs()
        FileOutputStream(outFile).use { document.write(it) }
    }
}