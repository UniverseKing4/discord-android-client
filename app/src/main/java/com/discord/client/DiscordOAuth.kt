package com.discord.client

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class DiscordOAuth(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("discord_oauth", Context.MODE_PRIVATE)
    private val client = OkHttpClient()
    private val gson = Gson()
    
    companion object {
        const val CLIENT_ID = "1470848672626376911"
        const val CLIENT_SECRET = "up5jnVrA_hK741leMOwAJR4HIyDbUOeI"
        const val REDIRECT_URI = "discord://oauth2/callback"
        const val AUTH_URL = "https://discord.com/api/oauth2/authorize"
        const val TOKEN_URL = "https://discord.com/api/oauth2/token"
    }
    
    fun getAuthUrl(): String {
        val state = generateState()
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        
        prefs.edit()
            .putString("state", state)
            .putString("code_verifier", codeVerifier)
            .apply()
        
        return "$AUTH_URL?" +
                "client_id=$CLIENT_ID&" +
                "redirect_uri=$REDIRECT_URI&" +
                "response_type=code&" +
                "scope=identify%20guilds%20messages.read&" +
                "state=$state&" +
                "code_challenge=$codeChallenge&" +
                "code_challenge_method=S256"
    }
    
    suspend fun exchangeCode(code: String, state: String): Boolean = withContext(Dispatchers.IO) {
        val savedState = prefs.getString("state", null)
        if (state != savedState) return@withContext false
        
        val codeVerifier = prefs.getString("code_verifier", null) ?: return@withContext false
        
        val formBody = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("code_verifier", codeVerifier)
            .build()
        
        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                
                val json = response.body?.string() ?: return@withContext false
                val tokenResponse = gson.fromJson(json, TokenResponse::class.java)
                
                prefs.edit()
                    .putString("access_token", tokenResponse.access_token)
                    .putString("refresh_token", tokenResponse.refresh_token)
                    .putLong("expires_at", System.currentTimeMillis() + (tokenResponse.expires_in * 1000))
                    .apply()
                
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun refreshToken(): Boolean = withContext(Dispatchers.IO) {
        val refreshToken = prefs.getString("refresh_token", null) ?: return@withContext false
        
        val formBody = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()
        
        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                
                val json = response.body?.string() ?: return@withContext false
                val tokenResponse = gson.fromJson(json, TokenResponse::class.java)
                
                prefs.edit()
                    .putString("access_token", tokenResponse.access_token)
                    .putString("refresh_token", tokenResponse.refresh_token)
                    .putLong("expires_at", System.currentTimeMillis() + (tokenResponse.expires_in * 1000))
                    .apply()
                
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun getAccessToken(): String? {
        val expiresAt = prefs.getLong("expires_at", 0)
        if (System.currentTimeMillis() >= expiresAt) return null
        return prefs.getString("access_token", null)
    }
    
    fun isLoggedIn(): Boolean = getAccessToken() != null
    
    fun logout() {
        prefs.edit().clear().apply()
    }
    
    private fun generateState(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    
    private fun generateCodeVerifier(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    
    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}

data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Long,
    val token_type: String
)
