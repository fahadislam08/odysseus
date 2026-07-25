package com.odysseus.wrapper.ui.screens.research

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odysseus.wrapper.data.remote.*
import com.odysseus.wrapper.data.repository.ResearchRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ResearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ResearchRepository()

    private val _sessions = MutableStateFlow<List<ResearchSession>>(emptyList())
    val sessions: StateFlow<List<ResearchSession>> = _sessions

    private val _active   = MutableStateFlow<ResearchSession?>(null)
    val active: StateFlow<ResearchSession?> = _active

    private val _status   = MutableStateFlow<ResearchStatus?>(null)
    val status: StateFlow<ResearchStatus?> = _status

    private val _report   = MutableStateFlow<ResearchReport?>(null)
    val report: StateFlow<ResearchReport?> = _report

    private val _loading  = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _starting = MutableStateFlow(false)
    val starting: StateFlow<Boolean> = _starting

    private val _error    = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { loadLibrary() }

    fun loadLibrary() = viewModelScope.launch {
        _loading.value = true
        repo.library().fold(onSuccess = { _sessions.value = it }, onFailure = { _error.value = it.message })
        _loading.value = false
    }

    fun start(query: String, maxSources: Int = 10) = viewModelScope.launch {
        _starting.value = true
        repo.start(query, maxSources).fold(
            onSuccess = { session ->
                _active.value = session
                _sessions.value = listOf(session) + _sessions.value
                pollStatus(session.id)
            },
            onFailure = { _error.value = it.message }
        )
        _starting.value = false
    }

    private fun pollStatus(id: String) = viewModelScope.launch {
        repeat(120) {
            repo.status(id).fold(
                onSuccess = { s ->
                    _status.value = s
                    if (s.status in listOf("done", "error", "cancelled")) {
                        if (s.status == "done") loadReport(id)
                        loadLibrary()
                        return@launch
                    }
                },
                onFailure = {}
            )
            delay(3000)
        }
    }

    fun openSession(session: ResearchSession) {
        _active.value = session
        if (session.status == "done") loadReport(session.id)
        else pollStatus(session.id)
    }

    fun loadReport(id: String) = viewModelScope.launch {
        repo.report(id).fold(onSuccess = { _report.value = it }, onFailure = { _error.value = it.message })
    }

    fun cancel(id: String) = viewModelScope.launch {
        repo.cancel(id).fold(onSuccess = { loadLibrary() }, onFailure = { _error.value = it.message })
    }

    fun archive(id: String) = viewModelScope.launch {
        repo.archive(id).fold(onSuccess = { loadLibrary() }, onFailure = { _error.value = it.message })
    }

    fun delete(id: String) = viewModelScope.launch {
        repo.delete(id).fold(
            onSuccess = {
                _sessions.value = _sessions.value.filter { it.id != id }
                if (_active.value?.id == id) { _active.value = null; _report.value = null; _status.value = null }
            },
            onFailure = { _error.value = it.message }
        )
    }

    fun close() { _active.value = null; _report.value = null; _status.value = null }
    fun clearError() { _error.value = null }
}
