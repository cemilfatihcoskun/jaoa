package com.sstek.jaoa.word

import android.app.Activity
import android.content.Context
import android.os.*
import android.print.*
import android.util.Base64
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import com.lowagie.text.Document
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import com.sstek.jaoa.R
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions
import fr.opensagres.xdocreport.itext.extension.font.IFontProvider
import java.awt.Color

fun sanitizeDocxColors(document: XWPFDocument) {
    document.paragraphs.forEach { paragraph ->
        paragraph.runs.forEach { run ->
            try {
                val color = run.color
                if (color == null || color.equals("AUTO", ignoreCase = true)) {
                    run.setColor("000000")
                }
            } catch (e: Exception) {
                run.setColor("000000")
            }
        }
    }
}

fun removeCustomXml(docxFile: File): File {
    val sanitizedFile = File(docxFile.parentFile, "sanitized.docx")
    ZipFile(docxFile).use { zip ->
        ZipOutputStream(FileOutputStream(sanitizedFile)).use { out ->
            zip.entries().asSequence().forEach { entry ->
                if (!entry.name.equals("docProps/custom.xml", ignoreCase = true)) {
                    zip.getInputStream(entry).use { input ->
                        out.putNextEntry(ZipEntry(entry.name))
                        input.copyTo(out)
                        out.closeEntry()
                    }
                }
            }
        }
    }
    return sanitizedFile
}

fun mapDocxFontToTtf(
    context: Context,
    fontFamily: String,
    style: Int,
    size: Float,
    color: java.awt.Color?
): Font {
    val normalizedFontName = fontFamily.replace(" ", "").lowercase()
    val fontsDir = context.assets.list("word_editor/fonts") ?: emptyArray()
    val matchedFont = fontsDir.firstOrNull {
        it.replace(" ", "").lowercase() == normalizedFontName
    } ?: "Arial"

    val styleSuffix = when (style) {
        Font.BOLD -> "Bold"
        Font.ITALIC -> "Italic"
        Font.BOLDITALIC -> "BoldItalic"
        else -> ""
    }

    val ttfFileName =
        if (styleSuffix.isEmpty()) "$matchedFont.ttf" else "$matchedFont$styleSuffix.ttf"
    val ttfPath = "word_editor/fonts/$matchedFont/$ttfFileName"

    return try {
        val tmpFile = File(context.cacheDir, ttfFileName)
        context.assets.open(ttfPath).use { input ->
            FileOutputStream(tmpFile).use { output -> input.copyTo(output) }
        }

        val baseFont = BaseFont.createFont(
            tmpFile.absolutePath,
            BaseFont.IDENTITY_H,
            BaseFont.EMBEDDED
        )
        Font(baseFont, size, style, color ?: Color.BLACK)

    } catch (e: Exception) {
        Log.e("FontProvider", "Font yüklenemedi: $fontFamily, fallback Helvetica", e)
        Font(
            BaseFont.createFont(BaseFont.HELVETICA, BaseFont.IDENTITY_H, BaseFont.EMBEDDED),
            size,
            style,
            Color.BLACK
        )
    }
}

fun createFontProvider(context: Context) = object : IFontProvider {
    override fun getFont(
        familyName: String?,
        encoding: String?,
        size: Float,
        style: Int,
        color: Color?
    ): Font {
        return if (familyName == null) {
            Font(
                BaseFont.createFont(BaseFont.HELVETICA, BaseFont.IDENTITY_H, BaseFont.EMBEDDED),
                size,
                style,
                color ?: Color.BLACK
            )
        } else {
            mapDocxFontToTtf(context, familyName, style, size, color)
        }
    }
}

