package com.sstek.jaoa.core

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

fun getFileName(context: Context, uri: Uri?): String {
    if (uri == null) {
        return "Unknown"
    }

    return when (uri.scheme) {
        "content" -> {
            var name: String? = null
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) name = it.getString(index)
                }
            }
            name ?: "Unknown"
        }
        "file" -> {
            File(uri.path ?: "").name
        }
        else -> "Unknown"
    }
}
