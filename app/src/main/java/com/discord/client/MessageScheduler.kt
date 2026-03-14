package com.discord.client

import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class MessageScheduler(private val api: DiscordApi) {
    private val scheduled = ConcurrentHashMap<String, ScheduledMessage>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var updateCallback: (() -> Unit)? = null

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
        
        scope.launch {
            while (scheduled.containsKey(id) && scheduled[id]?.isActive == true) {
                delay(delaySeconds * 1000)
                
                try {
                    api.sendMessage(token, channelId, message)
                    
                    if (isInterval) {
                        scheduled[id]?.nextRunTime = System.currentTimeMillis() + (delaySeconds * 1000)
                        withContext(Dispatchers.Main) { updateCallback?.invoke() }
                    } else {
                        scheduled.remove(id)
                        withContext(Dispatchers.Main) { updateCallback?.invoke() }
                        break
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onResult(false, e.message)
                    }
                    if (!isInterval) {
                        scheduled.remove(id)
                        withContext(Dispatchers.Main) { updateCallback?.invoke() }
                        break
                    }
                }
            }
        }
        
        updateCallback?.invoke()
    }

    fun cancel(id: String) {
        scheduled.remove(id)
        updateCallback?.invoke()
    }

    fun cancelAll() {
        scheduled.clear()
        scope.cancel()
    }

    fun getScheduled(): List<ScheduledMessage> {
        return scheduled.values.toList().sortedBy { it.nextRunTime }
    }

    fun setUpdateCallback(callback: () -> Unit) {
        updateCallback = callback
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
