package com.sstek.jaoa.core

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sstek.jaoa.R

// ViewModel
class MainScreenViewModel : ViewModel() {
    var files = mutableStateListOf<Pair<String, Uri>>()
    var showNewFileMenu = mutableStateOf(false)
    var searchQuery = mutableStateOf("")
    var selectedFilter = mutableStateOf<FileType?>(null)
    var selectedTabIndex = mutableStateOf(1) // 0 = Internal, 1 = External
}

@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("UnrememberedMutableState")
@Composable
fun MainScreen(
    onOpenFile: (FileType, Uri) -> Unit,
    onCreateNew: (FileType) -> Unit
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = viewModel()

    val showNewFileMenu by viewModel.showNewFileMenu
    val searchQuery by viewModel.searchQuery
    val selectedFilter by viewModel.selectedFilter
    val selectedTabIndex by viewModel.selectedTabIndex
    val allFiles = viewModel.files

    val hasStoragePermission by remember { mutableStateOf(Environment.isExternalStorageManager()) }

    // Reactive filtered files
    val filteredFiles by derivedStateOf {
        allFiles.filter { (name, _) ->
            (searchQuery.isBlank() || name.contains(searchQuery, ignoreCase = true)) &&
                    (selectedFilter == null || FileType.fromFileName(name) == selectedFilter)
        }
    }

    val updateFiles: () -> Unit = {
        val filesList = if (selectedTabIndex == 0) {
            getInternalFiles(context, listOf(FileType.DOCX, FileType.XLSX))
        } else {
            if (hasStoragePermission) getExternalFiles(context, listOf(FileType.DOCX, FileType.XLSX))
            else emptyList()
        }
        viewModel.files.clear()
        viewModel.files.addAll(filesList)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateFiles()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // File picker launcher (kendi implementasyonunu kullan)
    val filePickerLauncher = rememberFilePickerLauncher(context, onOpenFile)

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
                    val tabs = listOf(
                        context.getString(R.string.mainscreen_internalStorage),
                        context.getString(R.string.mainscreen_externalStorage)
                    )
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { viewModel.selectedTabIndex.value = index },
                                text = { Text(title) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        placeholder = { Text(context.getString(R.string.mainscreen_search)) },
                        shape = RoundedCornerShape(30),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterButton(
                            label = context.getString(R.string.mainscreen_all),
                            icon = Icons.Default.FolderOpen,
                            isSelected = selectedFilter == null
                        ) { viewModel.selectedFilter.value = null }

                        FilterButton(
                            label = "Docx",
                            icon = Icons.Default.Description,
                            isSelected = selectedFilter == FileType.DOCX
                        ) { viewModel.selectedFilter.value = FileType.DOCX }

                        FilterButton(
                            label = "Xlsx",
                            icon = Icons.Default.TableChart,
                            isSelected = selectedFilter == FileType.XLSX
                        ) { viewModel.selectedFilter.value = FileType.XLSX }
                    }
                }
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (hasStoragePermission) {
                                    filePickerLauncher.launch(arrayOf(
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    ))
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.permissionController_permissionNeededMessage),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                        ) { Icon(Icons.Default.FolderOpen, contentDescription = context.getString(R.string.mainscreen_openFile)) }

                        FloatingActionButton(
                            onClick = { viewModel.showNewFileMenu.value = true }
                        ) { Icon(Icons.Default.Add, contentDescription = context.getString(R.string.mainscreen_newFile)) }
                    }

                    DropdownMenu(
                        expanded = showNewFileMenu,
                        onDismissRequest = { viewModel.showNewFileMenu.value = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(context.getString(R.string.mainscreen_wordDocument)) },
                            onClick = {
                                onCreateNew(FileType.DOCX)
                                viewModel.showNewFileMenu.value = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(context.getString(R.string.mainscreen_excelDocument)) },
                            onClick = {
                                onCreateNew(FileType.XLSX)
                                viewModel.showNewFileMenu.value = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(context.getString(R.string.mainscreen_powerpointDocument)) },
                            onClick = {
                                onCreateNew(FileType.PPTX)
                                viewModel.showNewFileMenu.value = false
                            }
                        )
                    }
                }
            },
            bottomBar = {
                val layoutDirection = LocalLayoutDirection.current
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(bottom = 48.dp)
                ) {
                    if (!hasStoragePermission) {
                        PermissionWarningCard()
                    }
                }
            }
        ) { padding ->
            val layoutDirection = LocalLayoutDirection.current
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = padding.calculateBottomPadding(),
                        top = padding.calculateTopPadding(),
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection)
                    )
            ) {
                items(filteredFiles, key = { it.second.toString() }) { (name, uri) ->
                    var expandedMenu by remember { mutableStateOf(false) }
                    FileCard(
                        name = name,
                        uri = uri,
                        onOpenFile = onOpenFile,
                        expandedMenuState = { expandedMenu = it },
                        isInternal = selectedTabIndex == 0,
                        updateFiles = updateFiles
                    )
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
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

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(context.getString(R.string.mainscreen_rename)) },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(context.getString(R.string.mainscreen_newName)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    renameFile(context, uri, newName)
                    showRenameDialog = false
                    updateFiles()
                    Toast.makeText(context, context.getString(R.string.mainscreen_renameSuccessMessage), Toast.LENGTH_SHORT).show()
                }) { Text(context.getString(R.string.mainscreen_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(context.getString(R.string.mainscreen_dismiss)) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(context.getString(R.string.mainscreen_deleteWarningTitle)) },
            text = { Text(context.getString(R.string.mainscreen_deleteWarningMessage)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteFile(context, uri)
                    showDeleteDialog = false
                    updateFiles()
                    Toast.makeText(context, context.getString(R.string.mainscreen_deleteSuccessMessage), Toast.LENGTH_SHORT).show()
                }) { Text(context.getString(R.string.mainscreen_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(context.getString(R.string.mainscreen_dismiss)) }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onOpenFile(FileType.fromFileName(name), uri) }
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp),
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
                    Icon(Icons.Default.MoreVert, contentDescription = context.getString(R.string.mainscreen_more))
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false; expandedMenuState(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text(context.getString(R.string.mainscreen_rename)) },
                        onClick = { expandedMenu = false; showRenameDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text(context.getString(R.string.mainscreen_delete)) },
                        onClick = { expandedMenu = false; showDeleteDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text(context.getString(R.string.mainscreen_share)) },
                        onClick = { expandedMenu = false; shareFile(context, uri, name) }
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
