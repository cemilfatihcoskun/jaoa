package com.sstek.jaoa.editor

import EditorToolbar
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
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
import androidx.hilt.navigation.compose.hiltViewModel
import jp.wasabeef.richeditor.RichEditor
import kotlinx.coroutines.flow.collectLatest


@Composable
fun OldEditorScreen(
    filePath: Uri?,
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val htmlContent by viewModel.htmlContent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOpened by viewModel.selectedFileUri.collectAsState()

    var editorRef by remember { mutableStateOf<RichEditor?>(null) }
    var isEditorInitialized by remember { mutableStateOf(false) }

    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var pageDropdownExpanded by remember { mutableStateOf(false) }

    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri: Uri? ->
        uri?.let {
            val currentHtml = editorRef?.html ?: ""
            viewModel.saveAs(currentHtml, it)
        }
    }

    // Sayfa takip işlemi
    LaunchedEffect(editorRef) {
        while (true) {
            editorRef?.evaluateJavascript(
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
            IconButton(onClick = {
                viewModel.clearSelectedFile()
                onBack()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilePickerButton { uri ->
                viewModel.loadAndConvert(uri)
            }

            Spacer(modifier = Modifier.width(8.dp))



            IconButton(onClick = {
                val currentHtml = editorRef?.html ?: ""
                if (currentHtml.isEmpty()) {
                    Toast.makeText(context, "Boş dosyayı kaydettirme bize!", Toast.LENGTH_SHORT).show()
                    return@IconButton
                }
                viewModel.saveHtmlAsDocx(currentHtml)
            }) {
                Icon(Icons.Filled.Save, "Kaydet")
            }

            IconButton(onClick = {
                val currentHtml = editorRef?.html ?: ""
                if (currentHtml.isEmpty()) {
                    Toast.makeText(context, "Boş dosyayı kaydettirme bize!", Toast.LENGTH_SHORT).show()
                    return@IconButton
                }
                createFileLauncher.launch("document.docx")
            }) {
                Icon(Icons.Filled.SaveAs, "Farklı Kaydet")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Page
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
                                editorRef?.evaluateJavascript(
                                    "window.scrollTo(0, ${(i - 1) * 1123});",
                                    null
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

        Spacer(modifier = Modifier.height(8.dp))

        RichEditorView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            onInstanceReady = { editor ->
                editorRef = editor
                editor.settings.javaScriptEnabled = true
                isEditorInitialized = true

                Handler(Looper.getMainLooper()).postDelayed({
                    val html = viewModel.htmlContent.value
                    if (!html.isNullOrEmpty()) {
                        editor.html = html
                    }
                }, 300)
            }
        )

        EditorToolbar(
            editorRef = editorRef,
            modifier = Modifier.fillMaxWidth()
        )

        LaunchedEffect(htmlContent, isEditorInitialized) {
            if (isEditorInitialized && htmlContent != null) {
                editorRef?.html = htmlContent ?: ""
            }
        }

        LaunchedEffect(Unit) {
            viewModel.toastMessage.collectLatest { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}




