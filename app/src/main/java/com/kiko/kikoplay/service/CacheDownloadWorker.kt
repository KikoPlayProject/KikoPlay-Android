package com.kiko.kikoplay.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kiko.kikoplay.data.local.dao.CacheTaskDao
import com.kiko.kikoplay.data.local.entity.CacheTaskEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class CacheDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val cacheTaskDao: CacheTaskDao
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_MEDIA_URL = "media_url"
        const val KEY_OUTPUT_PATH = "output_path"
        const val PROGRESS_BYTES = "progress_bytes"
        const val PROGRESS_TOTAL = "progress_total"
    }

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1)
        val mediaUrl = inputData.getString(KEY_MEDIA_URL) ?: return Result.failure()
        val outputPath = inputData.getString(KEY_OUTPUT_PATH) ?: return Result.failure()
        if (taskId < 0) return Result.failure()

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        try {
            cacheTaskDao.updateStatus(taskId, CacheTaskEntity.STATUS_DOWNLOADING)

            val requestBuilder = Request.Builder().url(mediaUrl)

            // Support resume from partial download
            if (outputFile.exists() && outputFile.length() > 0) {
                requestBuilder.addHeader("Range", "bytes=${outputFile.length()}-")
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

            val contentLength = body.contentLength()
            val isResume = response.code == 206
            val existingBytes = if (isResume) outputFile.length() else 0L
            val totalBytes = if (contentLength > 0) contentLength + existingBytes else 0L

            if (totalBytes > 0) {
                cacheTaskDao.update(
                    cacheTaskDao.getByMediaId(
                        inputData.getString("media_id") ?: ""
                    )?.copy(totalBytes = totalBytes) ?: return Result.failure()
                )
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

            cacheTaskDao.updateStatus(taskId, CacheTaskEntity.STATUS_COMPLETED)
            return Result.success()

        } catch (e: Exception) {
            cacheTaskDao.updateStatus(taskId, CacheTaskEntity.STATUS_FAILED)
            return Result.failure()
        }
    }
}
