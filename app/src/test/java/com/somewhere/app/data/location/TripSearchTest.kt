package com.somewhere.app.data.location

import com.somewhere.app.data.remote.NearbyDrop
import org.junit.Assert.*
import org.junit.Test

class TripSearchTest {
    private val origin = TripPoint(28.6,77.2)
    private val now = 1_800_000_000_000L
    private fun request(query: String, options: TripOptions = TripOptions(), exclusions: List<String> = emptyList()) =
        TripRequest(origin,query = query,options = options,exclusions = exclusions)
    private fun drop(text: String, category: String? = null, image: String = "", audio: String? = null) =
        NearbyDrop("one",text,image,audio,origin.latitude,origin.longitude,category = category)
    private fun matches(drop: NearbyDrop, request: TripRequest, visited: Set<String> = emptySet(), userId: String? = null) =
        TripSearch.matches(drop,request,visited,userId,now)

    @Test fun matchesWordsWithoutSubstringFalsePositives() {
        assertFalse(matches(drop("A cart outside"),request("art")))
        assertTrue(matches(drop("Street art outside"),request("art")))
    }
    @Test fun matchesCategoriesWithoutCaseBug() { assertTrue(matches(drop("A beautiful place","Photography"),request("photography"))) }
    @Test fun combinesCategoryMediaAndAuthor() {
        val matching = drop("A new mural","Photography","image").copy(authorName = "sam")
        assertTrue(matches(matching,request("photography photos by @sam")))
        assertFalse(matches(matching.copy(imageUrl = ""),request("photography photos by @sam")))
        assertFalse(matches(matching.copy(authorName = "samantha"),request("photography photos by @sam")))
    }
    @Test fun handlesSynonymsAccentsAndTypos() {
        assertTrue(matches(drop("A café by the garden"),request("coffee")))
        assertTrue(matches(drop("Our historical quarter"),request("histroy")))
        assertFalse(matches(drop("A car on the road"),request("art")))
    }
    @Test fun requiresEverySearchTerm() {
        assertFalse(matches(drop("Food beside the road","Food"),request("food music")))
        assertTrue(matches(drop("Food and live music","Food"),request("food music")))
    }
    @Test fun requiresExactHashtags() {
        assertTrue(matches(drop("A place #art"),request("#art")))
        assertFalse(matches(drop("A place #artist"),request("#art")))
    }
    @Test fun appliesExclusionsBeforeRanking() {
        assertFalse(matches(drop("Food by the road","Food"),request("food",exclusions = listOf("road"))))
        assertTrue(matches(drop("A broad view","Food"),request("food",exclusions = listOf("road"))))
    }
    @Test fun excludesExpiredAndCarriedDrops() {
        assertFalse(matches(drop("Story").copy(expiresAt = "2020-01-01T00:00:00Z"),request("")))
        assertFalse(matches(drop("Story").copy(carriedByUserId = "user"),request("")))
    }
    @Test fun handlesMineAndUnvisitedWithoutInventingAccess() {
        val own = drop("Story").copy(authorId = "me")
        assertFalse(matches(own,request("my drops")))
        assertTrue(matches(own,request("my drops"),userId = "me"))
        assertFalse(matches(own,request("",TripOptions(unvisitedOnly = true)),setOf("one")))
    }
    @Test fun unknownAccessibilityDoesNotPassStepFreeFilter() {
        assertFalse(matches(drop("Story"),request("",TripOptions(stepFreeOnly = true))))
        assertTrue(matches(drop("Story").copy(stepFreeAccess = true),request("",TripOptions(stepFreeOnly = true))))
    }
    @Test fun lowerDetoursAndNewPlacesRankHigher() {
        val request = request("photography")
        val drop = drop("A mural","Photography")
        assertTrue(TripSearch.rank(drop,request,emptySet(),now,60).score > TripSearch.rank(drop,request,setOf("one"),now,600).score)
    }
}
