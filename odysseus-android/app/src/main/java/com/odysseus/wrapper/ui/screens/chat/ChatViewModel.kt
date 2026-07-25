package com.odysseus.wrapper.ui.screens.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odysseus.wrapper.data.remote.*
import com.odysseus.wrapper.data.repository.ChatRepository
import com.odysseus.wrapper.data.repository.SessionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val sessionRepo = SessionRepository()
    private val chatRepo    = ChatRepository()

    private val _sessions       = MutableStateFlow<List<SessionItem>>(emptyList())
    val sessions: StateFlow<List<SessionItem>> = _sessions

    private val _currentSession = MutableStateFlow<SessionItem?>(null)
    val currentSession: StateFlow<SessionItem?> = _currentSession

    private val _messages       = MutableStateFlow<List<MessageItem>>(emptyList())
    val messages: StateFlow<List<MessageItem>> = _messages

    private val _modelNames     = MutableStateFlow<List<String>>(emptyList())
    val modelNames: StateFlow<List<String>> = _modelNames

    private val _sending        = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    private val _loading        = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error          = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { loadSessions(); loadModels() }

    fun loadSessions() = viewModelScope.launch {
        _loading.value = true
        sessionRepo.list().fold(
            onSuccess = {
                _sessions.value = it
                if (_currentSession.value == null && it.isNotEmpty()) selectSession(it.first())
            },
            onFailure = { _error.value = it.message }
        )
        _loading.value = false
    }

    fun selectSession(session: SessionItem) {
        _currentSession.value = session
        viewModelScope.launch {
            // history() already unwraps {"history":[...]} → List<MessageItem>
            sessionRepo.history(session.id).fold(
                onSuccess = { _messages.value = it },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun newSession() = viewModelScope.launch {
        sessionRepo.create().fold(
            onSuccess = { s -> _sessions.value = listOf(s) + _sessions.value; selectSession(s) },
            onFailure = { _error.value = it.message }
        )
    }

    fun renameSession(id: String, name: String) = viewModelScope.launch {
        sessionRepo.rename(id, name).fold(onSuccess = { loadSessions() }, onFailure = { _error.value = it.message })
    }

    fun deleteSession(id: String) = viewModelScope.launch {
        sessionRepo.delete(id).fold(
            onSuccess = {
                val updated = _sessions.value.filter { it.id != id }
                _sessions.value = updated
                if (_currentSession.value?.id == id) {
                    _messages.value = emptyList()
                    _currentSession.value = updated.firstOrNull()
                    updated.firstOrNull()?.let { selectSession(it) }
                }
            },
            onFailure = { _error.value = it.message }
        )
    }

    fun archiveSession(id: String) = viewModelScope.launch {
        sessionRepo.archive(id).fold(onSuccess = { loadSessions() }, onFailure = { _error.value = it.message })
    }

    fun sendMessage(text: String, useWeb: Boolean = false) = viewModelScope.launch {
        val sid = _currentSession.value?.id ?: run { _error.value = "No active session"; return@launch }
        _messages.value = _messages.value + MessageItem(role = "user", content = text)
        _sending.value = true
        chatRepo.send(text, sid, useWeb).fold(
            onSuccess = { _messages.value = _messages.value + MessageItem(role = "assistant", content = it.response) },
            onFailure = { _error.value = it.message }
        )
        _sending.value = false
    }

    fun stopGeneration() = viewModelScope.launch {
        _currentSession.value?.id?.let { chatRepo.stop(it) }
        _sending.value = false
    }

    private fun loadModels() = viewModelScope.launch {
        // modelNames() unwraps {"hosts":[],"items":[...]} → flat List<String>
        sessionRepo.modelNames().fold(onSuccess = { _modelNames.value = it }, onFailure = {})
    }

    fun clearError() { _error.value = null }
}
