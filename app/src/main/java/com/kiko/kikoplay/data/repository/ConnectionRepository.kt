package com.kiko.kikoplay.data.repository

import com.kiko.kikoplay.data.local.dao.ConnectionDao
import com.kiko.kikoplay.data.local.entity.ConnectionEntity
import com.kiko.kikoplay.data.remote.ConnectionInfo
import com.kiko.kikoplay.data.remote.ConnectionManager
import com.kiko.kikoplay.data.remote.KikoPlayApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepository @Inject constructor(
    private val connectionDao: ConnectionDao,
    private val connectionManager: ConnectionManager,
    private val api: KikoPlayApi
) {
    val connectionHistory: Flow<List<ConnectionEntity>> = connectionDao.getAll()
    val activeConnection: StateFlow<ConnectionInfo?> = connectionManager.connection

    val isConnected: Boolean get() = connectionManager.isConnected

    suspend fun connect(host: String, port: Int, deviceName: String? = null): Result<Unit> {
        val previousConnection = connectionManager.connection.value
        return try {
            connectionManager.connect(host, port, deviceName)
            // Validate by calling playstate
            api.getPlayState()
            // Save to history
            val existing = connectionDao.findByAddress(host, port)
            if (existing != null) {
                connectionDao.updateLastConnected(existing.id)
            } else {
                connectionDao.insert(
                    ConnectionEntity(
                        host = host,
                        port = port,
                        deviceName = deviceName,
                        lastConnected = System.currentTimeMillis()
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (previousConnection != null) {
                connectionManager.connect(
                    previousConnection.host,
                    previousConnection.port,
                    previousConnection.deviceName
                )
            } else {
                connectionManager.disconnect()
            }
            Result.failure(e)
        }
    }

    suspend fun connect(entity: ConnectionEntity): Result<Unit> {
        return connect(entity.host, entity.port, entity.deviceName)
    }

    suspend fun getLastConnection(): ConnectionEntity? {
        return connectionDao.getLatest()
    }

    suspend fun reconnectLastConnection(): Result<Boolean> {
        if (connectionManager.isConnected) return Result.success(false)

        val lastConnection = connectionDao.getLatest() ?: return Result.success(false)
        return connect(lastConnection).map { true }
    }

    fun disconnect() {
        connectionManager.disconnect()
    }

    suspend fun deleteHistory(entity: ConnectionEntity) {
        connectionDao.delete(entity)
    }
}
