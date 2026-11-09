package com.somewhere.app.data.repository

import android.content.Context
import com.somewhere.app.BuildConfig
import com.somewhere.app.data.location.*
import com.somewhere.app.data.remote.NearbyDrop
import com.somewhere.app.data.remote.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Maps transport and spatial discovery. Wayfinder never sends queries or Drops to an AI service. */
class TripRepository(private val context: Context) : TripRouting {
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }
    private val apiKey get() = BuildConfig.GOOGLE_MAPS_API_KEY
    val currentUserId: String? get() = SupabaseManager.client.auth.currentUserOrNull()?.id

    override suspend fun route(origin: TripPoint, destination: TripPoint, waypoints: List<TripPoint>, mode: TravelMode): TripRoute =
        routeLocations(origin.coordinate, destination.coordinate, waypoints.map { it.coordinate }, mode)

    suspend fun routeLocations(origin: String, destination: String, waypoints: List<String>, mode: TravelMode): TripRoute {
        require(waypoints.size <= 23) { "This trip has too many stops." }
        val body = buildJsonObject {
            put("origin", waypoint(origin)); put("destination", waypoint(destination))
            put("intermediates", buildJsonArray { waypoints.forEach { add(waypoint(it)) } })
            put("travelMode", mode.routesValue); put("polylineQuality", "HIGH_QUALITY")
        }
        val request = Request.Builder().url("https://routes.googleapis.com/directions/v2:computeRoutes")
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline,routes.legs.startLocation,routes.legs.endLocation,routes.description,routes.warnings")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        return try {
            TripRouteParser.routes(json.parseToJsonElement(execute(request)).jsonObject)
        } catch (e: MapsHttpException) {
            // Existing installations can continue using their enabled legacy service while Routes is enabled.
            if (e.status !in listOf(403, 404)) throw e
            legacyRoute(origin, destination, waypoints, mode)
        }
    }
    private fun waypoint(value: String): JsonObject = TripPoint.parse(value)?.let { point ->
        buildJsonObject { put("location", buildJsonObject { put("latLng", buildJsonObject { put("latitude", point.latitude); put("longitude", point.longitude) }) }) }
    } ?: buildJsonObject { put("address", value) }

    private suspend fun legacyRoute(origin: String, destination: String, via: List<String>, mode: TravelMode): TripRoute {
        val url = HttpUrl.Builder().scheme("https").host("maps.googleapis.com").addPathSegments("maps/api/directions/json")
            .addQueryParameter("origin", origin).addQueryParameter("destination", destination)
            .addQueryParameter("mode", mode.apiValue).addQueryParameter("key", apiKey)
        if (via.isNotEmpty()) url.addQueryParameter("waypoints", via.joinToString("|"))
        return TripRouteParser.legacy(json.parseToJsonElement(execute(Request.Builder().url(url.build()).build())).jsonObject)
    }
    suspend fun resolvePlace(value: String, near: TripPoint, mode: TravelMode): TripPoint =
        TripPoint.parse(value) ?: routeLocations(near.coordinate, value, emptyList(), mode).end

    suspend fun getPlaceSuggestions(query: String): List<String> {
        if (query.length < 3) return emptyList()
        val url = HttpUrl.Builder().scheme("https").host("maps.googleapis.com").addPathSegments("maps/api/place/autocomplete/json")
            .addQueryParameter("input", query).addQueryParameter("key", apiKey).build()
        val response = json.parseToJsonElement(execute(Request.Builder().url(url).build())).jsonObject
        return response["predictions"]?.jsonArray.orEmpty().mapNotNull { it.jsonObject["description"]?.jsonPrimitive?.content }.take(5)
    }

    /** The RPC applies all mandatory filters before relevance ordering and pagination. */
    suspend fun candidates(request: TripRequest, visited: Set<String>, route: List<TripPoint> = emptyList()): List<NearbyDrop> {
        val prefs = context.getSharedPreferences("somewhere_prefs", Context.MODE_PRIVATE)
        val query = TripSearch.parse(request.query)
        val params = buildJsonObject {
            put("p_lat", request.searchCenter.latitude); put("p_lon", request.searchCenter.longitude)
            put("p_radius_m", request.options.radiusMeters)
            put("p_route", buildJsonArray { route.forEach { point -> add(buildJsonArray { add(point.longitude); add(point.latitude) }) } })
            put("p_corridor_m", when (request.options.travelMode) { TravelMode.WALKING -> 600; TravelMode.CYCLING -> 1200; TravelMode.DRIVING -> 2000 })
            put("p_filter", buildJsonObject {
                put("groups", json.encodeToJsonElement(query.groups)); put("exclusions", json.encodeToJsonElement(request.exclusions))
                put("media", json.encodeToJsonElement((query.media + listOfNotNull(request.options.media.takeUnless { it == TripMedia.ANY })).map { it.name }))
                put("category", request.options.category?.let(::JsonPrimitive) ?: JsonNull)
                put("author", query.authorName?.let(::JsonPrimitive) ?: JsonNull)
                put("mine", query.mine); put("anonymous", query.anonymous)
                put("recent", query.recent || request.options.preferRecent)
                put("tags", json.encodeToJsonElement(query.hashtags))
                put("step_free", request.options.stepFreeOnly)
                put("radius_required", request.radiusIsConstraint)
                put("excluded_ids", json.encodeToJsonElement(prefs.getStringSet("reported_drops", emptySet()).orEmpty() + if (request.options.unvisitedOnly) visited else emptySet()))
                put("blocked_authors", json.encodeToJsonElement(prefs.getStringSet("blocked_users", emptySet()).orEmpty()))
            })
            put("p_limit", 100); put("p_offset", 0)
        }
        val result = mutableListOf<NearbyDrop>()
        var offset = 0
        try {
            do {
                val pageParams = JsonObject(params.toMutableMap().apply { put("p_offset", JsonPrimitive(offset)) })
                val page = SupabaseManager.client.postgrest.rpc("wayfinder_drops", pageParams).decodeList<NearbyDrop>()
                result += page
                offset += page.size
            } while (page.size == 100 && offset < 1000)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (e.message.orEmpty().contains("PGRST202") || e.message.orEmpty().contains("Could not find the function")) {
                throw IllegalStateException("Wayfinder search is not available on this server yet.", e)
            }
            throw e
        }
        return result.filter { TripSearch.matches(it, request, visited, currentUserId, System.currentTimeMillis()) }
    }
    suspend fun dropsByIds(ids: List<String>): List<NearbyDrop> {
        if (ids.isEmpty()) return emptyList()
        val params = buildJsonObject { put("p_ids", json.encodeToJsonElement(ids)) }
        return SupabaseManager.client.postgrest.rpc("wayfinder_stops", params).decodeList<NearbyDrop>()
    }
    suspend fun trails(center: TripPoint, radiusMeters: Double): List<CuratedTrail> {
        val params = buildJsonObject { put("p_lat", center.latitude); put("p_lon", center.longitude); put("p_radius_m", radiusMeters) }
        return SupabaseManager.client.postgrest.rpc("wayfinder_trails_near", params).decodeList<CuratedTrail>()
    }
    suspend fun publishTrail(title: String, description: String, trip: SavedTrip): String {
        require(trip.stopIds.isNotEmpty()) { "Add stops to this trip before publishing a trail." }
        val params = buildJsonObject {
            put("p_title", title.trim().take(80)); put("p_description", description.trim().take(500))
            put("p_stop_ids", json.encodeToJsonElement(trip.stopIds)); put("p_travel_mode", trip.request.options.travelMode.apiValue)
            put("p_return_to_start", trip.request.options.returnToStart)
        }
        return SupabaseManager.client.postgrest.rpc("publish_wayfinder_trail", params).decodeAs<String>()
    }
    suspend fun setStepFreeAccess(id: String, accessible: Boolean?) {
        val params = buildJsonObject { put("p_drop_id", id); put("p_accessible", accessible?.let(::JsonPrimitive) ?: JsonNull) }
        SupabaseManager.client.postgrest.rpc("set_wayfinder_stop_access", params)
    }
    private suspend fun execute(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call = http.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        if (!it.isSuccessful) throw MapsHttpException(it.code)
                        val body = it.body?.string() ?: throw IOException("The maps service returned an empty response.")
                        if (continuation.isActive) continuation.resume(body)
                    } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
                }
            }
        })
    }
}
class MapsHttpException(val status: Int) : IOException("The maps service could not complete this request ($status).")
