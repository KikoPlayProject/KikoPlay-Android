package com.kiko.kikoplay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_tasks")
data class CacheTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: String,
    val title: String,
    val animeTitle: String? = null,
    val danmuPool: String? = null,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: Int = STATUS_QUEUED, // 0=queued, 1=downloading, 2=paused, 3=completed, 4=failed
    val priority: Int = 0,
    val localPath: String? = null,
    val serverAddress: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    companion object {
        const val STATUS_QUEUED = 0
        const val STATUS_DOWNLOADING = 1
        const val STATUS_PAUSED = 2
        const val STATUS_COMPLETED = 3
        const val STATUS_FAILED = 4
    }
}
