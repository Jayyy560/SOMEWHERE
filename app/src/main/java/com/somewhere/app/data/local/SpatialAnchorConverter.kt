package com.somewhere.app.data.local

import androidx.room.TypeConverter
import com.somewhere.app.data.model.SpatialAnchor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SpatialAnchorConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun encode(value: SpatialAnchor?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun decode(value: String?): SpatialAnchor? = value?.let {
        runCatching { json.decodeFromString<SpatialAnchor>(it) }.getOrNull()
    }
}
