package com.somewhere.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NearbyDrop(
    val id: String,
    val text: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("audio_url") val audioUrl: String? = null,
    val latitude: Double,
    val longitude: Double,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("distance_m") val distanceMeters: Double? = null
)
