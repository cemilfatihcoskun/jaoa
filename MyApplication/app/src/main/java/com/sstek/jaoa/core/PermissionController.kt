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
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.sstek.jaoa.R

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun CheckStoragePermissionWithExplanation(): Boolean {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    permissionGranted = Environment.isExternalStorageManager()


    return permissionGranted
}
