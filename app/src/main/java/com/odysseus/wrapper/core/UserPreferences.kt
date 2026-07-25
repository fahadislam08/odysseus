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
        val KEY_DARK_MODE     = booleanPreferencesKey("dark_mode")
        val KEY_FIRST_RUN     = booleanPreferencesKey("first_run_done")
        val KEY_BEARER_TOKEN  = stringPreferencesKey("bearer_token")
    }

    /** Empty string means not configured yet — triggers FirstRunScreen. */
    val serverUrl: Flow<String>     = context.dataStore.data.map { it[KEY_SERVER_URL] ?: "" }
    val username: Flow<String>      = context.dataStore.data.map { it[KEY_USERNAME] ?: "" }
    val isLoggedIn: Flow<Boolean>   = context.dataStore.data.map { it[KEY_LOGGED_IN] ?: false }
    val darkMode: Flow<Boolean>     = context.dataStore.data.map { it[KEY_DARK_MODE] ?: true }
    val firstRunDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_FIRST_RUN] ?: false }
    val bearerToken: Flow<String>   = context.dataStore.data.map { it[KEY_BEARER_TOKEN] ?: "" }

    suspend fun setServerUrl(url: String) = context.dataStore.edit {
        val clean = url.trim().let { if (it.isNotEmpty() && !it.endsWith("/")) "$it/" else it }
        it[KEY_SERVER_URL] = clean
    }
    suspend fun setUsername(name: String)      = context.dataStore.edit { it[KEY_USERNAME] = name }
    suspend fun setLoggedIn(v: Boolean)        = context.dataStore.edit { it[KEY_LOGGED_IN] = v }
    suspend fun setDarkMode(v: Boolean)        = context.dataStore.edit { it[KEY_DARK_MODE] = v }
    suspend fun setFirstRunDone(v: Boolean)    = context.dataStore.edit { it[KEY_FIRST_RUN] = v }
    suspend fun setBearerToken(token: String)  = context.dataStore.edit { it[KEY_BEARER_TOKEN] = token }

    suspend fun clear() = context.dataStore.edit {
        it.remove(KEY_USERNAME)
        it.remove(KEY_LOGGED_IN)
        // Keep server URL, dark mode, and bearer token after logout
    }
}
