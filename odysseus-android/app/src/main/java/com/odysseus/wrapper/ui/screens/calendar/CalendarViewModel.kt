package com.odysseus.wrapper.ui.screens.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odysseus.wrapper.data.remote.*
import com.odysseus.wrapper.data.repository.CalendarRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CalendarViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CalendarRepository()

    private val _events     = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events

    private val _calendars  = MutableStateFlow<List<CalendarItem>>(emptyList())
    val calendars: StateFlow<List<CalendarItem>> = _calendars

    private val _loading    = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error      = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus

    init { load() }

    fun load() = viewModelScope.launch {
        _loading.value = true
        repo.calendars().fold(onSuccess = { _calendars.value = it }, onFailure = {})
        repo.events().fold(
            onSuccess = { _events.value = it.sortedBy { e -> e.dtstart } },
            onFailure = { _error.value = it.message }
        )
        _loading.value = false
    }

    fun createEvent(req: EventCreateRequest) = viewModelScope.launch {
        repo.createEvent(req).fold(
            onSuccess = { _events.value = (_events.value + it).sortedBy { e -> e.dtstart } },
            onFailure = { _error.value = it.message }
        )
    }

    fun updateEvent(uid: String, req: EventUpdateRequest) = viewModelScope.launch {
        repo.updateEvent(uid, req).fold(
            onSuccess = { updated ->
                _events.value = _events.value.map { if (it.uid == uid) updated else it }
            },
            onFailure = { _error.value = it.message }
        )
    }

    fun deleteEvent(uid: String) = viewModelScope.launch {
        repo.deleteEvent(uid).fold(
            onSuccess = { _events.value = _events.value.filter { it.uid != uid } },
            onFailure = { _error.value = it.message }
        )
    }

    fun sync() = viewModelScope.launch {
        _syncStatus.value = "Syncing…"
        repo.sync().fold(
            onSuccess = { _syncStatus.value = "Synced ✓"; load() },
            onFailure = { _syncStatus.value = "Sync failed: ${it.message}" }
        )
    }

    fun clearError() { _error.value = null }
    fun clearSyncStatus() { _syncStatus.value = null }
}
