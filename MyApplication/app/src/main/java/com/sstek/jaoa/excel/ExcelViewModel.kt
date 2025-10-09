package com.sstek.jaoa.excel

import android.app.Application
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sstek.jaoa.excel.utils.ExcelToLuckysheetConverter
import com.sstek.jaoa.excel.utils.LuckysheetToExcelConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.json.JSONObject
import java.io.File

class ExcelViewModel(application: Application) : AndroidViewModel(application) {

    private val converter = LuckysheetToExcelConverter()
    private val excelToLuckyConverter = ExcelToLuckysheetConverter()


    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileUri: StateFlow<Uri?> = _selectedFileUri

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    private val _excelJsonData = MutableStateFlow<String?>(null)
    val excelJsonData: StateFlow<String?> = _excelJsonData

    private val _isDocumentModified = MutableStateFlow(false)
    val isDocumentModified: StateFlow<Boolean> = _isDocumentModified

    // ✅ WebView referansı
    private var currentWebView: WebView? = null


    fun setSelectedFile(uri: Uri) {
        Log.d("ExcelViewModel", "Setting selected file: $uri")
        _selectedFileUri.value = uri
    }


    fun onWebViewReady(webView: WebView) {
        Log.d("ExcelViewModel", "WebView ready event received!")
        currentWebView = webView


        val uri = _selectedFileUri.value
        if (uri != null) {
            val uriString = uri.toString()
            if (uriString != "new" && uriString.isNotBlank()) {
                Log.d("ExcelViewModel", "Loading file from ready event: $uri")
                loadAndConvertFile(uri, webView)
            } else {
                Log.d("ExcelViewModel", "New file - no loading needed")
            }
        } else {
            Log.d("ExcelViewModel", "No file selected")
        }
    }


