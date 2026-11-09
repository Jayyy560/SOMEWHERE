package com.somewhere.app.data.location

import com.somewhere.app.data.remote.NearbyDrop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Provider distances and durations always describe the whole itinerary, including every leg. */
data class TripRoute(
    val points: List<TripPoint>,
    val durationSeconds: Long,
    val distanceMeters: Long,
    val start: TripPoint,
    val end: TripPoint,
    val startAddress: String = "",
    val endAddress: String = "",
    val summary: String = "",
    val warnings: List<String> = emptyList()
)

interface TripRouting {
    suspend fun route(origin: TripPoint, destination: TripPoint, waypoints: List<TripPoint>, mode: TravelMode): TripRoute
}

data class TripPlan(
    val request: TripRequest,
    val route: TripRoute?,
    val discoveries: List<RankedTripDrop>,
    val stops: List<NearbyDrop> = emptyList(),
    val message: String? = null
)

/** Bounded insertion heuristic. All accepted plans are validated against provider travel times. */
class TripPlanner(private val routing: TripRouting, private val now: () -> Long = System::currentTimeMillis) {
    private val routePermits = Semaphore(3)
    suspend fun plan(request: TripRequest, candidates: List<NearbyDrop>, visited: Set<String>, userId: String?): TripPlan {
        val filtered = candidates.distinctBy { it.id }.filter {
            TripSearch.matches(it, if (it.id in request.fixedStopIds) request.copy(options = request.options.copy(unvisitedOnly = false)) else request, visited, userId, now()) &&
                (request.action == TripAction.ROUTE && !request.radiusIsConstraint || TripGeometry.distance(request.searchCenter, it.point()) <= request.options.radiusMeters)
        }
        val ranked = filtered.map { TripSearch.rank(it, request, visited, now()) }
            .sortedWith(compareByDescending<RankedTripDrop> { it.score }.thenBy { it.drop.id })
        val endpoint = if (request.options.returnToStart) request.returnPoint ?: request.origin else request.destination
        val wantsItinerary = request.buildItinerary || request.options.returnToStart || request.options.timeBudgetMinutes != null || request.action == TripAction.SURPRISE_ME || (request.useFixedStops || request.fixedStopIds.isNotEmpty())
        if (endpoint == null && !wantsItinerary) return TripPlan(request, null, ranked)

        val base = endpoint?.takeUnless { it == request.origin && request.waypoints.isEmpty() }?.let {
            routing.route(request.origin, it, request.waypoints, request.options.travelMode)
        }
        val budgetSeconds = request.options.timeBudgetMinutes?.times(60L)
        if (base != null && budgetSeconds != null && base.durationSeconds > budgetSeconds) {
            throw IllegalArgumentException("The direct trip already exceeds your time budget. Increase it or choose a closer destination.")
        }
        if (request.useFixedStops || request.fixedStopIds.isNotEmpty()) {
            if (request.fixedStopIds.isEmpty()) return TripPlan(request, base, ranked)
            val byId = filtered.associateBy { it.id }
            val stops = request.fixedStopIds.map { byId[it] ?: throw IllegalArgumentException("A saved stop is unavailable or does not match your filters.") }
            val finalEnd = endpoint ?: stops.last().point()
            val via = request.waypoints + if (endpoint == null) stops.dropLast(1).map { it.point() } else stops.map { it.point() }.filterNot { it == finalEnd }
            val route = routing.route(request.origin, finalEnd, via, request.options.travelMode)
            checkBudget(route, stops.size, request, budgetSeconds)
            require(base == null || request.options.maxDetourMinutes == null || route.durationSeconds - base.durationSeconds <= request.options.maxDetourMinutes * 60L) {
                "These stops exceed your extra travel limit. Increase it or remove a stop."
            }
            return TripPlan(request, route, stops.map { TripSearch.rank(it, request, visited, now()) }, stops)
        }

        if (!wantsItinerary && base != null) {
            // Broad-phase corridor search is followed by real route checks, including barriers and access.
            val verified = coroutineScope {
                ranked.take(MAX_CANDIDATE_CHECKS).map { candidate -> async {
                    routePermits.withPermit {
                        currentCoroutineContext().ensureActive()
                        val waypoints = bestInsertion(request.origin, request.waypoints, endpoint!!, candidate.drop.point())
                        val route = reachable { routing.route(request.origin, endpoint, waypoints, request.options.travelMode) } ?: return@withPermit null
                        val extra = (route.durationSeconds - base.durationSeconds).coerceAtLeast(0)
                        val maxExtra = request.options.maxDetourMinutes?.times(60L)
                        if (maxExtra != null && extra > maxExtra) return@withPermit null
                        TripSearch.rank(candidate.drop, request, visited, now(), extra,
                            TripGeometry.project(candidate.drop.point(), base.points).progressMeters)
                    }
                } }.awaitAll().filterNotNull()
            }
            return TripPlan(request, base, verified.sortedWith(compareByDescending<RankedTripDrop> { it.score }.thenBy { it.drop.id }),
                message = if (ranked.size > MAX_CANDIDATE_CHECKS) "Showing the best ${MAX_CANDIDATE_CHECKS} checked matches." else null)
        }

        val pool = ranked.take(MAX_CANDIDATE_CHECKS).toMutableList()
        var stops = emptyList<NearbyDrop>()
        var acceptedRoute: TripRoute? = base
        var acceptedDiscoveries = emptyList<RankedTripDrop>()
        var requests = 0
        val requestedStops = if (request.action == TripAction.SURPRISE_ME) 1 else request.options.stopCount
        while (stops.size < requestedStops && pool.isNotEmpty() && requests < MAX_ROUTE_REQUESTS) {
            var best: Pair<List<NearbyDrop>, TripRoute>? = null
            var offset = 0
            val previousSeconds = acceptedRoute?.durationSeconds ?: 0L
            // If the first batch is inaccessible or too expensive, try the next ranked candidates.
            while (best == null && offset < pool.size && requests < MAX_ROUTE_REQUESTS) {
                val batch = pool.drop(offset).take(minOf(6, MAX_ROUTE_REQUESTS - requests))
                requests += batch.size; offset += batch.size
                val evaluated = coroutineScope {
                    batch.map { candidate -> async {
                        routePermits.withPermit {
                            currentCoroutineContext().ensureActive()
                            val ordered = insertStop(stops, candidate.drop, request.origin, endpoint)
                            val end = endpoint ?: ordered.last().point()
                            val via = request.waypoints + if (endpoint == null) ordered.dropLast(1).map { it.point() } else ordered.map { it.point() }
                            val route = reachable { routing.route(request.origin, end, via, request.options.travelMode) } ?: return@withPermit null
                            val total = route.durationSeconds + ordered.size * request.options.dwellMinutes * 60L
                            if (budgetSeconds != null && total > budgetSeconds) return@withPermit null
                            val extra = (route.durationSeconds - (base?.durationSeconds ?: 0)).coerceAtLeast(0)
                            if (base != null && request.options.maxDetourMinutes != null && extra > request.options.maxDetourMinutes * 60L) return@withPermit null
                            val added = (route.durationSeconds - previousSeconds).coerceAtLeast(0)
                            Triple(candidate.score / (1.0 + added / 300.0), ordered, route)
                        }
                    } }.awaitAll().filterNotNull()
                }
                best = evaluated.maxByOrNull { it.first }?.let { it.second to it.third }
            }
            if (best == null) break
            stops = best.first
            acceptedRoute = best.second
            pool.removeAll { candidate -> stops.any { it.id == candidate.drop.id } }
            acceptedDiscoveries = stops.map { drop ->
                TripSearch.rank(drop, request, visited, now())
            }
        }
        if (stops.isEmpty()) throw IllegalArgumentException("No reachable Drops fit these limits. Try a larger radius or more time.")
        // Check a simple reversal with actual network cost; retain curated order and mandatory waypoints.
        if (stops.size >= 3 && request.waypoints.isEmpty() && requests < MAX_ROUTE_REQUESTS) {
            val reversed = stops.reversed()
            val end = endpoint ?: reversed.last().point()
            val via = if (endpoint == null) reversed.dropLast(1).map { it.point() } else reversed.map { it.point() }
            val alternative = reachable { routing.route(request.origin, end, via, request.options.travelMode) }
            if (alternative != null && alternative.durationSeconds < acceptedRoute!!.durationSeconds) {
                stops = reversed; acceptedRoute = alternative
                acceptedDiscoveries = stops.map { drop -> acceptedDiscoveries.first { it.drop.id == drop.id } }
            }
        }
        checkBudget(acceptedRoute!!, stops.size, request, budgetSeconds)
        val resolved = request.copy(destination = endpoint ?: stops.last().point())
        return TripPlan(resolved, acceptedRoute, acceptedDiscoveries, stops,
            if (stops.size < requestedStops) "${stops.size} of $requestedStops stops fit your limits." else null)
    }

