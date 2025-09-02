package com.sstek.jaoa.core

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.sstek.jaoa.ApplicationNavigationHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.sstek.jaoa.R

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val intentViewModel: IntentViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIncomingIntent(intent)

        setContent {
            JAOATheme {
                Surface() {
                    val navController = rememberNavController()
                    ApplicationNavigationHost(navController)

                    val uri = intentViewModel.intentUri.collectAsState().value
                    val fileType = intentViewModel.intentFileType.collectAsState().value

                    if (uri != null && fileType != FileType.UNKNOWN) {
                        HandleIntentUri(uri, fileType, navController, intentViewModel)
                    }

                    val toastMessage = intentViewModel.toastMessage.collectAsState().value
                    LaunchedEffect(toastMessage) {
                        if (!toastMessage.isNullOrEmpty()) {
                            Toast.makeText(this@MainActivity, toastMessage, Toast.LENGTH_SHORT).show()
                            intentViewModel.clearToast()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        intent?.let { intent ->
            Log.d("MainActivity", "INTENT TYPE ${intent.action}")
            if (intent.action == Intent.ACTION_OPEN_DOCUMENT) {
                val dataUri = intent.data
                if (dataUri != null) {
                    Log.d("MainActivity", "$dataUri")

                    val context = applicationContext
                    val resolver = context.contentResolver

                    val fileType = FileType.fromMime(resolver.getType(dataUri).toString())

                    try {
                        resolver.takePersistableUriPermission(
                            dataUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        intentViewModel.setIntentUri(dataUri)
                        intentViewModel.setIntentFileType(fileType)
                    } catch (e: SecurityException) {
                        Log.e("MainActivity", "URI permission error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun HandleIntentUri(uri: Uri, fileType: FileType, navController: NavHostController, viewModel: IntentViewModel) {
        LaunchedEffect(uri, fileType) {
            val path = uri.path ?: ""

            Log.d("MainActivity", "$uri   $fileType")

            when (fileType) {
                FileType.DOCX -> {
                    val encodedUri = encodeUri(uri)
                    navController.navigate("word/$encodedUri")
                }
                FileType.XLSX -> {
                    val encodedUri = encodeUri(uri)
                    navController.navigate("excel/$encodedUri")
                }
                else -> {
                    viewModel.showToast(applicationContext.getString(R.string.mainactivity_unknownFileTypeError))
                }
            }
            viewModel.clearIntentUri()
        }
    }
}

class IntentViewModel : ViewModel() {
    private val _intentUri = MutableStateFlow<Uri?>(null)
    val intentUri = _intentUri.asStateFlow()

    private val _intentFileType = MutableStateFlow<FileType>(FileType.UNKNOWN)
    val intentFileType = _intentFileType.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    fun setIntentUri(uri: Uri) {
        viewModelScope.launch {
            _intentUri.value = uri
        }
    }

    fun clearIntentUri() {
        viewModelScope.launch {
            _intentUri.value = null
        }
    }

    fun setIntentFileType(fileType: FileType) {
        viewModelScope.launch {
            _intentFileType.value = fileType
        }
    }

    fun clearIntentFileType() {
        viewModelScope.launch {
            _intentFileType.value = FileType.UNKNOWN
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.value = message
        }
    }

    fun clearToast() {
        viewModelScope.launch {
            _toastMessage.value = null
        }
    }
}
