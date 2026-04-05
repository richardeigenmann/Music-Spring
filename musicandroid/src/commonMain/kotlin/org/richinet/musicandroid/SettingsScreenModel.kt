package org.richinet.musicandroid

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsScreenModel(private val settingsRepository: SettingsRepository) : ScreenModel {
    val baseUrl: StateFlow<String> = settingsRepository.baseUrl
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), "http://octan:8011")

    fun updateBaseUrl(url: String) {
        screenModelScope.launch {
            settingsRepository.updateBaseUrl(url)
        }
    }
}
