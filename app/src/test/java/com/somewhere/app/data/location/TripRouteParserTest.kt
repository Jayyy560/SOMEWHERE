package com.somewhere.app.data.location

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class TripRouteParserTest {
    @Test fun legacyRouteSumsEveryLegAndUsesFinalDestination() {
        val response = Json.parseToJsonElement("""{"status":"OK","routes":[{"overview_polyline":{"points":"_p~iF~ps|U_ulLnnqC_mqNvxq`@"},"legs":[
            {"duration":{"value":100},"distance":{"value":1000},"start_location":{"lat":38.5,"lng":-120.2},"end_location":{"lat":40.7,"lng":-120.95},"start_address":"Origin","end_address":"Waypoint"},
            {"duration":{"value":200},"distance":{"value":2000},"start_location":{"lat":40.7,"lng":-120.95},"end_location":{"lat":43.252,"lng":-126.453},"start_address":"Waypoint","end_address":"Final"}]}]}""").jsonObject
        val route = TripRouteParser.legacy(response)
        assertEquals(300L,route.durationSeconds); assertEquals(3000L,route.distanceMeters)
        assertEquals("Final",route.endAddress); assertEquals(43.252,route.end.latitude,0.00001)
    }
    @Test fun routesApiUsesWholeRouteTotalsAndPreservesWarnings() {
        val response = Json.parseToJsonElement("""{"routes":[{"duration":"300s","distanceMeters":3000,"polyline":{"encodedPolyline":"_p~iF~ps|U_ulLnnqC_mqNvxq`@"},"warnings":["Check pedestrian access"],"legs":[
            {"startLocation":{"latLng":{"latitude":38.5,"longitude":-120.2}},"endLocation":{"latLng":{"latitude":40.7,"longitude":-120.95}}},
            {"startLocation":{"latLng":{"latitude":40.7,"longitude":-120.95}},"endLocation":{"latLng":{"latitude":43.252,"longitude":-126.453}}}]}]}""").jsonObject
        val route = TripRouteParser.routes(response)
        assertEquals(300L,route.durationSeconds); assertEquals(3000L,route.distanceMeters)
        assertEquals("Check pedestrian access",route.warnings.single()); assertEquals(43.252,route.end.latitude,0.00001)
    }
    @Test fun noRouteIsDistinctFromServiceFailure() {
        assertTrue(runCatching { TripRouteParser.legacy(Json.parseToJsonElement("""{"status":"ZERO_RESULTS"}""").jsonObject) }.exceptionOrNull() is NoTripRouteException)
    }
    @Test fun malformedGeometryIsRejected() { assertTrue(runCatching { TripRouteParser.decode("~~~~") }.isFailure) }
}
