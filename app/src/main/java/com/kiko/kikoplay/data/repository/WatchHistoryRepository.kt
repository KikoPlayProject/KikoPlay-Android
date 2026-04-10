package com.kiko.kikoplay.data.repository

import com.kiko.kikoplay.data.local.dao.WatchHistoryDao
import com.kiko.kikoplay.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryRepository @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao
) {
    fun getRecent(limit: Int = 20): Flow<List<WatchHistoryEntity>> = watchHistoryDao.getRecent(limit)

    fun getAll(): Flow<List<WatchHistoryEntity>> = watchHistoryDao.getAll()

    suspend fun record(
        mediaId: String,
        title: String,
        animeTitle: String?,
        playTime: Long,
        duration: Long,
        playTimeState: Int,
        sourceType: Int,
        danmuPool: String?,
        serverAddress: String?
    ) {
        val existing = watchHistoryDao.getByMediaId(mediaId)
        watchHistoryDao.upsert(
            WatchHistoryEntity(
                id = existing?.id ?: 0,
                mediaId = mediaId,
                title = title,
                animeTitle = animeTitle,
                playTime = playTime,
                duration = duration,
                playTimeState = playTimeState,
                sourceType = sourceType,
                thumbnailPath = existing?.thumbnailPath,
                lastWatched = System.currentTimeMillis(),
                danmuPool = danmuPool,
                serverAddress = serverAddress
            )
        )
    }

    suspend fun delete(entity: WatchHistoryEntity) = watchHistoryDao.delete(entity)

    suspend fun clearAll() = watchHistoryDao.clearAll()
}
