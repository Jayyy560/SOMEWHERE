package com.somewhere.app.data.location

import com.somewhere.app.data.remote.NearbyDrop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TripPlannerTest {
    private val start = TripPoint(0.0,0.0)
    private val end = TripPoint(0.0,.02)
    private fun drop(id: String, longitude: Double, category: String = "Photography") = NearbyDrop(id,"A view","",latitude = 0.0,longitude = longitude,category = category)
    private class Router(private val cost: (List<TripPoint>) -> Long) : TripRouting {
        val journeys = mutableListOf<List<TripPoint>>()
        override suspend fun route(origin: TripPoint,destination: TripPoint,waypoints: List<TripPoint>,mode: TravelMode): TripRoute {
            val points = listOf(origin)+waypoints+destination; journeys += points
            return TripRoute(points,cost(points),TripGeometry.length(points).toLong(),origin,destination)
        }
    }
    @Test fun nearbySearchRanksWithoutCallingRouter() = runBlocking {
        val router = Router { error("A radius search must not request routes") }
        val plan = TripPlanner(router).plan(TripRequest(start,query = "photography"),listOf(drop("near",.001),drop("far",.1)),emptySet(),null)
        assertNull(plan.route); assertEquals(listOf("near"),plan.discoveries.map { it.drop.id })
    }
    @Test fun surpriseHonorsRadiusInterestsAndExclusions() = runBlocking {
        val router = Router { 100 }
        val request = TripRequest(start,query = "photography", exclusions = listOf("road"), options = TripOptions(radiusMeters = 1000.0),action = TripAction.SURPRISE_ME)
        val candidates = listOf(drop("outside",.1),drop("wrong",.001,"Food"),drop("excluded",.001).copy(text = "By the road"),drop("valid",.002))
        val plan = TripPlanner(router).plan(request,candidates,emptySet(),null)
        assertEquals(listOf("valid"),plan.stops.map { it.id })
    }
    @Test fun rejectsCloseDropsWithLargeNetworkDetours() = runBlocking {
        val blocked = drop("across-river",.009)
        val accessible = drop("bridge",.011)
        val router = Router { points -> when { blocked.point() in points -> 3000; accessible.point() in points -> 660; else -> 600 } }
        val request = TripRequest(start,end,query = "photography",options = TripOptions(maxDetourMinutes = 5),action = TripAction.ROUTE)
        val plan = TripPlanner(router).plan(request,listOf(blocked,accessible),emptySet(),null)
        assertEquals(listOf("bridge"),plan.discoveries.map { it.drop.id })
        assertEquals(60L,plan.discoveries.single().detourSeconds)
    }
    @Test fun circularTripIncludesDwellInHardBudget() = runBlocking {
        val router = Router { points -> if (points.size > 3) 1700 else 1000 }
        val request = TripRequest(start,query = "photography",options = TripOptions(returnToStart = true,timeBudgetMinutes = 20,dwellMinutes = 2,stopCount = 3))
        val plan = TripPlanner(router).plan(request,listOf(drop("one",.001),drop("two",.002)),emptySet(),null)
        assertEquals(1,plan.stops.size); assertEquals(start,plan.route!!.end)
        assertTrue(plan.route.durationSeconds+plan.stops.size*120 <= 1200)
        assertEquals("1 of 3 stops fit your limits.",plan.message)
    }
    @Test fun curatedTrailsRetainTheirPublishedOrder() = runBlocking {
        val router = Router { 100 }
        val one = drop("one",.003); val two = drop("two",.001)
        val request = TripRequest(start,options = TripOptions(),action = TripAction.ROUTE,fixedStopIds = listOf("one","two"))
        val plan = TripPlanner(router).plan(request,listOf(two,one),emptySet(),null)
        assertEquals(listOf("one","two"),plan.stops.map { it.id })
        assertEquals(listOf(start,one.point(),two.point()),router.journeys.last())
    }
    @Test fun missingCuratedStopIsReportedInsteadOfSilentlySkipped() = runBlocking {
        val failure = runCatching { TripPlanner(Router { 100 }).plan(TripRequest(start,fixedStopIds = listOf("missing")),emptyList(),emptySet(),null) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }
    @Test fun cancellationIsNotTreatedAsAnUnreachableDrop() = runBlocking {
        val router = object : TripRouting {
            override suspend fun route(origin: TripPoint,destination: TripPoint,waypoints: List<TripPoint>,mode: TravelMode): TripRoute = throw CancellationException()
        }
        val failure = runCatching { TripPlanner(router).plan(TripRequest(start,action = TripAction.SURPRISE_ME),listOf(drop("one",.001)),emptySet(),null) }.exceptionOrNull()
        assertTrue(failure is CancellationException)
    }
    @Test fun fixedItineraryCannotExceedTotalBudget() = runBlocking {
        val router = Router { 1200 }
        val request = TripRequest(start,options = TripOptions(timeBudgetMinutes = 20,dwellMinutes = 2),fixedStopIds = listOf("one"))
        assertTrue(runCatching { TripPlanner(router).plan(request,listOf(drop("one",.001)),emptySet(),null) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun explicitStopCountBuildsItineraryWithoutTimeLimit() = runBlocking {
        val request = TripRequest(start,options = TripOptions(stopCount = 2),buildItinerary = true)
        val plan = TripPlanner(Router { 100 }).plan(request,listOf(drop("one",.001),drop("two",.002)),emptySet(),null)
        assertEquals(2,plan.stops.size)
    }
    @Test fun fixedStopsCannotBypassDetourLimit() = runBlocking {
        val router = Router { points -> if (points.size > 2) 2000 else 500 }
        val request = TripRequest(start,end,options = TripOptions(maxDetourMinutes = 5),action = TripAction.ROUTE,fixedStopIds = listOf("one"))
        assertTrue(runCatching { TripPlanner(router).plan(request,listOf(drop("one",.001)),emptySet(),null) }.exceptionOrNull() is IllegalArgumentException)
    }
    @Test fun removingAllStopsDoesNotAutomaticallyAddThemBack() = runBlocking {
        val request = TripRequest(start,options = TripOptions(returnToStart = true,timeBudgetMinutes = 30),useFixedStops = true)
        val plan = TripPlanner(Router { 100 }).plan(request,listOf(drop("one",.001)),emptySet(),null)
        assertTrue(plan.stops.isEmpty())
        assertNull(plan.route)
    }
    @Test fun explicitRouteRadiusRemainsAHardConstraint() = runBlocking {
        val request = TripRequest(start,end,options = TripOptions(radiusMeters = 500.0),action = TripAction.ROUTE,radiusIsConstraint = true)
        val plan = TripPlanner(Router { 100 }).plan(request,listOf(drop("near",.001),drop("far",.01)),emptySet(),null)
        assertEquals(listOf("near"),plan.discoveries.map { it.drop.id })
    }

    @Test fun triesNextBatchWhenTopCandidatesDoNotFit() = runBlocking {
        val candidates = (1..8).map { drop("$it",it*.0001) }
        val reachable = candidates[6].point()
        val router = Router { points -> if (reachable in points) 100 else 2000 }
        val request = TripRequest(start,options = TripOptions(timeBudgetMinutes = 5),action = TripAction.SURPRISE_ME)
        val plan = TripPlanner(router).plan(request,candidates,emptySet(),null)
        assertEquals(listOf("7"),plan.stops.map { it.id })
    }
}
