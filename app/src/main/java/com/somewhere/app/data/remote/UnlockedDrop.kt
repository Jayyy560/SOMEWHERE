package com.somewhere.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnlockedDrop(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("drop_id") val dropId: String,
    @SerialName("unlocked_at") val unlockedAt: String? = null
)
