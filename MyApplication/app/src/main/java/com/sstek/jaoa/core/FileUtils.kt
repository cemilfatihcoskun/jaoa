package com.sstek.jaoa.core

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.File
import com.sstek.jaoa.R

fun deleteFile(context: Context, uri: Uri) {
    try {
        val rowsDeleted = context.contentResolver.delete(uri, null, null)
        if (rowsDeleted > 0) Log.d("FileUtils", "Dosya silindi.")
        else Log.d("FileUtils", "Dosya silinemedi.")
    } catch (e: Exception) {
        Log.e("FileUtils", "Dosya silme hatası: ${e.message}", e)
    }
}

fun renameFile(context: Context, uri: Uri, newName: String) {
    try {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, newName) }
        val rowsUpdated = context.contentResolver.update(uri, values, null, null)
        if (rowsUpdated > 0) Log.d("FileUtils", "Dosya yeniden adlandırıldı: $newName")
        else Log.d("FileUtils", "Yeniden adlandırma başarısız")
    } catch (e: Exception) {
        Log.e("FileUtils", "Rename hatası: ${e.message}", e)
    }
}

fun shareFile(context: Context, uri: Uri, displayName: String) {
    try {
        val tempFile = File(context.cacheDir, displayName)
        tempFile.deleteOnExit()

        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val fileUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.fileutils_share)))

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Paylaşma sırasında hata oluştu", Toast.LENGTH_SHORT).show()
    }
}


// MediaStore üzerine kaydetme (Documents/Public)
fun saveToMediaStore(context: Context, fileName: String, fileType: FileType, content: ByteArray): Uri? {
    val resolver = context.contentResolver
    val collection = when (fileType) {
        FileType.DOCX, FileType.DOC -> MediaStore.Files.getContentUri("external")
        FileType.XLSX, FileType.XLS -> MediaStore.Files.getContentUri("external")
        else -> MediaStore.Files.getContentUri("external")
    }

    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, when (fileType) {
            FileType.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            FileType.DOC -> "application/msword"
            FileType.XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            FileType.XLS -> "application/vnd.ms-excel"
            else -> "*/*"
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/JAOA")
        }
    }

    return try {
        val uri = resolver.insert(collection, values)
        uri?.let {
            resolver.openOutputStream(uri)?.use { stream -> stream.write(content) }
        }
        uri
    } catch (e: Exception) {
        Log.e("FileUtils", "Kaydetme hatası: ${e.message}", e)
        null
    }
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

/*
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
*/



fun getFilesFromMediaStore(context: Context, fileTypes: List<FileType>): List<Pair<String, Uri>> {
    val files = mutableListOf<Pair<String, Uri>>()
    val resolver = context.contentResolver
    val uri = MediaStore.Files.getContentUri("external")

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.MIME_TYPE
    )

    // Uzantılara göre filtreleme
    val extensions = fileTypes.map { it.extension.lowercase() }.filter { it.isNotEmpty() }
    if (extensions.isEmpty()) return emptyList()

    val selection = extensions.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?" }
    val selectionArgs = extensions.map { "%.$it" }.toTypedArray()

    resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            val name = cursor.getString(nameIndex)
            val contentUri = Uri.withAppendedPath(uri, id.toString())
            files.add(name to contentUri)
        }
    }

    return files
}



fun getAllFilesByExtensions(context: Context, extensions: List<FileType>): List<Pair<String, Uri>> {
    return getFilesFromMediaStore(context, extensions)
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
