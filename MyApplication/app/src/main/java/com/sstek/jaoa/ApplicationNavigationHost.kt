package com.sstek.jaoa

import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sstek.jaoa.word.QuillEditorScreen
import com.sstek.jaoa.core.MainScreen
import android.util.Log
import androidx.annotation.RequiresApi
import com.sstek.jaoa.core.FileType
import com.sstek.jaoa.excel.ExcelScreen
import com.sstek.jaoa.core.decodeUri
import com.sstek.jaoa.core.encodeUri

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun ApplicationNavigationHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onOpenFile = { fileType, filePath ->
                    navigate(navController, fileType, filePath)
                },
                onCreateNew = { fileType ->
                    navigate(navController, fileType, null)
                }
            )
        }

        composable(
            route = "word/{fileUri}",
            arguments = listOf(navArgument("fileUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val fileUriString = backStackEntry.arguments?.getString("fileUri")
            val fileUri = fileUriString?.let { decodeUri(it) }

            Log.d("AppNavHost", "word/fileUri ${fileUri}")

            QuillEditorScreen(
                filePath = fileUri,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "excel/{fileUri}",
            arguments = listOf(navArgument("fileUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val fileUriString = backStackEntry.arguments?.getString("fileUri")
            val fileUri = fileUriString?.let { decodeUri(it) }

            ExcelScreen(
                filePath = fileUri,
                onBack = { navController.popBackStack() }
            )
        }

    }
}

fun navigate(navController: NavHostController, fileType: FileType, fileUri: Uri?) {
    var uriEncoded = ""
    if (fileUri != null) {
        uriEncoded = encodeUri(fileUri)
    }

    Log.d("ApplicationNavigationHost", "$fileType, $fileUri")

    when (fileType) {
        FileType.DOCX -> navController.navigate("word/$uriEncoded")
        FileType.DOC -> TODO()
        FileType.XLSX -> navController.navigate("excel/$uriEncoded")
        FileType.XLS -> TODO()
        FileType.PPTX -> ""
        FileType.PPT -> TODO()
        FileType.UNKNOWN -> ""
    }
}

