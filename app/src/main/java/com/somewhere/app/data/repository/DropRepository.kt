package com.somewhere.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.somewhere.app.data.local.DropDao
import com.somewhere.app.data.model.Drop
import com.somewhere.app.data.remote.DropInsert
import com.somewhere.app.data.remote.NearbyDrop
import com.somewhere.app.data.remote.SupabaseManager
import com.somewhere.app.util.LocationUtils
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonPrimitive
import io.github.jan.supabase.gotrue.auth
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Single source of truth for Drop operations.
 * Handles ID generation, timestamp, and precise Haversine distance filtering.
 */
class DropRepository(
    private val context: Context,
    private val dao: DropDao
) {
    private val sharedPrefs = context.getSharedPreferences("somewhere_prefs", Context.MODE_PRIVATE)
    private val WIPE_TIMESTAMP_KEY = "wipe_timestamp"


    private val dropChangeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val dropChanges: SharedFlow<Unit> = dropChangeEvents.asSharedFlow()

    val allDrops: Flow<List<Drop>> = dao.getAll()

    suspend fun saveDrop(
        text: String,
        imagePath: String,
        audioPath: String?,
        latitude: Double,
        longitude: Double
    ): Drop {
        require(text.isNotBlank()) { "Drop text must not be blank" }
        val uploadedImageUrl = uploadImageAndGetPublicUrl(Uri.parse(imagePath))
        val normalizedAudioPath = audioPath?.let { normalizeLocalPath(it) ?: it }
        val uploadedAudioUrl = audioPath?.let { path ->
            uploadAudioAndGetPublicUrl(Uri.parse(path))
        }

        val currentUser = SupabaseManager.client.auth.currentUserOrNull()
        val authorName = currentUser?.userMetadata?.get("name")?.jsonPrimitive?.content
        val authorAvatarUrl = currentUser?.userMetadata?.get("avatar_url")?.jsonPrimitive?.content

        SupabaseManager.client.from("drops").insert(
            DropInsert(
                text = text,
                imageUrl = uploadedImageUrl,
                audioUrl = uploadedAudioUrl,
                latitude = latitude,
                longitude = longitude,
                authorName = authorName,
                authorAvatarUrl = authorAvatarUrl
            )
        )

        val drop = Drop(
            id = UUID.randomUUID().toString(),
            text = text.take(120),
            imagePath = uploadedImageUrl,
            audioPath = uploadedAudioUrl ?: normalizedAudioPath,
            latitude = latitude,
            longitude = longitude,
            timestamp = System.currentTimeMillis(),
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl
        )
        dao.insert(drop)
        return drop
    }

    suspend fun deleteDrop(drop: Drop) {
        // Delete locally
        dao.deleteById(drop.id)
        deleteLocalFileIfPresent(drop.imagePath)
        drop.audioPath?.let { deleteLocalFileIfPresent(it) }
        
        // Delete remotely
        runCatching {
            SupabaseManager.client.from("drops").delete {
                filter { eq("id", drop.id) }
            }
        }
    }

    suspend fun deleteAllDrops() {
        val existing = dao.getAllOnce()
        
        // Delete locally
        dao.deleteAll()
        existing.forEach {
            deleteLocalFileIfPresent(it.imagePath)
            it.audioPath?.let { path -> deleteLocalFileIfPresent(path) }
        }
        
        // Delete remotely for current user
        val currentUser = SupabaseManager.client.auth.currentUserOrNull()
        val authorName = currentUser?.userMetadata?.get("name")?.jsonPrimitive?.content
        if (authorName != null) {
            runCatching {
                SupabaseManager.client.from("drops").delete {
                    filter { eq("author_name", authorName) }
                }
            }
        }
        
        // Save wipe timestamp to hide ghost remote drops that RLS prevented us from deleting
        sharedPrefs.edit().putLong(WIPE_TIMESTAMP_KEY, System.currentTimeMillis()).apply()
    }

    fun deleteLocalImage(path: String) {
        deleteLocalFileIfPresent(path)
    }

    fun deleteLocalAudio(path: String) {
        deleteLocalFileIfPresent(path)
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
        // Hardcode fallback wipe to June 14, 2026 to permanently hide all old hackathon ghost drops
        // on fresh app installs where sharedPrefs are empty.
        val fallbackWipe = 1718323200000L 
        val storedWipe = sharedPrefs.getLong(WIPE_TIMESTAMP_KEY, 0L)
        val wipeTimestamp = if (storedWipe > fallbackWipe) storedWipe else fallbackWipe
        
        val remote = runCatching {
            fetchRemoteDropsNear(lat, lon, radiusMeters, maxItems)
        }.getOrNull()?.filter { it.first.timestamp >= wipeTimestamp } ?: emptyList()

        val local = getLocalDropsNear(lat, lon, radiusMeters, maxItems)
            .filter { it.first.timestamp >= wipeTimestamp }

        return (remote + local)
            .distinctBy { it.first.id }
            .sortedBy { it.second }
            .take(maxItems)
    }

    suspend fun startRealtimeDrops() {
        val channel = SupabaseManager.client.channel("realtime-drops")
        channel.subscribe()
        channel
            .postgresChangeFlow<PostgresAction>("public") {
                table = "drops"
            }
            .collect { dropChangeEvents.tryEmit(Unit) }
    }

    suspend fun cleanupOrphanMedia() {
        val existing = dao.getAllOnce()
        val existingPaths = existing
            .flatMap { drop ->
                buildList {
                    normalizeLocalPath(drop.imagePath)?.let(::add)
                    drop.audioPath?.let(::normalizeLocalPath)?.let(::add)
                }
            }
            .toSet()

        val filesDir = context.filesDir
        filesDir.listFiles()?.forEach { file ->
            val isDropMedia = file.name.startsWith("drop_") || file.name.startsWith("audiodrop_")
            if (isDropMedia && file.absolutePath !in existingPaths) {
                file.delete()
            }
        }
    }

    private fun deleteLocalFileIfPresent(path: String) {
        runCatching {
            normalizeLocalPath(path)?.let { absolutePath ->
                File(absolutePath).delete()
            }
        }
    }

    private fun normalizeLocalPath(path: String): String? {
        val uri = Uri.parse(path)
        return when {
            uri.scheme == "file" -> uri.path?.let { File(it).absolutePath }
            uri.scheme.isNullOrBlank() && path.startsWith("/") -> File(path).absolutePath
            else -> null
        }
    }

    private suspend fun uploadImageAndGetPublicUrl(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val imageBytes = inputStream?.readBytes()
        inputStream?.close()

        require(imageBytes != null && imageBytes.isNotEmpty()) { "Image bytes are empty" }

        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: error("Failed to decode image")

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        bitmap.recycle()

        val compressedBytes = outputStream.toByteArray()
        outputStream.close()

        val fileName = "drops/${UUID.randomUUID()}.jpg"

        SupabaseManager.client.storage
            .from("drops-images")
            .upload(fileName, compressedBytes, upsert = false)

        return SupabaseManager.client.storage
            .from("drops-images")
            .publicUrl(fileName)
    }

    private suspend fun uploadAudioAndGetPublicUrl(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val audioBytes = inputStream?.readBytes()
        inputStream?.close()

        require(audioBytes != null && audioBytes.isNotEmpty()) { "Audio bytes are empty" }

        val fileName = "audio/${UUID.randomUUID()}.m4a"

        SupabaseManager.client.storage
            .from("drops-audio")
            .upload(fileName, audioBytes, upsert = false)

        return SupabaseManager.client.storage
            .from("drops-audio")
            .publicUrl(fileName)
    }

    private suspend fun fetchRemoteDropsNear(
        lat: Double,
        lon: Double,
        radiusMeters: Float,
        maxItems: Int
    ): List<Pair<Drop, Float>> {
        val params = buildJsonObject {
            put("p_lat", lat)
            put("p_lon", lon)
            put("p_radius_m", radiusMeters.toInt())
            put("p_limit", maxItems)
        }

        val remoteDrops = SupabaseManager.client
            .postgrest
            .rpc("nearby_drops", params)
            .decodeList<NearbyDrop>()

        return remoteDrops
            .filter { it.text.isNotBlank() }
            .map { remote ->
                val drop = Drop(
                    id = remote.id,
                    text = remote.text.take(120),
                    imagePath = remote.imageUrl,
                    audioPath = remote.audioUrl,
                    latitude = remote.latitude,
                    longitude = remote.longitude,
                    timestamp = parseTimestamp(remote.createdAt),
                    authorName = remote.authorName
                )

                val dist = remote.distanceMeters?.toFloat()
                    ?: LocationUtils.haversineDistance(
                        lat, lon, drop.latitude, drop.longitude
                    )

                drop to dist
            }
            .filter { (_, dist) -> dist <= radiusMeters }
            .sortedBy { (_, dist) -> dist }
            .take(maxItems)
    }

    private suspend fun getLocalDropsNear(
        lat: Double,
        lon: Double,
        radiusMeters: Float,
        maxItems: Int
    ): List<Pair<Drop, Float>> {
        // Rough bounding box: ~0.001 degrees ≈ 111 meters
        val delta = (radiusMeters / 111_000.0) * 1.5
        val candidates = dao.getInBoundingBox(lat, lon, delta)

        return candidates
            .filter { it.text.isNotBlank() }
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

    private fun parseTimestamp(createdAt: String?): Long {
        if (createdAt.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching { Instant.parse(createdAt).toEpochMilli() }
            .getOrDefault(System.currentTimeMillis())
    }
}
