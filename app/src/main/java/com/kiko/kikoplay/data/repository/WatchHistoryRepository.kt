package com.kiko.kikoplay.data.repository

import com.kiko.kikoplay.data.local.dao.WatchHistoryDao
import com.kiko.kikoplay.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryRepository @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao,
    private val historyThumbnailRepository: HistoryThumbnailRepository
) {
    fun getRecent(limit: Int = 20): Flow<List<WatchHistoryEntity>> =
        watchHistoryDao.getRecent(limit.coerceAtLeast(20)).map { items ->
            mergeHistory(items).take(limit)
        }

    fun getAll(): Flow<List<WatchHistoryEntity>> =
        watchHistoryDao.getAll().map(::mergeHistory)

    suspend fun record(
        mediaId: String,
        title: String,
        animeTitle: String?,
        playTime: Long,
        duration: Long,
        playTimeState: Int,
        sourceType: Int,
        isCached: Boolean,
        remoteUri: String?,
        localPath: String?,
        danmuPool: String?,
        serverAddress: String?
    ) {
        val normalizedSourceType = when {
            sourceType == SOURCE_TYPE_CACHE && !serverAddress.isNullOrBlank() -> SOURCE_TYPE_PC
            else -> sourceType
        }
        val existing = when (normalizedSourceType) {
            SOURCE_TYPE_LOCAL -> watchHistoryDao.findLocalExisting(
                mediaId = mediaId,
                localPath = localPath
            )
            else -> watchHistoryDao.findRemoteOrCachedExisting(
                mediaId = mediaId,
                serverAddress = serverAddress
            )
        }
        val thumbnailData = when {
            existing?.thumbnailData != null && existing.localPath == localPath -> existing.thumbnailData
            else -> historyThumbnailRepository.createThumbnail(localPath) ?: existing?.thumbnailData
        }
        watchHistoryDao.upsert(
            WatchHistoryEntity(
                id = existing?.id ?: 0,
                mediaId = mediaId,
                title = title,
                animeTitle = animeTitle,
                playTime = playTime,
                duration = duration,
                playTimeState = playTimeState,
                sourceType = normalizedSourceType,
                isCached = isCached,
                remoteUri = remoteUri,
                localPath = localPath,
                thumbnailData = thumbnailData,
                lastWatched = System.currentTimeMillis(),
                danmuPool = danmuPool,
                serverAddress = serverAddress
            )
        )
    }

    suspend fun delete(entity: WatchHistoryEntity) {
        if (entity.sourceType == SOURCE_TYPE_LOCAL) {
            watchHistoryDao.delete(entity)
        } else {
            watchHistoryDao.deleteRemoteAndCachedByMediaId(
                mediaId = entity.mediaId,
                serverAddress = entity.serverAddress
            )
        }
    }

    suspend fun clearCachedState(
        mediaId: String,
        serverAddress: String?
    ) {
        watchHistoryDao.clearCachedStateByMediaId(
            mediaId = mediaId,
            serverAddress = serverAddress
        )
    }

    suspend fun clearAll() = watchHistoryDao.clearAll()

    private fun mergeHistory(items: List<WatchHistoryEntity>): List<WatchHistoryEntity> {
        val merged = LinkedHashMap<String, WatchHistoryEntity>()
        items.forEach { item ->
            val key = historyKey(item)
            val existing = merged[key]
            merged[key] = if (existing == null) item else mergeEntries(existing, item)
        }
        return merged.values.toList()
    }

    private fun historyKey(item: WatchHistoryEntity): String {
        return when (item.sourceType) {
            SOURCE_TYPE_LOCAL -> "local:${item.localPath ?: item.mediaId}"
            else -> "remote:${item.serverAddress.orEmpty()}:${item.mediaId}"
        }
    }

    private fun mergeEntries(
        current: WatchHistoryEntity,
        incoming: WatchHistoryEntity
    ): WatchHistoryEntity {
        val preferredIdentity = when {
            current.sourceType == SOURCE_TYPE_PC -> current
            incoming.sourceType == SOURCE_TYPE_PC -> incoming
            else -> current
        }
        val newest = if (current.lastWatched >= incoming.lastWatched) current else incoming

        return preferredIdentity.copy(
            title = newest.title,
            animeTitle = newest.animeTitle ?: preferredIdentity.animeTitle,
            playTime = newest.playTime,
            duration = maxOf(current.duration, incoming.duration, newest.duration),
            playTimeState = newest.playTimeState,
            sourceType = preferredIdentity.sourceType,
            isCached = current.isCached || incoming.isCached || current.sourceType == SOURCE_TYPE_CACHE || incoming.sourceType == SOURCE_TYPE_CACHE,
            remoteUri = preferredIdentity.remoteUri ?: newest.remoteUri,
            localPath = newest.localPath ?: current.localPath ?: incoming.localPath,
            thumbnailData = newest.thumbnailData ?: preferredIdentity.thumbnailData,
            lastWatched = maxOf(current.lastWatched, incoming.lastWatched),
            danmuPool = newest.danmuPool ?: preferredIdentity.danmuPool,
            serverAddress = preferredIdentity.serverAddress ?: newest.serverAddress
        )
    }

    private companion object {
        const val SOURCE_TYPE_PC = 0
        const val SOURCE_TYPE_LOCAL = 1
        const val SOURCE_TYPE_CACHE = 2
    }
}
