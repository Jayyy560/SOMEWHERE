package com.somewhere.app.data.local

import android.content.Context
import com.somewhere.app.data.location.SavedTrip
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Account-scoped bookmarks, journals, and visit history. Never stores provider map responses. */
class TripStore(context: Context, userId: String) {
    val photoDirectory = java.io.File(context.filesDir, "wayfinder_photos/${userId.ifBlank { "guest" }}")
    private val prefs = context.getSharedPreferences("wayfinder_${userId.ifBlank { "guest" }}", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized fun trips(): List<SavedTrip> = runCatching {
        json.decodeFromString<List<SavedTrip>>(prefs.getString("trips", "[]") ?: "[]")
    }.getOrDefault(emptyList()).sortedByDescending { it.createdAtMillis }
    @Synchronized fun save(trip: SavedTrip) {
        val all = listOf(trip) + trips().filterNot { it.id == trip.id }
        all.drop(200).flatMap { it.photoPaths }.forEach(::deletePhotoFile)
        val updated = all.take(200)
        prefs.edit().putString("trips", json.encodeToString(updated)).apply()
    }
    @Synchronized fun delete(id: String) {
        trips().firstOrNull { it.id == id }?.photoPaths?.forEach(::deletePhotoFile)
        prefs.edit().putString("trips", json.encodeToString(trips().filterNot { it.id == id })).apply()
        if (activeId() == id) setActive(null)
    }
    fun visitedIds(): Set<String> = prefs.getStringSet("visited", emptySet())?.toSet().orEmpty()
    @Synchronized fun recordVisit(dropId: String, tripId: String?) {
        prefs.edit().putStringSet("visited", visitedIds() + dropId).apply()
        trips().firstOrNull { it.id == tripId }?.let { save(it.copy(visitedStopIds = it.visitedStopIds + dropId)) }
    }
    @Synchronized fun updateNote(id: String, note: String, stopId: String? = null) {
        trips().firstOrNull { it.id == id }?.let { trip ->
            save(if (stopId == null) trip.copy(note = note.take(2000)) else trip.copy(stopNotes = trip.stopNotes + (stopId to note.take(1000))))
        }
    }
    @Synchronized fun addPhotos(id: String, paths: List<String>): Boolean {
        val trip = trips().firstOrNull { it.id == id } ?: return false
        val updated = (trip.photoPaths + paths).distinct().take(12)
        if (!updated.containsAll(paths)) return false
        save(trip.copy(photoPaths = updated))
        return true
    }
    @Synchronized fun removePhoto(id: String, path: String) {
        trips().firstOrNull { it.id == id }?.let { trip ->
            if (path in trip.photoPaths) { save(trip.copy(photoPaths = trip.photoPaths - path)); deletePhotoFile(path) }
        }
    }
    private fun deletePhotoFile(path: String) {
        val file = java.io.File(path)
        if (file.canonicalPath.startsWith(photoDirectory.canonicalPath + java.io.File.separator)) file.delete()
    }
    fun activeId(): String? = prefs.getString("active", null)
    fun setActive(id: String?) { prefs.edit().putString("active", id).apply() }
    fun dismissedIds(): Set<String> = prefs.getStringSet("dismissed", emptySet())?.toSet().orEmpty()
    fun dismiss(id: String) { prefs.edit().putStringSet("dismissed", dismissedIds() + id).apply() }
    fun clearDismissed() { prefs.edit().remove("dismissed").apply() }
    fun lastAlert(id: String): Long = prefs.getLong("alert_$id", 0)
    @Synchronized fun claimAlert(id: String, nowMillis: Long): Boolean {
        if (id in visitedIds() || id in dismissedIds() || nowMillis - lastAlert(id) < 10 * 60_000L) return false
        prefs.edit().putLong("alert_$id", nowMillis).apply()
        return true
    }
}
