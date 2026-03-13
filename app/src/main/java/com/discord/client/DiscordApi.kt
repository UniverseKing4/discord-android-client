package com.discord.client

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class DiscordApi {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://discord.com/api/v10"

    suspend fun getMessages(token: String, channelId: String): List<Message> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/channels/$channelId/messages?limit=50")
            .header("Authorization", token)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed: ${response.code}")
            val json = response.body?.string() ?: "[]"
            gson.fromJson(json, Array<Message>::class.java).toList()
        }
    }

    suspend fun sendMessage(token: String, channelId: String, content: String) = withContext(Dispatchers.IO) {
        val json = gson.toJson(mapOf("content" to content))
        val body = json.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("$baseUrl/channels/$channelId/messages")
            .header("Authorization", token)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed: ${response.code}")
        }
    }
}

data class Message(
    val id: String,
    val content: String,
    val author: Author
)

data class Author(
    val username: String
)
