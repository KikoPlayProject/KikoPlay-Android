package com.kiko.kikoplay.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kikoplay.db"
        ).build()
    }

    @Provides
    fun provideConnectionDao(db: AppDatabase): ConnectionDao = db.connectionDao()

    @Provides
    fun provideWatchHistoryDao(db: AppDatabase): WatchHistoryDao = db.watchHistoryDao()

    @Provides
    fun provideCacheTaskDao(db: AppDatabase): CacheTaskDao = db.cacheTaskDao()
}
