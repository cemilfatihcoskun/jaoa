package com.sstek.jaoa.core

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.File
import com.sstek.jaoa.R


// Ortak dosya silme
fun deleteFile(context: Context, uri: Uri) {
    val file = File(uri.path!!)
    if (file.exists() && file.delete()) {
        Log.d("FileUtils", "Dosya silindi.")
    } else {
        Log.d("FileUtils", "Dosya silinemedi.")
    }
}


// Ortak yeniden adlandırma
fun renameFile(context: Context, uri: Uri, newName: String) {
    val file = File(uri.path!!)
    if (!file.exists()) return

    val newFile = File(file.parentFile, newName)
    if (file.renameTo(newFile)) {
        Log.d("FileUtils", "Dosya yeniden adlandırıldı: ${file.name} -> $newName")
    } else {
        Log.d("FileUtils", "Yeniden adlandırma başarısız")
    }
}


// Ortak paylaşma
fun shareFile(context: android.content.Context, uri: Uri) {
    val file = File(uri.path!!)
    if (!file.exists()) return

    val fileUri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, context.resources.getString(R.string.fileutils_share)))
}

// Internal storage için FileUtils wrapper
fun getInternalFiles(context: Context, extensions: List<FileType>): List<Pair<String, Uri>> {
    // Internal sadece app-specific directories
    val internalDirs = listOf(context.filesDir, context.cacheDir)
    val fileList = mutableListOf<Pair<String, Uri>>()
    internalDirs.forEach { dir ->
        dir.walkTopDown().forEach { file ->
            if (file.isFile && extensions.any { file.name.endsWith(".${it.extension}", ignoreCase = true) }) {
                fileList += file.name to Uri.fromFile(file)
            }
        }
    }
    return fileList
}

// External storage için mevcut FileUtils fonksiyonunu kullan
fun getExternalFiles(context: Context, extensions: List<FileType>): List<Pair<String, Uri>> {
    return getAllFilesByExtensions(context, extensions)
}

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
        Toast.makeText(context, context.resources.getString(R.string.fileutils_manageExternalStoragePermissionNeededMessage), Toast.LENGTH_LONG).show()
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
                        isNotTrashed(file)
                    ) {
                        docxList += Triple(file.name, Uri.fromFile(file), file.absolutePath)
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("FileUtils", "File API scan failed: ${e.message}", e)
        Toast.makeText(context, context.resources.getString(R.string.fileutils_filesCouldNotScanned), Toast.LENGTH_LONG).show()
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
        getDocxFilesWithFileApi(context, extensions)
    }
}

fun saveToInternalStorage(context: Context, fileName: String, content: String): Uri? {
    return try {
        val file = File(context.filesDir, fileName)
        file.outputStream().use { it.write(content.toByteArray()) }
        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
