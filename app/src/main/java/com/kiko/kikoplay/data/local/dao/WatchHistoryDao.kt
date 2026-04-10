package com.kiko.kikoplay.data.local.dao

import androidx.room.*
import com.kiko.kikoplay.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Query("SELECT * FROM watch_history ORDER BY lastWatched DESC")
    fun getAll(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history ORDER BY lastWatched DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getByMediaId(mediaId: String): WatchHistoryEntity?

    @Upsert
    suspend fun upsert(entity: WatchHistoryEntity)

    @Delete
    suspend fun delete(entity: WatchHistoryEntity)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}
