package com.sstek.jaoa.word

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import com.sstek.jaoa.R
import com.sstek.jaoa.core.getFileName
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class WordViewModel(application: Application) : AndroidViewModel(application) {
    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileUri: StateFlow<Uri?> = _selectedFileUri

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()


    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var activity: Activity? = null
    fun setActivity(act: Activity) { activity = act }

    private val _isBold = MutableStateFlow(false)
    val isBold: StateFlow<Boolean> = _isBold

    private val _isItalic = MutableStateFlow(false)
    val isItalic: StateFlow<Boolean> = _isItalic

    private val _isUnderline = MutableStateFlow(false)
    val isUnderline: StateFlow<Boolean> = _isUnderline

    fun toggleBold() {
        _isBold.value = !_isBold.value
    }

    fun toggleItalic() {
        _isItalic.value = !_isItalic.value
    }

    fun toggleUnderline() {
        _isUnderline.value = !_isUnderline.value
    }

    private val context = getApplication<Application>().applicationContext

    fun setSelectedFileUri(uri: Uri) {
        _selectedFileUri.value = uri
    }

    fun clearSelectedFile() {
        _selectedFileUri.value = null
        _isLoading.value = false
    }

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

    fun print(base64: String, activity: Activity) {
        viewModelScope.launch(Dispatchers.IO) {
            var tempDocx: File? = null
            try {
                _isLoading.value = true
                _toastMessage.emit(context.getString(R.string.wordViewModel_printStartedMessage))
                val fileName = getFileName(context, _selectedFileUri.value)


                val tempDocx = File(activity.cacheDir, "printTemp.docx")
                FileOutputStream(tempDocx).use { fos ->
                    fos.write(Base64.decode(base64, Base64.DEFAULT))
                }


                // 2️⃣ DOCX'i PDF'e çevir ve yazdır
                printDocxFileToPdf(activity, tempDocx, fileName)
                //printHtml(activity, webView, fileName)
            } catch (e: Exception) {
                Log.e("WordViewModel", "Print error", e)
                withContext(Dispatchers.Main) {
                    val errorMessage = context.getString(R.string.printingError)
                    Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

}
