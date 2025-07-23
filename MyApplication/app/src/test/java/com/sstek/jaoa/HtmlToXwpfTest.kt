package com.sstek.jaoa

import com.sstek.jaoa.utils.htmlToXwpf
import com.sstek.jaoa.utils.xwpfToHtml
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.jsoup.Jsoup
import org.junit.Test

import org.junit.Assert.*
import java.io.File
import java.io.FileOutputStream

class HtmlToXwpfTest {
    @Test
    fun testHtmlToXwpf() {
        val filePath = "src/test/resources/sample.html"
        val file = File(filePath)
        if (!file.exists()) {
            println("Sample html dosyası bulunamadı!")
            return
        }

        val document = XWPFDocument()
        val body = Jsoup.parse(file).body()

        for (element in body.children()) {
            htmlToXwpf(element, document)
        }

        val outFile = File("build/test-output/test_output.docx")
        outFile.parentFile!!.mkdirs()
        FileOutputStream(outFile).use { document.write(it) }

        println("Word file is created: ${outFile.absolutePath}")
    }


}