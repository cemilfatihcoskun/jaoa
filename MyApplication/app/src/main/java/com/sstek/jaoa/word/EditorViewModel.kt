package com.sstek.jaoa.word

import android.app.Application
import android.net.Uri
import android.util.Log
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



    fun loadAndConvert(uri: Uri) {
        if (_isLoading.value) {
            viewModelScope.launch {
                _toastMessage.emit(context.resources.getString(R.string.wordViewModel_busyMessage))
            }
        }

        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val application = getApplication<Application>()
            val inputStream = if (uri.scheme == "content") {
                Log.d("EditorViewModel", "uri=$uri")
                application.contentResolver.openInputStream(uri)
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
            val html = document?.let { xwpfToHtml(getApplication<Application>().applicationContext, it) }
            Log.d("EditorViewModel", "$html")
            _htmlContent.value = html
            _selectedFileUri.value = uri

            _isLoading.value = false
        }
    }

    fun saveToInternalStorageWithName(fileName: String) {
        val html = htmlContent.value
        if (html.isNullOrEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Dosya uzantısı otomatik .docx olsun
                val finalName = if (fileName.endsWith(".docx")) fileName else "$fileName.docx"
                val file = File(getApplication<Application>().filesDir, finalName)

                // HTML’i DOCX’e dönüştür
                val doc = convertHtmlToXwpf(getApplication(), html)
                file.outputStream().use { doc.write(it) }

                _selectedFileUri.value = Uri.fromFile(file)
                _toastMessage.emit(context.resources.getString(R.string.wordViewModel_fileSavedSuccessfullyMessage))
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.emit(context.resources.getString(R.string.wordViewModel_fileSaveErrorMessage))
            }
        }
    }




    fun saveHtmlAsDocx(html: String) {
        if (_isLoading.value) {
            viewModelScope.launch {
                _toastMessage.emit(context.resources.getString(R.string.wordViewModel_busyMessage))
            }
        }

        _isLoading.value = true
        _selectedFileUri.value?.let { uri ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val context = getApplication<Application>().applicationContext
                    val doc = convertHtmlToXwpf(context, html)
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        doc.write(out)
                    }
                    _toastMessage.emit(context.resources.getString(R.string.wordViewModel_fileSavedSuccessfullyMessage))
                } catch (e: Exception) {
                    e.printStackTrace()
                    _toastMessage.emit(context.resources.getString(R.string.wordViewModel_fileSaveErrorMessage))
                } finally {
                    _isLoading.value = false
                }
            }
        } ?: viewModelScope.launch {
            _toastMessage.emit(context.resources.getString(R.string.wordViewModel_firstChooseFileMessage))
            _isLoading.value = false
        }
    }

    fun saveAs(html: String, uri: Uri) {
        if (_isLoading.value) {
            viewModelScope.launch {
                _toastMessage.emit(context.resources.getString(R.string.wordViewModel_busyMessage))
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val doc = convertHtmlToXwpf(context, html)  // context gönder!
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    doc.write(outputStream)
                }
                _selectedFileUri.value = uri
                _toastMessage.emit(context.resources.getString(R.string.wordViewModel_fileSavedAsSuccessfullyMessage))
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.emit(context.resources.getString(R.string.wordViewModel_fileSavedAsErrorMessage))
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