package com.somewhere.app.data.repository

import android.content.Context
import com.somewhere.app.BuildConfig
import com.somewhere.app.data.remote.NearbyDrop
import com.somewhere.app.data.remote.SupabaseManager
import com.google.android.gms.maps.model.LatLng
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.somewhere.app.util.CategoryUtils
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import java.net.URL
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class AiDrop(val id: String, val text: String)
data class AiFilterRequest(val query: String, val drops: List<AiDrop>)
data class AiFilterResponse(val matching_drop_ids: List<String>)

interface AiFilterService {
    @POST("filter_drops")
    suspend fun filterDrops(@Body request: AiFilterRequest): AiFilterResponse
}

/**
 * Repository for Trip Mode — fetches routes from Google Directions API
 * and finds Drops that are spatially near the route polyline.
 */
class TripRepository(private val context: Context) {

    private val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY

    /**
     * Data class representing a decoded route from the Directions API.
     */
    data class RouteResult(
        val polylinePoints: List<LatLng>,
        val encodedPolyline: String,
        val durationText: String,
        val distanceText: String,
        val summary: String,
        val startAddress: String,
        val endAddress: String
    )

    /**
     * Fetch the driving route between two locations using Google Directions API.
     * [origin] and [destination] can be place names, addresses, or "lat,lng" strings.
     */
    suspend fun fetchRoute(origin: String, destination: String, waypoint: String? = null): RouteResult = withContext(Dispatchers.IO) {
        val url = buildString {
            append("https://maps.googleapis.com/maps/api/directions/json?")
            append("origin=${java.net.URLEncoder.encode(origin, "UTF-8")}")
            append("&destination=${java.net.URLEncoder.encode(destination, "UTF-8")}")
            if (!waypoint.isNullOrBlank()) {
                append("&waypoints=${java.net.URLEncoder.encode(waypoint, "UTF-8")}")
            }
            append("&mode=driving")
            append("&key=$apiKey")
        }

        val response = URL(url).readText()
        val json = JSONObject(response)

        val status = json.getString("status")
        if (status != "OK") {
            throw Exception("Directions API error: $status. ${json.optString("error_message", "")}")
        }

        val route = json.getJSONArray("routes").getJSONObject(0)
        val leg = route.getJSONArray("legs").getJSONObject(0)
        val overviewPolyline = route.getJSONObject("overview_polyline").getString("points")

        RouteResult(
            polylinePoints = decodePolyline(overviewPolyline),
            encodedPolyline = overviewPolyline,
            durationText = leg.getJSONObject("duration").getString("text"),
            distanceText = leg.getJSONObject("distance").getString("text"),
            summary = route.optString("summary", ""),
            startAddress = leg.getString("start_address"),
            endAddress = leg.getString("end_address")
        )
    }

