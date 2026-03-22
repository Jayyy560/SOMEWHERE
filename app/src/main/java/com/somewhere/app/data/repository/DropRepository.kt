package com.somewhere.app.data.repository

import android.content.Context
import android.net.Uri
import com.somewhere.app.data.local.DropDao
import com.somewhere.app.data.model.Drop
import com.somewhere.app.util.LocationUtils
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

/**
 * Single source of truth for Drop operations.
 * Handles ID generation, timestamp, and precise Haversine distance filtering.
 */
class DropRepository(
    private val context: Context,
    private val dao: DropDao
) {

    val allDrops: Flow<List<Drop>> = dao.getAll()

    suspend fun saveDrop(
        text: String,
        imagePath: String,
        latitude: Double,
        longitude: Double
    ): Drop {
        val drop = Drop(
            id = UUID.randomUUID().toString(),
            text = text.take(120),
            imagePath = imagePath,
            latitude = latitude,
            longitude = longitude,
            timestamp = System.currentTimeMillis()
        )
        dao.insert(drop)
        return drop
    }

    suspend fun deleteDrop(drop: Drop) {
        dao.deleteById(drop.id)
        deleteImageIfLocal(drop.imagePath)
    }

    suspend fun deleteAllDrops() {
        val existing = dao.getAllOnce()
        dao.deleteAll()
        existing.forEach { deleteImageIfLocal(it.imagePath) }
    }

    fun deleteLocalImage(path: String) {
        deleteImageIfLocal(path)
    }

    /**
     * Returns drops within [radiusMeters] of the given coordinates,
     * sorted by distance (closest first), limited to [maxItems].
     */
    suspend fun getDropsNear(
        lat: Double,
        lon: Double,
        radiusMeters: Float = LocationUtils.DISCOVERY_RADIUS,
        maxItems: Int = LocationUtils.MAX_VISIBLE
    ): List<Pair<Drop, Float>> {
        // Rough bounding box: ~0.001 degrees ≈ 111 meters
        val delta = (radiusMeters / 111_000.0) * 1.5
        val candidates = dao.getInBoundingBox(lat, lon, delta)

        return candidates
            .map { drop ->
                val dist = LocationUtils.haversineDistance(
                    lat, lon, drop.latitude, drop.longitude
                )
                drop to dist
            }
            .filter { (_, dist) -> dist <= radiusMeters }
            .sortedBy { (_, dist) -> dist }
            .take(maxItems)
    }

    suspend fun cleanupOrphanImages() {
        val existing = dao.getAllOnce()
        val existingPaths = existing.map { it.imagePath }.toSet()
        val filesDir = context.filesDir
        filesDir.listFiles()?.forEach { file ->
            val path = file.toURI().toString()
            if (path !in existingPaths && file.name.startsWith("drop_")) {
                file.delete()
            }
        }
    }

    private fun deleteImageIfLocal(path: String) {
        runCatching {
            val uri = Uri.parse(path)
            if (uri.scheme == "file") {
                File(uri.path.orEmpty()).delete()
            }
        }
    }
}
