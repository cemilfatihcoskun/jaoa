package com.sstek.jaoa.core

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MainScreenViewModel : ViewModel() {
    var files = mutableStateListOf<Pair<String, Uri>>()
    var showNewFileMenu = mutableStateOf(false)
    var searchQuery = mutableStateOf("")
    var selectedFilter = mutableStateOf<FileType?>(null)
    var selectedTabIndex = mutableStateOf(1) // 0 = Internal, 1 = External
}
