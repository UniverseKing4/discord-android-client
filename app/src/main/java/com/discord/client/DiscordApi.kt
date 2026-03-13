package com.discord.client

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class DiscordApi(private val oauth: DiscordOAuth) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://discord.com/api/v10"

    suspend fun getMessages(channelId: String): List<Message> = withContext(Dispatchers.IO) {
        val token = oauth.getAccessToken() ?: throw Exception("Not authenticated")
        
        val request = Request.Builder()
            .url("$baseUrl/channels/$channelId/messages?limit=50")
            .header("Authorization", "Bearer $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) {
                if (oauth.refreshToken()) {
                    return@withContext getMessages(channelId)
                }
                throw Exception("Authentication expired")
            }
            if (!response.isSuccessful) throw Exception("Failed: ${response.code}")
            val json = response.body?.string() ?: "[]"
            gson.fromJson(json, Array<Message>::class.java).toList()
        }
    }

    suspend fun sendMessage(channelId: String, content: String) = withContext(Dispatchers.IO) {
        val token = oauth.getAccessToken() ?: throw Exception("Not authenticated")
        val json = gson.toJson(mapOf("content" to content))
        val body = json.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("$baseUrl/channels/$channelId/messages")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) {
                if (oauth.refreshToken()) {
                    return@withContext sendMessage(channelId, content)
                }
                throw Exception("Authentication expired")
            }
            if (!response.isSuccessful) throw Exception("Failed: ${response.code}")
        }
    }
    
    suspend fun getCurrentUser(): User = withContext(Dispatchers.IO) {
        val token = oauth.getAccessToken() ?: throw Exception("Not authenticated")
        
        val request = Request.Builder()
            .url("$baseUrl/users/@me")
            .header("Authorization", "Bearer $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed: ${response.code}")
            val json = response.body?.string() ?: throw Exception("Empty response")
            gson.fromJson(json, User::class.java)
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

data class User(
    val id: String,
    val username: String,
    val discriminator: String,
    val avatar: String?
)
