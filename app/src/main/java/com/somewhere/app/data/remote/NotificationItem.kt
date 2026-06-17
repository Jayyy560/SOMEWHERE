package com.somewhere.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationItem(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("actor_name") val actorName: String? = null,
    @SerialName("type") val type: String, // 'like', 'comment', 'system'
    @SerialName("drop_id") val dropId: String? = null,
    @SerialName("message") val message: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)
