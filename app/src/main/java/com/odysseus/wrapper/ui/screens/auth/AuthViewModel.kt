package com.odysseus.wrapper.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odysseus.wrapper.core.NetworkClient
import com.odysseus.wrapper.core.UserPreferences
import com.odysseus.wrapper.data.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle    : AuthState()
    object Loading : AuthState()
    data class Success(val username: String) : AuthState()
    data class Error(val message: String)    : AuthState()
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository()
    val prefs = UserPreferences(app)

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    /** Empty string = not configured yet → show FirstRunScreen */
    val savedServerUrl = prefs.serverUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val darkMode       = prefs.darkMode.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val firstRunDone   = prefs.firstRunDone.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun login(username: String, password: String) = viewModelScope.launch {
        _state.value = AuthState.Loading
        repo.login(username, password).fold(
            onSuccess = { resp ->
                if (resp.ok) {
                    prefs.setLoggedIn(true)
                    prefs.setUsername(resp.username ?: username)
                    _state.value = AuthState.Success(resp.username ?: username)
                } else {
                    _state.value = AuthState.Error(
                        if (resp.requires_totp) "2FA required — not supported in app yet"
                        else "Login failed. Check username and password."
                    )
                }
            },
            onFailure = {
                _state.value = AuthState.Error(
                    "Cannot reach server at ${NetworkClient.baseUrl}\n${it.message}"
                )
            }
        )
    }

    fun logout() = viewModelScope.launch {
        repo.logout()
        NetworkClient.clearSession()
        prefs.clear()
        _state.value = AuthState.Idle
    }

    fun setServerUrl(url: String) = viewModelScope.launch {
        val clean = url.trim().let { if (it.isNotEmpty() && !it.endsWith("/")) "$it/" else it }
        prefs.setServerUrl(clean)
        NetworkClient.baseUrl = clean
    }

    fun setBearerToken(token: String) = viewModelScope.launch {
        val t = token.trim()
        prefs.setBearerToken(t)
        NetworkClient.bearerToken = t
    }

    fun completeFirstRun(url: String, token: String = "") = viewModelScope.launch {
        setServerUrl(url)
        if (token.isNotBlank()) setBearerToken(token)
        prefs.setFirstRunDone(true)
    }

    fun resetState() { _state.value = AuthState.Idle }
}
