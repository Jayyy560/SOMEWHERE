package com.somewhere.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DropLikeInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("drop_id") val dropId: String
)

@Serializable
data class DropCommentInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("drop_id") val dropId: String,
    @SerialName("author_name") val authorName: String,
    @SerialName("author_avatar_url") val authorAvatarUrl: String? = null,
    @SerialName("text") val text: String
)

@Serializable
data class NotificationInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("actor_name") val actorName: String,
    @SerialName("type") val type: String,
    @SerialName("drop_id") val dropId: String,
    @SerialName("message") val message: String,
    @SerialName("is_read") val isRead: Boolean = false
)
