package com.kiko.kikoplay.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiko.kikoplay.data.local.entity.ConnectionEntity
import com.kiko.kikoplay.data.remote.ConnectionInfo
import com.kiko.kikoplay.data.repository.ConnectionRepository
import com.kiko.kikoplay.util.DiscoveredDevice
import com.kiko.kikoplay.util.NetworkScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionUiState(
    val isScanning: Boolean = false,
    val discoveredDevices: List<DiscoveredDevice> = emptyList(),
    val manualHost: String = "",
    val manualPort: String = "8000",
    val isConnecting: Boolean = false,
    val connectionError: String? = null,
    val isConnected: Boolean = false
)

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    val connectionHistory: StateFlow<List<ConnectionEntity>> =
        connectionRepository.connectionHistory.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    val activeConnection: StateFlow<ConnectionInfo?> = connectionRepository.activeConnection

    private var scanJob: Job? = null

    init {
        _uiState.update { it.copy(isConnected = connectionRepository.isConnected) }
    }

    fun startScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = true, discoveredDevices = emptyList()) }
        scanJob = viewModelScope.launch {
            NetworkScanner.scan().collect { device ->
                _uiState.update { state ->
                    val devices = state.discoveredDevices.toMutableList()
                    if (devices.none { it.host == device.host && it.port == device.port }) {
                        devices.add(device)
                    }
                    state.copy(discoveredDevices = devices)
                }
            }
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
    }

    fun updateManualHost(host: String) {
        _uiState.update { it.copy(manualHost = host, connectionError = null) }
    }

    fun updateManualPort(port: String) {
        _uiState.update { it.copy(manualPort = port, connectionError = null) }
    }

    fun connect(host: String, port: Int, deviceName: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, connectionError = null) }
            val result = connectionRepository.connect(host, port, deviceName)
            _uiState.update {
                it.copy(
                    isConnecting = false,
                    isConnected = result.isSuccess,
                    connectionError = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun connectManual() {
        val host = _uiState.value.manualHost.trim()
        val port = _uiState.value.manualPort.trim().toIntOrNull() ?: 8000
        if (host.isBlank()) {
            _uiState.update { it.copy(connectionError = "请输入服务地址") }
            return
        }
        connect(host, port)
    }

    fun connectFromHistory(entity: ConnectionEntity) {
        connect(entity.host, entity.port, entity.deviceName)
    }

    fun deleteHistory(entity: ConnectionEntity) {
        viewModelScope.launch {
            connectionRepository.deleteHistory(entity)
        }
    }

    fun disconnect() {
        connectionRepository.disconnect()
        _uiState.update { it.copy(isConnected = false) }
    }
}
