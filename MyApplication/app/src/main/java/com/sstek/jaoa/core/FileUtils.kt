package com.sstek.jaoa.core

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.File

fun isNotTrashed(file: File): Boolean {
    val path = file.absolutePath.lowercase()
    val name = file.name.lowercase()
    return !path.contains("/.trash") &&
            !path.contains("/.local/share/trash") &&
            !name.startsWith(".trashed")
}


fun getDocxFilesWithFileApi(context: Context, extensions: List<FileType>): List<Pair<String, Uri>> {
    val docxList = mutableListOf<Triple<String, Uri, String>>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
        Log.w("FileUtils", "MANAGE_EXTERNAL_STORAGE required for File API")
        Toast.makeText(context, "Tüm dosyalara erişim izni gerekli.", Toast.LENGTH_LONG).show()
        return emptyList()
    }

    try {
        context.getExternalFilesDirs(null).forEach { storageDir ->
            storageDir?.takeIf { it.exists() }?.let { dir ->
                Log.d("FileUtils", "Scanning storage: ${dir.absolutePath}")
                dir.walkTopDown().forEach { file ->
                    // Çöp kutusunu filtrele
                    if (file.isFile &&
                        extensions.any { ext -> file.name.endsWith(".${ext.extension}", ignoreCase = true) } &&
                        isNotTrashed(file)
                    ) {
                        docxList += Triple(file.name, Uri.fromFile(file), file.absolutePath)
                    }
                }
            }
        }

        val extraDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        )

        extraDirs.forEach { dir ->
            if (dir.exists()) {
                Log.d("FileUtils", "Scanning extra dir: ${dir.absolutePath}")
                dir.walkTopDown().forEach { file ->
                    if (file.isFile &&
                        extensions.any { ext -> file.name.endsWith(".${ext.extension}", ignoreCase = true) } &&
                        !file.absolutePath.contains("/.Trash") &&
                        !file.absolutePath.contains("/.Trash-") &&
                        !file.absolutePath.contains("/.local/share/Trash")
                    ) {
                        docxList += Triple(file.name, Uri.fromFile(file), file.absolutePath)
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("FileUtils", "File API scan failed: ${e.message}", e)
        Toast.makeText(context, "Dosyalar yüklenirken hata: ${e.message}", Toast.LENGTH_LONG).show()
    }

    return docxList.distinctBy { it.third }.map { it.first to it.second }
}


fun getDocxFilesWithMediaStore(context: Context, extensions: List<FileType>): List<Pair<String, Uri>> {
    val docxList = mutableListOf<Pair<String, Uri>>()
    val uri = MediaStore.Files.getContentUri("external")
    val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME)

    // selection = "LOWER(display_name) LIKE ? OR LOWER(display_name) LIKE ? OR ..."
    val selection = extensions.joinToString(separator = " OR ") {
        "LOWER(${MediaStore.Files.FileColumns.DISPLAY_NAME}) LIKE ?"
    }
    val selectionArgs = extensions.map { "%.${it.extension.lowercase()}" }.toTypedArray()

    val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

    context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val name = cursor.getString(nameCol)
            val contentUri = Uri.withAppendedPath(uri, id.toString())
            docxList += name to contentUri
        }
    }
    return docxList
}

fun getAllFilesByExtensions(context: Context, extensions: List<FileType>): List<Pair<String, Uri>> {
    return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
        getDocxFilesWithFileApi(context, extensions)
    } else {
        getDocxFilesWithMediaStore(context, extensions)
    }
}