    /**
     * Fetch place autocomplete suggestions using Google Places API.
     */
    suspend fun getPlaceSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.length < 3) return@withContext emptyList()
        val url = buildString {
            append("https://maps.googleapis.com/maps/api/place/autocomplete/json?")
            append("input=${java.net.URLEncoder.encode(query, "UTF-8")}")
            append("&key=$apiKey")
        }
        try {
            val response = URL(url).readText()
            val json = JSONObject(response)
            if (json.getString("status") != "OK") return@withContext emptyList()
            
            val predictions = json.getJSONArray("predictions")
            val suggestions = mutableListOf<String>()
            for (i in 0 until predictions.length()) {
                suggestions.add(predictions.getJSONObject(i).getString("description"))
            }
            suggestions
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Find drops that lie within [corridorMeters] of the route polyline.
     * We sample points along the polyline and query Supabase for drops near each segment.
     */
    suspend fun getDropsAlongRoute(
        polylinePoints: List<LatLng>,
        corridorMeters: Double = 2000.0
    ): List<NearbyDrop> = withContext(Dispatchers.IO) {
        // Sample points along the polyline every ~2km to create search circles
        val sampledPoints = samplePolyline(polylinePoints, intervalMeters = 2000.0)

        val allDrops = mutableListOf<NearbyDrop>()
        val seenIds = mutableSetOf<String>()
        
        val sharedPrefs = context.getSharedPreferences("somewhere_prefs", Context.MODE_PRIVATE)
        val reportedDrops = sharedPrefs.getStringSet("reported_drops", emptySet()) ?: emptySet()
        val blockedUsers = sharedPrefs.getStringSet("blocked_users", emptySet()) ?: emptySet()

        for (point in sampledPoints) {
            try {
                val params = buildJsonObject {
                    put("lat", point.latitude)
                    put("lng", point.longitude)
                    put("radius_meters", corridorMeters)
                }
                val drops = SupabaseManager.client.postgrest.rpc("nearby_drops", params)
                    .decodeList<NearbyDrop>()

                for (drop in drops) {
                    if (drop.id !in seenIds
                        && drop.id !in reportedDrops
                        && (drop.authorName == null || drop.authorName !in blockedUsers)
                    ) {
                        seenIds.add(drop.id)
                        allDrops.add(drop)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        allDrops
    }

    /**
     * Find drops within a specific radius of a single point (for radius searches without driving routes).
     */
    suspend fun getDropsAroundLocation(
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Double
    ): List<NearbyDrop> = withContext(Dispatchers.IO) {
        val allDrops = mutableListOf<NearbyDrop>()
        
        val sharedPrefs = context.getSharedPreferences("somewhere_prefs", Context.MODE_PRIVATE)
        val reportedDrops = sharedPrefs.getStringSet("reported_drops", emptySet()) ?: emptySet()
        val blockedUsers = sharedPrefs.getStringSet("blocked_users", emptySet()) ?: emptySet()

        try {
            val params = buildJsonObject {
                put("lat", centerLat)
                put("lng", centerLng)
                put("radius_meters", radiusMeters)
            }
            val drops = SupabaseManager.client.postgrest.rpc("nearby_drops", params)
                .decodeList<NearbyDrop>()

            for (drop in drops) {
                if (drop.id !in reportedDrops && (drop.authorName == null || drop.authorName !in blockedUsers)) {
                    allDrops.add(drop)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        allDrops
    }

    /**
     * Filter drops by a keyword/intent query. 
     * Uses Colab AI Semantic Server if configured, otherwise falls back to local text matching.
     */
    suspend fun filterDropsByQuery(
        drops: List<NearbyDrop>, 
        query: String,
        exclusions: List<String> = emptyList()
    ): List<NearbyDrop> {
        // 0. Apply Exclusions immediately
        var filteredDrops = drops
        if (exclusions.isNotEmpty()) {
            filteredDrops = drops.filterNot { drop ->
                val searchTarget = "${drop.text} ${drop.category}".lowercase()
                exclusions.any { ex -> searchTarget.contains(ex.lowercase()) }
            }
        }

        val q = query.lowercase().trim()
        if (q.isBlank()) return filteredDrops
        
        // ── Fast-Path Local Intents (Bypass AI) ──
        
        // 1. All Drops
        if (q in listOf("all drops", "all", "everything", "anything", "show all", "drops", "")) {
            return filteredDrops
        }
        
        // 2. Photos / Images
        if (q in listOf("photos", "images", "pictures", "visuals", "photo drops", "picture drops")) {
            return filteredDrops.filter { it.imageUrl.isNotBlank() }
        }
        
        // 3. Audio / Sound
        if (q in listOf("audio", "audio drops", "sounds", "sound drops", "voice", "voice drops", "voice notes")) {
            return filteredDrops.filter { !it.audioUrl.isNullOrBlank() }
        }
        
        // 4. Text / Stories
        if (q in listOf("text", "text drops", "stories", "written", "written drops")) {
            return filteredDrops.filter { it.imageUrl.isBlank() && it.audioUrl.isNullOrBlank() }
        }
        
        // 5. My Drops
        if (q in listOf("my drops", "mine", "my own drops", "my posts")) {
            val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
            return if (currentUserId != null) {
                filteredDrops.filter { it.authorId == currentUserId }
            } else {
                emptyList() // Or maybe return drops if not logged in? Empty makes more sense.
            }
        }
        
        // 6. Anonymous / Secrets
        if (q in listOf("anonymous", "anonymous drops", "secrets", "secret drops")) {
            return filteredDrops.filter { it.isAnonymous }
        }
        
        // 7. Recent / Latest
        if (q in listOf("recent", "latest", "new", "newest", "recent drops", "newest drops", "latest drops")) {
            return filteredDrops.sortedByDescending { it.createdAt ?: "" }
        }
        

        if (CategoryUtils.CATEGORIES.contains(q)) {
            return filteredDrops.filter { it.category.equals(q, ignoreCase = true) }
        }
        
        // 9. Hashtags
        if (q.startsWith("#") && q.length > 1) {
            val tag = q.substring(1)
            return filteredDrops.filter { it.text.contains(tag, ignoreCase = true) }
        }
        
        // 10. Username ("by @username")
        val byRegex = Regex("(?i)by\\s+@(\\w+)")
        val match = byRegex.find(q)
        if (match != null) {
            val username = match.groupValues[1]
            return filteredDrops.filter { it.authorName.equals(username, ignoreCase = true) }
        }

        // ── Fallback to AI Semantic Filtering ──
        val colabUrl = BuildConfig.COLAB_AI_URL
        if (colabUrl.isNotBlank()) {
            try {
                // Use AI Semantic Filtering via Ngrok
                val retrofit = Retrofit.Builder()
                    .baseUrl(if (colabUrl.endsWith("/")) colabUrl else "$colabUrl/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    
                val service = retrofit.create(AiFilterService::class.java)
                
                val aiDrops = filteredDrops.map { AiDrop(it.id, it.text) }
                val request = AiFilterRequest(q, aiDrops)
                
                val response = service.filterDrops(request)
                
                val matchingIds = response.matching_drop_ids.toSet()
                return filteredDrops.filter { matchingIds.contains(it.id) }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to text matching if AI server is down
            }
        }

        // Local Fallback: simple text matching
        val keywords = q.split(" ").filter { it.length > 2 }
        return filteredDrops.filter { drop ->
            val text = drop.text.lowercase()
            val category = drop.category?.lowercase() ?: ""
            keywords.any { keyword ->
                text.contains(keyword) || category.contains(keyword)
            }
        }
    }

    suspend fun getCategories(): List<String> = withContext(Dispatchers.IO) {
        CategoryUtils.CATEGORIES
    }

    /**
     * Decode a Google Maps encoded polyline string into LatLng points.
     */
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1F) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dLat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1F) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dLng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }

        return poly
    }

    /**
     * Sample points along a polyline at roughly [intervalMeters] intervals.
     */
    private fun samplePolyline(points: List<LatLng>, intervalMeters: Double): List<LatLng> {
        if (points.isEmpty()) return emptyList()

        val sampled = mutableListOf(points.first())
        var accumulated = 0.0

        for (i in 1 until points.size) {
            val dist = haversineDistance(
                points[i - 1].latitude, points[i - 1].longitude,
                points[i].latitude, points[i].longitude
            )
            accumulated += dist
            if (accumulated >= intervalMeters) {
                sampled.add(points[i])
                accumulated = 0.0
            }
        }

        // Always include the last point
        if (sampled.last() != points.last()) {
            sampled.add(points.last())
        }

        return sampled
    }

    /**
     * Haversine formula to calculate distance between two lat/lng points in meters.
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
