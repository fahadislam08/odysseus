package com.odysseus.wrapper.ui.screens.documents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odysseus.wrapper.data.remote.*
import com.odysseus.wrapper.data.repository.DocumentsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DocumentsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DocumentsRepository()

    private val _docs    = MutableStateFlow<List<DocumentItem>>(emptyList())
    val docs: StateFlow<List<DocumentItem>> = _docs

    private val _current = MutableStateFlow<DocumentDetail?>(null)
    val current: StateFlow<DocumentDetail?> = _current

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _saving  = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    private val _error   = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load(q: String? = null) = viewModelScope.launch {
        _loading.value = true
        repo.library(q).fold(onSuccess = { _docs.value = it }, onFailure = { _error.value = it.message })
        _loading.value = false
    }

    fun open(id: String) = viewModelScope.launch {
        repo.get(id).fold(onSuccess = { _current.value = it }, onFailure = { _error.value = it.message })
    }

    fun close() { _current.value = null }

    fun create(title: String, content: String = "", type: String = "markdown") = viewModelScope.launch {
        repo.create(DocumentCreateRequest(title, content, type)).fold(
            onSuccess = { _current.value = it; load() },
            onFailure = { _error.value = it.message }
        )
    }

    fun save(id: String, title: String, content: String) = viewModelScope.launch {
        _saving.value = true
        repo.update(id, DocumentUpdateRequest(title, content)).fold(
            onSuccess = { _current.value = it },
            onFailure = { _error.value = it.message }
        )
        _saving.value = false
    }

    fun delete(id: String) = viewModelScope.launch {
        repo.delete(id).fold(
            onSuccess = { _docs.value = _docs.value.filter { it.id != id }; close() },
            onFailure = { _error.value = it.message }
        )
    }

    fun archive(id: String) = viewModelScope.launch {
        repo.archive(id).fold(onSuccess = { load() }, onFailure = { _error.value = it.message })
    }

    fun clearError() { _error.value = null }
}
