package com.kiko.kikoplay.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class StatsEventRequest(
    val ts: Long,
    val ev: String,
    val os: String,
    val kv: String,
    val st: Long,
    val did: String,
    val app: String,
    val vc: Long,
    val sdk: Int,
    val model: String,
    val brand: String,
    val tz: String,
    val ld: String,
    val lang: String
)
