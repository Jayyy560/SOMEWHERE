package com.somewhere.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.somewhere.app.data.local.DropDao
import com.somewhere.app.data.model.Drop
import com.somewhere.app.data.remote.DropInsert
import com.somewhere.app.data.remote.NearbyDrop
import com.somewhere.app.data.remote.DropLike
import com.somewhere.app.data.remote.DropComment
import com.somewhere.app.data.remote.NotificationItem
import com.somewhere.app.data.remote.UnlockedDrop
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allDrops: Flow<List<Drop>> = SupabaseManager.client.auth.sessionStatus
        .flatMapLatest {
            val userId = SupabaseManager.client.auth.currentUserOrNull()?.id
            if (userId != null) {
                dao.getAll(userId)
            } else {
                flowOf(emptyList())
            }
        }

    suspend fun saveDrop(
        text: String,
        imagePath: String,
        audioPath: String?,
        latitude: Double,
        longitude: Double,
        expiresAt: Long? = null,
        isAnonymous: Boolean = false,
        category: String? = null,
        isDeadDrop: Boolean = false,
        fileUri: String? = null,
        fileType: String? = null,
        fileName: String? = null,
        fileSize: Long? = null
    ): Drop {
        require(text.isNotBlank()) { "Drop text must not be blank" }

        val uploadedImageUrl = uploadImageAndGetPublicUrl(Uri.parse(imagePath))
        val normalizedAudioPath = audioPath?.let { normalizeLocalPath(it) ?: it }
        val uploadedAudioUrl = audioPath?.let { path ->
            uploadAudioAndGetPublicUrl(Uri.parse(path))
        }
        val uploadedFileUrl = fileUri?.let { uri ->
            uploadDeadDropFile(Uri.parse(uri))
        }

        val currentUser = SupabaseManager.client.auth.currentUserOrNull()
        val authorName = currentUser?.userMetadata?.get("name")?.jsonPrimitive?.content
        val authorAvatarUrl = currentUser?.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
        
        val dropId = UUID.randomUUID().toString()

        SupabaseManager.client.from("drops").insert(
            DropInsert(
                id = dropId,
                text = text,
                imageUrl = uploadedImageUrl,
                audioUrl = uploadedAudioUrl,
                latitude = latitude,
                longitude = longitude,
                authorName = authorName,
                authorAvatarUrl = authorAvatarUrl,
                expiresAt = expiresAt?.let { Instant.ofEpochMilli(it).toString() },
                isAnonymous = isAnonymous,
                category = category,
                isDeadDrop = isDeadDrop,
                fileUrl = uploadedFileUrl,
                fileType = fileType,
                fileName = fileName,
                fileSize = fileSize
            )
        )

        val drop = Drop(
            id = dropId,
            text = text.take(120),
            imagePath = uploadedImageUrl,
            audioPath = uploadedAudioUrl ?: normalizedAudioPath,
            latitude = latitude,
            longitude = longitude,
            timestamp = System.currentTimeMillis(),
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            authorId = currentUser?.id,
            expiresAt = expiresAt,
            isAnonymous = isAnonymous,
            category = category,
            isDeadDrop = isDeadDrop,
            fileUrl = uploadedFileUrl,
            fileType = fileType,
            fileName = fileName,
            fileSize = fileSize
        )
        dao.insert(drop)
        return drop
    }

    suspend fun deleteDrop(drop: Drop): Boolean {
        // Delete locally
        dao.deleteById(drop.id)
        deleteLocalFileIfPresent(drop.imagePath)
        drop.audioPath?.let { deleteLocalFileIfPresent(it) }
        
        // Delete remotely
        return try {
            SupabaseManager.client.from("drops").delete {
                filter { eq("id", drop.id) }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("DropRepository", "Failed to delete drop", e)
            false
        }
    }

    suspend fun clearLocalCache() {
        dao.deleteAll()
    }

    suspend fun syncMyDrops() {
        val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return
        runCatching {
            val remoteDrops = SupabaseManager.client.postgrest["drops"]
                .select { filter { eq("user_id", currentUser.id) } }
                .decodeList<NearbyDrop>()

            val drops = remoteDrops.map { remote ->
                Drop(
                    id = remote.id,
                    text = remote.text.take(120),
                    imagePath = remote.imageUrl,
                    audioPath = remote.audioUrl,
                    latitude = remote.latitude,
                    longitude = remote.longitude,
                    timestamp = remote.createdAt?.let { Instant.parse(it).toEpochMilli() } ?: System.currentTimeMillis(),
                    authorName = remote.authorName,
                    authorAvatarUrl = remote.authorAvatarUrl,
                    authorId = remote.authorId,
                    expiresAt = remote.expiresAt?.let { Instant.parse(it).toEpochMilli() },
                    isAnonymous = remote.isAnonymous,
                    category = remote.category
                )
            }

            // Wipe old local cache to prevent leaking drops from other accounts
            dao.deleteAll()

            // Upsert into local database
            drops.forEach { drop ->
                dao.insert(drop) // Since DropDao.insert has ON CONFLICT REPLACE, this is safe
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

    suspend fun deleteAllDrops() {
        val currentUser = SupabaseManager.client.auth.currentUserOrNull()
        val existing = if (currentUser != null) dao.getAllOnce(currentUser.id) else emptyList()
        
        // Delete locally
        dao.deleteAll()
        existing.forEach {
            deleteLocalFileIfPresent(it.imagePath)
            it.audioPath?.let { path -> deleteLocalFileIfPresent(path) }
        }
        
        // Delete remotely for current user
        if (currentUser != null) {
            try {
                SupabaseManager.client.from("drops").delete {
                    filter { eq("user_id", currentUser.id) }
                }
            } catch (e: Exception) {
                android.util.Log.e("DropRepository", "Failed to delete all drops", e)
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

        val reportedDrops = sharedPrefs.getStringSet("reported_drops", emptySet()) ?: emptySet()
        val blockedUsers = sharedPrefs.getStringSet("blocked_users", emptySet()) ?: emptySet()

        return (remote + local)
            .distinctBy { it.first.id }
            .filter { !reportedDrops.contains(it.first.id) }
            .filter { it.first.authorName == null || !blockedUsers.contains(it.first.authorName) }
            .sortedBy { it.second }
            .take(maxItems)
    }

    fun reportDrop(dropId: String) {
        val reported = sharedPrefs.getStringSet("reported_drops", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        reported.add(dropId)
        sharedPrefs.edit().putStringSet("reported_drops", reported).apply()
    }

    fun blockUser(authorName: String) {
        val blocked = sharedPrefs.getStringSet("blocked_users", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        blocked.add(authorName)
        sharedPrefs.edit().putStringSet("blocked_users", blocked).apply()
    }

    fun unblockUser(authorName: String) {
        val blocked = sharedPrefs.getStringSet("blocked_users", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        blocked.remove(authorName)
        sharedPrefs.edit().putStringSet("blocked_users", blocked).apply()
    }

    fun getBlockedUsers(): Set<String> {
        return sharedPrefs.getStringSet("blocked_users", emptySet()) ?: emptySet()
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
        val currentUser = SupabaseManager.client.auth.currentUserOrNull()
        val existing = if (currentUser != null) dao.getAllOnce(currentUser.id) else emptyList()
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

        // Scale down to max 1024px to dramatically reduce size and upload/download times
        val maxDim = 1024f
        val scale = kotlin.math.min(maxDim / bitmap.width, maxDim / bitmap.height)
        val finalBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        
        if (finalBitmap != bitmap) {
            finalBitmap.recycle()
        }
        bitmap.recycle()

        val compressedBytes = outputStream.toByteArray()
        outputStream.close()

        val currentUser = SupabaseManager.client.auth.currentUserOrNull()
        val userId = currentUser?.id ?: UUID.randomUUID().toString()
        val fileName = "$userId/${UUID.randomUUID()}.jpg"

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

        val currentUser = SupabaseManager.client.auth.currentUserOrNull()
        val userId = currentUser?.id ?: UUID.randomUUID().toString()
        val fileName = "$userId/${UUID.randomUUID()}.m4a"

        SupabaseManager.client.storage
            .from("drops-audio")
            .upload(fileName, audioBytes, upsert = false)

        return SupabaseManager.client.storage
            .from("drops-audio")
            .publicUrl(fileName)
    }

    suspend fun uploadDeadDropFile(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()

        require(bytes != null && bytes.isNotEmpty()) { "File bytes are empty" }
        require(bytes.size <= 50 * 1024 * 1024) { "File exceeds 50MB limit" }

        val currentUser = SupabaseManager.client.auth.currentUserOrNull()
        val userId = currentUser?.id ?: java.util.UUID.randomUUID().toString()
        val extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uri.toString()) ?: "bin"
        val fileName = "$userId/${java.util.UUID.randomUUID()}.$extension"

        SupabaseManager.client.storage
            .from("drops-media")
            .upload(fileName, bytes, upsert = false)

        return SupabaseManager.client.storage
            .from("drops-media")
            .publicUrl(fileName)
    }

    suspend fun downloadDeadDropFile(url: String, fileName: String): java.io.File {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val file = java.io.File(context.cacheDir, fileName)
            if (file.exists()) return@withContext file

            // The fileUrl is a full public URL, so download directly via HTTP
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            try {
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.connect()
                
                if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    throw Exception("Download failed: HTTP ${connection.responseCode}")
                }
                
                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } finally {
                connection.disconnect()
            }
            
            file
        }
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
                    authorName = remote.authorName,
                    authorAvatarUrl = remote.authorAvatarUrl,
                    authorId = remote.authorId,
                    expiresAt = remote.expiresAt?.let { parseTimestamp(it) },
                    isAnonymous = remote.isAnonymous,
                    category = remote.category,
                    isDeadDrop = remote.isDeadDrop,
                    fileUrl = remote.fileUrl,
                    fileType = remote.fileType,
                    fileName = remote.fileName,
                    fileSize = remote.fileSize
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
        val currentUser = SupabaseManager.client.auth.currentUserOrNull()
        // Fetch all local drops and compute distance in memory to avoid SQLite floating point precision issues
        val candidates = if (currentUser != null) dao.getAllOnce(currentUser.id) else emptyList()

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

    // --- Social & Notification Features ---

    suspend fun getDropLikes(dropId: String): Int {
        return runCatching {
            val countResponse = SupabaseManager.client.postgrest["drop_likes"]
                .select { filter { eq("drop_id", dropId) } }
                .decodeList<DropLike>()
            countResponse.size
        }.getOrDefault(0)
    }

    fun getDropLikesFlow(dropId: String): Flow<Int> = kotlinx.coroutines.flow.flow {
        emit(getDropLikes(dropId))
        val channel = SupabaseManager.client.channel("likes-$dropId")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") {
            table = "drop_likes"
            filter = "drop_id=eq.$dropId"
        }
        channel.subscribe()
        try {
            changes.collect {
                emit(getDropLikes(dropId))
            }
        } finally {
            channel.unsubscribe()
        }
    }

    fun getCommentsFlow(dropId: String): Flow<List<DropComment>> = kotlinx.coroutines.flow.flow {
        emit(getComments(dropId))
        val channel = SupabaseManager.client.channel("comments-$dropId")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") {
            table = "drop_comments"
            filter = "drop_id=eq.$dropId"
        }
        channel.subscribe()
        try {
            changes.collect {
                emit(getComments(dropId))
            }
        } finally {
            channel.unsubscribe()
        }
    }

    suspend fun isDropLikedByMe(dropId: String): Boolean {
        val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return false
        return runCatching {
            val response = SupabaseManager.client.postgrest["drop_likes"]
                .select { 
                    filter { 
                        eq("drop_id", dropId)
                        eq("user_id", currentUser.id)
                    } 
                }.decodeList<DropLike>()
            response.isNotEmpty()
        }.getOrDefault(false)
    }

    suspend fun likeDrop(drop: Drop) {
        val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return
        runCatching {
            SupabaseManager.client.postgrest["drop_likes"].insert(
                com.somewhere.app.data.remote.DropLike(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = currentUser.id, 
                    dropId = drop.id
                )
            )
            
            // Send Notification to author
            val currentName = currentUser.userMetadata?.get("name")?.jsonPrimitive?.content ?: "Someone"
            if (drop.authorId != null && drop.authorId != currentUser.id) {
                SupabaseManager.client.postgrest["notifications"].insert(
                    com.somewhere.app.data.remote.NotificationItem(
                        id = java.util.UUID.randomUUID().toString(),
                        userId = drop.authorId,
                        actorName = currentName,
                        type = "like",
                        dropId = drop.id,
                        message = "$currentName liked your drop!"
                    )
                )
            }
        }.onFailure {
            println("!!! CRITICAL BACKEND ERROR IN LIKEDROP: ${it.message}")
            it.printStackTrace()
            android.util.Log.e("DropRepository", "Failed to like drop: ${it.message}", it)
        }
    }

    suspend fun unlikeDrop(dropId: String): Boolean {
        val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return false
        return try {
            SupabaseManager.client.postgrest["drop_likes"].delete {
                filter { 
                    eq("drop_id", dropId)
                    eq("user_id", currentUser.id)
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("DropRepository", "Failed to unlike drop", e)
            false
        }
    }

    suspend fun getComments(dropId: String): List<DropComment> {
        return runCatching {
            SupabaseManager.client.postgrest["drop_comments"]
                .select { filter { eq("drop_id", dropId) } }
                .decodeList<DropComment>()
                .sortedBy { it.createdAt }
        }.getOrDefault(emptyList())
    }

    suspend fun addComment(drop: Drop, text: String) {
        val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return

        val currentName = currentUser.userMetadata?.get("name")?.jsonPrimitive?.content ?: "Anonymous"
        val currentAvatar = currentUser.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
        runCatching {
            SupabaseManager.client.postgrest["drop_comments"].insert(
                com.somewhere.app.data.remote.DropComment(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = currentUser.id,
                    dropId = drop.id,
                    authorName = currentName,
                    authorAvatarUrl = currentAvatar,
                    text = text
                )
            )

            // Send Notification
            if (drop.authorId != null && drop.authorId != currentUser.id) {
                SupabaseManager.client.postgrest["notifications"].insert(
                    com.somewhere.app.data.remote.NotificationItem(
                        id = java.util.UUID.randomUUID().toString(),
                        userId = drop.authorId,
                        actorName = currentName,
                        type = "comment",
                        dropId = drop.id,
                        message = "$currentName commented: \"${text.take(30)}${if(text.length > 30) "..." else ""}\""
                    )
                )
            }
        }.onFailure {
            println("!!! CRITICAL BACKEND ERROR IN ADDCOMMENT: ${it.message}")
            it.printStackTrace()
            android.util.Log.e("DropRepository", "Failed to add comment: ${it.message}", it)
        }
    }

    suspend fun unlockDrop(dropId: String): Boolean {
        val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return false
        return try {
            SupabaseManager.client.postgrest["unlocked_drops"].insert(
                com.somewhere.app.data.remote.UnlockedDrop(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = currentUser.id, 
                    dropId = dropId
                )
            )
            true
        } catch (e: Exception) {
            println("!!! CRITICAL BACKEND ERROR IN UNLOCKDROP: ${e.message}")
            e.printStackTrace()
            android.util.Log.e("DropRepository", "Failed to unlock drop", e)
            false
        }
    }

    suspend fun getUnlockedDrops(): List<Drop> {
        val currentUser = SupabaseManager.client.auth.currentUserOrNull() ?: return emptyList()
        return runCatching {
            val unlocked = SupabaseManager.client.postgrest["unlocked_drops"]
                .select { filter { eq("user_id", currentUser.id) } }
                .decodeList<UnlockedDrop>()
                
            if (unlocked.isEmpty()) return@runCatching emptyList()

            val ids = unlocked.map { it.dropId }
            
            val dropsResponse = SupabaseManager.client.postgrest["drops"]
                .select { filter { isIn("id", ids) } }
                .decodeList<NearbyDrop>()

            dropsResponse.map { remote ->
                Drop(
                    id = remote.id,
                    text = remote.text.take(120),
                    imagePath = remote.imageUrl,
                    audioPath = remote.audioUrl,
                    latitude = remote.latitude,
                    longitude = remote.longitude,
                    timestamp = parseTimestamp(remote.createdAt),
                    authorName = remote.authorName,
                    authorAvatarUrl = remote.authorAvatarUrl,
                    authorId = remote.authorId,
                    expiresAt = remote.expiresAt?.let { parseTimestamp(it) },
                    isAnonymous = remote.isAnonymous,
                    category = remote.category
                )
            }.sortedByDescending { it.timestamp }
        }.onFailure {
            println("!!! CRITICAL BACKEND ERROR IN GETUNLOCKEDDROPS: ${it.message}")
            it.printStackTrace()
        }.getOrDefault(emptyList())
    }
    suspend fun updateDropText(dropId: String, newText: String) {
        runCatching {
            // Update remote
            SupabaseManager.client.postgrest["drops"]
                .update(
                    {
                        set("text", newText)
                    }
                ) {
                    filter { eq("id", dropId) }
                }
            
            // Update local
            val localDrop = dao.getDropById(dropId)
            if (localDrop != null) {
                dao.insert(localDrop.copy(text = newText))
            }
        }.onFailure {
            println("!!! ERROR updating drop text: ${it.message}")
            it.printStackTrace()
        }
    }
}
