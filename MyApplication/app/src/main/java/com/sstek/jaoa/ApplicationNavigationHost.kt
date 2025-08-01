package com.sstek.jaoa

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sstek.jaoa.editor.EditorScreen
import com.sstek.jaoa.main.MainScreen

@Composable
fun ApplicationNavigationHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onOpenFile = { filePath ->
                    navController.navigate("editor/${filePath}")
                },
                onCreateNew = {
                    navController.navigate("editor/") // boş path → yeni dosya
                }
            )
        }

        composable(
            route = "editor/{filePath}",
            arguments = listOf(navArgument("filePath") {
                type = NavType.StringType
                nullable = true
                defaultValue = ""
            })
        ) { backStackEntry ->
            val filePath = backStackEntry.arguments?.getString("filePath") ?: ""
            EditorScreen(
                filePath = Uri.parse(filePath),
                onBack = { navController.popBackStack()}
            )
        }
    }
}