    private fun loadAndConvertFile(uri: Uri, webView: WebView) {
        if (_isLoading.value) {
            viewModelScope.launch {
                _toastMessage.emit("Şu an bir işlem yapılıyor.")
            }
            return
        }

        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("ExcelViewModel", "Loading Excel file: $uri")


                val inputStream = when (uri.scheme) {
                    "content" -> {
                        getApplication<Application>().contentResolver.openInputStream(uri)
                    }
                    "file", null -> {
                        val file = File(uri.path ?: "")
                        if (file.exists()) file.inputStream() else null
                    }
                    else -> {
                        Log.w("ExcelViewModel", "Unsupported URI scheme: ${uri.scheme}")
                        null
                    }
                }

                if (inputStream == null) {
                    _toastMessage.emit("Dosya açılamadı.")
                    return@launch
                }

                val workbook = XSSFWorkbook(inputStream)
                val jsonData = excelToLuckyConverter.convert(workbook)
                Log.d("deneme",jsonData);
                workbook.close()
                inputStream.close()

                _excelJsonData.value = jsonData
                Log.d("ExcelViewModel", "Excel converted to JSON, size: ${jsonData.length}")


                withContext(Dispatchers.Main) {
                    loadDataToWebView(webView, jsonData)
                }

            } catch (e: Exception) {
                Log.e("ExcelViewModel", "Load error", e)
                _toastMessage.emit("Dosya yüklenirken hata oluştu: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun saveExcelFile(webView: WebView?) {
        if (webView == null) return

        if (_isLoading.value) {
            viewModelScope.launch {
                _toastMessage.emit("Şu an bir işlem yapılıyor.")
            }
            return
        }

        val uri = _selectedFileUri.value
        Log.d("ExcelViewModel", "Save attempt - URI: $uri")


        if (uri == null || uri.toString().isBlank() || uri.toString() == "new") {
            Log.d("ExcelViewModel", "No valid URI - redirecting to Save As")
            viewModelScope.launch {
                _toastMessage.emit("Yeni dosya için 'Farklı Kaydet' butonunu kullanın.")
            }
            return
        }

        // ✅ Geçerli URI varsa normal save
        Log.d("ExcelViewModel", "Saving to existing file: $uri")
        performSave(webView, uri)

        _isDocumentModified.value = false
        webView.evaluateJavascript("window.isDocumentModified = false;", null)
    }


    fun saveAsExcelFile(webView: WebView?, uri: Uri) {
        if (webView == null) return

        if (_isLoading.value) {
            viewModelScope.launch {
                _toastMessage.emit("Şu an bir işlem yapılıyor.")
            }
            return
        }

        Log.d("ExcelViewModel", "Save As to: $uri")
        performSave(webView, uri, isNewFile = true)
    }

    private fun performSave(webView: WebView, uri: Uri, isNewFile: Boolean = false) {
        _isLoading.value = true

        webView.evaluateJavascript("window.ExcelEditorAPI.getData()") { result ->
            viewModelScope.launch {
                try {
                    val cleanResult = result
                        .trim()
                        .removePrefix("\"")
                        .removeSuffix("\"")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")

                    if (!cleanResult.startsWith("{") || !cleanResult.endsWith("}")) {
                        _toastMessage.emit("Geçersiz veri formatı")
                        return@launch
                    }
                    Log.d("deneme",cleanResult);
                    Log.d("DebugJSON", "Cleaned length: ${cleanResult.length}")
                    Log.d("DebugJSON", "Last 500 chars: ${cleanResult.takeLast(1500)}")
                    val jsonData = JSONObject(cleanResult)

                    if (jsonData.getBoolean("success")) {
                        val sheetsArray = jsonData.getJSONArray("sheets")
                        val sheetsData = sheetsArray.toString()

                        withContext(Dispatchers.IO) {
                            converter.convert(sheetsData).use { workbook ->
                                getApplication<Application>()
                                    .contentResolver
                                    .openOutputStream(uri, "rwt")
                                    ?.use { stream ->
                                        workbook.write(stream)
                                        stream.flush() //Zip dosyası tam yazılsın
                                        Log.d("ExcelViewModel", "File written successfully to: $uri")
                                    } ?: run {
                                    Log.e("ExcelViewModel", "Could not open output stream for: $uri")
                                    _toastMessage.emit("Dosya yazılamadı.")
                                    return@withContext
                                }
                            }
                            withContext(Dispatchers.Main) {
                                if (isNewFile) {
                                    _selectedFileUri.value = uri
                                    _toastMessage.emit("Dosya farklı kaydedildi.")
                                } else {
                                    _toastMessage.emit("Dosya başarıyla kaydedildi.")
                                }
                            }
                        }
                    } else {
                        val error = jsonData.optString("error", "Bilinmeyen hata")
                        _toastMessage.emit("Hata: $error")
                    }
                } catch (e: Exception) {
                    Log.e("ExcelViewModel", "Save error", e)
                    _toastMessage.emit("Kayıt hatası: ${e.message}")
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }


    fun clearSelectedFile() {
        _selectedFileUri.value = null
        _excelJsonData.value = null
        _isLoading.value = false
        currentWebView = null
    }



    private fun loadDataToWebView(webView: WebView, jsonData: String) {
        val safeJsonData = jsonData
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        val jsCode = """
            try {
                console.log('📥 Loading Excel data...');
                if (window.ExcelEditorAPI && window.ExcelEditorAPI.loadData) {
                    var result = window.ExcelEditorAPI.loadData('$safeJsonData');
                    console.log('✅ Load result:', result);
                } else {
                    console.error('❌ ExcelEditorAPI not found');
                }
            } catch (e) {
                console.error('❌ JavaScript error:', e);
            }
        """.trimIndent()

        webView.evaluateJavascript(jsCode) { result ->
            Log.d("ExcelViewModel", "Load result: $result")
        }
    }

    fun setIsDocumentModified(flag: Boolean) {
        _isDocumentModified.value = flag
    }
}