package com.kiko.kikoplay.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kiko.kikoplay.data.local.dao.CacheTaskDao
import com.kiko.kikoplay.data.local.dao.ConnectionDao
import com.kiko.kikoplay.data.local.dao.WatchHistoryDao
import com.kiko.kikoplay.data.local.entity.CacheTaskEntity
import com.kiko.kikoplay.data.local.entity.ConnectionEntity
import com.kiko.kikoplay.data.local.entity.WatchHistoryEntity

@Database(
    entities = [
        ConnectionEntity::class,
        WatchHistoryEntity::class,
        CacheTaskEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun cacheTaskDao(): CacheTaskDao
}
