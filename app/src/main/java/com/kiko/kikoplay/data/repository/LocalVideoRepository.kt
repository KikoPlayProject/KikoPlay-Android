package com.kiko.kikoplay.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class LocalVideo(
    val id: Long,
    val displayName: String,
    val path: String,
    val duration: Long, // ms
    val size: Long,
    val dateModified: Long,
    val relativePath: String
)

@Singleton
class LocalVideoRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun scanVideos(): List<LocalVideo> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<LocalVideo>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.RELATIVE_PATH
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val relPathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                videos.add(
                    LocalVideo(
                        id = cursor.getLong(idCol),
                        displayName = cursor.getString(nameCol) ?: "",
                        path = cursor.getString(pathCol) ?: "",
                        duration = cursor.getLong(durationCol),
                        size = cursor.getLong(sizeCol),
                        dateModified = cursor.getLong(dateCol),
                        relativePath = cursor.getString(relPathCol) ?: ""
                    )
                )
            }
        }
        videos
    }

    fun groupByFolder(videos: List<LocalVideo>): Map<String, List<LocalVideo>> {
        return videos.groupBy { it.relativePath.trimEnd('/') }
    }
}
