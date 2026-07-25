package com.odysseus.wrapper.ui.screens.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odysseus.wrapper.data.remote.*
import com.odysseus.wrapper.data.repository.NotesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NotesRepository()

    private val _notes   = MutableStateFlow<List<NoteItem>>(emptyList())
    val notes: StateFlow<List<NoteItem>> = _notes

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error   = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _filter  = MutableStateFlow("all")  // "all","note","todo","reminder"
    val filter: StateFlow<String> = _filter

    init { load() }

    fun load(archived: Boolean = false) = viewModelScope.launch {
        _loading.value = true
        val type = if (_filter.value == "all") null else _filter.value
        // list() already unwraps {"notes":[...]} → List<NoteItem>
        repo.list(type, archived).fold(
            onSuccess = { _notes.value = it },
            onFailure = { _error.value = it.message }
        )
        _loading.value = false
    }

    fun setFilter(f: String) { _filter.value = f; load() }

    fun create(title: String, content: String, type: String = "note",
               color: String? = null, pinned: Boolean = false, dueDate: String? = null) = viewModelScope.launch {
        repo.create(NoteCreateRequest(title, content, null, type, color, pinned, dueDate)).fold(
            onSuccess = { _notes.value = listOf(it) + _notes.value },
            onFailure = { _error.value = it.message }
        )
    }

    fun createTodo(title: String, items: List<NoteCheckItem>) = viewModelScope.launch {
        repo.create(NoteCreateRequest(title = title, items = items, note_type = "todo")).fold(
            onSuccess = { _notes.value = listOf(it) + _notes.value },
            onFailure = { _error.value = it.message }
        )
    }

    fun update(id: Int, title: String? = null, content: String? = null,
               color: String? = null, pinned: Boolean? = null) = viewModelScope.launch {
        repo.update(id, NoteUpdateRequest(title, content, null, null, color, pinned)).fold(
            onSuccess = { updated -> _notes.value = _notes.value.map { if (it.id == id) updated else it } },
            onFailure = { _error.value = it.message }
        )
    }

    fun delete(id: Int) = viewModelScope.launch {
        repo.delete(id).fold(
            onSuccess = { _notes.value = _notes.value.filter { it.id != id } },
            onFailure = { _error.value = it.message }
        )
    }

    fun pin(id: Int) = viewModelScope.launch {
        repo.pin(id).fold(onSuccess = { load() }, onFailure = { _error.value = it.message })
    }

    fun archive(id: Int) = viewModelScope.launch {
        repo.archive(id).fold(onSuccess = { load() }, onFailure = { _error.value = it.message })
    }

    fun toggleItem(noteId: Int, index: Int) = viewModelScope.launch {
        repo.toggleItem(noteId, index).fold(
            onSuccess = { updated -> _notes.value = _notes.value.map { if (it.id == noteId) updated else it } },
            onFailure = { _error.value = it.message }
        )
    }

    fun clearError() { _error.value = null }
}
