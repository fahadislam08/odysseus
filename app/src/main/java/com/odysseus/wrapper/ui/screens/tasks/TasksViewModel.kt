package com.odysseus.wrapper.ui.screens.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odysseus.wrapper.data.remote.*
import com.odysseus.wrapper.data.repository.TasksRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TasksRepository()

    private val _tasks   = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks

    private val _runs    = MutableStateFlow<List<TaskRun>>(emptyList())
    val runs: StateFlow<List<TaskRun>> = _runs

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error   = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionMsg = MutableStateFlow<String?>(null)
    val actionMsg: StateFlow<String?> = _actionMsg

    init { load() }

    fun load() = viewModelScope.launch {
        _loading.value = true
        repo.list().fold(onSuccess = { _tasks.value = it }, onFailure = { _error.value = it.message })
        repo.recentRuns().fold(onSuccess = { _runs.value = it }, onFailure = {})
        _loading.value = false
    }

    fun create(req: TaskCreateRequest) = viewModelScope.launch {
        repo.create(req).fold(
            onSuccess = { _tasks.value = listOf(it) + _tasks.value; _actionMsg.value = "Task created" },
            onFailure = { _error.value = it.message }
        )
    }

    fun delete(id: String) = viewModelScope.launch {
        repo.delete(id).fold(
            onSuccess = { _tasks.value = _tasks.value.filter { it.id != id } },
            onFailure = { _error.value = it.message }
        )
    }

    fun run(id: String) = viewModelScope.launch {
        repo.run(id).fold(
            onSuccess = { _actionMsg.value = "Task started"; load() },
            onFailure = { _error.value = it.message }
        )
    }

    fun pause(id: String) = viewModelScope.launch {
        repo.pause(id).fold(onSuccess = { load() }, onFailure = { _error.value = it.message })
    }

    fun resume(id: String) = viewModelScope.launch {
        repo.resume(id).fold(onSuccess = { load() }, onFailure = { _error.value = it.message })
    }

    fun loadRuns(taskId: String) = viewModelScope.launch {
        repo.runs(taskId).fold(onSuccess = { _runs.value = it }, onFailure = { _error.value = it.message })
    }

    fun clearError() { _error.value = null }
    fun clearMsg()   { _actionMsg.value = null }
}
