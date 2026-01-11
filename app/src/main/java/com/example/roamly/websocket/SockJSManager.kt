package com.example.roamly.websocket

import android.util.Log
import com.example.roamly.factory.RetrofitFactory.BASE_URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SockJSManager private constructor() {

    companion object {
        private const val TAG = "SockJSManager"

        @Volatile
        private var INSTANCE: SockJSManager? = null

        fun getInstance(): SockJSManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SockJSManager().also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null
    private var currentUserId: String? = null
    private var isConnecting = false
    private var lastConnectionTime: Long = 0
    private val RECONNECT_DELAY = 3000L

    private var lastMessageHash: Int = 0

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _connectionState = MutableSharedFlow<ConnectionState>()
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()

    sealed class ConnectionState {
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        object Disconnected : ConnectionState()
        data class Failed(val reason: String) : ConnectionState()
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun getWebSocketUrl(userId: String): String {
        val serverUrl = BASE_URL.replace("https://", "").replace("http://", "")
        return "ws://$serverUrl/ws/notifications?userId=$userId&token=android_token_$userId"
    }

    fun connectWithUser(userId: String) {
        if (currentUserId == userId && isConnected()) {
            Log.d(TAG, "Already connected for user $userId")
            return
        }

        // Очистить предыдущее соединение
        cleanup()

        // Проверка на слишком раннюю попытку ДО установки времени
        val now = System.currentTimeMillis()
        if (now - lastConnectionTime < 2000) {
            Log.d(TAG, "Connection attempt too soon, skipping...")
            return
        }

        // Теперь устанавливаем новые значения
        currentUserId = userId
        isConnecting = true
        lastConnectionTime = now  // Устанавливаем ПОСЛЕ проверки

        scope.launch {
            _connectionState.emit(ConnectionState.Connecting)
        }

        try {
            val url = getWebSocketUrl(userId)
            Log.d(TAG, "🔗 Connecting to WebSocket: $url")

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Roamly-Android-App")
                .addHeader("Accept", "*/*")
                .addHeader("Sec-WebSocket-Protocol", "chat, superchat")
                .build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "✅ WebSocket connected successfully!")
                    isConnecting = false

                    scope.launch {
                        _connectionState.emit(ConnectionState.Connected)

                        // 🔥 ОТПРАВЛЯЕМ ТЕСТОВОЕ СООБЩЕНИЕ ПРИ ПОДКЛЮЧЕНИИ
                        sendTestMessage("connection_established")

                        // Подписываемся на канал пользователя
                        val subscribeMsg = JSONObject().apply {
                            put("type", "subscribe")
                            put("channel", "user_$currentUserId")
                            put("requestId", System.currentTimeMillis().toString())
                            put("userId", currentUserId)
                        }
                        webSocket.send(subscribeMsg.toString())
                        Log.d(TAG, "📤 Subscribed to user channel: user_$currentUserId")

                        // Подписываемся на общий канал
                        val globalMsg = JSONObject().apply {
                            put("type", "subscribe")
                            put("channel", "global")
                            put("requestId", System.currentTimeMillis().toString())
                            put("userId", currentUserId)
                        }
                        webSocket.send(globalMsg.toString())
                        Log.d(TAG, "📤 Subscribed to global channel")
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "📩 Received message: $text")

                    if (text.isBlank()) return

                    // Игнорировать ping/pong сообщения для логов
                    if (text.contains("\"type\":\"ping\"") || text.contains("\"type\":\"pong\"")) {
                        Log.d(TAG, "🏓 Ping/pong received")
                        return
                    }

                    // Обрабатываем только уникальные сообщения
                    val messageHash = text.hashCode()
                    if (lastMessageHash == messageHash) {
                        Log.d(TAG, "🔄 Duplicate message ignored")
                        return
                    }
                    lastMessageHash = messageHash

                    // 🔥 ДЕТАЛЬНОЕ ЛОГИРОВАНИЕ ВСЕХ СООБЩЕНИЙ
                    try {
                        val json = JSONObject(text)
                        val type = json.optString("type")
                        Log.d(TAG, "📦 Message type: $type")

                        when (type) {


                            "ORDER_NOTIFICATION" -> {
                                Log.d(TAG, "🛒 Received order notification")
                                // Обработка уведомления о заказе
                                scope.launch {
                                    _messages.emit(text)
                                }
                            }


                            "ping" -> {
                                Log.d(TAG, "🏓 Received ping from server")
                                val pongMsg = JSONObject().apply {
                                    put("type", "pong")
                                    put("timestamp", json.optLong("timestamp"))
                                    put("requestId", json.optString("requestId", ""))
                                }
                                webSocket.send(pongMsg.toString())
                                Log.d(TAG, "📤 Sent pong response")
                            }


                            "TEST_NOTIFICATION", "TEST_CHANNEL_NOTIFICATION" -> {
                                Log.d(TAG, "🎯 RECEIVED TEST NOTIFICATION! Full message:")
                                Log.d(TAG, "📋 ${json.toString(2)}")
                            }


                            else -> {
                                Log.d(TAG, "📄 Other message of type: $type")
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "📝 Plain text message: $text")
                    }

                    scope.launch {
                        _messages.emit(text)
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "🔒 WebSocket closing: $code - $reason")
                    cleanup()
                    scope.launch {
                        _connectionState.emit(ConnectionState.Disconnected)
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "🔒 WebSocket closed: $code - $reason")
                    cleanup()
                    scope.launch {
                        _connectionState.emit(ConnectionState.Disconnected)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "❌ WebSocket error: ${t.message}")
                    t.printStackTrace()

                    cleanup()
                    scope.launch {
                        _connectionState.emit(
                            ConnectionState.Failed(t.message ?: "Unknown error")
                        )
                    }

                    // Попытка переподключения
                    currentUserId?.let { userId ->
                        scope.launch {
                            kotlinx.coroutines.delay(RECONNECT_DELAY)
                            if (this@SockJSManager.currentUserId == userId) {
                                Log.d(TAG, "🔄 Attempting to reconnect for user $userId")
                                connectWithUser(userId)
                            }
                        }
                    }
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create WebSocket connection: ${e.message}")
            isConnecting = false
            scope.launch {
                _connectionState.emit(ConnectionState.Failed(e.message ?: "Unknown error"))
            }
        }
    }

    // 🔥 НОВЫЙ МЕТОД: Отправка тестового сообщения на сервер
    fun sendTestMessage(trigger: String = "manual") {
        val userId = currentUserId
        val socket = webSocket

        if (socket == null) {
            Log.w(TAG, "Cannot send test message: WebSocket not connected")
            return
        }

        val testMsg = JSONObject().apply {
            put("type", "test_notification")
            put("notificationType", trigger)
            put("requestId", System.currentTimeMillis().toString())
            put("userId", userId)
            put("timestamp", System.currentTimeMillis())
            put("clientTime", System.currentTimeMillis().toString())
        }

        val sent = socket.send(testMsg.toString())
        if (sent) {
            Log.d(TAG, "📤 Sent test message to server, trigger: $trigger")
        } else {
            Log.e(TAG, "❌ Failed to send test message")
        }
    }

    fun subscribe(channel: String) {
        val userId = currentUserId
        val socket = webSocket

        if (socket == null) {
            Log.w(TAG, "Cannot subscribe: WebSocket not connected")
            return
        }

        val subscribeMsg = JSONObject().apply {
            put("type", "subscribe")
            put("channel", channel)
            put("requestId", System.currentTimeMillis().toString())
            put("userId", userId)
        }

        val sent = socket.send(subscribeMsg.toString())
        if (sent) {
            Log.d(TAG, "✅ Subscribed to channel: $channel")
        } else {
            Log.e(TAG, "❌ Failed to subscribe to channel: $channel")
        }
    }

    fun send(destination: String, payload: String) {
        val socket = webSocket
        if (socket != null) {
            val message = JSONObject().apply {
                put("type", "message")
                put("destination", destination)
                put("payload", payload)
                put("timestamp", System.currentTimeMillis())
                put("userId", currentUserId)
            }

            val sent = socket.send(message.toString())
            if (sent) {
                Log.d(TAG, "📤 Sent to $destination: $payload")
            } else {
                Log.e(TAG, "❌ Failed to send to $destination")
            }
        } else {
            Log.w(TAG, "⚠️ Cannot send message: WebSocket is not connected")
        }
    }

    fun disconnect() {
        cleanup()
        currentUserId = null
        scope.launch {
            _connectionState.emit(ConnectionState.Disconnected)
        }
        Log.d(TAG, "WebSocket disconnected")
    }

    private fun cleanup() {
        webSocket?.close(1000, "Manual disconnect")
        webSocket = null
        isConnecting = false
    }

    fun isConnected(): Boolean {
        return webSocket != null
    }

    fun diagnoseConnection(): String {
        return buildString {
            appendLine("=== WebSocket Diagnosis ===")
            appendLine("User ID: $currentUserId")
            appendLine("Connected: ${isConnected()}")
            appendLine("Is Connecting: $isConnecting")
            appendLine("WebSocket instance: ${if (webSocket != null) "Present" else "Null"}")
        }
    }
}