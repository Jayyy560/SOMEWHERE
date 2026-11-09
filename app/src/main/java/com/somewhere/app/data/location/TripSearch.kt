package com.somewhere.app.data.location

import com.somewhere.app.data.remote.NearbyDrop
import java.text.Normalizer
import java.time.Instant
import kotlin.math.exp

/** Deterministic matching shared by discovery, surprise selection, and rerouting. */
object TripSearch {
    private val synonymGroups = listOf(
        setOf("food", "restaurant", "restaurants", "eat", "eating", "dining"),
        setOf("cafe", "cafes", "coffee", "espresso"),
        setOf("photography", "photograph", "photographer"),
        setOf("music", "musical", "concert", "concerts", "gig", "gigs"),
        setOf("history", "historic", "historical", "heritage"),
        setOf("story", "stories", "storytelling"),
        setOf("memory", "memories"),
        setOf("event", "events"),
        setOf("recommendation", "recommendations", "recommended"),
        setOf("hidden", "secret", "secrets", "gem", "gems")
    )
    private val stopWords = setOf("all", "everything", "anything", "drop", "drops", "spot", "spots", "nearby", "around", "the", "of", "in", "and", "with", "only", "notes", "note", "own", "posts")
    private val photoWords = setOf("photo", "photos", "image", "images", "picture", "pictures", "visuals")
    private val audioWords = setOf("audio", "sound", "sounds", "voice")
    private val textWords = setOf("text", "written")
    private val recentWords = setOf("recent", "latest", "new", "newest")
    private val anonymousWords = setOf("anonymous")

    data class Query(
        val groups: List<List<String>>,
        val media: Set<TripMedia>,
        val authorName: String?,
        val mine: Boolean,
        val anonymous: Boolean,
        val recent: Boolean,
        val hashtags: Set<String>
    )
    data class Weights(val relevance: Double = .45, val detour: Double = .25, val novelty: Double = .20, val freshness: Double = .10)

