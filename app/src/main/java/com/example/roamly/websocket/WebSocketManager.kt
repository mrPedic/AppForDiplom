package com.example.roamly.websocket

import android.util.Log
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

class WebSocketManager private constructor() {

    // В companion object
    private var currentUserId: String? = null

    // Замени константу на функцию
    private fun getWsUrl(userId: String): String {
        return "ws://10.39.189.228:8080/ws?userId=$userId" // Для эмулятора
        // return "ws://x8tsh9tc-8080.euw.devtunnels.ms/ws?userId=$userId" //Для реального устройства: "ws://192.168.1.XXX:8080/ws?userId=$userId"
    }

    // Добавь метод для переподключения с новым userId
    fun connectWithUser(userId: String) {
        if (currentUserId == userId && webSocket != null) return // Уже подключены

        disconnect() // Закрываем старое соединение, если было

        currentUserId = userId

        scope.launch { _connectionState.emit(ConnectionState.Connecting) }

        val request = Request.Builder()
            .url(getWsUrl(userId))
            .build()

        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    // Переопредели connect() — теперь без параметров он не делает ничего важного
    fun connect() {
        currentUserId?.let { connectWithUser(it) } ?: run {
            Log.w(TAG, "Attempted to connect without userId")
        }
    }


    companion object {
        private const val TAG = "WebSocketManager"
        // Для реального устройства используй IP твоего ПК в локальной сети, напр. ws://192.168.1.100:8080/ws

        @Volatile
        private var INSTANCE: WebSocketManager? = null

        fun getInstance(): WebSocketManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebSocketManager().also { INSTANCE = it }
            }
        }
    }

    private val okHttpClient = OkHttpClient()
    private var webSocket: WebSocket? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Для получения сообщений от сервера
    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    // Состояние подключения
    private val _connectionState = MutableSharedFlow<ConnectionState>()
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()

    sealed class ConnectionState {
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        object Disconnected : ConnectionState()
        data class Failed(val reason: String) : ConnectionState()
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
            scope.launch { _connectionState.emit(ConnectionState.Connected) }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received: $text")
            scope.launch { _messages.emit(text) }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "Closing: $code / $reason")
            webSocket.close(1000, null)
            scope.launch { _connectionState.emit(ConnectionState.Disconnected) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "Error: ${t.message}", t)
            scope.launch {
                _connectionState.emit(
                    ConnectionState.Failed(
                        t.message ?: "Unknown error"
                    )
                )
            }
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Manual disconnect")
        webSocket = null
        scope.launch { _connectionState.emit(ConnectionState.Disconnected) }
    }

    fun subscribe(channel: String) {
        val json = JSONObject().apply {
            put("type", "subscribe")
            put("channel", channel)
        }
        send(json.toString())
    }

    fun unsubscribe(channel: String) {
        val json = JSONObject().apply {
            put("type", "unsubscribe")
            put("channel", channel)
        }
        send(json.toString())
    }

    private fun send(message: String) {
        val sent = webSocket?.send(message) ?: false
        Log.d(TAG, "Sent: $message | Success: $sent")
    }

    fun isConnected(): Boolean = webSocket != null
}