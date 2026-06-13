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
    val timestamp: Long
) {
    val imageUrl: String
        get() = if (imagePath.startsWith("/")) {
            "file://$imagePath"
        } else {
            imagePath
        }
}
