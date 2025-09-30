package com.sstek.jaoa.powerpoint

import IconButtonWithTooltip
import android.app.Activity
import android.net.Uri
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.sstek.jaoa.word.MyWebChromeClient // WebViewClient için gerekiyorsa
import kotlinx.coroutines.flow.collectLatest
import android.util.Log

@Composable
fun PowerpointEditorScreen(
    filePath: Uri?,
    onBack: () -> Unit,
    viewModel: PowerpointViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    val scrollState = rememberScrollState()
    var webView: WebView? by remember { mutableStateOf(null) }

    // Dosya seçici ve kaydetme launcher'ları artık GEREKSİZ, kaldırıldı.

    // Sadece ChromeClient tanımı kaldı
    val myWebChromeClientInstance = remember { MyWebChromeClient(activity) { /* Artık boş */ } }
    var myWebChromeClient: MyWebChromeClient? by remember { mutableStateOf(myWebChromeClientInstance) }

    JAOATheme {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            // Toolbar: Sadece Geri ve Paylaş butonları
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Geri Butonu
                IconButtonWithTooltip(Icons.Default.ArrowBack, R.string.editorscreen_tooltipBack) { onBack() }

                Spacer(Modifier.weight(1f)) // Sağ hizalama için araya boşluk

                // Paylaş Butonu (Eğer bir dosya URI'si varsa)
                IconButtonWithTooltip(Icons.Default.Share, R.string.editorscreen_tooltipShare) {
                    viewModel.selectedFileUri.value?.let { uri ->
                        shareDocument(
                            context,
                            uri,
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            getFileName(context, uri)
                        )
                    } ?: Toast.makeText(context, "Dosya paylaşım için hazır değil.", Toast.LENGTH_SHORT).show()
                }
            }

            // WebView
            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true

                        webChromeClient = myWebChromeClient

                        addJavascriptInterface(object {
                            // Kaydetme ve Dosya Açma arayüzleri kaldırıldı (Görüntüleyici)
                            @android.webkit.JavascriptInterface
                            fun receiveFile(base64: String) { /* Boş */ }
                            @android.webkit.JavascriptInterface
                            fun openFilePicker() { /* Boş */ }
                        }, "AndroidInterface")

                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                try {
                                    filePath?.let {
                                        // 1. URI'den Base64'e çevir
                                        val base64 = viewModel.uriToBase64(it)
                                        val fileName = getFileName(context, it)

                                        // 2. HTML'deki global fonksiyonu çağırarak pptxjs'i Base64 ile yükle
                                        view?.evaluateJavascript("window.loadPptxFromBase64('$base64', '$fileName');", null)

                                        viewModel.setSelectedFileUri(it)
                                    }
                                } catch (e: Exception) {
                                    Log.e("PowerpointViewer", "File load error: ${e.message}", e)
                                    Toast.makeText(context, context.getString(R.string.wordViewModel_fileLoadErrorMessage), Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        loadUrl("file:///android_asset/powerpoint_editor/powerpoint_editor.html")
                        webView = this
                    }
                }
            )
        }
    }

    // Toast
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            if (message.isNotEmpty()) Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}