package com.kiko.kikoplay.data.local.dao

import androidx.room.*
import com.kiko.kikoplay.data.local.entity.ConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {

    @Query("SELECT * FROM connections ORDER BY lastConnected DESC")
    fun getAll(): Flow<List<ConnectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ConnectionEntity): Long

    @Delete
    suspend fun delete(entity: ConnectionEntity)

    @Query("UPDATE connections SET lastConnected = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM connections WHERE host = :host AND port = :port LIMIT 1")
    suspend fun findByAddress(host: String, port: Int): ConnectionEntity?
}
