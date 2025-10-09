package com.sstek.jaoa.excel

import IconButtonWithTooltip
import android.content.Context
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
import com.sstek.jaoa.R
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
    val isDocumentModified = viewModel.isDocumentModified.collectAsState()

    // 1. Yeni durum: Çıkış onaylama iletişim kutusunun gösterilip gösterilmeyeceğini yönetir
    val showExitConfirmationDialog = remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveAsExcelFile(webView, uri)
        }
    }



    LaunchedEffect(filePath) {
        if (filePath != null) {
            viewModel.setSelectedFile(filePath)
        } else {
            viewModel.clearSelectedFile()
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {

        TopAppBar(
            title = {
                Text(
                    if (selectedFileUri != null) {
                        context.resources.getString(R.string.excelscreen_excelEditor)
                    } else {
                        context.resources.getString(R.string.excelscreen_newExcelDocument)
                    }
                )
            },
            navigationIcon = {
                IconButtonWithTooltip(
                    icon = Icons.Default.ArrowBack,
                    contentDescriptionResId = R.string.editorscreen_tooltipBack,
                    onClick = {
                        if (isDocumentModified.value) {
                            showExitConfirmationDialog.value = true
                            return@IconButtonWithTooltip
                        }

                        viewModel.clearSelectedFile()
                        onBack()
                    },
                )
            },
            actions = {

                IconButtonWithTooltip(
                    icon = Icons.Filled.Save,
                    contentDescriptionResId = R.string.editorscreen_tooltipSave,
                    onClick = {
                        if (selectedFileUri != null) {
                            val uriString = selectedFileUri.toString()
                            if (uriString.isNotBlank() && uriString != "new") {
                                viewModel.saveExcelFile(webView)
                            } else {
                                createDocumentLauncher.launch("${context.resources.getString(R.string.excelscreen_defaultDocumentName)}.xlsx")
                            }
                        } else {
                            createDocumentLauncher.launch("${context.resources.getString(R.string.excelscreen_defaultDocumentName)}.xlsx")
                        }
                    },
                )

                IconButtonWithTooltip(
                    icon = Icons.Filled.SaveAs,
                    contentDescriptionResId = R.string.editorscreen_tooltipSaveas,
                    onClick = {
                        createDocumentLauncher.launch("${context.resources.getString(R.string.excelscreen_defaultDocumentName)}.xlsx")
                    },
                )
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
                    Text(context.resources.getString(R.string.excelscreen_loading))
                }
            }
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webView = this

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setSupportZoom(false)
                    settings.builtInZoomControls = false
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
                            android.util.Log.d("ExcelEditor", "Ready event received from JavaScript")
                            viewModel.onWebViewReady(this@apply)
                        }

                        @android.webkit.JavascriptInterface
                        fun setIsDocumentModified(flagAsString: String) {
                            val flag = flagAsString.toBoolean()
                            android.util.Log.d("ExcelEditor", "setIsDocumentModified($flag) event received from JavaScript")
                            viewModel.setIsDocumentModified(flag)
                        }
                    }, "AndroidInterface")

                    loadUrl("file:///android_asset/excel_editor/simple_editor.html")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    if (showExitConfirmationDialog.value) {
        UnsavedChangesAlertDialog(
            onConfirmExit = {
                showExitConfirmationDialog.value = false
                viewModel.clearSelectedFile()
                onBack()
            },
            onDismiss = {
                showExitConfirmationDialog.value = false
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

@Composable
fun UnsavedChangesAlertDialog(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    val context: Context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.resources.getString(R.string.excelscreen_unsavedChangesTitle)) },
        text = { Text(context.resources.getString(R.string.excelscreen_unsavedChangesContent)) },
        confirmButton = {
            TextButton(
                onClick = onConfirmExit
            ) {
                Text(context.resources.getString(R.string.excelscreen_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(context.resources.getString(R.string.excelscreen_cancel))
            }
        }
    )
}
