package com.sstek.jaoa.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _docxFiles = MutableStateFlow<List<File>>(emptyList())
    val docxFiles: StateFlow<List<File>> = _docxFiles

    fun loadDocxFiles() {
        viewModelScope.launch {
            val filesDir = context.filesDir
            val docxFiles = filesDir?.listFiles()?.filter { it.extension == "docx" } ?: emptyList()
            _docxFiles.value = docxFiles
        }
    }
}
