package com.sstek.jaoa.word

import IconButtonWithTooltip
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sstek.jaoa.R
import com.sstek.jaoa.core.JAOATheme
import com.sstek.jaoa.core.getFileName
import com.sstek.jaoa.core.shareDocument
import com.sstek.jaoa.word.utils.htmlPrint
import kotlinx.coroutines.flow.collectLatest
import java.io.InputStream

@Composable
fun SuperDocEditorScreen(
    filePath: Uri?,
    onBack: () -> Unit,
    viewModel: WordViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    var webView: WebView? by remember { mutableStateOf(null) }

    var currentPage by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var pageDropdownExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Dosya seçici launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val client = webView?.webChromeClient
            if (client is MyWebChromeClient) {
                client.uploadMessage?.onReceiveValue(arrayOf(it))
                client.uploadMessage = null
            }
        }
    }

    // "Farklı kaydet" için dosya oluşturucu
    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    ) { uri: Uri? ->
        uri?.let {
            // Seçilen URI'yi ViewModel'e kaydet
            viewModel.setSelectedFileUri(it)
            // Seçildikten sonra kaydetme işlemini tetikle
            webView?.evaluateJavascript("window.saveDocumentToAndroid();", null)
        }
    }


    val myWebChromeClient = remember {
        MyWebChromeClient(activity) {
            filePickerLauncher.launch("*/*")
        }
    }

    // Sayfa takibi (scroll → page hesaplama)
    LaunchedEffect(webView) {
        while (true) {
            webView?.evaluateJavascript(
                """
                (function() {
                    if (window.superdoc == null) {
                        return [1, 1];
                    }
                    
                    const editorEl = document.querySelector('#editor');
                    if (editorEl === null) {
                        return [1, 1];
                    }
                    
                    if (window.superdoc.activeEditor == null) {
                        return [1, 1];
                    }
                    
                    const pageHeight = 1123;
                    const totalHeight = editorEl.scrollHeight;
                    const scrollTop = editorEl.scrollTop || 0;
                    const currentPage = Math.floor(scrollTop / pageHeight) + 1;
                    
                    const totalPages = window.superdoc.activeEditor.currentTotalPages;
                    return [currentPage, totalPages];
                })();
                """
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

    // URI → Base64
    fun uriToBase64(uri: Uri?): String {
        Log.d("SuperDocEditorScreen", "uri=$uri")
        if (uri == null || uri.toString().isEmpty()) {
            return android.util.Base64.encodeToString("".toByteArray(), android.util.Base64.NO_WRAP)
        }
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    fun getFileNameFromUri(uri: Uri): String {
        var name = "document.docx"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
        return name
    }


    JAOATheme {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp)
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonWithTooltip(
                    Icons.Default.ArrowBack, R.string.editorscreen_tooltipBack,
                    onClick = {
                        onBack()
                    },
                )

                Spacer(Modifier.width(8.dp))

                IconButtonWithTooltip(
                    Icons.Default.Undo, R.string.editorscreen_tooltipUndo,
                    onClick = {
                        webView?.evaluateJavascript("superdoc.activeEditor.commands.undo() ;", null)
                    },
                )
                IconButtonWithTooltip(
                    Icons.Default.Redo, R.string.editorscreen_tooltipRedo,
                    onClick = {
                        webView?.evaluateJavascript("superdoc.activeEditor.commands.redo() ;", null)
                    },
                )

                Spacer(Modifier.width(8.dp))

                // Sayfa seçici dropdown
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
                                        "document.querySelector('#editor').scrollTop = ${(i - 1) * 1123};",
                                        null
                                    )
                                    pageDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Kaydet
                IconButtonWithTooltip(
                    Icons.Default.Save, R.string.editorscreen_tooltipSave,
                    onClick = {
                        val uri = viewModel.selectedFileUri.value
                        if (uri != null) {
                            // Dosya zaten varsa direkt kaydet
                            webView?.evaluateJavascript("window.saveDocumentToAndroid();", null)
                        } else {
                            // Dosya yoksa farklı kaydet akışı başlat
                            val documentName = context.getString(R.string.wordscreen_defaultDocumentName)
                            createDocumentLauncher.launch("${documentName}.docx")
                        }
                    }
                )

                // Farklı kaydet
                IconButtonWithTooltip(
                    Icons.Default.SaveAs, R.string.editorscreen_tooltipSaveas,
                    onClick = {
                        val uri = viewModel.selectedFileUri.value
                        var documentName = "${context.getString(R.string.wordscreen_defaultDocumentName)}.docx"
                        if (uri != null) {
                            documentName = getFileNameFromUri(uri)
                        }

                        createDocumentLauncher.launch(documentName)
                    }
                )


                // Yazdır
                IconButtonWithTooltip(
                    Icons.Default.Print, R.string.editorscreen_tooltipPrint,
                    onClick = {
                        webView?.let { htmlPrint(it, context) }
                            ?: Toast.makeText(context, "Document not ready", Toast.LENGTH_SHORT).show()
                    },
                )

                // Paylaş
                IconButtonWithTooltip(
                    Icons.Default.Share, R.string.editorscreen_tooltipShare,
                    onClick = {
                        viewModel.selectedFileUri.value?.let { uri ->
                            shareDocument(
                                context,
                                uri,
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                getFileName(context, uri)
                            )
                        } ?: Toast.makeText(context, "Share works after save", Toast.LENGTH_SHORT).show()
                    },
                )
            }

            // WebView
            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    //.clipToBounds()
                ,
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.textZoom = 100
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.setSupportZoom(true)


                        // Scale ayarları
                        setInitialScale(100) // 60 yerine 100 kullan

                        webChromeClient = myWebChromeClient

                        //settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING

                        addJavascriptInterface(object {
                            @android.webkit.JavascriptInterface
                            fun receiveFile(base64: String) {
                                val uri = viewModel.selectedFileUri.value
                                Log.d("WordEditorScreen", "receiveFile() ${uri}")
                                if (uri != null) viewModel.saveAs(base64, uri)
                            }
                        }, "AndroidInterface")

                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                Log.d("WordEditorScreen", "onPageFinished() ${filePath != null} ${filePath}");
                                if (filePath.toString().equals("")) {
                                    val base64 = loadTemplateFromAssets(context)
                                    val fileName = context.getString(R.string.wordscreen_defaultDocumentName)
                                    view?.evaluateJavascript(
                                        "window.loadDocxFromBase64('$base64', '$fileName');",
                                        null
                                    )
                                    return
                                }

                                try {
                                    val base64 = uriToBase64(filePath)
                                    val fileName = getFileNameFromUri(filePath!!)

                                    view?.evaluateJavascript(
                                        "window.loadDocxFromBase64('$base64', '$fileName');",
                                        null
                                    )
                                    viewModel.setSelectedFileUri(filePath)
                                } catch (e: Exception) {
                                    Log.d("WordEditorScreen", "${e.message}")
                                }
                            }
                        }

                        loadUrl("file:///android_asset/word_editor/superdoc_editor.html")
                        webView = this
                    }
                }
            )

            TextFormatToolbar(webView, viewModel)
        }
    }

    // Toast mesajları
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun loadTemplateFromAssets(context: Context): String {
    val inputStream = context.assets.open("word_editor/template.docx")
    val bytes = inputStream.readBytes()
    inputStream.close()
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}
