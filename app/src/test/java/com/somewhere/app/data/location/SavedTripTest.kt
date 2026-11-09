package com.somewhere.app.data.location

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SavedTripTest {
    @Test fun itineraryAndPrivateJournalSurviveSerialization() {
        val origin = TripPoint(28.6,77.2)
        val request = TripRequest(origin,TripPoint(28.7,77.3),listOf(TripPoint(28.65,77.25)),"history audio",listOf("food"),
            TripOptions(travelMode = TravelMode.CYCLING,timeBudgetMinutes = 45,maxDetourMinutes = 5,unvisitedOnly = true,returnToStart = true),
            TripAction.ROUTE, fixedStopIds = listOf("one","two"), returnPoint = origin)
        val saved = SavedTrip("trip","Morning",request,listOf("one","two"),100,visitedStopIds = setOf("one"),note = "Private note",stopNotes = mapOf("one" to "My photo"),photoPaths = listOf("/private/trip/photo.jpg"))
        assertEquals(saved,Json.decodeFromString<SavedTrip>(Json.encodeToString(saved)))
        assertEquals(origin,saved.request.returnPoint)
    }
}