suspend fun printHtml(activity: Activity, webView: WebView, fileName: String) {
    withContext(Dispatchers.Main) {
        try {
            // 1. WebView Ayarları (Öncekiyle Aynı)
            webView.settings.apply {
                javaScriptEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
            }

            // 2. PrintManager ve Adapter Hazırlığı
            val printManager = activity.getSystemService(Activity.PRINT_SERVICE) as PrintManager
            val printAdapter = webView.createPrintDocumentAdapter(fileName)

            // ÖZEL BOYUT TANIMLAMA (Örnek: B5 boyutu - 176mm x 250mm)
            // Genişlik ve Yükseklik mikrometre (microns) cinsinden verilir.
            val customWidthMicrons = 176000
            val customHeightMicrons = 250000
            val customSizeName = "CUSTOM_B5_SIZE" // Özel boyutunuz için benzersiz bir isim

            val customMediaSize = PrintAttributes.MediaSize(
                customSizeName,         // Benzersiz kağıt adı (Örn: "custom_b5")
                customSizeName,         // Kullanıcıya gösterilecek yerel ad (Örn: "Özel B5")
                customWidthMicrons,     // Genişlik (mikrometre)
                customHeightMicrons     // Yükseklik (mikrometre)
            )

            // 3. PrintAttributes'ı Özel Boyuta Ayarlama
            val builder = PrintAttributes.Builder()
                .setMediaSize(customMediaSize) // Özel olarak tanımlanan boyutu kullan

                // Yüksek çözünürlük (600 DPI)
                .setResolution(PrintAttributes.Resolution("res1", "resolution", 600, 600))

                // Kenar boşluklarını kaldırma
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)

            val printJobName = fileName.replace(".pdf", "")

            // 4. Yazdırma İşini Başlatma
            val printJob = printManager.print(printJobName, printAdapter, builder.build())

            // Not: PrintManager'ın PDF kaydetme mekanizması asenkron çalışır.

        } catch (e: Exception) {
            e.printStackTrace()
            // Hata yönetimi
        }
    }
}



// PDF oluşturma (com.lowagie itext 2 sürümü)
suspend fun printDocxFileToPdf(
    activity: Activity,
    docxFile: File,
    fileName: String = "document_to_print",
    scale: Float = 0.75f // PDF içeriğini ölçek
) {
    var pdfFile: File? = null
    var scaledPdfFile: File? = null
    withContext(Dispatchers.IO) {
        try {
            val sanitizedFile = removeCustomXml(docxFile)
            val docx = XWPFDocument(FileInputStream(sanitizedFile))
            sanitizeDocxColors(docx)

            pdfFile = File(activity.cacheDir, "temp.pdf")
            FileOutputStream(pdfFile!!).use { output ->
                val pdfOptions = PdfOptions.create()
                    .fontEncoding(BaseFont.IDENTITY_H)
                    .fontProvider(createFontProvider(activity))
                PdfConverter.getInstance().convert(docx, output, pdfOptions)
            }

            // PDF ölçekleme
            //scaledPdfFile = File(activity.cacheDir, "temp_scaled.pdf")
            //scalePdf(pdfFile!!, scaledPdfFile!!, scale)

            withContext(Dispatchers.Main) {
                val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val adapter = SimplePdfPrintAdapter(activity, pdfFile)
                printManager.print(fileName, adapter, null)
            }

        } catch (e: Exception) {
            Log.e("WordUtils", "PDF yazdırma hatası: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, activity.getString(R.string.printingError), Toast.LENGTH_LONG).show()
            }
        }
    }
}

// PDF ölçekleme fonksiyonu
fun scalePdf(input: File, output: File, scale: Float = 0.75f) {
    val reader = com.lowagie.text.pdf.PdfReader(input.absolutePath)
    val stamper = com.lowagie.text.pdf.PdfStamper(reader, FileOutputStream(output))

    for (i in 1..reader.numberOfPages) {
        val page = stamper.getOverContent(i)
        val pageSize = reader.getPageSizeWithRotation(i)
        val xOffset = (pageSize.width * (1 - scale)) / 2
        val yOffset = (pageSize.height * (1 - scale)) / 2
        page.concatCTM(scale, 0f, 0f, scale, xOffset, yOffset)
    }

    stamper.close()
    reader.close()
}


// Base64 sürümü (aynı şekilde page break destekli)
suspend fun printDocxBase64ToPdfLowagie(
    activity: Activity,
    docxBase64: String,
    fileName: String = "document_to_print"
) {
    val tmpFile = File(activity.cacheDir, "tmp_docx.docx")
    FileOutputStream(tmpFile).use { it.write(Base64.decode(docxBase64, Base64.DEFAULT)) }
    printDocxFileToPdf(activity, tmpFile, fileName)
}

class SimplePdfPrintAdapter(private val activity: Activity, private val pdfFile: File) :
    PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(pdfFile.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        try {
            FileInputStream(pdfFile).use { input ->
                FileOutputStream(destination?.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            Log.e("SimplePdfPrintAdapter", "PDF yazma hatası", e)
            callback?.onWriteFailed(e.message)
        }
    }
}
