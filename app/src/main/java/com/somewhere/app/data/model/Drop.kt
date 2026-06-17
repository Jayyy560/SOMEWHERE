package com.somewhere.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A message "dropped" at a real-world location.
 * Stores the text, a local image path, GPS coordinates, and creation time.
 */
@Entity(tableName = "drops")
data class Drop(
    @PrimaryKey val id: String,
    val text: String,
    val imagePath: String,
    val audioPath: String? = null,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val authorName: String? = null,
    val authorAvatarUrl: String? = null,
    val authorId: String? = null,
    val expiresAt: Long? = null,
    val isAnonymous: Boolean = false,
    val category: String? = null
) {
    val imageUrl: String
        get() = if (imagePath.startsWith("/")) {
            "file://$imagePath"
        } else {
            imagePath
        }
}
