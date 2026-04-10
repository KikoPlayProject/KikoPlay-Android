package com.kiko.kikoplay.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ConnectionInfo(
    val host: String,
    val port: Int,
    val deviceName: String? = null
) {
    val baseUrl: String get() = "http://$host:$port/"
}

@Singleton
class ConnectionManager @Inject constructor() {

    private val _connection = MutableStateFlow<ConnectionInfo?>(null)
    val connection: StateFlow<ConnectionInfo?> = _connection.asStateFlow()

    val isConnected: Boolean get() = _connection.value != null

    fun connect(host: String, port: Int, deviceName: String? = null) {
        _connection.value = ConnectionInfo(host, port, deviceName)
    }

    fun disconnect() {
        _connection.value = null
    }

    fun getBaseUrl(): String {
        return _connection.value?.baseUrl ?: "http://localhost/"
    }
}
