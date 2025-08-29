package com.sstek.jaoa.core

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.sstek.jaoa.R

@Composable
fun SaveLocationDialog(
    context: Context,
    onDismiss: () -> Unit,
    onInternal: () -> Unit,
    onExternal: () -> Unit
) {

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(context.resources.getString(R.string.saveLocationDialog_title)) },
        text = { Text(context.resources.getString(R.string.saveLocationDialog_content)) },
        confirmButton = {
            TextButton(onClick = { onInternal() }) {
                Text(context.resources.getString(R.string.saveLocationDialog_internalStorage))
            }
        },
        dismissButton = {
            TextButton(onClick = { onExternal() }) {
                Text(context.resources.getString(R.string.saveLocationDialog_externalStorage))
            }
        }
    )
}