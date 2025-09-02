package com.sstek.jaoa.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.documentfile.provider.DocumentFile

@Composable
fun rememberFilePickerLauncher(
    context: Context,
    onOpenFile: (FileType, Uri) -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri: Uri? ->
    uri?.let {
        val name = DocumentFile.fromSingleUri(context, it)?.name ?: "Unknown"
        Log.d("FilePickerUtils", "Selected file: $name, Uri: $it")

        val resolver = context.contentResolver
        val persistedPermissions = resolver.persistedUriPermissions

        try {
            resolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            Log.e("FilePickerUtils", "URI permission error: ${e.message}")
        }

        /*
        for (perm in persistedPermissions) {
            Log.d("FilePickerUtils", "${perm.uri} ${perm.isReadPermission} ${perm.isWritePermission}")
        }
         */

        val fileType = FileType.fromFileName(name)
        onOpenFile(fileType, it)
    }
}
