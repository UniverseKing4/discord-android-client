package com.discord.client

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var tokenInput: EditText
    private lateinit var channelInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var messagesView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private val api = DiscordApi()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        tokenInput = findViewById(R.id.tokenInput)
        channelInput = findViewById(R.id.channelInput)
        messageInput = findViewById(R.id.messageInput)
        messagesView = findViewById(R.id.messagesView)
        
        adapter = MessageAdapter()
        messagesView.layoutManager = LinearLayoutManager(this)
        messagesView.adapter = adapter
        
        findViewById<Button>(R.id.loadBtn).setOnClickListener { loadMessages() }
        findViewById<Button>(R.id.sendBtn).setOnClickListener { sendMessage() }
    }

    private fun loadMessages() {
        val token = tokenInput.text.toString()
        val channelId = channelInput.text.toString()
        if (token.isEmpty() || channelId.isEmpty()) return
        
        scope.launch {
            try {
                val messages = api.getMessages(token, channelId)
                adapter.setMessages(messages)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage() {
        val token = tokenInput.text.toString()
        val channelId = channelInput.text.toString()
        val content = messageInput.text.toString()
        if (token.isEmpty() || channelId.isEmpty() || content.isEmpty()) return
        
        scope.launch {
            try {
                api.sendMessage(token, channelId, content)
                messageInput.setText("")
                loadMessages()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
