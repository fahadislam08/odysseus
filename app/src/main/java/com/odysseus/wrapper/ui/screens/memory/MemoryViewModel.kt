package com.odysseus.wrapper.ui.screens.memory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odysseus.wrapper.data.remote.*
import com.odysseus.wrapper.data.repository.MemoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MemoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MemoryRepository()

    private val _memories = MutableStateFlow<List<MemoryItem>>(emptyList())
    val memories: StateFlow<List<MemoryItem>> = _memories

    private val _skills   = MutableStateFlow<List<SkillItem>>(emptyList())
    val skills: StateFlow<List<SkillItem>> = _skills

    private val _loading  = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error    = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _tab      = MutableStateFlow(0)  // 0=memories, 1=skills
    val tab: StateFlow<Int> = _tab

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter

    init { loadAll() }

    fun loadAll() = viewModelScope.launch {
        _loading.value = true
        repo.listMemories(_categoryFilter.value).fold(
            onSuccess = { _memories.value = it }, onFailure = { _error.value = it.message })
        repo.listSkills().fold(
            onSuccess = { _skills.value = it }, onFailure = {})
        _loading.value = false
    }

    fun setTab(t: Int) { _tab.value = t }
    fun setCategory(cat: String?) { _categoryFilter.value = cat; loadAll() }

    fun addMemory(text: String, category: String = "fact") = viewModelScope.launch {
        repo.addMemory(text, category).fold(
            onSuccess = { _memories.value = listOf(it) + _memories.value },
            onFailure = { _error.value = it.message }
        )
    }

    fun deleteMemory(id: String) = viewModelScope.launch {
        repo.deleteMemory(id).fold(
            onSuccess = { _memories.value = _memories.value.filter { it.id != id } },
            onFailure = { _error.value = it.message }
        )
    }

    fun pinMemory(id: String) = viewModelScope.launch {
        repo.pinMemory(id).fold(onSuccess = { loadAll() }, onFailure = { _error.value = it.message })
    }

    fun updateMemory(id: String, text: String, category: String?) = viewModelScope.launch {
        repo.updateMemory(id, text, category).fold(
            onSuccess = { updated -> _memories.value = _memories.value.map { if (it.id == id) updated else it } },
            onFailure = { _error.value = it.message }
        )
    }

    fun addSkill(title: String, problem: String, solution: String, tags: String) = viewModelScope.launch {
        repo.createSkill(title, problem, solution, tags).fold(
            onSuccess = { _skills.value = _skills.value + it },
            onFailure = { _error.value = it.message }
        )
    }

    fun deleteSkill(id: String) = viewModelScope.launch {
        repo.deleteSkill(id).fold(
            onSuccess = { _skills.value = _skills.value.filter { it.id != id } },
            onFailure = { _error.value = it.message }
        )
    }

    fun clearError() { _error.value = null }
}
