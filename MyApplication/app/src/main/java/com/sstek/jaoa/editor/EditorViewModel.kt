package com.sstek.jaoa.editor

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sstek.jaoa.utils.convertHtmlToXwpf
import com.sstek.jaoa.utils.xwpfToHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File

class EditorViewModel(application: Application) : AndroidViewModel(application) {
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


    fun loadAndConvert(uri: Uri) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val inputStream = if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)
            } else if (uri.scheme == "file" || uri.scheme == null) {
                Log.d("EditorViewModel", "${uri.path}")
                try {
                    val file = File(uri.path ?: "")
                    file.inputStream()
                } catch (e: Exception) {
                    Log.d("EditorViewModel", "${e.message}")
                    _isLoading.value = false
                    return@launch
                }
            } else {
                null
            }

            val document = inputStream?.use {
                try {
                    XWPFDocument(it)
                } catch (e: Exception) {
                    Log.d("EditorViewModel", "${e.message}")
                    _isLoading.value = false
                    return@launch
                }
            }
            val html = document?.let { xwpfToHtml(it) }
            Log.d("EditorViewModel", "$html")
            _htmlContent.value = html
            _selectedFileUri.value = uri

            _isLoading.value = false
        }
    }


    fun saveHtmlAsDocx(html: String) {
        _isLoading.value = true
        _selectedFileUri.value?.let { uri ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val context = getApplication<Application>().applicationContext
                    val doc = convertHtmlToXwpf(context, html)  // context gönder!
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        doc.write(out)
                    }
                    _toastMessage.emit("Dosya başarıyla kaydedildi.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    _toastMessage.emit("Dosya kaydedilirken hata oluştu: ${e.message}")
                } finally {
                    _isLoading.value = false
                }
            }
        } ?: viewModelScope.launch {
            _toastMessage.emit("Lütfen önce dosya seçin.")
            _isLoading.value = false
        }
    }

    fun saveAs(html: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val doc = convertHtmlToXwpf(context, html)  // context gönder!
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    doc.write(outputStream)
                }
                _selectedFileUri.value = uri
                _toastMessage.emit("Farklı kaydedildi.")
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.emit("Dosya kaydedilirken hata oluştu: ${e.message}")
            }
        }
    }

    fun clearSelectedFile() {
        _selectedFileUri.value = null
        _htmlContent.value = null
        _docxDocument.value = null
        _isLoading.value = false
    }

    fun updateHtmlContent(newHtml: String) {
        _htmlContent.value = newHtml
    }
}