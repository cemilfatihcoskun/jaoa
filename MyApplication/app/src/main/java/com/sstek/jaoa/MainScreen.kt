package com.sstek.jaoa

import EditorToolbar
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import jp.wasabeef.richeditor.RichEditor
import kotlinx.coroutines.flow.collectLatest


@Composable
fun MainScreen(viewModel: MainViewModel) {
    val htmlContent by viewModel.htmlContent.collectAsState()
    var editorRef by remember { mutableStateOf<RichEditor?>(null) }
    var isEditorInitialized by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val isOpened by viewModel.selectedFileUri.collectAsState()



    val context = LocalContext.current

    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri: Uri? ->
        uri?.let {
            val currentHtml = editorRef?.html ?: ""
            viewModel.saveAs(currentHtml, it)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilePickerButton { uri ->
                viewModel.loadAndConvert(uri)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = {
                if (htmlContent == null) {
                    Toast.makeText(context, "Açılmamış dosyayı kaydettirme bize!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val currentHtml = editorRef?.html ?: ""

                if (currentHtml == "") {
                    Toast.makeText(context, "Boş dosyayı kaydettirme bize!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                viewModel.saveHtmlAsDocx(currentHtml)
            }) {
                Text("Kaydet")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = {
                val currentHtml = editorRef?.html ?: ""
                if (currentHtml == "") {
                    Toast.makeText(context, "Boş dosyayı kaydettirme bize!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                createFileLauncher.launch("test.docx")
            }) {
                Text("Farklı Kaydet")
            }
        }


        if (isLoading) {
            Log.d("MainScreen", "hey")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }


        Spacer(modifier = Modifier.height(8.dp))


        RichEditorView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.Black),
            onInstanceReady = { editor ->
                editorRef = editor
                editor.settings.javaScriptEnabled = true
                isEditorInitialized = true
                // biraz gecikme ile HTML set edelim
                Handler(Looper.getMainLooper()).postDelayed({
                    val html = viewModel.htmlContent.value
                    if (!html.isNullOrEmpty()) {
                        editor.html = html
                    }
                }, 300)  // 300 ms gibi küçük bir gecikme
            }
        )



        EditorToolbar(editorRef)

        // 👇 Dosya yüklendikten sonra editör hazırsa HTML yükle
        LaunchedEffect(htmlContent, isEditorInitialized) {
            if (isEditorInitialized && htmlContent != null) {
                val cleanedHtml = htmlContent ?: ""
                editorRef?.html = cleanedHtml
                Log.d("MainScreen", "htmlContent değişti: $htmlContent")
            }
        }



        LaunchedEffect(Unit) {
            viewModel.toastMessage.collectLatest { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}



