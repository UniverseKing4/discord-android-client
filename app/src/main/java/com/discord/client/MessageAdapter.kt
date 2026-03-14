package com.discord.client

import android.app.AlertDialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class ScheduledMessageAdapter(
    private val onDelete: (String) -> Unit,
    private val onUpdate: () -> Unit,
    private val isPaused: () -> Boolean,
    private val getStorage: () -> Storage
) : RecyclerView.Adapter<ScheduledMessageAdapter.ViewHolder>() {
    private var messages = listOf<ScheduledMessage>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
        val channelText: TextView = view.findViewById(R.id.channelText)
        val timeText: TextView = view.findViewById(R.id.timeText)
        val deleteBtn: Button = view.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scheduled, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        holder.messageText.text = msg.message
        holder.channelText.text = "Channel: ${msg.channelId}"
        holder.deleteBtn.setBackgroundColor(Color.parseColor("#FF5555"))
        holder.deleteBtn.setTextColor(Color.WHITE)
        
        updateTimeDisplay(holder, msg)
        
        holder.deleteBtn.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this scheduled message?")
                .setPositiveButton("Delete") { _, _ ->
                    onDelete(msg.id)
                    onUpdate()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount() = messages.size

    fun setMessages(newMessages: List<ScheduledMessage>) {
        val oldMessages = messages
        messages = newMessages
        
        if (oldMessages.size != newMessages.size) {
            notifyDataSetChanged()
        } else {
            for (i in newMessages.indices) {
                if (oldMessages.getOrNull(i)?.isInterval != newMessages[i].isInterval ||
                    oldMessages.getOrNull(i)?.message != newMessages[i].message ||
                    oldMessages.getOrNull(i)?.channelId != newMessages[i].channelId) {
                    notifyItemChanged(i)
                }
            }
        }
        
        startRealtimeUpdates()
    }
    
    private fun updateTimeDisplay(holder: ViewHolder, msg: ScheduledMessage) {
        val storage = getStorage()
        val savedMessages = storage.getScheduled()
        val savedMsg = savedMessages.find { it.id == msg.id }
        val nextTime = savedMsg?.nextRunTime ?: msg.nextRunTime
        
        val timeRemaining = maxOf(0, (nextTime - System.currentTimeMillis()) / 1000)
        val hours = timeRemaining / 3600
        val minutes = (timeRemaining % 3600) / 60
        val seconds = timeRemaining % 60
        
        val timeStr = if (isPaused()) {
            "⏸ PAUSED"
        } else if (storage.getBackgroundEnabled()) {
            if (msg.isInterval) {
                "🔁 Every ${formatTime(msg.delaySeconds)} (Next: ${hours}h ${minutes}m ${seconds}s) [BG]"
            } else {
                "⏱ In ${hours}h ${minutes}m ${seconds}s [BG]"
            }
        } else {
            if (msg.isInterval) {
                "🔁 Every ${formatTime(msg.delaySeconds)} (Next: ${hours}h ${minutes}m ${seconds}s)"
            } else {
                "⏱ In ${hours}h ${minutes}m ${seconds}s"
            }
        }
        
        holder.timeText.text = timeStr
    }
    
    private fun startRealtimeUpdates() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                delay(1000)
                val storage = getStorage()
                val savedMessages = storage.getScheduled()
                
                messages.forEachIndexed { index, msg ->
                    val savedMsg = savedMessages.find { it.id == msg.id }
                    if (savedMsg != null) {
                        msg.nextRunTime = savedMsg.nextRunTime
                    }
                }
                
                notifyDataSetChanged()
            }
        }
    }
    
    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "${h}h ${m}m ${s}s"
    }
    
    fun cleanup() {
        updateJob?.cancel()
        scope.cancel()
    }
}
