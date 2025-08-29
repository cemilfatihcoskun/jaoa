package com.sstek.jaoa.core

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.sstek.jaoa.R

@Composable
fun CheckStoragePermissionWithExplanation(): Boolean {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            permissionGranted = true
            return@rememberLauncherForActivityResult
        }

        if (Environment.isExternalStorageManager()) {
            permissionGranted = true
        } else {
            Toast.makeText(context, context.resources.getString(R.string.permissionController_permissionDeniedMessage), Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()) {
            permissionGranted = true
        } else {
            showDialog = true // dialogu göster
        }
    }

    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { androidx.compose.material3.Text(context.resources.getString(R.string.permissionController_permissionNeededTitle)) },
            text = {
                androidx.compose.material3.Text(
                    context.resources.getString(R.string.permissionController_permissionNeededMessage)
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDialog = false
                    launcher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }) {
                    androidx.compose.material3.Text(context.resources.getString(R.string.permissionController_grantPermission))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDialog = false
                    Toast.makeText(
                        context,
                        context.resources.getString(R.string.permissionController_cannotListDocumentsDueToNotGivenPermissionMessage),
                        Toast.LENGTH_LONG
                    ).show()
                }) {
                    androidx.compose.material3.Text(context.resources.getString(R.string.permissionController_cancel))
                }
            }
        )
    }

    return permissionGranted
}



