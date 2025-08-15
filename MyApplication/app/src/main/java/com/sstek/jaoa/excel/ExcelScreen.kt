package com.sstek.jaoa.excel

import android.net.Uri
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelScreen(
    filePath: Uri?,
    onBack: () -> Unit,
    viewModel: ExcelViewModel = viewModel()
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }


    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFileUri by viewModel.selectedFileUri.collectAsState()


    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveAsExcelFile(webView, uri)
        }
    }


    var showExitDialog by remember { mutableStateOf(false) }


    LaunchedEffect(filePath) {
        if (filePath != null) {
            viewModel.setSelectedFile(filePath)
        } else {
            viewModel.clearSelectedFile()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Text(if (selectedFileUri != null) "Excel Düzenleyici" else "Yeni Excel Dosyası")
            },
            navigationIcon = {
                IconButton(onClick = {

                    if (viewModel.hasUnsavedChanges()) {
                        showExitDialog = true
                    } else {
                        viewModel.clearSelectedFile()
                        onBack()
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, "Geri")
                }
            },
            actions = {

                IconButton(
                    onClick = {
                        if (selectedFileUri != null) {
                            val uriString = selectedFileUri.toString()
                            if (uriString.isNotBlank() && uriString != "new") {
                                // Mevcut dosyayı kaydet
                                viewModel.saveExcelFile(webView)
                            } else {
                                // Yeni dosya - farklı kaydet dialog'u aç
                                createDocumentLauncher.launch("Excel_Dosyasi.xlsx")
                            }
                        } else {
                            // URI yok - farklı kaydet dialog'u aç
                            createDocumentLauncher.launch("Excel_Dosyasi.xlsx")
                        }
                    },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Filled.Save, "Kaydet")
                }

                // ✅ Save As butonu
                IconButton(
                    onClick = {
                        createDocumentLauncher.launch("Excel_Dosyasi.xlsx")
                    },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Filled.SaveAs, "Farklı Kaydet")
                }
            }
        )


        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Yükleniyor...")
                }
            }
        }

        // WebView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webView = this

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            Log.d("ExcelScreen", "WebView page finished")
                        }
                    }

                    addJavascriptInterface(object {
                        @android.webkit.JavascriptInterface
                        fun saveData(jsonData: String) {
                            android.util.Log.d("ExcelEditor", "Save data called: ${jsonData.take(100)}...")
                        }

                        @android.webkit.JavascriptInterface
                        fun log(message: String) {
                            android.util.Log.d("ExcelEditor", "JS: $message")
                        }


                        @android.webkit.JavascriptInterface
                        fun onEditorReady() {
                            android.util.Log.d("ExcelEditor", "📱 Ready event received from JavaScript")
                            viewModel.onWebViewReady(this@apply) // ✅ WebView referansını gönder
                        }
                    }, "AndroidInterface")

                    loadUrl("file:///android_asset/excel_editor/simple_editor.html")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }


    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Kaydedilmemiş Değişiklikler") },
            text = { Text("Değişiklikleriniz kaydedilmemiş. Çıkmak istediğinizden emin misiniz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        viewModel.clearSelectedFile()
                        onBack()
                    }
                ) {
                    Text("Çık")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }


    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            if (message.isNotEmpty()) {
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}