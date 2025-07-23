package com.sstek.jaoa

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.sstek.jaoa.utils.convertHtmlToXwpf
import com.sstek.jaoa.utils.htmlToXwpf
import com.sstek.jaoa.utils.xwpfToHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileUri: StateFlow<Uri?> = _selectedFileUri

    private val _docxDocument = MutableStateFlow<XWPFDocument?>(null)
    val docxDocument: StateFlow<XWPFDocument?> = _docxDocument

    private val _htmlContent = MutableStateFlow<String?>(null)
    val htmlContent: StateFlow<String?> = _htmlContent

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading


    public fun loadAndConvert(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val inputStream = context.contentResolver.openInputStream(uri)
            val document = inputStream?.use { XWPFDocument(it) }
            val html = document?.let { xwpfToHtml(it) }
            _htmlContent.value = html
            _selectedFileUri.value = uri
        }
    }

    fun saveHtmlAsDocx(html: String) {
        _isLoading.value = true
        _selectedFileUri.value?.let { uri ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val doc = convertHtmlToXwpf(html)
                    val context = getApplication<Application>().applicationContext
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        doc.write(out)
                    }
                    _toastMessage.emit("Dosya başarıyla kaydedildi.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    _toastMessage.emit("Dosya kaydedilirken hata oluştu: ${e.message}")
                }
            }
        } ?: viewModelScope.launch {
            _toastMessage.emit("Lütfen önce dosya seçin.")
        }
        _isLoading.value = false
    }

    fun saveAs(html: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            // Örneğin farklı dosya adı oluştur
            val doc = convertHtmlToXwpf(html)
            val context = getApplication<Application>().applicationContext

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                doc.write(outputStream)
            }

            _selectedFileUri.value = uri
            _toastMessage.emit("Farklı kaydedildi.")
        }
    }
}
