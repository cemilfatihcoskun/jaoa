package com.sstek.jaoa.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.sstek.jaoa.R
import java.io.File
import java.io.InputStream
import java.io.OutputStream

fun shareDocument(context: Context, sourceUri: Uri, mimeType: String, fileName: String) {
    try {
        // 1️⃣ Cache dizininde geçici dosya oluştur
        val cacheFile = File(context.cacheDir, fileName)

        // 2️⃣ Dosyayı cache’e kopyala
        context.contentResolver.openInputStream(sourceUri).use { input: InputStream? ->
            cacheFile.outputStream().use { output: OutputStream ->
                input?.copyTo(output)
            }
        }

        // 3️⃣ FileProvider URI üret
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            cacheFile
        )

        // 4️⃣ Paylaşım intent’i
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, context.resources.getString(R.string.sharer_share)))

    } catch (e: Exception) {
        Log.d("Sharer", "${e.message}")
    }
}

