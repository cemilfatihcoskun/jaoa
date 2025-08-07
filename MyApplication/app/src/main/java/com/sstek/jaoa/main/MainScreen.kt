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
        //getDocxFilesWithFileApi(context)
        getDocxFilesWithFileApi(context)
    }
}

fun getDocxFilesWithMediaStore(context: Context): List<Pair<String, Uri>> {
    val docxList = mutableListOf<Pair<String, Uri>>()
    val uri = MediaStore.Files.getContentUri("external")
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME
    )
    val selection = "LOWER(${MediaStore.Files.FileColumns.DISPLAY_NAME}) LIKE ?"
    val selectionArgs = arrayOf("%.docx")
    val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

    context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val contentUri = Uri.withAppendedPath(uri, id.toString())
            docxList += name to contentUri
        }
    }

    return docxList
}


fun getDocxFilesWithFileApi(context: Context): List<Pair<String, Uri>> {
    val docxList = mutableListOf<Triple<String, Uri, String>>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
        Log.w("DocxFiles", "MANAGE_EXTERNAL_STORAGE required for File API")
        Toast.makeText(context, "Tüm dosyalara erişim izni gerekli.", Toast.LENGTH_LONG).show()
        return emptyList()
    }

    try {
        // Uygulama dizinleri
        context.getExternalFilesDirs(null).forEach { storageDir ->
            if (storageDir != null && storageDir.exists()) {
                Log.d("DocxFiles", "Scanning storage: ${storageDir.absolutePath}")
                storageDir.walkTopDown().forEach { file ->
                    if (file.isFile && file.name.endsWith(".docx", ignoreCase = true)) {
                        docxList += Triple(file.name, Uri.fromFile(file), file.absolutePath)
                    }
                }
            }
        }

        // Yaygın dış dizinler
        val extraDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        )

        extraDirs.forEach { dir ->
            if (dir.exists()) {
                Log.d("DocxFiles", "Scanning extra dir: ${dir.absolutePath}")
                dir.walkTopDown().forEach { file ->
                    if (file.isFile && file.name.endsWith(".docx", ignoreCase = true)) {
                        docxList += Triple(file.name, Uri.fromFile(file), file.absolutePath)
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("DocxFiles", "File API scan failed: ${e.message}", e)
        Toast.makeText(context, "Dosyalar yüklenirken hata: ${e.message}", Toast.LENGTH_LONG).show()
    }

    // Yolu baz alarak tekrarı kaldır
    return docxList.distinctBy { it.third }
        .map { it.first to it.second }
}
