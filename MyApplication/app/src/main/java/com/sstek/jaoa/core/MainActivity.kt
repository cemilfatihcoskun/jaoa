package com.sstek.jaoa.core

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.sstek.jaoa.ApplicationNavigationHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val intentViewModel: IntentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIncomingIntent(intent)

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                val navController = rememberNavController()
                ApplicationNavigationHost(navController)

                val uri = intentViewModel.intentUri.collectAsState().value
                uri?.let {
                    HandleIntentUri(it, navController, intentViewModel)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        intent?.let { intent ->
            if (intent.action == Intent.ACTION_VIEW) {
                val dataUri = intent.data
                if (dataUri != null) {
                    try {
                        Log.d("DocxFiles", "dataUri=$dataUri")

                        // Intent'ten izin bayraklarını al
                        val flags = intent.flags and
                                (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                        // Kalıcı izin ancak persistable bayrağı varsa alınır
                        if ((flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0) {
                            contentResolver.takePersistableUriPermission(dataUri, flags)
                            Log.d("DocxFiles", "Persistable permission taken for $dataUri")
                        } else {
                            Log.d("DocxFiles", "Persistable permission NOT available for $dataUri")
                        }

                        // ViewModel'e URI'yı set et
                        intentViewModel.setIntentUri(dataUri)
                        Log.d("DocxFiles", "Received docx Uri from external app: $dataUri")

                    } catch (e: SecurityException) {
                        Log.e("DocxFiles", "Permission denied for Uri: $dataUri", e)
                        intentViewModel.showToast("Dosya izni alınamadı, lütfen dosyayı uygulama içinden seçin.")
                    }
                }
            }
        }
    }


    @androidx.compose.runtime.Composable
    private fun HandleIntentUri(uri: Uri, navController: NavHostController, viewModel: IntentViewModel) {
        LaunchedEffect(uri) {
            val encodedUri = Uri.encode(uri.toString())
            navController.navigate("editor/$encodedUri")
            viewModel.clearIntentUri()
        }
    }
}

class IntentViewModel : ViewModel() {
    private val _intentUri = MutableStateFlow<Uri?>(null)
    val intentUri = _intentUri.asStateFlow()

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
