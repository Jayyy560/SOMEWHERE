package com.somewhere.app.data.repository

import com.somewhere.app.data.model.Drop
import com.somewhere.app.data.remote.CuratorRequest
import com.somewhere.app.data.remote.GemmaApiService
import com.somewhere.app.data.remote.PlaceSummaryRequest
import com.somewhere.app.data.remote.SimpleDropDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val api: GemmaApiService
) {
    suspend fun getPlaceSummary(drops: List<Drop>): String {
        if (drops.isEmpty()) return "There's nothing here yet."
        
        val dtos = drops.map { SimpleDropDto(id = it.id, text = it.text) }
        return try {
            val response = api.getPlaceSummary(PlaceSummaryRequest(drops = dtos))
            response.summary
        } catch (e: Exception) {
            "Hmm, I'm having trouble thinking. Enjoy the silence for now."
        }
    }

    suspend fun getCuratorDrop(drops: List<Drop>): Pair<String, String>? {
        if (drops.isEmpty()) return null
        
        val dtos = drops.map { SimpleDropDto(id = it.id, text = it.text) }
        return try {
            val response = api.getCuratorDrop(CuratorRequest(drops = dtos))
            Pair(response.short_intro, response.selected_drop_text)
        } catch (e: Exception) {
            null
        }
    }
}
