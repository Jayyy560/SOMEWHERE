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
    @SerialName("distance_m") val distanceMeters: Double? = null,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("author_avatar_url") val authorAvatarUrl: String? = null,
    @SerialName("user_id") val authorId: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
    val category: String? = null,
    @SerialName("is_dead_drop") val isDeadDrop: Boolean = false,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("file_type") val fileType: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_size") val fileSize: Long? = null
)
