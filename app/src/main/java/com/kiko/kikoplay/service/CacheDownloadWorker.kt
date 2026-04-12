package com.kiko.kikoplay.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kiko.kikoplay.data.local.dao.CacheTaskDao
import com.kiko.kikoplay.data.local.entity.CacheTaskEntity
import com.kiko.kikoplay.data.local.model.CachedDanmakuPayload
import com.kiko.kikoplay.data.remote.model.DanmakuFullResponse
import com.kiko.kikoplay.data.remote.model.LocalDanmakuResponse
import com.kiko.kikoplay.util.CacheFileHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class CacheDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val cacheTaskDao: CacheTaskDao,
    private val json: Json
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_MEDIA_URL = "media_url"
        const val KEY_OUTPUT_PATH = "output_path"
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_DANMU_POOL = "danmu_pool"
        const val PROGRESS_BYTES = "progress_bytes"
        const val PROGRESS_TOTAL = "progress_total"
    }

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1)
        val mediaUrl = inputData.getString(KEY_MEDIA_URL) ?: return Result.failure()
        val outputPath = inputData.getString(KEY_OUTPUT_PATH) ?: return Result.failure()
        val mediaId = inputData.getString(KEY_MEDIA_ID).orEmpty()
        val danmuPool = inputData.getString(KEY_DANMU_POOL)
        if (taskId < 0) return Result.failure()

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        try {
            cacheTaskDao.updateStatus(taskId, CacheTaskEntity.STATUS_DOWNLOADING)

            val existingBytes = if (outputFile.exists()) outputFile.length() else 0L
            val probedTotalBytes = probeTotalBytes(mediaUrl)

            val requestBuilder = Request.Builder().url(mediaUrl)

            // Support resume from partial download
            if (existingBytes > 0) {
                requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                cacheTaskDao.updateStatus(taskId, CacheTaskEntity.STATUS_FAILED)
                return Result.failure()
            }

            val body = response.body ?: run {
                cacheTaskDao.updateStatus(taskId, CacheTaskEntity.STATUS_FAILED)
                return Result.failure()
            }

            val isResume = response.code == 206
            val totalBytes = resolveTotalBytes(response, existingBytes, probedTotalBytes)

            if (totalBytes > 0) {
                cacheTaskDao.getById(taskId)?.let { task ->
                    cacheTaskDao.update(task.copy(totalBytes = totalBytes))
                }
            }

            val appendMode = isResume
            FileOutputStream(outputFile, appendMode).use { fos ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloadedBytes = existingBytes

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            cacheTaskDao.updateStatus(taskId, CacheTaskEntity.STATUS_PAUSED)
                            cacheTaskDao.updateProgress(taskId, downloadedBytes)
                            return Result.success()
                        }

                        fos.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // Update progress every 64KB
                        if (downloadedBytes % (64 * 1024) < 8192) {
                            cacheTaskDao.updateProgress(taskId, downloadedBytes)
                            setProgress(
                                workDataOf(
                                    PROGRESS_BYTES to downloadedBytes,
                                    PROGRESS_TOTAL to totalBytes
                                )
                            )
                        }
                    }

                    cacheTaskDao.updateProgress(taskId, downloadedBytes)
                }
            }

            val finalBytes = outputFile.length().coerceAtLeast(existingBytes)
            cacheTaskDao.getById(taskId)?.let { task ->
                cacheTaskDao.update(
                    task.copy(
                        downloadedBytes = finalBytes,
                        totalBytes = if (task.totalBytes > 0) task.totalBytes else finalBytes,
                        status = CacheTaskEntity.STATUS_COMPLETED,
                        completedAt = System.currentTimeMillis()
                    )
                )
            } ?: cacheTaskDao.updateStatus(taskId, CacheTaskEntity.STATUS_COMPLETED)

            cacheDanmakuIfAvailable(
                mediaUrl = mediaUrl,
                mediaId = mediaId,
                danmuPool = danmuPool,
                outputFile = outputFile
            )
            return Result.success()

        } catch (e: Exception) {
            cacheTaskDao.updateStatus(taskId, CacheTaskEntity.STATUS_FAILED)
            return Result.failure()
        }
    }

    private fun probeTotalBytes(mediaUrl: String): Long {
        val headRequest = Request.Builder()
            .url(mediaUrl)
            .head()
            .build()

        runCatching {
            okHttpClient.newCall(headRequest).execute().use { response ->
                parseTotalBytesFromResponse(response, existingBytes = 0L)
            }
        }.getOrNull()?.takeIf { it > 0 }?.let { return it }

        val rangeProbe = Request.Builder()
            .url(mediaUrl)
            .addHeader("Range", "bytes=0-0")
            .build()

        return runCatching {
            okHttpClient.newCall(rangeProbe).execute().use { response ->
                parseTotalBytesFromResponse(response, existingBytes = 0L)
            }
        }.getOrDefault(0L)
    }

    private fun resolveTotalBytes(
        response: Response,
        existingBytes: Long,
        fallbackBytes: Long
    ): Long {
        return parseTotalBytesFromResponse(response, existingBytes)
            .takeIf { it > 0 }
            ?: fallbackBytes
    }

    private fun parseTotalBytesFromResponse(
        response: Response,
        existingBytes: Long
    ): Long {
        response.header("Content-Range")
            ?.substringAfter('/', "")
            ?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?.let { return it }

        val contentLength = response.header("Content-Length")?.toLongOrNull()
            ?: response.body?.contentLength()
            ?: 0L
        if (contentLength <= 0) return 0L

        return if (response.code == 206) contentLength + existingBytes else contentLength
    }

    private fun cacheDanmakuIfAvailable(
        mediaUrl: String,
        mediaId: String,
        danmuPool: String?,
        outputFile: File
    ) {
        if (mediaId.isBlank()) return

        val payload = fetchDanmakuPayload(
            mediaUrl = mediaUrl,
            mediaId = mediaId,
            danmuPool = danmuPool
        ) ?: return

        val danmakuFile = CacheFileHelper.getDanmakuCacheFile(
            mediaId = mediaId,
            mediaPath = outputFile.absolutePath
        )
        danmakuFile.parentFile?.mkdirs()
        danmakuFile.writeText(json.encodeToString(CachedDanmakuPayload.serializer(), payload))
    }

    private fun fetchDanmakuPayload(
        mediaUrl: String,
        mediaId: String,
        danmuPool: String?
    ): CachedDanmakuPayload? {
        val baseUrl = mediaUrl.toHttpUrlOrNull() ?: return null
        return runCatching {
            if (!danmuPool.isNullOrBlank()) {
                val request = Request.Builder()
                    .url(
                        baseUrl.newBuilder()
                            .encodedPath("/api/danmu/full/")
                            .addQueryParameter("id", danmuPool)
                            .build()
                    )
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string() ?: return@use null
                    val result = json.decodeFromString(DanmakuFullResponse.serializer(), body)
                    CachedDanmakuPayload(
                        format = CachedDanmakuPayload.FORMAT_FULL,
                        comment = result.comment,
                        sources = result.source,
                        launchScripts = result.launchScripts
                    )
                }
            } else {
                val request = Request.Builder()
                    .url(
                        baseUrl.newBuilder()
                            .encodedPath("/api/danmu/local/")
                            .addQueryParameter("mediaId", mediaId)
                            .build()
                    )
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string() ?: return@use null
                    val result = json.decodeFromString(LocalDanmakuResponse.serializer(), body)
                    CachedDanmakuPayload(
                        format = CachedDanmakuPayload.FORMAT_LOCAL,
                        comment = result.comment
                    )
                }
            }
        }.getOrNull()
    }
}
