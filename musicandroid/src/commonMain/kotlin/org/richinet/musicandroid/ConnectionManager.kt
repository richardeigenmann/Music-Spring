package org.richinet.musicandroid

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

enum class ConnectionState {
    CONNECTED,
    LOCAL_MODE,
    CONNECTING
}

class ConnectionManager(
    private val networkObserver: NetworkObserver,
    private val apiService: ApiService,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTING)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val isConnected: Boolean get() = _connectionState.value == ConnectionState.CONNECTED
    val isLocalMode: Boolean get() = _connectionState.value == ConnectionState.LOCAL_MODE

    init {
        scope.launch {
            // Re-evaluate when manual mode or base url changes
            launch {
                settingsRepository.isManualLocalMode.collectLatest { manualOffline ->
                    if (manualOffline) {
                        _connectionState.value = ConnectionState.LOCAL_MODE
                    } else {
                        verifyReachability()
                    }
                }
            }

            // Re-evaluate when network connectivity status changes
            launch {
                networkObserver.isOnline.collectLatest { online ->
                    val manualOffline = settingsRepository.isManualLocalMode.first()
                    if (manualOffline) {
                        _connectionState.value = ConnectionState.LOCAL_MODE
                    } else if (!online) {
                        _connectionState.value = ConnectionState.LOCAL_MODE
                    } else {
                        verifyReachability()
                    }
                }
            }
        }
    }

    suspend fun verifyReachability(): Boolean {
        val manualOffline = settingsRepository.isManualLocalMode.first()
        if (manualOffline) {
            _connectionState.value = ConnectionState.LOCAL_MODE
            return false
        }

        _connectionState.value = ConnectionState.CONNECTING
        val isReachable = withTimeoutOrNull(2500) {
            try {
                apiService.getVersion()
                true
            } catch (e: Exception) {
                false
            }
        } ?: false

        _connectionState.value = if (isReachable) ConnectionState.CONNECTED else ConnectionState.LOCAL_MODE
        return isReachable
    }

    suspend fun connectNow(): Boolean {
        settingsRepository.setManualLocalMode(false)
        return verifyReachability()
    }

    suspend fun disconnectNow() {
        settingsRepository.setManualLocalMode(true)
        _connectionState.value = ConnectionState.LOCAL_MODE
    }

    fun markCallFailed() {
        // If an API call fails unexpectedly, update state to LOCAL_MODE
        _connectionState.value = ConnectionState.LOCAL_MODE
    }
}
