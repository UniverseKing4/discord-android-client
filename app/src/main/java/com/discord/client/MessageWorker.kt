package com.discord.client

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

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
            
            if (storage.getNotificationsEnabled()) {
                showNotification(channelId, message, true)
            }
            
            if (isInterval && storage.getBackgroundEnabled() && storage.getMasterEnabled()) {
                val messages = storage.getScheduled().toMutableList()
                val msgIndex = messages.indexOfFirst { it.id == messageId }
                if (msgIndex >= 0) {
                    messages[msgIndex].nextRunTime = System.currentTimeMillis() + (delaySeconds * 1000)
                    storage.saveScheduled(messages)
                }
                scheduleNext(token, channelId, message, delaySeconds, messageId)
            } else {
                storage.removeScheduledMessage(messageId)
            }
            
            Result.success()
        } catch (e: Exception) {
            if (storage.getNotificationsEnabled()) {
                showNotification(channelId, message, false)
            }
            
            if (isInterval && storage.getBackgroundEnabled() && storage.getMasterEnabled()) {
                val messages = storage.getScheduled().toMutableList()
                val msgIndex = messages.indexOfFirst { it.id == messageId }
                if (msgIndex >= 0) {
                    messages[msgIndex].nextRunTime = System.currentTimeMillis() + (delaySeconds * 1000)
                    storage.saveScheduled(messages)
                }
                scheduleNext(token, channelId, message, delaySeconds, messageId)
            } else {
                storage.removeScheduledMessage(messageId)
            }
            Result.failure()
        }
    }

    private fun showNotification(channelId: String, message: String, success: Boolean) {
        val channelName = "Message Notifications"
        val channel = NotificationChannel(
            "message_channel",
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        
        val title = if (success) "✓ Message Sent" else "✗ Message Failed"
        val text = "Channel: $channelId\n${message.take(50)}${if (message.length > 50) "..." else ""}"
        
        val notification = NotificationCompat.Builder(applicationContext, "message_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(applicationContext).notify(System.currentTimeMillis().toInt(), notification)
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
