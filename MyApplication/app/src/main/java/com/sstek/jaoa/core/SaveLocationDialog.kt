package com.sstek.jaoa.core

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun SaveLocationDialog(
    onDismiss: () -> Unit,
    onInternal: () -> Unit,
    onExternal: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Kaydetme Yeri Seç") },
        text = { Text("Dosyayı nereye kaydetmek istiyorsunuz?") },
        confirmButton = {
            TextButton(onClick = { onInternal() }) {
                Text("İç Depolama")
            }
        },
        dismissButton = {
            TextButton(onClick = { onExternal() }) {
                Text("Dış Depolama")
            }
        }
    )
}