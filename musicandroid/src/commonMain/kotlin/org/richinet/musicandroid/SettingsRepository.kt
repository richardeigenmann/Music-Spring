package org.richinet.musicandroid

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val BACKEND_URL = stringPreferencesKey("backend_url")
    private val MANUAL_LOCAL_MODE = booleanPreferencesKey("manual_local_mode")

    val baseUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[BACKEND_URL] ?: "http://octan:8011"
    }

    val isManualLocalMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[MANUAL_LOCAL_MODE] ?: false
    }

    suspend fun updateBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[BACKEND_URL] = url
        }
    }

    suspend fun setManualLocalMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[MANUAL_LOCAL_MODE] = enabled
        }
    }
}
