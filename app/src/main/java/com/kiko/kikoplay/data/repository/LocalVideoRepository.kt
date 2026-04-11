package com.kiko.kikoplay.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        queryVideos(
            projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.RELATIVE_PATH
            ),
            sortOrder = sortOrder,
            target = videos
        )
        if (videos.isEmpty()) {
            queryVideos(
                projection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.DATE_MODIFIED
                ),
                sortOrder = sortOrder,
                target = videos
            )
        }
        videos
    }

    private fun queryVideos(
        projection: Array<String>,
        sortOrder: String,
        target: MutableList<LocalVideo>
    ) {
        runCatching {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                appendVideos(cursor, target)
            }
        }
    }

    private fun appendVideos(cursor: Cursor, target: MutableList<LocalVideo>) {
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
        val relPathCol = cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)

        while (cursor.moveToNext()) {
            val path = cursor.getString(pathCol) ?: ""
            val relativePath = if (relPathCol >= 0) {
                cursor.getString(relPathCol) ?: deriveRelativePath(path)
            } else {
                deriveRelativePath(path)
            }
            target.add(
                LocalVideo(
                    id = cursor.getLong(idCol),
                    displayName = cursor.getString(nameCol) ?: "",
                    path = path,
                    duration = cursor.getLong(durationCol),
                    size = cursor.getLong(sizeCol),
                    dateModified = cursor.getLong(dateCol),
                    relativePath = relativePath
                )
            )
        }
    }

    private fun deriveRelativePath(path: String): String {
        return File(path).parent ?: ""
    }

    fun groupByFolder(videos: List<LocalVideo>): Map<String, List<LocalVideo>> {
        return videos.groupBy { it.relativePath.trimEnd('/') }
    }
}
