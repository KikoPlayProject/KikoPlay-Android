package com.kiko.kikoplay.data.repository

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryThumbnailRepository @Inject constructor() {

    suspend fun createThumbnail(localPath: String?): ByteArray? = withContext(Dispatchers.IO) {
        if (localPath.isNullOrBlank()) return@withContext null

        val file = File(localPath)
        if (!file.exists()) return@withContext null

        runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(localPath)
            val frame = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            retriever.release()

            frame?.toThumbnailBytes(maxWidth = 320)
        }.getOrNull()
    }

    suspend fun createThumbnail(bitmap: Bitmap?): ByteArray? = withContext(Dispatchers.Default) {
        bitmap?.toThumbnailBytes(maxWidth = 320)
    }

    private fun Bitmap.toThumbnailBytes(maxWidth: Int): ByteArray {
        return useScaledCopy(maxWidth = maxWidth).toJpegBytes()
    }

    private fun Bitmap.useScaledCopy(maxWidth: Int): Bitmap {
        if (width <= maxWidth || width <= 0 || height <= 0) return this

        val scaledHeight = (height.toFloat() * maxWidth / width).toInt().coerceAtLeast(1)
        val scaledBitmap = Bitmap.createScaledBitmap(this, maxWidth, scaledHeight, true)
        if (scaledBitmap != this) recycle()
        return scaledBitmap
    }

    private fun Bitmap.toJpegBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, output)
        if (!isRecycled) recycle()
        return output.toByteArray()
    }
}
