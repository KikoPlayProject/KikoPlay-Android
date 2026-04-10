package com.kiko.kikoplay.data.local.dao

import androidx.room.*
import com.kiko.kikoplay.data.local.entity.CacheTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheTaskDao {

    @Query("SELECT * FROM cache_tasks WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    fun getByStatus(status: Int): Flow<List<CacheTaskEntity>>

    @Query("SELECT * FROM cache_tasks WHERE status IN (0, 1, 2) ORDER BY priority DESC, createdAt ASC")
    fun getActiveTasks(): Flow<List<CacheTaskEntity>>

    @Query("SELECT * FROM cache_tasks WHERE status = 3 ORDER BY completedAt DESC")
    fun getCompleted(): Flow<List<CacheTaskEntity>>

    @Query("SELECT * FROM cache_tasks ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CacheTaskEntity>>

    @Query("SELECT * FROM cache_tasks WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getByMediaId(mediaId: String): CacheTaskEntity?

    @Query("SELECT * FROM cache_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CacheTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CacheTaskEntity): Long

    @Update
    suspend fun update(entity: CacheTaskEntity)

    @Delete
    suspend fun delete(entity: CacheTaskEntity)

    @Query("UPDATE cache_tasks SET downloadedBytes = :bytes WHERE id = :id")
    suspend fun updateProgress(id: Long, bytes: Long)

    @Query("UPDATE cache_tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int)
}
