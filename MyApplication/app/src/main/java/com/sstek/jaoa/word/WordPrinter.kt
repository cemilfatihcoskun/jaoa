package com.sstek.jaoa.word

import android.app.Activity
import android.content.Context
import android.os.*
import android.print.*
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.lowagie.text.Font
import com.lowagie.text.pdf.BaseFont
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions
import fr.opensagres.xdocreport.itext.extension.font.IFontProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.awt.Color
import java.io.*
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBr
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBrType
import com.sstek.jaoa.R

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


fun ensurePageBreaks(document: XWPFDocument) {
    try {
        val paragraphs = document.paragraphs
        val indicesWithBreaks = mutableListOf<Int>()

        // Scan for page breaks
        for (pi in paragraphs.indices) {
            val p = paragraphs[pi]
            for (run in p.runs) {
                val brList: List<CTBr> = run.ctr.brList
                if (brList.isEmpty()) continue
                for (br in brList) {
                    val isPageType = try { br.type != null && br.type == STBrType.PAGE } catch (_: Exception) { false }
                    val xmlHasLastRendered = try { br.xmlText()?.contains("lastRenderedPageBreak", true) == true } catch (_: Exception) { false }

                    if (isPageType || xmlHasLastRendered) {
                        indicesWithBreaks.add(pi)
                        break
                    }
                }
                if (indicesWithBreaks.lastOrNull() == pi) break
            }
        }

        if (indicesWithBreaks.isEmpty()) {
            Log.d("ensurePageBreaks", "No inline page-breaks found.")
            return
        }
        Log.d("ensurePageBreaks", "Paragraphs with breaks: $indicesWithBreaks")

        // Process page breaks in reverse order
        indicesWithBreaks.sortedDescending().forEach { pi ->
            val p = paragraphs[pi]

            // Remove inline <w:br> elements
            for (rIndex in p.runs.indices.reversed()) {
                val run = p.runs[rIndex]
                val brList = run.ctr.brList
                if (brList.isEmpty()) continue

                for (bIndex in brList.indices.reversed()) {
                    val br = brList[bIndex]
                    val isPageType = try { br.type != null && br.type == STBrType.PAGE } catch (_: Exception) { false }
                    val xmlHasLastRendered = try { br.xmlText()?.contains("lastRenderedPageBreak", true) == true } catch (_: Exception) { false }

                    if (isPageType || xmlHasLastRendered) {
                        try {
                            run.ctr.removeBr(bIndex)
                            Log.d("ensurePageBreaks", "Removed inline <w:br> at paragraph $pi run $rIndex brIndex $bIndex")
                        } catch (e: Exception) {
                            Log.e("ensurePageBreaks", "run.ctr.removeBr failed: ${e.message}", e)
                        }
                    }
                }
            }

            // Add pageBreakBefore to the next paragraph
            val nextIndex = pi + 1
            if (nextIndex < document.paragraphs.size) {
                val nextPara = document.paragraphs[nextIndex]
                val pPr = nextPara.ctp.pPr ?: nextPara.ctp.addNewPPr()
                try {
                    pPr.addNewPageBreakBefore()
                    // Add spacing before to prevent content from shifting up
                    pPr.spacing?.setBefore(240) // 240 twips = 12pt spacing before
                    Log.d("ensurePageBreaks", "Added pageBreakBefore and spacing on paragraph index $nextIndex")
                } catch (e: Exception) {
                    Log.w("ensurePageBreaks", "addNewPageBreakBefore warning: ${e.message}")
                }
            } else {
                // Append a new paragraph with page break
                val newParagraph = document.createParagraph()
                newParagraph.isPageBreak = true
                newParagraph.spacingBefore = 240 // Add spacing for safety
                newParagraph.createRun().setText("")
                Log.d("ensurePageBreaks", "Appended page-break paragraph at end")
            }
        }
    } catch (e: Exception) {
        Log.e("ensurePageBreaks", "Error: ${e.message}", e)
    }
}

fun mapDocxFontToTtf(
    context: Context,
    fontFamily: String,
    style: Int,
    size: Float,
    color: Color?
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

suspend fun printBase64DocxToPdf(
    activity: Activity,
    base64: String,
    fileName: String = "document_to_print"
) {
    var pdfFile: File? = null
    withContext(Dispatchers.IO) {
        try {
            val docxBytes = Base64.decode(base64, Base64.DEFAULT)
            val document = XWPFDocument(ByteArrayInputStream(docxBytes))
            sanitizeDocxColors(document)
            ensurePageBreaks(document)

            File(activity.cacheDir, "debug_after.docx").outputStream().use { document.write(it) }


            val pdfOptions = PdfOptions.create()
                .fontEncoding(BaseFont.IDENTITY_H)
                .fontProvider(createFontProvider(activity))

            pdfFile = File(activity.cacheDir, "temp.pdf")
            FileOutputStream(pdfFile!!).use { output ->
                PdfConverter.getInstance().convert(document, output, pdfOptions)
            }

            withContext(Dispatchers.Main) {
                val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val adapter = SimplePdfPrintAdapter(activity, pdfFile!!)
                printManager.print(fileName, adapter, null)
            }
        } catch (e: Exception) {
            Log.e("WordUtils", "PDF yazdırma hatası: ${e.message}", e)

            val errorMessage = activity.applicationContext.getString(R.string.printingError)
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG)
                    .show()
            }
        }
    }
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
