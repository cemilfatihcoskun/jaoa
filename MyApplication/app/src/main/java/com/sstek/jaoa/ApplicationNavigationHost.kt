package com.sstek.jaoa

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sstek.jaoa.word.QuillEditorScreen
import com.sstek.jaoa.core.MainScreen
import android.util.Log
import com.sstek.jaoa.core.FileType
import com.sstek.jaoa.excel.ExcelScreen

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
            val fileUri = fileUriString?.let { Uri.parse(it) }

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
            val fileUri = fileUriString?.let { Uri.parse(it) }

            ExcelScreen(
                filePath = fileUri,
                onBack = { navController.popBackStack() }
            )
        }

    }
}

fun navigate(navController: NavHostController, fileType: FileType, fileUri: Uri?) {
    val uriEncoded = fileUri.let {
        it.toString().replace("/", "%2F")
    } ?: ""

    Log.d("ApplicationNavigationHost", "$fileType, $fileUri")

    when (fileType) {
        FileType.DOCX -> navController.navigate("word/$uriEncoded")
        FileType.DOC -> TODO()
        FileType.XLSX -> navController.navigate("excel/$uriEncoded")
        FileType.XLS -> TODO()
        FileType.PPTX -> TODO()
        FileType.PPT -> TODO()
        FileType.UNKNOWN -> ""
    }
}
