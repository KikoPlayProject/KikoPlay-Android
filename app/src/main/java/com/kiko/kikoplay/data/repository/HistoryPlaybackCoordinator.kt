package com.kiko.kikoplay.data.repository

import com.kiko.kikoplay.data.local.entity.WatchHistoryEntity
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class HistoryPlaybackTarget(
    val mediaId: String,
    val title: String,
    val sourceType: Int,
    val danmuPool: String?,
    val animeTitle: String?,
    val serverAddress: String? = null,
    val localPath: String? = null,
    val startPositionMs: Long = 0L,
    val initialPlayTimeState: Int = 0
)

data class SwitchConnectionRequest(
    val historyItem: WatchHistoryEntity,
    val currentAddress: String,
    val targetAddress: String
)

sealed interface HistoryPlaybackDecision {
    data class Play(val target: HistoryPlaybackTarget) : HistoryPlaybackDecision
    data class ConfirmSwitch(val request: SwitchConnectionRequest) : HistoryPlaybackDecision
    data object None : HistoryPlaybackDecision
}

@Singleton
class HistoryPlaybackCoordinator @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val cacheRepository: CacheRepository
) {

    suspend fun resolve(item: WatchHistoryEntity): HistoryPlaybackDecision {
        val localTarget = getStoredLocalTarget(item)
        if (localTarget != null) {
            return HistoryPlaybackDecision.Play(localTarget)
        }

        val cachedTarget = getCachedTarget(item)
        if (cachedTarget != null) {
            return HistoryPlaybackDecision.Play(cachedTarget)
        }

        if (item.sourceType == SOURCE_TYPE_LOCAL || item.sourceType == SOURCE_TYPE_CACHE) {
            return HistoryPlaybackDecision.None
        }

        val targetAddress = item.serverAddress?.takeIf { it.isNotBlank() }
            ?: return HistoryPlaybackDecision.None

        val activeConnection = connectionRepository.activeConnection.value
        val currentAddress = activeConnection?.let { "${it.host}:${it.port}" }

        return when {
            currentAddress == targetAddress -> HistoryPlaybackDecision.Play(item.toRemoteTarget())
            currentAddress != null -> HistoryPlaybackDecision.ConfirmSwitch(
                SwitchConnectionRequest(
                    historyItem = item,
                    currentAddress = currentAddress,
                    targetAddress = targetAddress
                )
            )
            else -> connectAndResolve(item)
        }
    }

    suspend fun connectAndResolve(item: WatchHistoryEntity): HistoryPlaybackDecision {
        if (item.sourceType != SOURCE_TYPE_PC) {
            return HistoryPlaybackDecision.None
        }

        val targetAddress = item.serverAddress?.takeIf { it.isNotBlank() }
            ?: return HistoryPlaybackDecision.None
        val (host, port) = parseServerAddress(targetAddress) ?: return HistoryPlaybackDecision.None

        val result = connectionRepository.connect(host, port)
        if (result.isFailure) return HistoryPlaybackDecision.None

        return HistoryPlaybackDecision.Play(item.toRemoteTarget())
    }

    private suspend fun getCachedTarget(item: WatchHistoryEntity): HistoryPlaybackTarget? {
        val cacheTask = cacheRepository.getPlayableCache(item.mediaId, item.serverAddress) ?: return null
        return HistoryPlaybackTarget(
            mediaId = item.mediaId,
            title = item.title,
            sourceType = SOURCE_TYPE_CACHE,
            danmuPool = item.danmuPool,
            animeTitle = item.animeTitle,
            serverAddress = item.serverAddress,
            localPath = cacheTask.localPath,
            startPositionMs = normalizeResumePositionMs(item.playTime, item.playTimeState),
            initialPlayTimeState = item.playTimeState
        )
    }

    private fun getStoredLocalTarget(item: WatchHistoryEntity): HistoryPlaybackTarget? {
        val localPath = item.localPath?.takeIf { File(it).exists() } ?: return null

        return when {
            item.sourceType == SOURCE_TYPE_LOCAL -> HistoryPlaybackTarget(
                mediaId = item.mediaId,
                title = item.title,
                sourceType = SOURCE_TYPE_LOCAL,
                danmuPool = item.danmuPool,
                animeTitle = item.animeTitle,
                localPath = localPath,
                startPositionMs = normalizeResumePositionMs(item.playTime, item.playTimeState),
                initialPlayTimeState = item.playTimeState
            )
            item.isCached || item.sourceType == SOURCE_TYPE_CACHE -> HistoryPlaybackTarget(
                mediaId = item.mediaId,
                title = item.title,
                sourceType = SOURCE_TYPE_CACHE,
                danmuPool = item.danmuPool,
                animeTitle = item.animeTitle,
                serverAddress = item.serverAddress,
                localPath = localPath,
                startPositionMs = normalizeResumePositionMs(item.playTime, item.playTimeState),
                initialPlayTimeState = item.playTimeState
            )
            else -> null
        }
    }

    private fun parseServerAddress(serverAddress: String): Pair<String, Int>? {
        val parts = serverAddress.split(":", limit = 2)
        if (parts.size != 2) return null

        val host = parts[0].trim()
        val port = parts[1].trim().toIntOrNull() ?: return null
        if (host.isBlank()) return null

        return host to port
    }

    private fun WatchHistoryEntity.toRemoteTarget(): HistoryPlaybackTarget {
        return HistoryPlaybackTarget(
            mediaId = mediaId,
            title = title,
            sourceType = SOURCE_TYPE_PC,
            danmuPool = danmuPool,
            animeTitle = animeTitle,
            serverAddress = serverAddress,
            startPositionMs = normalizeResumePositionMs(playTime, playTimeState),
            initialPlayTimeState = playTimeState
        )
    }

    private fun normalizeResumePositionMs(playTimeMs: Long, playTimeState: Int): Long {
        return if (playTimeState == 2) 0L else playTimeMs
    }

    private companion object {
        const val SOURCE_TYPE_PC = 0
        const val SOURCE_TYPE_LOCAL = 1
        const val SOURCE_TYPE_CACHE = 2
    }
}
