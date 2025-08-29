package com.sstek.jaoa.core

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.widget.Toast
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

import com.sstek.jaoa.R

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
    var selectedTabIndex by remember { mutableStateOf(0) } // 0 = Internal, 1 = External

    val extensions = listOf(FileType.DOCX, FileType.XLSX)

    val updateFiles: () -> Unit = {
        val allFiles = if (selectedTabIndex == 0) {
            getInternalFiles(context, extensions)
        } else {
            getExternalFiles(context, extensions)
        }

        files = allFiles.filter { (name, _) ->
            (searchQuery.isBlank() || name.contains(searchQuery, ignoreCase = true)) &&
                    (selectedFilter == null || FileType.fromFileName(name) == selectedFilter)
        }
    }

    val filePickerLauncher = rememberFilePickerLauncher(
        context,
        mutableStateOf(files),
        onOpenFile
    )

    // Dosyaları güncelle
    LaunchedEffect(selectedTabIndex, searchQuery, selectedFilter) {
        updateFiles()
    }

    JAOATheme {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                ) {
                    // TabRow: Internal / External


                    val tabs = listOf(
                        context.resources.getString(R.string.mainscreen_internalStorage),
                        context.resources.getString(R.string.mainscreen_externalStorage)
                    )
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }

                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        placeholder = { Text(context.resources.getString(R.string.mainscreen_search)) },
                        shape = RoundedCornerShape(30),
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
                            label = context.resources.getString(R.string.mainscreen_all),
                            icon = Icons.Default.FolderOpen,
                            isSelected = selectedFilter == null,
                            onClick = { selectedFilter = null }
                        )
                        FilterButton(
                            label = "Docx",
                            icon = Icons.Default.Description,
                            isSelected = selectedFilter == FileType.DOCX,
                            onClick = { selectedFilter = FileType.DOCX }
                        )
                        FilterButton(
                            label = "Xlsx",
                            icon = Icons.Default.TableChart,
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
                            // Sadece external storage için dosya picker
                            if (selectedTabIndex == 1) {
                                // MIME tipini DOCX ile sınırla

                                filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                            }
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = context.resources.getString(R.string.mainscreen_openFile))
                    }
                    FloatingActionButton(
                        onClick = { showNewFileMenu = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = context.resources.getString(R.string.mainscreen_newFile))
                    }

                    DropdownMenu(
                        expanded = showNewFileMenu,
                        onDismissRequest = { showNewFileMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(context.resources.getString(R.string.mainscreen_wordDocument)) },
                            onClick = {
                                onCreateNew(FileType.DOCX)
                                showNewFileMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(context.resources.getString(R.string.mainscreen_excelDocument)) },
                            onClick = {
                                onCreateNew(FileType.XLSX)
                                showNewFileMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(context.resources.getString(R.string.mainscreen_powerpointDocument)) },
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
                    var expandedMenu by remember { mutableStateOf(false) }
                    FileCard(
                        name,
                        uri,
                        onOpenFile,
                        expandedMenuState = { expandedMenu = it },
                        isInternal = selectedTabIndex == 0,
                        updateFiles
                    )
                }
            }
        }
    }
}



@Composable
fun FileCard(
    name: String,
    uri: Uri,
    onOpenFile: (FileType, Uri) -> Unit,
    expandedMenuState: (Boolean) -> Unit,
    isInternal: Boolean = false,
    updateFiles: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(name) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(context.resources.getString(R.string.mainscreen_rename)) },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(context.resources.getString(R.string.mainscreen_newName)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    renameFile(context, uri, newName)
                    showRenameDialog = false
                    updateFiles()
                    Toast.makeText(context, context.resources.getString(R.string.mainscreen_renameSuccessMessage), Toast.LENGTH_SHORT).show()
                }) {
                    Text(context.resources.getString(R.string.mainscreen_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(context.resources.getString(R.string.mainscreen_dismiss))
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable {
                val fileType = FileType.fromFileName(name)
                onOpenFile(fileType, uri)
            }
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            Box {
                IconButton(onClick = { expandedMenu = true; expandedMenuState(true) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = context.resources.getString(R.string.mainscreen_more))
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false; expandedMenuState(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text(context.resources.getString(R.string.mainscreen_rename)) },
                        onClick = {
                            expandedMenu = false
                            showRenameDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(context.resources.getString(R.string.mainscreen_share)) },
                        onClick = {
                            expandedMenu = false
                            shareFile(context, uri)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(context.resources.getString(R.string.mainscreen_delete)) },
                        onClick = {
                            expandedMenu = false
                            deleteFile(context, uri)
                            updateFiles()
                        }
                    )
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