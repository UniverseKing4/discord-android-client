package com.discord.client

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var tokenInput: EditText
    private lateinit var channelInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var hoursInput: EditText
    private lateinit var minutesInput: EditText
    private lateinit var secondsInput: EditText
    private lateinit var intervalToggle: CheckBox
    private lateinit var scheduledList: RecyclerView
    private lateinit var adapter: ScheduledMessageAdapter
    private val api = DiscordApi()
    private val scheduler = MessageScheduler(api)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        
        tokenInput = findViewById(R.id.tokenInput)
        channelInput = findViewById(R.id.channelInput)
        messageInput = findViewById(R.id.messageInput)
        hoursInput = findViewById(R.id.hoursInput)
        minutesInput = findViewById(R.id.minutesInput)
        secondsInput = findViewById(R.id.secondsInput)
        intervalToggle = findViewById(R.id.intervalToggle)
        scheduledList = findViewById(R.id.scheduledList)
        
        adapter = ScheduledMessageAdapter(
            onDelete = { scheduler.cancel(it) },
            onUpdate = { updateList() }
        )
        scheduledList.layoutManager = LinearLayoutManager(this)
        scheduledList.adapter = adapter
        
        findViewById<Button>(R.id.addBtn).setOnClickListener { addScheduledMessage() }
        
        scheduler.setUpdateCallback { updateList() }
    }

    private fun addScheduledMessage() {
        val token = tokenInput.text.toString()
        val channelId = channelInput.text.toString()
        val message = messageInput.text.toString()
        val hours = hoursInput.text.toString().toLongOrNull() ?: 0
        val minutes = minutesInput.text.toString().toLongOrNull() ?: 0
        val seconds = secondsInput.text.toString().toLongOrNull() ?: 0
        val isInterval = intervalToggle.isChecked
        
        if (token.isEmpty() || channelId.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        val totalSeconds = hours * 3600 + minutes * 60 + seconds
        if (totalSeconds <= 0) {
            Toast.makeText(this, "Set a valid time", Toast.LENGTH_SHORT).show()
            return
        }
        
        scheduler.schedule(token, channelId, message, totalSeconds, isInterval) { success, error ->
            scope.launch {
                if (!success) {
                    Toast.makeText(this@MainActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        messageInput.setText("")
        hoursInput.setText("")
        minutesInput.setText("")
        secondsInput.setText("")
        intervalToggle.isChecked = false
        updateList()
    }

    private fun updateList() {
        scope.launch {
            adapter.setMessages(scheduler.getScheduled())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduler.cancelAll()
        scope.cancel()
    }
}
