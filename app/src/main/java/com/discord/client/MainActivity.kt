package com.discord.client

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var masterBtn: Button
    private lateinit var tokenInput: EditText
    private lateinit var channelInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var hoursInput: EditText
    private lateinit var minutesInput: EditText
    private lateinit var secondsInput: EditText
    private lateinit var intervalToggle: CheckBox
    private lateinit var backgroundToggle: CheckBox
    private lateinit var notificationToggle: CheckBox
    private lateinit var scheduledList: RecyclerView
    private lateinit var adapter: ScheduledMessageAdapter
    private lateinit var storage: Storage
    private val api = DiscordApi()
    private lateinit var scheduler: MessageScheduler
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        
        storage = Storage(this)
        scheduler = MessageScheduler(api, this)
        
        masterBtn = findViewById(R.id.masterBtn)
        tokenInput = findViewById(R.id.tokenInput)
        channelInput = findViewById(R.id.channelInput)
        messageInput = findViewById(R.id.messageInput)
        hoursInput = findViewById(R.id.hoursInput)
        minutesInput = findViewById(R.id.minutesInput)
        secondsInput = findViewById(R.id.secondsInput)
        intervalToggle = findViewById(R.id.intervalToggle)
        backgroundToggle = findViewById(R.id.backgroundToggle)
        notificationToggle = findViewById(R.id.notificationToggle)
        scheduledList = findViewById(R.id.scheduledList)
        
        loadSavedData()
        setupTextWatchers()
        setupMasterButton()
        
        adapter = ScheduledMessageAdapter(
            onDelete = { 
                scheduler.cancel(it)
                saveScheduled()
            },
            onUpdate = { updateList() },
            isPaused = { isPaused },
            getStorage = { storage }
        )
        scheduledList.layoutManager = LinearLayoutManager(this)
        scheduledList.adapter = adapter
        
        findViewById<Button>(R.id.addBtn).setOnClickListener { addScheduledMessage() }
        
        masterBtn.setOnClickListener {
            isPaused = !isPaused
            storage.saveMasterEnabled(!isPaused)
            updateMasterButton()
            
            if (!isPaused) {
                scheduler.resumeAll { _, _ -> }
            } else {
                scheduler.pauseAll()
            }
            updateList()
        }
        
        backgroundToggle.setOnCheckedChangeListener { _, isChecked ->
            storage.saveBackgroundEnabled(isChecked)
            if (isChecked && !isPaused) {
                scheduler.switchToBackground()
            } else if (!isPaused) {
                scheduler.switchToForeground { _, _ -> }
            }
            updateList()
        }
        
        notificationToggle.setOnCheckedChangeListener { _, isChecked ->
            storage.saveNotificationsEnabled(isChecked)
        }
        
        scheduler.setUpdateCallback { 
            updateList()
            saveScheduled()
        }
        
        restoreScheduled()
    }

    private fun setupMasterButton() {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        masterBtn.background = drawable
        masterBtn.setPadding(0, 0, 0, 0)
        updateMasterButton()
    }

    private fun updateMasterButton() {
        masterBtn.post {
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            if (isPaused) {
                drawable.setColor(Color.parseColor("#4CAF50"))
                masterBtn.text = "START"
            } else {
                drawable.setColor(Color.parseColor("#FF5555"))
                masterBtn.text = "STOP"
            }
            masterBtn.background = drawable
            masterBtn.setTextColor(Color.WHITE)
            masterBtn.invalidate()
        }
    }

    private fun loadSavedData() {
        tokenInput.setText(storage.getToken())
        channelInput.setText(storage.getChannelId())
        messageInput.setText(storage.getMessage())
        hoursInput.setText(storage.getHours())
        minutesInput.setText(storage.getMinutes())
        secondsInput.setText(storage.getSeconds())
        intervalToggle.isChecked = storage.getInterval()
        backgroundToggle.isChecked = storage.getBackgroundEnabled()
        notificationToggle.isChecked = storage.getNotificationsEnabled()
        isPaused = !storage.getMasterEnabled()
        updateMasterButton()
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
                    if (!success && error != null) {
                        Toast.makeText(this@MainActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        if (isPaused) {
            scheduler.pauseAll()
        }
        
        updateList()
    }

    private fun addScheduledMessage() {
        val token = tokenInput.text.toString().trim()
        val channelId = channelInput.text.toString().trim()
        val message = messageInput.text.toString().trim()
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
                if (!success && error != null) {
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
        adapter.cleanup()
        scope.cancel()
    }
}
