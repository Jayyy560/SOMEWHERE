package com.somewhere.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DropInsert(
    val text: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("audio_url") val audioUrl: String? = null,
    val latitude: Double,
    val longitude: Double
)
