package com.sstek.jaoa.main

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.sstek.jaoa.ApplicationNavigationHost
import com.sstek.jaoa.editor.EditorScreen
import dagger.hilt.android.AndroidEntryPoint

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var currentFileUri by remember { mutableStateOf<Uri?>(null) }

            if (currentFileUri == null) {
                MainScreen(
                    onOpenFile = { uri -> currentFileUri = uri },
                    onCreateNew = {
                        currentFileUri = Uri.EMPTY
                    }
                )
            } else {
                EditorScreen(
                    filePath = currentFileUri!!,
                    onBack = { currentFileUri = null }
                )
            }
        }
    }
}

