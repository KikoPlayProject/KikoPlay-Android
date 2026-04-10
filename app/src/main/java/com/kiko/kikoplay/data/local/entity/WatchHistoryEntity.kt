package com.kiko.kikoplay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: String,
    val title: String,
    val animeTitle: String? = null,
    val playTime: Long = 0,
    val duration: Long = 0,
    val playTimeState: Int = 0,
    val sourceType: Int = 0, // 0=PC, 1=local, 2=cached
    val thumbnailPath: String? = null,
    val lastWatched: Long = System.currentTimeMillis(),
    val danmuPool: String? = null,
    val serverAddress: String? = null
)
