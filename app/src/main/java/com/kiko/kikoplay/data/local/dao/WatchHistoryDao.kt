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

    @Query(
        """
        SELECT * FROM watch_history
        WHERE mediaId = :mediaId
          AND sourceType = 1
          AND ((localPath IS NULL AND :localPath IS NULL) OR localPath = :localPath)
        LIMIT 1
        """
    )
    suspend fun findLocalExisting(
        mediaId: String,
        localPath: String?
    ): WatchHistoryEntity?

    @Query(
        """
        SELECT * FROM watch_history
        WHERE mediaId = :mediaId
          AND sourceType IN (0, 2)
          AND ((serverAddress IS NULL AND :serverAddress IS NULL) OR serverAddress = :serverAddress)
        ORDER BY CASE WHEN sourceType = 0 THEN 0 ELSE 1 END, lastWatched DESC
        LIMIT 1
        """
    )
    suspend fun findRemoteOrCachedExisting(
        mediaId: String,
        serverAddress: String?
    ): WatchHistoryEntity?

    @Upsert
    suspend fun upsert(entity: WatchHistoryEntity)

    @Delete
    suspend fun delete(entity: WatchHistoryEntity)

    @Query(
        """
        DELETE FROM watch_history
        WHERE mediaId = :mediaId
          AND sourceType IN (0, 2)
          AND ((serverAddress IS NULL AND :serverAddress IS NULL) OR serverAddress = :serverAddress)
        """
    )
    suspend fun deleteRemoteAndCachedByMediaId(
        mediaId: String,
        serverAddress: String?
    )

    @Query(
        """
        UPDATE watch_history
        SET sourceType = 0,
            isCached = 0,
            localPath = NULL
        WHERE mediaId = :mediaId
          AND sourceType IN (0, 2)
          AND ((serverAddress IS NULL AND :serverAddress IS NULL) OR serverAddress = :serverAddress)
        """
    )
    suspend fun clearCachedStateByMediaId(
        mediaId: String,
        serverAddress: String?
    )

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}
