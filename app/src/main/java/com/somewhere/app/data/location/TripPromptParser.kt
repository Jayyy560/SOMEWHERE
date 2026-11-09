package com.somewhere.app.data.location

import kotlinx.serialization.Serializable

@Serializable
enum class TripAction { ROUTE, RADIUS_SEARCH, SURPRISE_ME }

data class ParsedPrompt(
    val action: TripAction,
    val origin: String?,
    val destination: String?,
    val waypoint: String?,
    val radiusMeters: Double?,
    val query: String,
    val exclusions: List<String>,
    val multiStepNextPrompt: String?,
    val travelMode: TravelMode? = null,
    val timeBudgetMinutes: Int? = null,
    val maxDetourMinutes: Int? = null,
    val stopCount: Int? = null,
    val returnToStart: Boolean = false,
    val unvisitedOnly: Boolean = false,
    val preferRecent: Boolean = false,
    val searchCenter: String? = null,
    val waypoints: List<String> = emptyList()
)

/** Small, explicit command grammar. No model, embedding, or remote interpretation. */
object TripPromptParser {
    private val here = setOf("here", "me", "current location", "my location", "around me", "near me")
    private val queryWords = setOf("food", "cafe", "cafes", "coffee", "restaurant", "restaurants", "music", "art",
        "photography", "photos", "photo", "pictures", "stories", "story", "hidden", "gems", "secret", "secrets",
        "anonymous", "history", "historic", "historical", "event", "events", "recommendation", "recommendations",
        "memory", "memories", "audio", "voice", "text", "recent", "latest", "new", "newest", "all", "everything",
        "anything", "drops", "spots", "spot", "unvisited", "mine", "images")
    private val numbers = mapOf("one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6)

    fun parse(prompt: String): ParsedPrompt {
        var text = prompt.trim()
        val then = Regex("(?i)\\s+then\\s+(?:take me to\\s+|to\\s+)?").find(text)
        val next = then?.let { text.substring(it.range.last + 1).trim() }
        if (then != null) text = text.substring(0, then.range.first)
        val quoted = mutableListOf<String>()
        text = Regex("\"[^\"]+\"").replace(text) { match -> quoted.add(match.value); "__quoted_${quoted.lastIndex}__" }
        fun take(pattern: String): MatchResult? {
            val match = Regex(pattern, RegexOption.IGNORE_CASE).find(text)
            if (match != null) text = text.replaceRange(match.range, " ")
            return match
        }
        val surprise = take("\\b(?:surprise me|take me somewhere)\\b") != null
        val mode = when {
            take("\\b(?:walking|walk|on foot)\\b") != null -> TravelMode.WALKING
            take("\\b(?:cycling|cycle|bicycling|bike)\\b") != null -> TravelMode.CYCLING
            take("\\b(?:driving|drive|by car)\\b") != null -> TravelMode.DRIVING
            else -> null
        }
        val loop = take("\\b(?:and\\s+)?(?:bring me back here|return to (?:the )?start|back to here|round trip|circular|loop)\\b") != null
        val unvisited = take("\\b(?:unvisited|not visited|new to me)\\b") != null
        val recent = Regex("(?i)\\b(?:recent|latest|newest)\\b").containsMatchIn(text)
        val detour = take("\\b(?:with\\s+|under\\s+|up to\\s+)?(\\d+)\\s*[- ]?\\s*(?:minutes?|mins?)\\s*(?:extra|detour)\\b")
            ?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 180)
        val time = take("\\b(?:on\\s+)?(?:a\\s+)?(?:within\\s+|under\\s+|for\\s+)?(\\d+)\\s*[- ]?\\s*(minutes?|mins?|hours?|hrs?)\\b")?.let {
            val value = it.groupValues[1].toIntOrNull() ?: 30
            (value * if (it.groupValues[2].startsWith("h", true)) 60 else 1).coerceIn(1, 480)
        }
        val count = take("\\b(?:find\\s+(?:me\\s+)?)?(\\d+|one|two|three|four|five|six)(?=\\s+(?:[\\p{L}]+\\s+){0,2}drops?\\b)")?.groupValues?.get(1)?.let {
            (it.toIntOrNull() ?: numbers[it.lowercase()])?.coerceIn(1, 8)
        }
        val radius = take("\\b(?:within|in)\\s+(\\d+(?:\\.\\d+)?)\\s*(miles?|kilometers?|km|meters?|m)\\b")?.let {
            val factor = when { it.groupValues[2].startsWith("k", true) -> 1000.0; it.groupValues[2].startsWith("mile", true) -> 1609.34; else -> 1.0 }
            (it.groupValues[1].toDouble() * factor).coerceIn(1.0, 100000.0)
        }
        val nearby = take("\\b(?:nearby|around me|near me|close by|close to me)\\b") != null
        val exclusions = mutableListOf<String>()
        val exclusionRegex = Regex("(?i)(?:\\bwithout\\s+|(?<!\\S)-(?=[\\p{L}_]))(\"[^\"]+\"|[\\p{L}\\p{N}_]+)")
        exclusionRegex.findAll(text).toList().asReversed().forEach {
            exclusions.add(0, it.groupValues[1].trim('"'))
            text = text.replaceRange(it.range, " ")
        }
        for (i in exclusions.indices) quoted.forEachIndexed { index, value -> exclusions[i] = exclusions[i].replace("__quoted_${index}__",value.trim('"')) }
        quoted.forEachIndexed { index, value -> text = text.replace("__quoted_${index}__", value) }
        text = text.replace(Regex("\\s+"), " ").trim()
        val markers = Regex("(?i)\\b(from|to|via|near)\\s+(\"[^\"]+\"|.+?)(?=\\s+\\b(?:from|to|via|near)\\b\\s+|$)").findAll(text).toList()
        var origin: String? = null; var destination: String? = null; var center: String? = null
        val waypoints = mutableListOf<String>()
        var query = if (markers.isEmpty()) text else text.substring(0, markers.first().range.first).trim()
        for (marker in markers) {
            val value = marker.groupValues[2].trim().trim('"')
            when (marker.groupValues[1].lowercase()) {
                "from" -> origin = value
                "to" -> destination = value
                "near" -> center = value
                "via" -> waypoints.addAll(value.split(Regex("\\s*;\\s*|\\s+and\\s+")).filter { it.isNotBlank() })
            }
        }
        query = query.replace(Regex("(?i)\\b(?:find|show|me|take|navigate|route|on|a|and|please)\\b"), " ")
            .replace(Regex("\\s+"), " ").trim()
        var action = when { surprise -> TripAction.SURPRISE_ME; destination != null || waypoints.isNotEmpty() -> TripAction.ROUTE; else -> TripAction.RADIUS_SEARCH }
        if (markers.isEmpty() && !surprise && radius == null && !nearby && time == null && !loop && count == null && mode == null && !unvisited) {
            val words = TripSearch.tokens(query)
            if (query.isNotBlank() && words.none { word -> word in queryWords || queryWords.any { TripSearch.fuzzy(word,it) } } && !query.contains('#') && !query.contains('@')) {
                destination = query; query = ""; action = TripAction.ROUTE
            }
        }
        fun normalize(value: String?) = value?.takeUnless { it.lowercase() in here }
        return ParsedPrompt(action, normalize(origin), normalize(destination), waypoints.firstOrNull()?.let(::normalize),
            radius ?: if (nearby) 2000.0 else null, query, exclusions, next, mode, time, detour, count, loop, unvisited, recent,
            normalize(center), waypoints.mapNotNull(::normalize))
    }
}
