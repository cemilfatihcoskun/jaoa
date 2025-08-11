package com.sstek.jaoa.core

sealed class Screen(val route: String) {
    object Main: Screen("main_screen")
    object Word: Screen("word_screen")
    object Excel: Screen("excel_screen")
}
