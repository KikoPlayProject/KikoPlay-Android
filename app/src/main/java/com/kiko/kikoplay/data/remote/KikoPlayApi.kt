package com.kiko.kikoplay.data.remote

import com.kiko.kikoplay.data.remote.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface KikoPlayApi {

    @GET("api/playlist")
    suspend fun getPlaylist(): Response<List<PlaylistNode>>

    @GET("api/recent")
    suspend fun getRecent(): List<PlaylistNode>

    @GET("api/playstate")
    suspend fun getPlayState(): PlayStateResponse

    @GET("api/danmu/v3/")
    suspend fun getDanmaku(
        @Query("id") poolId: String,
        @Query("update") update: Boolean = false
    ): DanmakuV3Response

    @GET("api/danmu/full/")
    suspend fun getDanmakuFull(
        @Query("id") poolId: String,
        @Query("update") update: Boolean = false
    ): DanmakuFullResponse

    @GET("api/danmu/local/")
    suspend fun getLocalDanmaku(
        @Query("mediaId") mediaId: String
    ): LocalDanmakuResponse

    @GET("api/subtitle")
    suspend fun checkSubtitle(
        @Query("id") mediaId: String
    ): SubtitleCheckResponse

    @GET("sub/{format}/{mediaId}")
    suspend fun getSubtitle(
        @Path("format") format: String,
        @Path("mediaId", encoded = true) mediaId: String
    ): ResponseBody

    @POST("api/updateTime")
    suspend fun updatePlayTime(@Body body: UpdateTimeRequest)

    @POST("api/updateDelay")
    suspend fun updateDelay(@Body body: UpdateDelayRequest)

    @POST("api/updateTimeline")
    suspend fun updateTimeline(@Body body: UpdateTimelineRequest)

    @POST("api/screenshot")
    suspend fun screenshot(@Body body: ScreenshotRequest)

    @POST("api/danmu/launch")
    suspend fun launchDanmaku(@Body body: LaunchDanmakuRequest)
}
