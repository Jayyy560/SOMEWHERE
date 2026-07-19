package com.somewhere.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DropJourneyStop(
    val id: String = "",
    @SerialName("drop_id") val dropId: String,
    @SerialName("user_id") val userId: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
