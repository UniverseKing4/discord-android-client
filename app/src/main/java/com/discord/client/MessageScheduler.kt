package com.discord.client

import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import androidx.work.*
import android.content.Context
import java.util.concurrent.TimeUnit

class MessageScheduler(private val api: DiscordApi, private val context: Context) {
    private val scheduled = ConcurrentHashMap<String, ScheduledMessage>()
    private val jobs = ConcurrentHashMap<String, Job>()
    private val pausedTimes = ConcurrentHashMap<String, Long>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var updateCallback: (() -> Unit)? = null
    private val storage = Storage(context)

    fun schedule(
        token: String,
        channelId: String,
        message: String,
        delaySeconds: Long,
        isInterval: Boolean,
        onResult: (Boolean, String?) -> Unit
    ) {
        val id = UUID.randomUUID().toString()
        val scheduledMsg = ScheduledMessage(
            id = id,
            token = token,
            channelId = channelId,
            message = message,
            delaySeconds = delaySeconds,
            isInterval = isInterval,
            nextRunTime = System.currentTimeMillis() + (delaySeconds * 1000),
            isActive = true
        )
        
        scheduled[id] = scheduledMsg
        
        if (storage.getMasterEnabled()) {
            if (storage.getBackgroundEnabled()) {
                scheduleWithWorkManager(scheduledMsg)
            } else {
                startSchedule(id, onResult)
            }
        }
        
        updateCallback?.invoke()
    }

    fun scheduleRestored(msg: ScheduledMessage, onResult: (Boolean, String?) -> Unit) {
        scheduled[msg.id] = msg
        
        if (storage.getMasterEnabled()) {
            if (storage.getBackgroundEnabled()) {
                scheduleWithWorkManager(msg)
            } else {
                startSchedule(msg.id, onResult)
            }
        }
    }

    private fun scheduleWithWorkManager(msg: ScheduledMessage) {
        if (!storage.getMasterEnabled()) return
        
        jobs[msg.id]?.cancel()
        jobs.remove(msg.id)
        
        val delay = maxOf(1, (msg.nextRunTime - System.currentTimeMillis()) / 1000)

        val data = Data.Builder()
            .putString("token", msg.token)
            .putString("channelId", msg.channelId)
            .putString("message", msg.message)
            .putBoolean("isInterval", msg.isInterval)
            .putLong("delaySeconds", msg.delaySeconds)
            .putString("messageId", msg.id)
            .build()

        val work = OneTimeWorkRequestBuilder<MessageWorker>()
            .setInitialDelay(delay, TimeUnit.SECONDS)
            .setInputData(data)
            .addTag(msg.id)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            msg.id,
            ExistingWorkPolicy.REPLACE,
            work
        )
    }

    private fun startSchedule(id: String, onResult: (Boolean, String?) -> Unit) {
        jobs[id]?.cancel()
        
        val job = scope.launch {
            while (scheduled.containsKey(id) && scheduled[id]?.isActive == true) {
                val msg = scheduled[id] ?: break
                val waitTime = msg.nextRunTime - System.currentTimeMillis()
                
                if (waitTime > 0) {
                    delay(waitTime)
                }
                
                if (!storage.getBackgroundEnabled() && storage.getMasterEnabled()) {
                    try {
                        api.sendMessage(msg.token, msg.channelId, msg.message)
                        
                        if (msg.isInterval) {
                            scheduled[id]?.nextRunTime = System.currentTimeMillis() + (msg.delaySeconds * 1000)
                            withContext(Dispatchers.Main) { updateCallback?.invoke() }
                        } else {
                            scheduled.remove(id)
                            jobs.remove(id)
                            withContext(Dispatchers.Main) { updateCallback?.invoke() }
                            break
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            onResult(false, e.message)
                        }
                        if (!msg.isInterval) {
                            scheduled.remove(id)
                            jobs.remove(id)
                            withContext(Dispatchers.Main) { updateCallback?.invoke() }
                            break
                        }
                    }
                } else {
                    break
                }
            }
        }
        
        jobs[id] = job
    }

    fun cancel(id: String) {
        scheduled.remove(id)
        jobs[id]?.cancel()
        jobs.remove(id)
        pausedTimes.remove(id)
        WorkManager.getInstance(context).cancelUniqueWork(id)
        updateCallback?.invoke()
    }

    fun cancelAll() {
        scheduled.keys.forEach { 
            jobs[it]?.cancel()
            WorkManager.getInstance(context).cancelUniqueWork(it) 
        }
        scheduled.clear()
        jobs.clear()
        pausedTimes.clear()
        scope.cancel()
    }

    fun pauseAll() {
        val currentTime = System.currentTimeMillis()
        scheduled.keys.forEach { id ->
            val msg = scheduled[id]
            if (msg != null) {
                pausedTimes[id] = msg.nextRunTime - currentTime
            }
            jobs[id]?.cancel()
            jobs.remove(id)
            WorkManager.getInstance(context).cancelUniqueWork(id)
        }
        updateCallback?.invoke()
    }

    fun resumeAll(onResult: (Boolean, String?) -> Unit) {
        val currentTime = System.currentTimeMillis()
        scheduled.values.forEach { msg ->
            val remainingTime = pausedTimes[msg.id]
            if (remainingTime != null && remainingTime > 0) {
                msg.nextRunTime = currentTime + remainingTime
            }
        }
        pausedTimes.clear()
        
        if (storage.getBackgroundEnabled()) {
            scheduled.values.forEach { scheduleWithWorkManager(it) }
        } else {
            scheduled.values.forEach { startSchedule(it.id, onResult) }
        }
        updateCallback?.invoke()
    }

    fun getScheduled(): List<ScheduledMessage> {
        return scheduled.values.toList().sortedBy { it.nextRunTime }
    }

    fun setUpdateCallback(callback: () -> Unit) {
        updateCallback = callback
    }

    fun switchToBackground() {
        if (!storage.getMasterEnabled()) return
        scheduled.keys.forEach { id ->
            jobs[id]?.cancel()
            jobs.remove(id)
        }
        scheduled.values.forEach { scheduleWithWorkManager(it) }
    }

    fun switchToForeground(onResult: (Boolean, String?) -> Unit) {
        if (!storage.getMasterEnabled()) return
        scheduled.keys.forEach { WorkManager.getInstance(context).cancelUniqueWork(it) }
        scheduled.values.forEach { startSchedule(it.id, onResult) }
    }
}

data class ScheduledMessage(
    val id: String,
    val token: String,
    val channelId: String,
    val message: String,
    val delaySeconds: Long,
    val isInterval: Boolean,
    var nextRunTime: Long,
    var isActive: Boolean
)
