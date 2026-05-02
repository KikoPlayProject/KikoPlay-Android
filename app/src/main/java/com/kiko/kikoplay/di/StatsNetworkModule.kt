package com.kiko.kikoplay.di

import com.kiko.kikoplay.data.remote.StatsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StatsOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StatsRetrofit

@Module
@InstallIn(SingletonComponent::class)
object StatsNetworkModule {

    @Provides
    @Singleton
    @StatsOkHttpClient
    fun provideStatsOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @StatsRetrofit
    fun provideStatsRetrofit(
        @StatsOkHttpClient okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("http://www.kstat.top/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideStatsApi(@StatsRetrofit retrofit: Retrofit): StatsApi {
        return retrofit.create(StatsApi::class.java)
    }
}
