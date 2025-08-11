package com.sstek.jaoa.word

import android.app.Activity
import android.net.Uri
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject

@Composable
fun QuillEditorScreen(
    filePath: Uri?,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    val htmlContent by viewModel.htmlContent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var webView: WebView? by remember { mutableStateOf(null) }

    var currentPage by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var pageDropdownExpanded by remember { mutableStateOf(false) }

    // Dosya seçici launcher (Compose ile)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val client = webView?.webChromeClient
        if (client is MyWebChromeClient) {
            client.uploadMessage?.onReceiveValue(uri?.let { arrayOf(it) } ?: arrayOf())
            client.uploadMessage = null
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri: Uri? ->
        if (uri != null) {
            webView?.evaluateJavascript("window.getHtmlContent();") { html ->
                val jsonWrapped = "{ \"data\": $html }"
                val obj = JSONObject(jsonWrapped)
                val decodedHtml = obj.getString("data")
                viewModel.saveAs(decodedHtml, uri)
            }
        }
    }

    // MyWebChromeClient örneği, file chooser açma fonksiyonunu veriyoruz
    val myWebChromeClient = remember {
        MyWebChromeClient(activity) {
            // Dosya seçici aç
            filePickerLauncher.launch("image/*")
        }
    }

    LaunchedEffect(webView) {
        while (true) {
            webView?.evaluateJavascript(
                """
                (function() {
                    const pageHeight = 1123;
                    const totalHeight = document.body.scrollHeight;
                    const scrollTop = window.scrollY;
                    const currentPage = Math.floor(scrollTop / pageHeight) + 1;
                    const totalPages = Math.ceil(totalHeight / pageHeight);
                    return [currentPage, totalPages];
                })();
                """.trimIndent()
            ) { result ->
                try {
                    val numbers = result
                        .removePrefix("[")
                        .removeSuffix("]")
                        .split(",")
                        .map { it.trim().toInt() }
                    currentPage = numbers[0]
                    totalPages = numbers[1]
                } catch (_: Exception) {}
            }
            kotlinx.coroutines.delay(500)
        }
    }

    LaunchedEffect(filePath) {
        if (filePath != null) {
            viewModel.loadAndConvert(filePath)
        } else {
            viewModel.clearSelectedFile()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.clearSelectedFile()
                onBack()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = {
                webView?.evaluateJavascript("quill.history.undo();", null)
            }) {
                Icon(Icons.Default.Undo, contentDescription = "Geri Al")
            }

            IconButton(onClick = {
                webView?.evaluateJavascript("quill.history.redo();", null)
            }) {
                Icon(Icons.Default.Redo, contentDescription = "İleri Al")
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = {

                if (viewModel.selectedFileUri.value != null) {
                    webView?.evaluateJavascript("window.getHtmlContent();") { html ->
                        val jsonWrapped = "{ \"data\": $html }"
                        val obj = JSONObject(jsonWrapped)
                        val decodedHtml = obj.getString("data")
                        viewModel.saveHtmlAsDocx(decodedHtml)
                    }
                } else {
                    createDocumentLauncher.launch("Dosya.docx")
                }

            }) {
                Icon(Icons.Filled.Save, contentDescription = "Kaydet")
            }

            IconButton(onClick = {
                createDocumentLauncher.launch("Dosya.docx")
            }) {
                Icon(Icons.Filled.SaveAs, contentDescription = "Farklı Kaydet")
            }

            Spacer(modifier = Modifier.weight(1f))

            Box {
                Button(onClick = { pageDropdownExpanded = true }) {
                    Text("$currentPage/$totalPages")
                }
                DropdownMenu(
                    expanded = pageDropdownExpanded,
                    onDismissRequest = { pageDropdownExpanded = false }
                ) {
                    for (i in 1..totalPages) {
                        DropdownMenuItem(
                            text = { Text("$i") },
                            onClick = {
                                webView?.evaluateJavascript(
                                    "window.scrollTo(0, ${(i - 1) * 1123});", null
                                )
                                pageDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            factory = { ctx ->
                WebView(ctx).apply {
                    fitsSystemWindows = true

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true

                    webChromeClient = myWebChromeClient
                    webViewClient = QuillWebViewClient {
                        val content = htmlContent ?: ""
                        val escaped = content.replace("\"", "\\\"").replace("\n", "\\n")
                        evaluateJavascript("window.setHtmlContent(\"$escaped\")", null)
                    }

                    addJavascriptInterface(object {
                        @android.webkit.JavascriptInterface
                        fun receiveHtml(html: String) {
                            viewModel.updateHtmlContent(html)
                        }
                    }, "AndroidInterface")

                    //addJavascriptInterface(AndroidWebInterface(this, htmlContent ?: ""), "AndroidInterface")
                    loadUrl("file:///android_asset/quill_editor.html")
                    webView = this
                }
            },
            update = { wv ->
                val content = htmlContent ?: ""
                val escaped = content.replace("\"", "\\\"").replace("\n", "\\n")
                wv.evaluateJavascript("window.setHtmlContent(\"$escaped\")", null)
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
