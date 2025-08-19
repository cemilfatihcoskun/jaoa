package com.sstek.jaoa.core

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    var showNewFileMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<FileType?>(null) } // null = tümü

    val filePickerLauncher = rememberFilePickerLauncher(context, mutableStateOf(files), onOpenFile)

    if (!CheckStoragePermissionWithExplanation()) {
        Log.d("MainScreen", "Give storage permission for all documents and files.")
    }

    val extensions = listOf(FileType.DOCX, FileType.XLSX)
    val allFiles = getAllFilesByExtensions(context, extensions)
    files = allFiles.filter { (name, _) ->
        (searchQuery.isBlank() || name.contains(searchQuery, ignoreCase = true)) &&
                (selectedFilter == null || FileType.fromFileName(name) == selectedFilter)
    }

    JAOATheme {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        //.background(MaterialTheme.colorScheme.primary) // üst bar rengi
                        .statusBarsPadding() // status barın altından başlasın
                        .padding(8.dp)
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        placeholder = { Text("Ara...") },
                        shape = RoundedCornerShape(30), // yuvarlak
                        singleLine = true
                    )

                    // Filter buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterButton(
                            label = "Tümü",
                            icon = Icons.Default.FolderOpen,
                            isSelected = selectedFilter == null,
                            onClick = { selectedFilter = null }
                        )
                        FilterButton(
                            label = "Docx",
                            icon = Icons.Default.Description, // DOCX için uygun ikon ekleyebilirsin
                            isSelected = selectedFilter == FileType.DOCX,
                            onClick = { selectedFilter = FileType.DOCX }
                        )
                        FilterButton(
                            label = "Xlsx",
                            icon = Icons.Default.TableChart, // XLSX için uygun ikon ekle
                            isSelected = selectedFilter == FileType.XLSX,
                            onClick = { selectedFilter = FileType.XLSX }
                        )
                    }
                }
            },
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
                    FloatingActionButton(
                        onClick = { showNewFileMenu = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Yeni dosya")
                    }

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
                    var expandedMenu by remember { mutableStateOf(false) } // üç nokta menüsü
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable {
                                val fileType = FileType.fromFileName(name)
                                onOpenFile(fileType, uri)
                            }
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            // Dosya adı
                            Text(
                                text = name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // 3 nokta menüsü
                            Box() {
                                IconButton(onClick = { expandedMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Daha fazla"
                                    )
                                }

                                DropdownMenu(
                                    expanded = expandedMenu,
                                    onDismissRequest = { expandedMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Yeniden Adlandır") },
                                        onClick = {
                                            expandedMenu = false
                                            // Yeniden adlandırma işlemini burada yap
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Paylaş") },
                                        onClick = {
                                            expandedMenu = false
                                            // Paylaşma işlemini burada yap
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sil") },
                                        onClick = {
                                            expandedMenu = false
                                            // Silme işlemini burada yap
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterButton(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
