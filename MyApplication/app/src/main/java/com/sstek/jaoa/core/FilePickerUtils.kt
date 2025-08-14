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
    files: MutableState<List<Pair<String, Uri>>>,
    onOpenFile: (FileType, Uri) -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let {
        val name = DocumentFile.fromSingleUri(context, it)?.name ?: "Unknown"
        Log.d("FilePickerUtils", "Selected file: $name, Uri: $it")
        context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        files.value = files.value + (name to it)
        val fileType = FileType.fromFileName(name)
        onOpenFile(fileType, it)
    }
}
