package com.somewhere.app.data.location

import kotlinx.serialization.json.*
import kotlin.math.roundToLong

/** Kept separate from transport so multi-leg and empty-route responses can be tested on the JVM. */
object TripRouteParser {
    fun routes(json: JsonObject): TripRoute {
        val route = json["routes"]?.jsonArray?.firstOrNull()?.jsonObject ?: throw NoTripRouteException()
        val legs = route.getValue("legs").jsonArray
        val start = legs.first().jsonObject.getValue("startLocation").jsonObject.getValue("latLng").jsonObject
        val end = legs.last().jsonObject.getValue("endLocation").jsonObject.getValue("latLng").jsonObject
        return TripRoute(decode(route.getValue("polyline").jsonObject.getValue("encodedPolyline").jsonPrimitive.content),
            route.getValue("duration").jsonPrimitive.content.removeSuffix("s").toDouble().roundToLong(),
            route.getValue("distanceMeters").jsonPrimitive.long,
            TripPoint(start.getValue("latitude").jsonPrimitive.double, start.getValue("longitude").jsonPrimitive.double),
            TripPoint(end.getValue("latitude").jsonPrimitive.double, end.getValue("longitude").jsonPrimitive.double),
            summary = route["description"]?.jsonPrimitive?.content.orEmpty(),
            warnings = route["warnings"]?.jsonArray.orEmpty().map { it.jsonPrimitive.content })
    }
    fun legacy(json: JsonObject): TripRoute {
        val status = json["status"]?.jsonPrimitive?.content
        if (status == "ZERO_RESULTS" || status == "NOT_FOUND") throw NoTripRouteException()
        check(status == "OK") { "The maps service could not build this trip (${status.orEmpty()})." }
        val route = json.getValue("routes").jsonArray.first().jsonObject
        val legs = route.getValue("legs").jsonArray.map { it.jsonObject }
        val start = legs.first().getValue("start_location").jsonObject
        val end = legs.last().getValue("end_location").jsonObject
        return TripRoute(decode(route.getValue("overview_polyline").jsonObject.getValue("points").jsonPrimitive.content),
            legs.sumOf { it.getValue("duration").jsonObject.getValue("value").jsonPrimitive.long },
            legs.sumOf { it.getValue("distance").jsonObject.getValue("value").jsonPrimitive.long },
            TripPoint(start.getValue("lat").jsonPrimitive.double, start.getValue("lng").jsonPrimitive.double),
            TripPoint(end.getValue("lat").jsonPrimitive.double, end.getValue("lng").jsonPrimitive.double),
            legs.first()["start_address"]?.jsonPrimitive?.content.orEmpty(), legs.last()["end_address"]?.jsonPrimitive?.content.orEmpty(),
            route["summary"]?.jsonPrimitive?.content.orEmpty(), route["warnings"]?.jsonArray.orEmpty().map { it.jsonPrimitive.content })
    }
    fun decode(encoded: String): List<TripPoint> {
        val result = mutableListOf<TripPoint>(); var index = 0; var latitude = 0; var longitude = 0
        fun delta(): Int {
            var value = 0; var shift = 0
            do {
                require(index < encoded.length && shift <= 30) { "Invalid route geometry." }
                val b = encoded[index++].code - 63
                require(b in 0..63) { "Invalid route geometry." }
                value = value or ((b and 0x1f) shl shift); shift += 5
            } while (b >= 0x20)
            return if (value and 1 != 0) (value shr 1).inv() else value shr 1
        }
        while (index < encoded.length) { latitude += delta(); longitude += delta(); result += TripPoint(latitude / 1e5, longitude / 1e5) }
        require(result.size >= 2) { "The route has no usable geometry." }
        return result
    }
}
