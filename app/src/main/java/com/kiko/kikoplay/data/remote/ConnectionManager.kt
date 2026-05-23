package com.kiko.kikoplay.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class ConnectionInfo(
    val host: String,
    val port: Int,
    val deviceName: String? = null,
    val kikoVersion: Int? = null
) {
    val baseUrl: String get() = "http://$host:$port/"
}

@Singleton
class ConnectionManager @Inject constructor() {

    private val _connection = MutableStateFlow<ConnectionInfo?>(null)
    val connection: StateFlow<ConnectionInfo?> = _connection.asStateFlow()

    val isConnected: Boolean get() = _connection.value != null

    fun connect(host: String, port: Int, deviceName: String? = null, kikoVersion: Int? = null) {
        _connection.value = ConnectionInfo(host, port, deviceName, kikoVersion)
    }

    fun disconnect() {
        _connection.value = null
    }

    fun updateKikoVersion(kikoVersion: Int?) {
        _connection.update { connection ->
            connection?.copy(kikoVersion = kikoVersion)
        }
    }

    fun getBaseUrl(): String {
        return _connection.value?.baseUrl ?: "http://localhost/"
    }
}
