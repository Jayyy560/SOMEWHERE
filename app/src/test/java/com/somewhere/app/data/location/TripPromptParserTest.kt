package com.somewhere.app.data.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripPromptParserTest {

    @Test
    fun testRouteFromTo() {
        val parsed = TripPromptParser.parse("from london to paris")
        assertEquals(TripAction.ROUTE, parsed.action)
        assertEquals("london", parsed.origin)
        assertEquals("paris", parsed.destination)
        assertNull(parsed.waypoint)
        assertEquals("", parsed.query)
        assertNull(parsed.radiusMeters)
    }

    @Test
    fun testRouteToFrom() {
        val parsed = TripPromptParser.parse("to paris from london")
        assertEquals(TripAction.ROUTE, parsed.action)
        assertEquals("london", parsed.origin)
        assertEquals("paris", parsed.destination)
        assertEquals("", parsed.query)
    }

    @Test
    fun testRadiusSearchWithExclusions() {
        val parsed = TripPromptParser.parse("cafes nearby without starbucks")
        assertEquals(TripAction.RADIUS_SEARCH, parsed.action)
        assertEquals("cafes", parsed.query)
        assertEquals(2000.0, parsed.radiusMeters!!, 0.01)
        assertEquals(listOf("starbucks"), parsed.exclusions)
        assertNull(parsed.origin)
        assertNull(parsed.destination)
    }

    @Test
    fun testSurpriseMeWithinRadius() {
        val parsed = TripPromptParser.parse("surprise me within 50 miles")
        assertEquals(TripAction.SURPRISE_ME, parsed.action)
        assertEquals(50 * 1609.34, parsed.radiusMeters!!, 0.01)
        assertEquals("", parsed.query)
    }

    @Test
    fun testDestinationInference() {
        val parsed = TripPromptParser.parse("central park")
        assertEquals(TripAction.ROUTE, parsed.action)
        assertEquals("central park", parsed.destination)
        assertNull(parsed.origin)
        assertEquals("", parsed.query)
    }

    @Test
    fun testCurrentLocationNormalization() {
        val parsed = TripPromptParser.parse("from here to central park")
        assertEquals(TripAction.ROUTE, parsed.action)
        assertNull(parsed.origin) // "here" should be normalized to null
        assertEquals("central park", parsed.destination)
    }

    @Test
    fun testMultiStepPrompt() {
        val parsed = TripPromptParser.parse("from here to london then take me to paris")
        assertEquals(TripAction.ROUTE, parsed.action)
        assertNull(parsed.origin)
        assertEquals("london", parsed.destination)
        assertEquals("paris", parsed.multiStepNextPrompt)
    }

    @Test fun testBudgetedPhotographyLoop() {
        val parsed = TripPromptParser.parse("Find three photography Drops on a 30-minute walk and bring me back here")
        assertEquals("photography Drops", parsed.query)
        assertEquals(3, parsed.stopCount)
        assertEquals(30, parsed.timeBudgetMinutes)
        assertEquals(TravelMode.WALKING, parsed.travelMode)
        assertEquals(true, parsed.returnToStart)
    }
    @Test fun testCommuteDetourIsSeparateFromTotalTime() {
        val parsed = TripPromptParser.parse("music to India Gate with 5 minutes detour driving")
        assertEquals(5, parsed.maxDetourMinutes)
        assertNull(parsed.timeBudgetMinutes)
        assertEquals("India Gate", parsed.destination)
        assertEquals(TravelMode.DRIVING, parsed.travelMode)
    }
    @Test fun testNamedSearchCenter() {
        val parsed = TripPromptParser.parse("hidden gems near Connaught Place")
        assertEquals("Connaught Place", parsed.searchCenter)
        assertEquals("hidden gems", parsed.query)
        assertEquals(TripAction.RADIUS_SEARCH, parsed.action)
    }
    @Test fun testQuotedExclusionsAndNegativeCoordinates() {
        val parsed = TripPromptParser.parse("photography from -28.6,77.2 to -28.61,77.21 without \"street art\"")
        assertEquals("-28.6,77.2", parsed.origin)
        assertEquals(listOf("street art"), parsed.exclusions)
    }
    @Test fun testMultipleWaypoints() {
        val parsed = TripPromptParser.parse("to Delhi via Agra and Jaipur")
        assertEquals(listOf("Agra", "Jaipur"), parsed.waypoints)
        assertEquals("Delhi", parsed.destination)
    }

    @Test fun testQuotedPlaceKeepsTravelWords() {
        val parsed = TripPromptParser.parse("to \"Walk of Fame\" cycling")
        assertEquals("Walk of Fame",parsed.destination)
        assertEquals(TravelMode.CYCLING,parsed.travelMode)
    }
    @Test fun testTypoStillClassifiesAsDiscovery() {
        val parsed = TripPromptParser.parse("histroy")
        assertEquals(TripAction.RADIUS_SEARCH,parsed.action)
        assertEquals("histroy",parsed.query)
    }
}
