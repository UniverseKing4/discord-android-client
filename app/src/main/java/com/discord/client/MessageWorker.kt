package com.discord.client

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MessageWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val storage = Storage(applicationContext)
        
        if (!storage.getBackgroundEnabled() || !storage.getMasterEnabled()) {
            return@withContext Result.success()
        }
        
        val token = inputData.getString("token") ?: return@withContext Result.failure()
        val channelId = inputData.getString("channelId") ?: return@withContext Result.failure()
        val message = inputData.getString("message") ?: return@withContext Result.failure()
        val isInterval = inputData.getBoolean("isInterval", false)
        val delaySeconds = inputData.getLong("delaySeconds", 0)
        val messageId = inputData.getString("messageId") ?: return@withContext Result.failure()

        try {
            DiscordApi().sendMessage(token, channelId, message)
            
            if (isInterval && storage.getBackgroundEnabled() && storage.getMasterEnabled()) {
                scheduleNext(token, channelId, message, delaySeconds, messageId)
            } else {
                storage.removeScheduledMessage(messageId)
            }
            
            Result.success()
        } catch (e: Exception) {
            if (isInterval && storage.getBackgroundEnabled() && storage.getMasterEnabled()) {
                scheduleNext(token, channelId, message, delaySeconds, messageId)
            } else {
                storage.removeScheduledMessage(messageId)
            }
            Result.failure()
        }
    }

    private fun scheduleNext(token: String, channelId: String, message: String, delaySeconds: Long, messageId: String) {
        val storage = Storage(applicationContext)
        if (!storage.getBackgroundEnabled() || !storage.getMasterEnabled()) return
        
        val data = Data.Builder()
            .putString("token", token)
            .putString("channelId", channelId)
            .putString("message", message)
            .putBoolean("isInterval", true)
            .putLong("delaySeconds", delaySeconds)
            .putString("messageId", messageId)
            .build()

        val work = OneTimeWorkRequestBuilder<MessageWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setInputData(data)
            .addTag(messageId)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            messageId,
            ExistingWorkPolicy.REPLACE,
            work
        )
    }
}
