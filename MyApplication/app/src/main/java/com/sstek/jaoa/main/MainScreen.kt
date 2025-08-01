package com.sstek.jaoa.main

import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun MainScreen(
    onOpenFile: (Uri) -> Unit,
    onCreateNew: () -> Unit
) {
    val context = LocalContext.current
    var docxFiles by remember { mutableStateOf<List<Pair<String, Uri>>>(emptyList()) }

    // dosyaları tarama
    LaunchedEffect(Unit) {
        val mainDir = Environment.getExternalStorageDirectory()
        val files = mainDir?.let { findDocxFiles(it) }?.map {
            it.name to Uri.fromFile(it)
        } ?: emptyList()
        docxFiles = files

    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNew) {
                Icon(Icons.Default.Add, contentDescription = "Yeni dosya")
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

fun findDocxFiles(dir: File): List<File> {
    val result = mutableListOf<File>()
    val files = dir.listFiles()
    if (files != null) {
        for (file in files) {
            if (file.isDirectory) {
                result += findDocxFiles(file)
            } else if (file.extension.equals("docx", ignoreCase = true)) {
                result += file
            }
        }
    }
    return result
}




