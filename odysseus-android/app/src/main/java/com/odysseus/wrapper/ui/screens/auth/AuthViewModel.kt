package com.odysseus.wrapper.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odysseus.wrapper.core.NetworkClient
import com.odysseus.wrapper.core.UserPreferences
import com.odysseus.wrapper.data.remote.AuthApiService
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
    /** "dark" | "light" | "system" */
    val theme          = prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, "system")
    /** Legacy bool alias — true when theme == "dark" */
    val darkMode       = prefs.darkMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val firstRunDone   = prefs.firstRunDone.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    /** Raw ody_… token stored after first login; empty string = not yet created. */
    val autoToken      = prefs.autoToken.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // ── Sidebar visibility StateFlows ─────────────────────────────────────────
    val sbSessions = prefs.sbSessions.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val sbEmail    = prefs.sbEmail.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val sbNotes    = prefs.sbNotes.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val sbTasks    = prefs.sbTasks.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val sbCalendar = prefs.sbCalendar.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val sbDocs     = prefs.sbDocs.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val sbGallery  = prefs.sbGallery.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val sbResearch = prefs.sbResearch.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val sbMemory   = prefs.sbMemory.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun login(username: String, password: String) = viewModelScope.launch {
        _state.value = AuthState.Loading
        repo.login(username, password).fold(
            onSuccess = { resp ->
                if (resp.ok) {
                    prefs.setLoggedIn(true)
                    prefs.setUsername(resp.username ?: username)
                    _state.value = AuthState.Success(resp.username ?: username)
                    // Auto-create a mobile token on first login if one doesn't exist yet
                    _ensureAutoToken(resp.username ?: username)
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

    /** Creates a mobile token silently if autoToken is blank, persists it locally. */
    private fun _ensureAutoToken(username: String) = viewModelScope.launch {
        val existing = prefs.autoToken.first()
        if (existing.isNotBlank()) return@launch          // already created
        try {
            val api = NetworkClient.create<AuthApiService>()
            val r = api.createMobileToken("Odysseus Android — $username")
            if (r.isSuccessful) {
                val token = r.body()?.token ?: return@launch
                prefs.setAutoToken(token)
                // Also set as bearer so the app is immediately authenticated
                if (NetworkClient.bearerToken.isBlank()) {
                    prefs.setBearerToken(token)
                    NetworkClient.bearerToken = token
                }
            }
        } catch (_: Exception) { /* silently ignore — user can generate manually */ }
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

    fun setTheme(t: String) = viewModelScope.launch { prefs.setTheme(t) }

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
