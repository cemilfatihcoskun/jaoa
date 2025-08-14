package com.sstek.jaoa

import com.sstek.jaoa.word.utils.xwpfToHtml
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.junit.Test
import java.io.File

class XwpfToHtmlTest {
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
        outFile.writeText(html)
    }
}