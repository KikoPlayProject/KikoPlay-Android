package com.kiko.kikoplay.data.remote

import com.kiko.kikoplay.data.remote.model.StatsEventRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface StatsApi {

    @POST("stats/")
    suspend fun reportStartup(@Body request: StatsEventRequest): Response<ResponseBody>
}
