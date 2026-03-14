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
        startSchedule(id, onResult)
        updateCallback?.invoke()
    }

    fun scheduleRestored(msg: ScheduledMessage, onResult: (Boolean, String?) -> Unit) {
        scheduled[msg.id] = msg
        startSchedule(msg.id, onResult)
    }

    private fun startSchedule(id: String, onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            while (scheduled.containsKey(id) && scheduled[id]?.isActive == true) {
                val msg = scheduled[id] ?: break
                val waitTime = msg.nextRunTime - System.currentTimeMillis()
                
                if (waitTime > 0) {
                    delay(waitTime)
                }
                
                try {
                    api.sendMessage(msg.token, msg.channelId, msg.message)
                    
                    if (msg.isInterval) {
                        scheduled[id]?.nextRunTime = System.currentTimeMillis() + (msg.delaySeconds * 1000)
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
                    if (!msg.isInterval) {
                        scheduled.remove(id)
                        withContext(Dispatchers.Main) { updateCallback?.invoke() }
                        break
                    }
                }
            }
        }
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
