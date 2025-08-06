package com.sstek.jaoa.main

import androidx.activity.viewModels
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import com.sstek.jaoa.ApplicationNavigationHost
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class IntentViewModel : ViewModel() {
    private val _intentUri = MutableStateFlow<Uri?>(null)
    val intentUri = _intentUri.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    fun setIntentUri(uri: Uri?) {
        viewModelScope.launch {
            Log.d("DocxFiles", "Setting intent Uri in ViewModel: $uri")
            _intentUri.value = uri
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            Log.d("DocxFiles", "Showing toast: $message")
            _toastMessage.value = message
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val intentViewModel: IntentViewModel by viewModels()

    // ACTION_OPEN_DOCUMENT için Activity Result Launcher
    private val documentPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("DocxFiles", "Document picked from ACTION_OPEN_DOCUMENT: $uri")
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                Log.d("DocxFiles", "Permission taken for picked Uri: $uri")
                intentViewModel.setIntentUri(uri)
            } catch (e: SecurityException) {
                Log.e("DocxFiles", "Failed to take permission for picked Uri: ${e.message}")
                intentViewModel.showToast("Dosya izni alınamadı: ${e.message}")
            }
        } ?: Log.d("DocxFiles", "No document picked from ACTION_OPEN_DOCUMENT")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Gelen intent'ten Uri'yi al ve detaylı logla
        handleIntent(intent)

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                val navController = rememberNavController()
                ApplicationNavigationHost(navController)

                // Intent Uri'sini işle
                val uri by intentViewModel.intentUri.collectAsState()
                uri?.let {
                    HandleIntentUri(it, navController, intentViewModel)
                }

                // Toast mesajlarını dinle
                val toastMessage by intentViewModel.toastMessage.collectAsState()
                LaunchedEffect(toastMessage) {
                    toastMessage?.let { message ->
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                        intentViewModel.showToast("") // Mesajı sıfırla
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            Log.d("DocxFiles", "handleIntent: Intent received: action=${it.action}, data=${it.data}, type=${it.type}, scheme=${it.data?.scheme}, path=${it.data?.path}, authority=${it.data?.authority}, extras=${it.extras?.keySet()?.joinToString() ?: "none"}, flags=${Integer.toHexString(it.flags)}")
            it.data?.let { uri ->
                Log.d("DocxFiles", "Received Uri: $uri")
                // İzin bayrağını kontrol et
                if (it.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) {
                    Log.d("DocxFiles", "Intent has FLAG_GRANT_READ_URI_PERMISSION")
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        Log.d("DocxFiles", "Permission taken for Uri: $uri")
                        intentViewModel.setIntentUri(uri)
                    } catch (e: SecurityException) {
                        Log.e("DocxFiles", "Permission not granted: ${e.message}")
                        intentViewModel.showToast("Dosyaya erişim izni alınamadı, lütfen dosyayı seçin.")
                        documentPickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    }
                } else {
                    Log.w("DocxFiles", "Intent does not have FLAG_GRANT_READ_URI_PERMISSION")
                    intentViewModel.showToast("Dosyaya erişim izni alınamadı, lütfen dosyayı seçin.")
                    documentPickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                }
            } ?: Log.d("DocxFiles", "handleIntent: No Uri in intent")
        } ?: Log.d("DocxFiles", "handleIntent: No intent received")
    }

    @Composable
    private fun HandleIntentUri(uri: Uri, navController: NavHostController, viewModel: IntentViewModel) {
        LaunchedEffect(uri) {
            Log.d("DocxFiles", "Navigating to editor with Uri: $uri")
            try {
                val encodedUri = Uri.encode(uri.toString())
                navController.navigate("editor/$encodedUri")
                viewModel.setIntentUri(null) // Navigasyon sonrası URI'yi sıfırla
            } catch (e: Exception) {
                Log.e("DocxFiles", "Navigation failed: ${e.message}", e)
                viewModel.showToast("Navigasyon hatası: ${e.message}")
                documentPickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            }
        }
    }
}