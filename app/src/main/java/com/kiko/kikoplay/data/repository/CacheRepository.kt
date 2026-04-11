package com.kiko.kikoplay.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kiko.kikoplay.data.local.dao.CacheTaskDao
import com.kiko.kikoplay.data.local.entity.CacheTaskEntity
import com.kiko.kikoplay.data.remote.ConnectionManager
import com.kiko.kikoplay.service.CacheDownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheRepository @Inject constructor(
    private val cacheTaskDao: CacheTaskDao,
    private val connectionManager: ConnectionManager,
    @ApplicationContext private val context: Context
) {
    fun getActiveTasks(): Flow<List<CacheTaskEntity>> = cacheTaskDao.getActiveTasks()

    fun getCompletedTasks(): Flow<List<CacheTaskEntity>> = cacheTaskDao.getCompleted()

    fun getAllTasks(): Flow<List<CacheTaskEntity>> = cacheTaskDao.getAll()

    suspend fun enqueue(
        mediaId: String,
        title: String,
        animeTitle: String?,
        danmuPool: String?,
        serverAddress: String?
    ): Long {
        val existing = cacheTaskDao.getByMediaId(mediaId)
        if (existing != null) return existing.id

        val conn = connectionManager.connection.value
        val addr = serverAddress ?: conn?.let { "${it.host}:${it.port}" } ?: return -1

        val cacheDir = File(context.getExternalFilesDir(null), "cache")
        cacheDir.mkdirs()
        val outputPath = File(cacheDir, "${mediaId}.mp4").absolutePath

        val taskId = cacheTaskDao.insert(
            CacheTaskEntity(
                mediaId = mediaId,
                title = title,
                animeTitle = animeTitle,
                danmuPool = danmuPool,
                serverAddress = addr,
                localPath = outputPath
            )
        )

        // Start WorkManager download
        val mediaUrl = "http://$addr/media/$mediaId"
        val inputData = Data.Builder()
            .putLong(CacheDownloadWorker.KEY_TASK_ID, taskId)
            .putString(CacheDownloadWorker.KEY_MEDIA_URL, mediaUrl)
            .putString(CacheDownloadWorker.KEY_OUTPUT_PATH, outputPath)
            .putString("media_id", mediaId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<CacheDownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("cache_download")
            .addTag("task_$taskId")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("download_$mediaId", ExistingWorkPolicy.KEEP, workRequest)

        return taskId
    }

    suspend fun pauseTask(id: Long) {
        cacheTaskDao.updateStatus(id, CacheTaskEntity.STATUS_PAUSED)
        // Cancel the worker — it checks isStopped and saves progress
        WorkManager.getInstance(context).cancelAllWorkByTag("task_$id")
    }

    suspend fun resumeTask(id: Long) {
        val task = cacheTaskDao.getById(id) ?: return
        cacheTaskDao.updateStatus(id, CacheTaskEntity.STATUS_QUEUED)

        val addr = task.serverAddress ?: return
        val mediaUrl = "http://$addr/media/${task.mediaId}"
        val outputPath = task.localPath ?: return

        val inputData = Data.Builder()
            .putLong(CacheDownloadWorker.KEY_TASK_ID, id)
            .putString(CacheDownloadWorker.KEY_MEDIA_URL, mediaUrl)
            .putString(CacheDownloadWorker.KEY_OUTPUT_PATH, outputPath)
            .putString("media_id", task.mediaId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<CacheDownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("cache_download")
            .addTag("task_$id")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("download_${task.mediaId}", ExistingWorkPolicy.REPLACE, workRequest)
    }

    suspend fun cancelTask(entity: CacheTaskEntity) {
        WorkManager.getInstance(context).cancelAllWorkByTag("task_${entity.id}")
        cacheTaskDao.delete(entity)
        // Delete partial file
        entity.localPath?.let { File(it).delete() }
    }

    suspend fun updateProgress(id: Long, bytes: Long) {
        cacheTaskDao.updateProgress(id, bytes)
    }

    suspend fun markCompleted(id: Long) {
        cacheTaskDao.updateStatus(id, CacheTaskEntity.STATUS_COMPLETED)
    }

    suspend fun deleteCompleted(entity: CacheTaskEntity) {
        cacheTaskDao.delete(entity)
        entity.localPath?.let { File(it).delete() }
    }

    suspend fun getByMediaId(mediaId: String): CacheTaskEntity? {
        return cacheTaskDao.getByMediaId(mediaId)
    }

    suspend fun getPlayableCache(mediaId: String, serverAddress: String? = null): CacheTaskEntity? {
        val task = cacheTaskDao.getByMediaId(mediaId) ?: return null
        if (task.status != CacheTaskEntity.STATUS_COMPLETED) return null

        val localPath = task.localPath ?: return null
        if (!File(localPath).exists()) return null

        if (!serverAddress.isNullOrBlank() && !task.serverAddress.isNullOrBlank() && task.serverAddress != serverAddress) {
            return null
        }

        return task
    }
}
