package com.discord.client

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Storage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveToken(token: String) = prefs.edit().putString("token", token).apply()
    fun getToken(): String = prefs.getString("token", "") ?: ""

    fun saveChannelId(channelId: String) = prefs.edit().putString("channel_id", channelId).apply()
    fun getChannelId(): String = prefs.getString("channel_id", "") ?: ""

    fun saveMessage(message: String) = prefs.edit().putString("message", message).apply()
    fun getMessage(): String = prefs.getString("message", "") ?: ""

    fun saveHours(hours: String) = prefs.edit().putString("hours", hours).apply()
    fun getHours(): String = prefs.getString("hours", "0") ?: "0"

    fun saveMinutes(minutes: String) = prefs.edit().putString("minutes", minutes).apply()
    fun getMinutes(): String = prefs.getString("minutes", "0") ?: "0"

    fun saveSeconds(seconds: String) = prefs.edit().putString("seconds", seconds).apply()
    fun getSeconds(): String = prefs.getString("seconds", "0") ?: "0"

    fun saveInterval(isInterval: Boolean) = prefs.edit().putBoolean("interval", isInterval).apply()
    fun getInterval(): Boolean = prefs.getBoolean("interval", false)

    fun saveScheduled(messages: List<ScheduledMessage>) {
        val json = gson.toJson(messages)
        prefs.edit().putString("scheduled", json).apply()
    }

    fun getScheduled(): List<ScheduledMessage> {
        val json = prefs.getString("scheduled", null) ?: return emptyList()
        val type = object : TypeToken<List<ScheduledMessage>>() {}.type
        return gson.fromJson(json, type)
    }
}