    fun normalize(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
    fun tokens(value: String): List<String> = Regex("[\\p{L}\\p{N}_]+").findAll(normalize(value)).map { it.value }.toList()
    fun parse(query: String): Query {
        val normalized = normalize(query)
        val author = Regex("\\bby\\s+@([\\p{L}\\p{N}_]+)").find(normalized)
        val tags = Regex("#([\\p{L}\\p{N}_]+)").findAll(normalized).map { it.groupValues[1] }.toSet()
        val cleaned = normalized.replace(Regex("\\bby\\s+@[\\p{L}\\p{N}_]+|#[\\p{L}\\p{N}_]+"), " ")
        val words = tokens(cleaned)
        val media = buildSet {
            if (words.any { it in photoWords }) add(TripMedia.PHOTOS)
            if (words.any { it in audioWords }) add(TripMedia.AUDIO)
            if (words.any { it in textWords }) add(TripMedia.TEXT)
        }
        val ignored = stopWords + photoWords + audioWords + textWords + recentWords + anonymousWords + setOf("my", "mine", "by", "unvisited")
        val terms = words.filterNot { it in ignored }.distinct()
        return Query(terms.map { word -> (synonymGroups.firstOrNull { word in it } ?: synonymGroups.firstOrNull { group -> group.any { fuzzy(word,it) } } ?: setOf(word)).toList() },
            media, author?.groupValues?.get(1), words.any { it == "my" || it == "mine" },
            words.any { it in anonymousWords }, words.any { it in recentWords }, tags)
    }

    fun matches(drop: NearbyDrop, request: TripRequest, visited: Set<String>, currentUserId: String?, nowMillis: Long): Boolean {
        if (drop.carriedByUserId != null) return false
        if (drop.expiresAt != null && (timestamp(drop.expiresAt) ?: 0L) <= nowMillis) return false
        if (request.options.unvisitedOnly && drop.id in visited) return false
        if (request.options.stepFreeOnly && drop.stepFreeAccess != true) return false
        if (request.options.category != null && !drop.category.equals(request.options.category, true)) return false
        val q = parse(request.query)
        val doc = tokens("${drop.text} ${drop.category.orEmpty()}")
        if (request.exclusions.any { excluded -> containsPhrase(doc, tokens(excluded)) }) return false
        if (q.mine && (currentUserId == null || drop.authorId != currentUserId)) return false
        if (q.authorName != null && !drop.authorName.equals(q.authorName, true)) return false
        if (q.anonymous && !drop.isAnonymous) return false
        val requiredMedia = q.media + request.options.media.takeUnless { it == TripMedia.ANY }.let { listOfNotNull(it) }
        if (requiredMedia.any { !hasMedia(drop, it) }) return false
        val dropTags = Regex("#([\\p{L}\\p{N}_]+)").findAll(normalize(drop.text)).map { it.groupValues[1] }.toSet()
        if (!dropTags.containsAll(q.hashtags)) return false
        return q.groups.all { alternatives -> alternatives.any { term -> doc.any { word -> word == term || fuzzy(word, term) } } }
    }

    fun rank(drop: NearbyDrop, request: TripRequest, visited: Set<String>, nowMillis: Long,
             detourSeconds: Long? = null, routeProgressMeters: Double? = null, weights: Weights = Weights()): RankedTripDrop {
        val q = parse(request.query)
        val categoryTokens = tokens(drop.category.orEmpty())
        val exactCategory = q.groups.count { group -> group.any { it in categoryTokens } }
        val relevance = if (q.groups.isEmpty()) .8 else (.7 + .3 * exactCategory / q.groups.size).coerceAtMost(1.0)
        val minutes = detourSeconds?.div(60.0)
        val proximity = if (minutes != null) 1.0 / (1.0 + minutes / 5.0)
            else 1.0 / (1.0 + TripGeometry.distance(request.origin, drop.point()) / 1000.0)
        val novelty = if (drop.id in visited) 0.0 else 1.0
        val ageDays = ((nowMillis - (timestamp(drop.createdAt) ?: 0L)).coerceAtLeast(0)) / 86400000.0
        val freshness = exp(-ageDays / 30.0)
        val freshnessWeight = if (q.recent || request.options.preferRecent) weights.freshness + .3 else weights.freshness
        val score = weights.relevance * relevance + weights.detour * proximity + weights.novelty * novelty + freshnessWeight * freshness
        val reasons = buildList {
            if (request.options.category != null) add(request.options.category)
            else if (q.groups.isNotEmpty()) add("Matches ${request.query}")
            if (novelty == 1.0) add("Unvisited")
            if (minutes != null) add(if (minutes < 1) "Under 1 min extra" else "+${kotlin.math.ceil(minutes).toInt()} min")
            if (ageDays < 7) add("Added this week")
        }
        return RankedTripDrop(drop, score, reasons, detourSeconds, routeProgressMeters)
    }
    fun timestamp(value: String?): Long? = value?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
    private fun hasMedia(drop: NearbyDrop, type: TripMedia) = when (type) {
        TripMedia.ANY -> true
        TripMedia.PHOTOS -> drop.imageUrl.isNotBlank()
        TripMedia.AUDIO -> !drop.audioUrl.isNullOrBlank()
        TripMedia.TEXT -> drop.imageUrl.isBlank() && drop.audioUrl.isNullOrBlank()
    }
    private fun containsPhrase(doc: List<String>, phrase: List<String>): Boolean = phrase.isNotEmpty() &&
        doc.windowed(phrase.size).any { it == phrase }
    /** One edit or adjacent transposition, only for longer words, avoiding art/cart substring matches. */
    fun fuzzy(a: String, b: String): Boolean {
        if (a == b) return true
        if (a.length < 4 || b.length < 4 || kotlin.math.abs(a.length - b.length) > 1) return false
        if (a.length == b.length) {
            val differences = a.indices.filter { a[it] != b[it] }
            return differences.size == 1 || (differences.size == 2 && differences[1] == differences[0] + 1 &&
                a[differences[0]] == b[differences[1]] && a[differences[1]] == b[differences[0]])
        }
        val longer = if (a.length > b.length) a else b
        val shorter = if (a.length > b.length) b else a
        var i = 0; var j = 0; var skipped = false
        while (i < longer.length && j < shorter.length) {
            if (longer[i] == shorter[j]) { i++; j++ }
            else { if (skipped) return false; skipped = true; i++ }
        }
        return true
    }
}

fun NearbyDrop.point() = TripPoint(latitude, longitude)
