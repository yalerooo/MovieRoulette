package com.movieroulette.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceToken(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val token: String,
    @SerialName("device_type")
    val deviceType: String = "android",
    @SerialName("created_at")
    val createdAt: String? = null
)
