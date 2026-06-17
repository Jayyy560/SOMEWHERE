package com.somewhere.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DropComment(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("drop_id") val dropId: String,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("author_avatar_url") val authorAvatarUrl: String? = null,
    @SerialName("text") val text: String,
    @SerialName("created_at") val createdAt: String? = null
)
