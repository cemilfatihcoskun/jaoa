package com.sstek.jaoa.powerpoint

import android.app.Application
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream

class PowerpointViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext

    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileUri = _selectedFileUri.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    fun setSelectedFileUri(uri: Uri) { _selectedFileUri.value = uri }

    fun uriToBase64(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
        return Base64.encodeToString(bytes ?: ByteArray(0), Base64.NO_WRAP)
    }

    fun save(base64: String) {
        val uri = _selectedFileUri.value
        if (uri != null) saveAs(base64, uri)
        else viewModelScope.launch { _toastMessage.emit("Select a file first") }
    }

    fun saveAs(base64: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                _selectedFileUri.value = uri
                Log.d("PowerpointViewModel", "Saved PPTX to $uri")
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch { _toastMessage.emit("Failed to save PPTX") }
            }
        }
    }
}
