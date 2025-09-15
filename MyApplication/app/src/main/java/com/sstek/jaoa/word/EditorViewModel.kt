package com.sstek.jaoa.word

import android.app.Application
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sstek.jaoa.word.utils.convertHtmlToXwpf
import com.sstek.jaoa.word.utils.xwpfToHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import com.sstek.jaoa.R

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

    private val context = getApplication<Application>().applicationContext

    fun setSelectedFileUri(uri: Uri) {
        _selectedFileUri.value = uri
    }

    // ---------------------- Dosya yükleme ----------------------
    fun loadAndConvert(uri: Uri) {
        if (_isLoading.value) {
            viewModelScope.launch { _toastMessage.emit("Busy, please wait") }
            return
        }
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = when (uri.scheme) {
                    "content" -> context.contentResolver.openInputStream(uri)
                    "file", null -> File(uri.path ?: "").inputStream()
                    else -> null
                }

                val document = inputStream?.use { XWPFDocument(it) }
                val html = document?.let { xwpfToHtml(context, it) }
                _htmlContent.value = html
                _selectedFileUri.value = uri
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.emit(context.getString(R.string.wordViewModel_fileLoadErrorMessage))
            } finally {
                _isLoading.value = false
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

    // ---------------------- Kaydetme ----------------------
    fun saveHtmlAsDocx(html: String) {
        if (_isLoading.value) {
            viewModelScope.launch { _toastMessage.emit(context.getString(R.string.wordViewModel_busyMessage)) }
            return
        }

        _isLoading.value = true
        _selectedFileUri.value?.let { uri ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val doc = convertHtmlToXwpf(context, html)
                    context.contentResolver.openOutputStream(uri)?.use { doc.write(it) }
                    _toastMessage.emit(context.getString(R.string.wordViewModel_fileSavedAsSuccessfullyMessage))
                } catch (e: Exception) {
                    e.printStackTrace()
                    _toastMessage.emit(context.getString(R.string.wordViewModel_fileSavedAsErrorMessage))
                } finally {
                    _isLoading.value = false
                }
            }
        } ?: viewModelScope.launch {
            _toastMessage.emit(context.getString(R.string.wordViewModel_firstChooseFileMessage))
            _isLoading.value = false
        }
    }

    fun saveAsOld(html: String, uri: Uri) {
        if (_isLoading.value) {
            viewModelScope.launch { _toastMessage.emit(context.getString(R.string.wordViewModel_busyMessage)) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = convertHtmlToXwpf(context, html)
                context.contentResolver.openOutputStream(uri)?.use { doc.write(it) }
                _selectedFileUri.value = uri
                _toastMessage.emit(context.getString(R.string.wordViewModel_fileSavedAsSuccessfullyMessage))
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.emit(context.getString(R.string.wordViewModel_fileSavedAsErrorMessage))
            }
        }
    }

    // ---------------------- SuperDoc Base64 Kaydetme ----------------------
    fun save(base64: String) {
        val uri = _selectedFileUri.value
        if (uri != null) {
            saveAs(base64, uri)
        } else {
            viewModelScope.launch {
                _toastMessage.emit(context.getString(R.string.wordViewModel_firstChooseFileMessage))
            }
        }
    }

    fun saveAs(base64: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("EditorViewModel", "saveBase64ToUri: starting, base64 length = ${base64.length}")
                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                _selectedFileUri.value = uri
                _toastMessage.emit(context.getString(R.string.wordViewModel_fileSavedSuccessfullyMessage))
                Log.d("EditorViewModel", "saveBase64ToUri: file saved at $uri")
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.emit(context.getString(R.string.wordViewModel_fileSavedAsErrorMessage))
            }
        }
    }


}
