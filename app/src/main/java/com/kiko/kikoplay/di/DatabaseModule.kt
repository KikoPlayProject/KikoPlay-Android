package com.kiko.kikoplay.di

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kiko.kikoplay.data.local.AppDatabase
import com.kiko.kikoplay.data.local.dao.CacheTaskDao
import com.kiko.kikoplay.data.local.dao.ConnectionDao
import com.kiko.kikoplay.data.local.dao.WatchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `watch_history_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `mediaId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `animeTitle` TEXT,
                    `playTime` INTEGER NOT NULL,
                    `duration` INTEGER NOT NULL,
                    `playTimeState` INTEGER NOT NULL,
                    `sourceType` INTEGER NOT NULL,
                    `isCached` INTEGER NOT NULL DEFAULT 0,
                    `remoteUri` TEXT,
                    `localPath` TEXT,
                    `thumbnailData` BLOB,
                    `lastWatched` INTEGER NOT NULL,
                    `danmuPool` TEXT,
                    `serverAddress` TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `watch_history_new` (
                    `id`, `mediaId`, `title`, `animeTitle`, `playTime`, `duration`,
                    `playTimeState`, `sourceType`, `isCached`, `remoteUri`, `localPath`,
                    `thumbnailData`, `lastWatched`, `danmuPool`, `serverAddress`
                )
                SELECT
                    `id`, `mediaId`, `title`, `animeTitle`, `playTime`, `duration`,
                    `playTimeState`, `sourceType`,
                    CASE WHEN `sourceType` = 2 THEN 1 ELSE 0 END,
                    NULL, NULL, NULL, `lastWatched`, `danmuPool`, `serverAddress`
                FROM `watch_history`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `watch_history`")
            db.execSQL("ALTER TABLE `watch_history_new` RENAME TO `watch_history`")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kikoplay.db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    @Provides
    fun provideConnectionDao(db: AppDatabase): ConnectionDao = db.connectionDao()

    @Provides
    fun provideWatchHistoryDao(db: AppDatabase): WatchHistoryDao = db.watchHistoryDao()

    @Provides
    fun provideCacheTaskDao(db: AppDatabase): CacheTaskDao = db.cacheTaskDao()
}
