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
}
