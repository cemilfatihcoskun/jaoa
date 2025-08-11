package com.sstek.jaoa.excel

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ExcelScreen(
    filePath: Uri?,
    onBack: () -> Unit,
    viewModel: ExcelViewModel = viewModel()
) {
    val context = LocalContext.current


    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                onBack()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = {

            }) {
                Icon(Icons.Filled.Save, contentDescription = "Kaydet")
            }
        }
    }

}