    private fun checkBudget(route: TripRoute, count: Int, request: TripRequest, budget: Long?) {
        require(budget == null || route.durationSeconds + count * request.options.dwellMinutes * 60L <= budget) {
            "This itinerary exceeds your time budget, including time at stops."
        }
    }
    private suspend fun reachable(block: suspend () -> TripRoute): TripRoute? = try { block() }
        catch (e: CancellationException) { throw e }
        catch (e: NoTripRouteException) { null }

    private fun bestInsertion(origin: TripPoint, via: List<TripPoint>, end: TripPoint, point: TripPoint): List<TripPoint> {
        val index = (0..via.size).minByOrNull { i ->
            val before = via.getOrNull(i - 1) ?: origin; val after = via.getOrNull(i) ?: end
            TripGeometry.distance(before, point) + TripGeometry.distance(point, after) - TripGeometry.distance(before, after)
        } ?: 0
        return via.toMutableList().apply { add(index, point) }
    }
    private fun insertStop(stops: List<NearbyDrop>, drop: NearbyDrop, origin: TripPoint, end: TripPoint?): List<NearbyDrop> {
        val index = (0..stops.size).minByOrNull { i ->
            val before = stops.getOrNull(i - 1)?.point() ?: origin
            val after = stops.getOrNull(i)?.point() ?: end
            TripGeometry.distance(before, drop.point()) + (after?.let { TripGeometry.distance(drop.point(), it) - TripGeometry.distance(before, it) } ?: 0.0)
        } ?: 0
        return stops.toMutableList().apply { add(index, drop) }
    }
    companion object { const val MAX_CANDIDATE_CHECKS = 24; const val MAX_ROUTE_REQUESTS = 40 }
}
class NoTripRouteException(message: String = "No route is available for this travel mode.") : Exception(message)
