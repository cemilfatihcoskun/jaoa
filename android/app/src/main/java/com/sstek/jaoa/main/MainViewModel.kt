package com.sstek.jaoa.main

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sstek.jaoa.utils.htmlToXwpf
import com.sstek.jaoa.utils.xwpfToHtml
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.apache.poi.xwpf.usermodel.UnderlinePatterns
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.jsoup.Jsoup
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

Import androidx.compose.runtime.getValue

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appContext: Application
): ViewModel() {

    private val _createResult = MutableStateFlow<Boolean?>(null)
    val createResult: StateFlow<Boolean?> = _createResult

    private val _readResult = MutableStateFlow<String?>("")
    val readResult: StateFlow<String?> = _readResult

    private val _htmlContent = MutableStateFlow<String>("")
    val htmlContent: StateFlow<String> = _htmlContent

    var text by mutableStateOf("")

    fun loadDocxFile(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val doc = XWPFDocument(inputStream)
                    val html = xwpfToHtml(doc)
                    _htmlContent.value = html
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetCreateResult() {
        _createResult.value = null
    }

    fun createWordFile(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val document = XWPFDocument()

                val paragraph = document.createParagraph()
                val run = paragraph.createRun()
                run.setText("APACHE POI ile oluşturuldu.")
                run.isBold = true
                run.fontSize = 14

                val paragraph2 = document.createParagraph()
                val run2 = paragraph2.createRun()
                run2.setText("Duygusalsa tamamen olur aynen John Waynen")
                run2.isBold = false
                run2.fontSize = 16

                val file = File(appContext.filesDir, fileName)
                FileOutputStream(file).use { out ->
                    document.write(out)
                }

                _createResult.value = true
            } catch (e: Exception) {
                _createResult.value = false
                Log.d("MainViewModel", "createWordFile() ${e.message}")
            }
        }

    }

    fun readWordFile(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(appContext.filesDir, fileName)
                if (!file.exists()) {
                    Log.d("MainViewModel", "readWordFile() file does not exists.")
                    _readResult.value = "Hata dosya okunamadı."
                } else {
                    val builder = StringBuilder()
                    FileInputStream(file).use { stream ->
                        val doc = XWPFDocument(stream)
                        for (paragraph in doc.paragraphs) {
                            builder.append(paragraph.text).append("\n")
                        }
                    }
                    _readResult.value = builder.toString()
                }
            } catch (e: Exception) {
                _readResult.value = "Hata dosya okunamadı."
                Log.d("MainViewModel", "readWordFile() ${e.message}")
            }
        }
    }

    fun saveDocxFromHtml(html: String, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val document = XWPFDocument()
                val body = Jsoup.parse(html).body()

                for (element in body.children()) {
                    htmlToXwpf(element, document)
                }

                val file = File(appContext.filesDir, fileName)
                FileOutputStream(file).use { output ->
                    document.write(output)
                }

                Log.d("MainViewModel", "Document successfully saved: ${file.absolutePath}")

            } catch (e: Exception) {
                Log.e("MainViewModel", "Document could not be saved: ${e.message}", e)
            }
        }
    }

}