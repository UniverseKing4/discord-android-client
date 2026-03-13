package com.discord.client

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var loginBtn: Button
    private lateinit var logoutBtn: Button
    private lateinit var userText: TextView
    private lateinit var channelInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var messagesView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private lateinit var oauth: DiscordOAuth
    private lateinit var api: DiscordApi
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        oauth = DiscordOAuth(this)
        api = DiscordApi(oauth)
        
        loginBtn = findViewById(R.id.loginBtn)
        logoutBtn = findViewById(R.id.logoutBtn)
        userText = findViewById(R.id.userText)
        channelInput = findViewById(R.id.channelInput)
        messageInput = findViewById(R.id.messageInput)
        messagesView = findViewById(R.id.messagesView)
        
        adapter = MessageAdapter()
        messagesView.layoutManager = LinearLayoutManager(this)
        messagesView.adapter = adapter
        
        loginBtn.setOnClickListener { startOAuth() }
        logoutBtn.setOnClickListener { logout() }
        findViewById<Button>(R.id.loadBtn).setOnClickListener { loadMessages() }
        findViewById<Button>(R.id.sendBtn).setOnClickListener { sendMessage() }
        
        updateUI()
        handleOAuthCallback(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }

    private fun startOAuth() {
        val authUrl = oauth.getAuthUrl()
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(this, Uri.parse(authUrl))
    }

    private fun handleOAuthCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "discord" || uri.host != "oauth2") return
        
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        
        if (code != null && state != null) {
            scope.launch {
                try {
                    val success = oauth.exchangeCode(code, state)
                    if (success) {
                        updateUI()
                        loadUserInfo()
                    } else {
                        Toast.makeText(this@MainActivity, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadUserInfo() {
        scope.launch {
            try {
                val user = api.getCurrentUser()
                userText.text = "Logged in as: ${user.username}"
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error loading user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        oauth.logout()
        updateUI()
    }

    private fun updateUI() {
        val loggedIn = oauth.isLoggedIn()
        loginBtn.visibility = if (loggedIn) View.GONE else View.VISIBLE
        logoutBtn.visibility = if (loggedIn) View.VISIBLE else View.GONE
        userText.visibility = if (loggedIn) View.VISIBLE else View.GONE
        channelInput.isEnabled = loggedIn
        messageInput.isEnabled = loggedIn
        findViewById<Button>(R.id.loadBtn).isEnabled = loggedIn
        findViewById<Button>(R.id.sendBtn).isEnabled = loggedIn
        
        if (loggedIn) {
            loadUserInfo()
        } else {
            userText.text = ""
        }
    }

    private fun loadMessages() {
        val channelId = channelInput.text.toString()
        if (channelId.isEmpty()) return
        
        scope.launch {
            try {
                val messages = api.getMessages(channelId)
                adapter.setMessages(messages)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage() {
        val channelId = channelInput.text.toString()
        val content = messageInput.text.toString()
        if (channelId.isEmpty() || content.isEmpty()) return
        
        scope.launch {
            try {
                api.sendMessage(channelId, content)
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
