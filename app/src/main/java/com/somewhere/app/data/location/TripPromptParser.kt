package com.somewhere.app.data.location

enum class TripAction {
    ROUTE,
    RADIUS_SEARCH,
    SURPRISE_ME
}

data class ParsedPrompt(
    val action: TripAction,
    val origin: String?,
    val destination: String?,
    val waypoint: String?,
    val radiusMeters: Double?,
    val query: String,
    val exclusions: List<String>,
    val multiStepNextPrompt: String?
)

object TripPromptParser {

    private val knownQueryWords = listOf(
        "food", "cafes", "cafe", "coffee", "restaurant", "restaurants",
        "music", "art", "photography", "photos", "photo", "stories", "story",
        "hidden", "gems", "gem", "secret", "secrets", "anonymous",
        "history", "historic", "historical", "event", "events",
        "recommendation", "recommendations", "memory", "memories",
        "audio", "voice", "text", "recent", "latest", "new", "newest",
        "all", "everything", "anything", "drops", "spots", "spot",
        "best", "worthy", "cool", "interesting", "beautiful", "popular",
        "nearby", "around", "close"
    )

    private val currentLocWords = listOf("here", "me", "current location", "my location", "around me", "near me", "close to me", "close by")

    fun parse(prompt: String): ParsedPrompt {
        var multiStepNextPrompt: String? = null
        var textToParse = prompt.trim()

        // Step 1: Multi-Step Split
        val thenRegex = Regex("(?i)\\s+then\\s+(?:take me to\\s+|to\\s+)?")
        val thenMatch = thenRegex.find(textToParse)
        if (thenMatch != null) {
            multiStepNextPrompt = textToParse.substring(thenMatch.range.last + 1).trim()
            textToParse = textToParse.substring(0, thenMatch.range.first).trim()
        }

        // Step 2: Action Detection
        var action = TripAction.ROUTE
        val surpriseMeRegex = Regex("(?i)\\b(?:surprise me|take me somewhere)\\b")
        if (surpriseMeRegex.containsMatchIn(textToParse)) {
            action = TripAction.SURPRISE_ME
            textToParse = textToParse.replace(surpriseMeRegex, " ").trim()
        }

        // Step 3: Keyword Extraction via Spans
        val spansToRemove = mutableListOf<IntRange>()
        var radiusMeters: Double? = null
        val exclusions = mutableListOf<String>()

        // 3a. Radius
        val radiusRegex = Regex("(?i)\\b(?:within|in)?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(miles|mile|kilometers|kilometer|km|meters|meter|m)\\b")
        val radMatch = radiusRegex.find(textToParse)
        if (radMatch != null) {
            spansToRemove.add(radMatch.range)
            val amount = radMatch.groupValues[1].toDoubleOrNull() ?: 5.0
            val unit = radMatch.groupValues[2].lowercase()
            radiusMeters = when {
                unit.startsWith("k") -> amount * 1000.0
                unit == "m" || unit == "meter" || unit == "meters" -> amount
                else -> amount * 1609.34
            }
        }
        
        // Handle "nearby" / "around me" radius defaults
        val nearbyRegex = Regex("(?i)\\b(nearby|around me|near me|close by|close to me|radius)\\b")
        val nearbyMatches = nearbyRegex.findAll(textToParse)
        for (match in nearbyMatches) {
            spansToRemove.add(match.range)
            if (radiusMeters == null && action != TripAction.SURPRISE_ME) {
                radiusMeters = 2000.0
            }
        }

        // 3b. Exclusions
        val excRegex = Regex("(?i)(?:without\\s+|-)([\\w]+)")
        val excMatches = excRegex.findAll(textToParse)
        for (match in excMatches) {
            spansToRemove.add(match.range)
            exclusions.add(match.groupValues[1])
        }

        // Clean up the text by removing the spans (process from back to front)
        var cleanedText = textToParse
        spansToRemove.sortedByDescending { it.first }.forEach { range ->
            // Prevent out-of-bounds just in case
            if (range.first >= 0 && range.last < cleanedText.length) {
                cleanedText = cleanedText.removeRange(range)
            }
        }
        // Condense spaces
        cleanedText = cleanedText.replace(Regex("\\s+"), " ").trim()

        // Step 4: Routing Markers
        var origin: String? = null
        var destination: String? = null
        var waypoint: String? = null
        var query: String = ""

        val fromRegex = Regex("(?i)\\bfrom\\b")
        val toRegex = Regex("(?i)\\bto\\b")
        val viaRegex = Regex("(?i)\\bvia\\b")

        val fromMatch = fromRegex.find(cleanedText)
        val toMatch = toRegex.find(cleanedText)
        val viaMatch = viaRegex.find(cleanedText)

        // Find all markers and their indices
        val markers = mutableListOf<Pair<String, Int>>()
        if (fromMatch != null) markers.add(Pair("from", fromMatch.range.last + 1))
        if (toMatch != null) markers.add(Pair("to", toMatch.range.last + 1))
        if (viaMatch != null) markers.add(Pair("via", viaMatch.range.last + 1))

        if (markers.isEmpty()) {
            query = cleanedText
        } else {
            markers.sortBy { it.second }
            // If there's text before the first marker, it's a query
            if (markers.first().second - markers.first().first.length - 1 > 0) {
                query = cleanedText.substring(0, markers.first().second - markers.first().first.length - 1).trim()
            }

            for (i in markers.indices) {
                val marker = markers[i]
                val start = marker.second
                val end = if (i + 1 < markers.size) markers[i + 1].second - markers[i + 1].first.length - 1 else cleanedText.length
                
                if (start <= end) {
                    val value = cleanedText.substring(start, end).trim()
                    if (value.isNotEmpty()) {
                        when (marker.first) {
                            "from" -> origin = value
                            "to" -> destination = value
                            "via" -> waypoint = value
                        }
                    }
                }
            }
        }

        // Step 5: Query vs Destination Fallback
        if (origin == null && destination == null && waypoint == null && action != TripAction.SURPRISE_ME) {
            if (query.isNotBlank()) {
                val words = query.lowercase().split("\\s+".toRegex())
                val isKnownQuery = words.any { knownQueryWords.contains(it) }
                if (isKnownQuery || radiusMeters != null) {
                    action = TripAction.RADIUS_SEARCH
                } else {
                    action = TripAction.ROUTE
                    destination = query
                    query = ""
                }
            }
        } else if (action != TripAction.SURPRISE_ME && (origin != null || destination != null || waypoint != null)) {
            action = TripAction.ROUTE
        }

        // Default query empty if only routing
        if (query.equals("route", ignoreCase = true) || query.equals("navigate", ignoreCase = true)) {
            query = ""
        }

        // Step 6: Normalization
        val normalize = { s: String? ->
            if (s != null && currentLocWords.contains(s.lowercase())) null else s
        }
        origin = normalize(origin)
        destination = normalize(destination)
        waypoint = normalize(waypoint)
        
        // If it's a RADIUS_SEARCH and origin was specified as "here", it's normalized to null.
        // This means we use current location. If origin is totally omitted, we also use current location.

        return ParsedPrompt(
            action = action,
            origin = origin,
            destination = destination,
            waypoint = waypoint,
            radiusMeters = radiusMeters,
            query = query,
            exclusions = exclusions,
            multiStepNextPrompt = multiStepNextPrompt
        )
    }
}
