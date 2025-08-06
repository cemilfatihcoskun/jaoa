package com.sstek.jaoa.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import android.util.Log
import java.io.File

@Composable
fun MainScreen(
    onOpenFile: (Uri) -> Unit,
    onCreateNew: () -> Unit
) {
    val context = LocalContext.current
    var docxFiles by remember { mutableStateOf<List<Pair<String, Uri>>>(emptyList()) }

    // MANAGE_EXTERNAL_STORAGE izni için launcher
    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            Log.d("DocxFiles", "MANAGE_EXTERNAL_STORAGE granted")
            Toast.makeText(context, "Tüm dosyalara erişim izni alındı!", Toast.LENGTH_SHORT).show()
            docxFiles = getAllDocxFiles(context)
        } else {
            Log.w("DocxFiles", "MANAGE_EXTERNAL_STORAGE denied")
            Toast.makeText(context, "Tüm dosyalara erişim izni reddedildi. Dosyalar listelenemiyor.", Toast.LENGTH_LONG).show()
        }
    }

    // Dosya seçici için launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val name = DocumentFile.fromSingleUri(context, it)?.name ?: "Unknown"
            Log.d("DocxFiles", "Selected file: $name, Uri: $it")
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            docxFiles = docxFiles + (name to it)
            onOpenFile(it)
        } ?: Log.w("DocxFiles", "No file selected")
    }

    // Uygulama açıldığında izni iste
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Log.d("DocxFiles", "Requesting MANAGE_EXTERNAL_STORAGE permission")
                manageStorageLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            } else {
                Log.d("DocxFiles", "MANAGE_EXTERNAL_STORAGE already granted")
                docxFiles = getAllDocxFiles(context)
            }
        } else {
            Log.d("DocxFiles", "Pre-Android 11, using MediaStore directly")
            docxFiles = getAllDocxFiles(context)
        }
    }

    Scaffold(
        floatingActionButton = {
            Column {
                // Dosya açma düğmesi
                FloatingActionButton(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Dosya aç")
                }
                // Yeni dosya oluşturma düğmesi
                FloatingActionButton(
                    onClick = onCreateNew
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Yeni dosya")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(docxFiles) { (name, uri) ->
                ListItem(
                    headlineContent = { Text(name) },
                    modifier = Modifier
                        .clickable { onOpenFile(uri) }
                        .padding(8.dp)
                )
                Divider()
            }
        }
    }
}

fun getAllDocxFiles(context: Context): List<Pair<String, Uri>> {
    return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
        // Android 12 (API 31-32) için File API'sini kullan
        getDocxFilesWithFileApi(context)
    } else {
        // Android 13+ (API 33+, örneğin Android 15) için MediaStore kullan
        getDocxFilesWithMediaStore(context)
    }
}

fun getDocxFilesWithMediaStore(context: Context): List<Pair<String, Uri>> {
    val docxList = mutableListOf<Triple<String, Uri, String>>()
    val uri = MediaStore.Files.getContentUri("external")
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.DATA
    )
    val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR LOWER(${MediaStore.Files.FileColumns.DISPLAY_NAME}) LIKE ?"
    val selectionArgs = arrayOf(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "%.docx"
    )
    val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

    Log.d("DocxFiles", "Starting MediaStore query with uri: $uri, selection: $selection")
    try {
        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            Log.d("DocxFiles", "Total files found in MediaStore: ${cursor.count}")
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val data = cursor.getString(dataColumn) ?: ""
                val contentUri = Uri.withAppendedPath(uri, id.toString())
                Log.d("DocxFiles", "Found file: $name, Path: $data, Uri: $contentUri")
                docxList += Triple(name, contentUri, data)
            }
        } ?: Log.e("DocxFiles", "MediaStore query returned null cursor")
    } catch (e: Exception) {
        Log.e("DocxFiles", "MediaStore query failed: ${e.message}", e)
        Toast.makeText(context, "Dosyalar yüklenirken hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
    }

    // Dosya yoluna göre tekrarları kaldır ve Pair formatına çevir
    val result = docxList.distinctBy { it.third } // Dosya yoluna göre filtrele
        .map { it.first to it.second } // Pair<String, Uri> formatına çevir

    Log.d("DocxFiles", "Returning ${result.size} files from MediaStore")
    if (result.isEmpty()) {
        Toast.makeText(context, "Cihazda .docx dosyası bulunamadı.", Toast.LENGTH_SHORT).show()
    }
    return result
}

fun getDocxFilesWithFileApi(context: Context): List<Pair<String, Uri>> {
    val docxList = mutableListOf<Triple<String, Uri, String>>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
        Log.w("DocxFiles", "MANAGE_EXTERNAL_STORAGE required for File API")
        Toast.makeText(context, "Tüm dosyalara erişim izni gerekli.", Toast.LENGTH_LONG).show()
        return emptyList()
    }

    Log.d("DocxFiles", "Scanning storage with File API")
    try {
        // Dahili depolama ve SD kart için tüm harici depolama dizinlerini tara
        context.getExternalFilesDirs(null).forEach { storageDir ->
            val parentDir = storageDir.parentFile?.parentFile?.parentFile?.parentFile // Kök dizine ulaş
            if (parentDir != null && parentDir.exists()) {
                Log.d("DocxFiles", "Scanning storage: ${parentDir.absolutePath}")
                parentDir.walkTopDown().forEach { file ->
                    if (file.isFile && file.name.endsWith(".docx", ignoreCase = true)) {
                        val uri = Uri.fromFile(file)
                        Log.d("DocxFiles", "Found file: ${file.name}, Path: ${file.absolutePath}, Uri: $uri")
                        docxList += Triple(file.name, uri, file.absolutePath)
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("DocxFiles", "File API scan failed: ${e.message}", e)
        Toast.makeText(context, "Dosyalar yüklenirken hata: ${e.message}", Toast.LENGTH_LONG).show()
    }

    // Dosya yoluna göre tekrarları kaldır ve Pair formatına çevir
    val result = docxList.distinctBy { it.third } // Dosya yoluna göre filtrele
        .map { it.first to it.second } // Pair<String, Uri> formatına çevir

    Log.d("DocxFiles", "Returning ${result.size} files from File API")
    if (result.isEmpty()) {
        Toast.makeText(context, "Cihazda .docx dosyası bulunamadı.", Toast.LENGTH_SHORT).show()
    }
    return result
}