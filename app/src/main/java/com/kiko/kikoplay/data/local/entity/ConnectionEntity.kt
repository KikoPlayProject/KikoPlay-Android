package com.kiko.kikoplay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val port: Int,
    val deviceName: String? = null,
    val lastConnected: Long = System.currentTimeMillis()
)
