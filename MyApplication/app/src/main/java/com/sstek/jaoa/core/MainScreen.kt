package com.sstek.jaoa.core

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@SuppressLint("UnrememberedMutableState")
@Composable
fun MainScreen(
    onOpenFile: (FileType, Uri) -> Unit,
    onCreateNew: (FileType) -> Unit
) {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<Pair<String, Uri>>>(emptyList()) }
    var showNewFileMenu by remember { mutableStateOf(false) } // Menü görünürlük durumu

    val filePickerLauncher = rememberFilePickerLauncher(context, mutableStateOf(files), onOpenFile)

    if (!checkStoragePermissionAndGiveIfNotExists()) {
        Log.d("MainScreen", "Give storage permission for all documents and files.")
    }

    val extensions = listOf(FileType.DOCX, FileType.XLSX)
    files = getAllFilesByExtensions(context, extensions)

    Scaffold(
        floatingActionButton = {
            Column {
                FloatingActionButton(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Dosya aç")
                }
                // Yeni dosya düğmesine tıklanınca menüyü aç
                FloatingActionButton(
                    onClick = { showNewFileMenu = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Yeni dosya")
                }

                // Dropdown menu (Yeni dosya seçenekleri)
                DropdownMenu(
                    expanded = showNewFileMenu,
                    onDismissRequest = { showNewFileMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Word Dosyası") },
                        onClick = {
                            onCreateNew(FileType.DOCX)
                            showNewFileMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Excel Dosyası") },
                        onClick = {
                            onCreateNew(FileType.XLSX)
                            showNewFileMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("PowerPoint Dosyası") },
                        onClick = {
                            onCreateNew(FileType.PPTX)
                            showNewFileMenu = false
                        }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(files) { (name, uri) ->
                ListItem(
                    headlineContent = { Text(name) },
                    modifier = Modifier
                        .clickable {
                            val fileType = FileType.fromFileName(name)
                            onOpenFile(fileType, uri)
                        }
                        .padding(8.dp)
                )
                Divider()
            }
        }
    }
}
