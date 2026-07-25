package com.odysseus.wrapper.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "odysseus_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_SERVER_URL    = stringPreferencesKey("server_url")
        val KEY_USERNAME      = stringPreferencesKey("username")
        val KEY_LOGGED_IN     = booleanPreferencesKey("logged_in")
        // theme: "dark" | "light" | "system"
        val KEY_THEME         = stringPreferencesKey("theme")
        val KEY_FIRST_RUN     = booleanPreferencesKey("first_run_done")
        val KEY_BEARER_TOKEN  = stringPreferencesKey("bearer_token")
        // auto-token shown once after login
        val KEY_AUTO_TOKEN    = stringPreferencesKey("auto_token")

        // Sidebar section visibility — one key per section (default true = visible)
        val KEY_SIDEBAR_SESSIONS  = booleanPreferencesKey("sb_sessions")
        val KEY_SIDEBAR_EMAIL     = booleanPreferencesKey("sb_email")
        val KEY_SIDEBAR_NOTES     = booleanPreferencesKey("sb_notes")
        val KEY_SIDEBAR_TASKS     = booleanPreferencesKey("sb_tasks")
        val KEY_SIDEBAR_CALENDAR  = booleanPreferencesKey("sb_calendar")
        val KEY_SIDEBAR_DOCS      = booleanPreferencesKey("sb_library")
        val KEY_SIDEBAR_GALLERY   = booleanPreferencesKey("sb_gallery")
        val KEY_SIDEBAR_RESEARCH  = booleanPreferencesKey("sb_research")
        val KEY_SIDEBAR_MEMORY    = booleanPreferencesKey("sb_memory")

        // Convenience list for sidebar toggle iteration
        val SIDEBAR_KEYS = listOf(
            "sessions"  to KEY_SIDEBAR_SESSIONS,
            "email"     to KEY_SIDEBAR_EMAIL,
            "notes"     to KEY_SIDEBAR_NOTES,
            "tasks"     to KEY_SIDEBAR_TASKS,
            "calendar"  to KEY_SIDEBAR_CALENDAR,
            "library"   to KEY_SIDEBAR_DOCS,
            "gallery"   to KEY_SIDEBAR_GALLERY,
            "research"  to KEY_SIDEBAR_RESEARCH,
            "memory"    to KEY_SIDEBAR_MEMORY,
        )
    }

    /** Empty string means not configured yet — triggers FirstRunScreen. */
    val serverUrl: Flow<String>     = context.dataStore.data.map { it[KEY_SERVER_URL] ?: "" }
    val username: Flow<String>      = context.dataStore.data.map { it[KEY_USERNAME] ?: "" }
    val isLoggedIn: Flow<Boolean>   = context.dataStore.data.map { it[KEY_LOGGED_IN] ?: false }
    /** "dark" | "light" | "system" */
    val theme: Flow<String>         = context.dataStore.data.map { it[KEY_THEME] ?: "system" }
    // Legacy alias kept for callers that still use darkMode
    val darkMode: Flow<Boolean>     = context.dataStore.data.map { (it[KEY_THEME] ?: "system") == "dark" }
    val firstRunDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_FIRST_RUN] ?: false }
    val bearerToken: Flow<String>   = context.dataStore.data.map { it[KEY_BEARER_TOKEN] ?: "" }
    val autoToken: Flow<String>     = context.dataStore.data.map { it[KEY_AUTO_TOKEN] ?: "" }

    // Sidebar flows (default = visible)
    val sbSessions : Flow<Boolean> = context.dataStore.data.map { it[KEY_SIDEBAR_SESSIONS]  ?: true }
    val sbEmail    : Flow<Boolean> = context.dataStore.data.map { it[KEY_SIDEBAR_EMAIL]     ?: true }
    val sbNotes    : Flow<Boolean> = context.dataStore.data.map { it[KEY_SIDEBAR_NOTES]     ?: true }
    val sbTasks    : Flow<Boolean> = context.dataStore.data.map { it[KEY_SIDEBAR_TASKS]     ?: true }
    val sbCalendar : Flow<Boolean> = context.dataStore.data.map { it[KEY_SIDEBAR_CALENDAR]  ?: true }
    val sbDocs     : Flow<Boolean> = context.dataStore.data.map { it[KEY_SIDEBAR_DOCS]      ?: true }
    val sbGallery  : Flow<Boolean> = context.dataStore.data.map { it[KEY_SIDEBAR_GALLERY]   ?: true }
    val sbResearch : Flow<Boolean> = context.dataStore.data.map { it[KEY_SIDEBAR_RESEARCH]  ?: true }
    val sbMemory   : Flow<Boolean> = context.dataStore.data.map { it[KEY_SIDEBAR_MEMORY]    ?: true }

    suspend fun setServerUrl(url: String) = context.dataStore.edit {
        val clean = url.trim().let { if (it.isNotEmpty() && !it.endsWith("/")) "$it/" else it }
        it[KEY_SERVER_URL] = clean
    }
    suspend fun setUsername(name: String)      = context.dataStore.edit { it[KEY_USERNAME] = name }
    suspend fun setLoggedIn(v: Boolean)        = context.dataStore.edit { it[KEY_LOGGED_IN] = v }
    suspend fun setTheme(t: String)            = context.dataStore.edit { it[KEY_THEME] = t }
    // Legacy alias
    suspend fun setDarkMode(v: Boolean)        = setTheme(if (v) "dark" else "light")
    suspend fun setFirstRunDone(v: Boolean)    = context.dataStore.edit { it[KEY_FIRST_RUN] = v }
    suspend fun setBearerToken(token: String)  = context.dataStore.edit { it[KEY_BEARER_TOKEN] = token }
    suspend fun setAutoToken(token: String)    = context.dataStore.edit { it[KEY_AUTO_TOKEN] = token }

    suspend fun setSidebarVisible(key: Preferences.Key<Boolean>, visible: Boolean) =
        context.dataStore.edit { it[key] = visible }

    suspend fun clear() = context.dataStore.edit {
        it.remove(KEY_USERNAME)
        it.remove(KEY_LOGGED_IN)
        it.remove(KEY_AUTO_TOKEN)
        // Keep server URL, theme, bearer token, sidebar toggles after logout
    }
}
