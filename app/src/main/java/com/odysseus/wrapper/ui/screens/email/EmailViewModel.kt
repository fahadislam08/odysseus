package com.odysseus.wrapper.ui.screens.email

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odysseus.wrapper.data.remote.*
import com.odysseus.wrapper.data.repository.EmailRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EmailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = EmailRepository()

    private val _emails    = MutableStateFlow<List<EmailItem>>(emptyList())
    val emails: StateFlow<List<EmailItem>> = _emails

    private val _selected  = MutableStateFlow<EmailDetail?>(null)
    val selected: StateFlow<EmailDetail?> = _selected

    private val _folders   = MutableStateFlow<List<EmailFolder>>(emptyList())
    val folders: StateFlow<List<EmailFolder>> = _folders

    private val _accounts  = MutableStateFlow<List<EmailAccountItem>>(emptyList())
    val accounts: StateFlow<List<EmailAccountItem>> = _accounts

    private val _loading   = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error     = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _folder    = MutableStateFlow("INBOX")
    val folder: StateFlow<String> = _folder

    private val _searchMode = MutableStateFlow(false)
    val searchMode: StateFlow<Boolean> = _searchMode

    private val _aiReply   = MutableStateFlow<String?>(null)
    val aiReply: StateFlow<String?> = _aiReply

    init { loadAccounts(); load() }

    private fun loadAccounts() = viewModelScope.launch {
        repo.accounts().fold(onSuccess = {
            _accounts.value = it
            if (it.isNotEmpty()) loadFolders(it.first().id)
        }, onFailure = {})
    }

    private fun loadFolders(accountId: Int) = viewModelScope.launch {
        repo.folders(accountId).fold(onSuccess = { _folders.value = it }, onFailure = {})
    }

    fun load() = viewModelScope.launch {
        _loading.value = true
        repo.list(folder = _folder.value).fold(
            onSuccess = { _emails.value = it },
            onFailure = { _error.value = it.message }
        )
        _loading.value = false
    }

    fun setFolder(f: String) { _folder.value = f; load() }

    fun open(uid: String) = viewModelScope.launch {
        repo.read(uid).fold(
            onSuccess = { _selected.value = it; markRead(uid) },
            onFailure = { _error.value = it.message }
        )
    }

    fun close() { _selected.value = null; _aiReply.value = null }

    fun markRead(uid: String) = viewModelScope.launch {
        repo.markRead(uid).fold(onSuccess = {
            _emails.value = _emails.value.map { if (it.uid == uid) it.copy(is_read = true) else it }
        }, onFailure = {})
    }

    fun markUnread(uid: String) = viewModelScope.launch {
        repo.markUnread(uid).fold(onSuccess = {
            _emails.value = _emails.value.map { if (it.uid == uid) it.copy(is_read = false) else it }
        }, onFailure = {})
    }

    fun flag(uid: String) = viewModelScope.launch {
        repo.flag(uid).fold(onSuccess = { load() }, onFailure = { _error.value = it.message })
    }

    fun archive(uid: String) = viewModelScope.launch {
        repo.archive(uid).fold(onSuccess = {
            _emails.value = _emails.value.filter { it.uid != uid }
            close()
        }, onFailure = { _error.value = it.message })
    }

    fun delete(uid: String) = viewModelScope.launch {
        repo.delete(uid).fold(onSuccess = {
            _emails.value = _emails.value.filter { it.uid != uid }
            close()
        }, onFailure = { _error.value = it.message })
    }

    fun send(accountId: Int, to: String, subject: String, body: String,
             cc: String = "", replyTo: String? = null) = viewModelScope.launch {
        repo.send(SendEmailRequest(accountId, to, subject, body, cc, replyTo)).fold(
            onSuccess = { _error.value = null },
            onFailure = { _error.value = it.message }
        )
    }

    fun generateAiReply(uid: String) = viewModelScope.launch {
        _aiReply.value = "Generating…"
        repo.aiReply(uid).fold(
            onSuccess = { _aiReply.value = it["reply"] ?: it["response"] ?: "" },
            onFailure = { _aiReply.value = "Error: ${it.message}" }
        )
    }

    fun search(q: String) = viewModelScope.launch {
        if (q.isBlank()) { load(); return@launch }
        _loading.value = true
        repo.search(q).fold(
            onSuccess = { _emails.value = it },
            onFailure = { _error.value = it.message }
        )
        _loading.value = false
    }

    fun clearError() { _error.value = null }
}
