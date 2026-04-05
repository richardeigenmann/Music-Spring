package org.richinet.musicandroid

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val BACKEND_URL = stringPreferencesKey("backend_url")

    val baseUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[BACKEND_URL] ?: "http://octan:8011"
    }

    suspend fun updateBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[BACKEND_URL] = url
        }
    }
}
