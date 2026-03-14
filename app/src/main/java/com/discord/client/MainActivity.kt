package com.discord.client

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private lateinit var storage: Storage
    private val api = DiscordApi()
    private val scheduler = MessageScheduler(api)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        
        storage = Storage(this)
        
        tokenInput = findViewById(R.id.tokenInput)
        channelInput = findViewById(R.id.channelInput)
        messageInput = findViewById(R.id.messageInput)
        hoursInput = findViewById(R.id.hoursInput)
        minutesInput = findViewById(R.id.minutesInput)
        secondsInput = findViewById(R.id.secondsInput)
        intervalToggle = findViewById(R.id.intervalToggle)
        scheduledList = findViewById(R.id.scheduledList)
        
        loadSavedData()
        setupTextWatchers()
        
        adapter = ScheduledMessageAdapter(
            onDelete = { 
                scheduler.cancel(it)
                saveScheduled()
            },
            onUpdate = { updateList() }
        )
        scheduledList.layoutManager = LinearLayoutManager(this)
        scheduledList.adapter = adapter
        
        findViewById<Button>(R.id.addBtn).setOnClickListener { addScheduledMessage() }
        
        scheduler.setUpdateCallback { 
            updateList()
            saveScheduled()
        }
        
        restoreScheduled()
    }

    private fun loadSavedData() {
        tokenInput.setText(storage.getToken())
        channelInput.setText(storage.getChannelId())
        messageInput.setText(storage.getMessage())
        hoursInput.setText(storage.getHours())
        minutesInput.setText(storage.getMinutes())
        secondsInput.setText(storage.getSeconds())
        intervalToggle.isChecked = storage.getInterval()
    }

    private fun setupTextWatchers() {
        tokenInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { storage.saveToken(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        channelInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { storage.saveChannelId(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        messageInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { storage.saveMessage(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        hoursInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { storage.saveHours(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        minutesInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { storage.saveMinutes(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        secondsInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { storage.saveSeconds(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        intervalToggle.setOnCheckedChangeListener { _, isChecked -> storage.saveInterval(isChecked) }
    }

    private fun restoreScheduled() {
        val saved = storage.getScheduled()
        for (msg in saved) {
            scheduler.scheduleRestored(msg) { success, error ->
                scope.launch {
                    if (!success) {
                        Toast.makeText(this@MainActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        updateList()
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
        storage.saveMessage("")
        updateList()
        saveScheduled()
    }

    private fun updateList() {
        scope.launch {
            adapter.setMessages(scheduler.getScheduled())
        }
    }

    private fun saveScheduled() {
        storage.saveScheduled(scheduler.getScheduled())
    }

    override fun onDestroy() {
        super.onDestroy()
        saveScheduled()
        scope.cancel()
    }
}
