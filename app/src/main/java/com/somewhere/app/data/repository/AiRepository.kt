package com.somewhere.app.data.repository

import com.somewhere.app.BuildConfig
import com.somewhere.app.data.model.Drop
import com.somewhere.app.data.remote.OpenRouterApiService
import com.somewhere.app.data.remote.OpenRouterMessage
import com.somewhere.app.data.remote.OpenRouterRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val api: OpenRouterApiService
) {
    private val modelName = "tencent/hy3:free"
    private val authHeader = "Bearer ${BuildConfig.OPENROUTER_API_KEY}"

    suspend fun getPlaceSummary(drops: List<Drop>): String {
        if (drops.isEmpty()) return "There's nothing here yet."
        val textDrops = drops.filter { it.text.isNotBlank() }
        if (textDrops.isEmpty()) return "There's no text here to summarize."
        
        val prompt = "Summarize the following places based on these descriptions. Keep it brief and engaging:\n" +
                textDrops.joinToString("\n") { "- ${it.id}: ${it.text}" }

        return try {
            val request = OpenRouterRequest(
                model = modelName,
                messages = listOf(OpenRouterMessage(role = "user", content = prompt))
            )
            val response = api.createChatCompletion(authHeader, request)
            response.choices.firstOrNull()?.message?.content ?: "Hmm, I'm having trouble thinking. Enjoy the silence for now."
        } catch (e: Exception) {
            e.printStackTrace()
            "Hmm, I'm having trouble thinking. Enjoy the silence for now."
        }
    }

    suspend fun getCuratorDrop(drops: List<Drop>): Pair<String, String>? {
        if (drops.isEmpty()) return null
        val textDrops = drops.filter { it.text.isNotBlank() }
        if (textDrops.isEmpty()) return null
        
        val prompt = "You are a local guide called 'The Whisperer'. Pick exactly ONE of the following places that is the most interesting. Write a short intro (1 sentence) and then return the selected place's text exactly. Format your response exactly like this: INTRO: [your intro]\nDROP: [the exact text of the drop]\n\nPlaces:\n" +
                textDrops.joinToString("\n") { "- ${it.id}: ${it.text}" }

        return try {
            val request = OpenRouterRequest(
                model = modelName,
                messages = listOf(OpenRouterMessage(role = "user", content = prompt))
            )
            val response = api.createChatCompletion(authHeader, request)
            val content = response.choices.firstOrNull()?.message?.content ?: return null
            
            val introRegex = Regex("(?i)INTRO:\\s*(.*?)\\n")
            val dropRegex = Regex("(?i)DROP:\\s*(.*)", RegexOption.DOT_MATCHES_ALL)
            
            val introMatch = introRegex.find(content)
            val dropMatch = dropRegex.find(content)
            
            val intro = introMatch?.groupValues?.get(1)?.trim() ?: "I found a hidden gem."
            var dropText = textDrops.first().text
            if (dropMatch != null) {
                dropText = dropMatch.groupValues[1].trim()
            }
            Pair(intro, dropText)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
