package com.discord.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ScheduledMessageAdapter(
    private val onDelete: (String) -> Unit,
    private val onUpdate: () -> Unit
) : RecyclerView.Adapter<ScheduledMessageAdapter.ViewHolder>() {
    private var messages = listOf<ScheduledMessage>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
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
        
        val timeRemaining = (msg.nextRunTime - System.currentTimeMillis()) / 1000
        val hours = timeRemaining / 3600
        val minutes = (timeRemaining % 3600) / 60
        val seconds = timeRemaining % 60
        
        val timeStr = if (msg.isInterval) {
            "Every ${formatTime(msg.delaySeconds)} (Next: ${hours}h ${minutes}m ${seconds}s)"
        } else {
            "In ${hours}h ${minutes}m ${seconds}s"
        }
        
        holder.timeText.text = timeStr
        holder.deleteBtn.setOnClickListener {
            onDelete(msg.id)
            onUpdate()
        }
    }

    override fun getItemCount() = messages.size

    fun setMessages(newMessages: List<ScheduledMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }
    
    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "${h}h ${m}m ${s}s"
    }
}
