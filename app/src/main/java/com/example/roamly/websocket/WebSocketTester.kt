package com.example.roamly.websocket

import android.util.Log
import com.example.roamly.factory.RetrofitFactory.BASE_URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object WebSocketTester {

    private const val TAG = "WebSocketTester"
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    fun testEndpoints(userId: String) {
        val endpoints = listOf(
            "/ws",
            "/ws/notifications",
            "/notifications",
            "/ws/websocket",
            "/api/websocket/stats",
            "/api/websocket/health"
        )

        CoroutineScope(Dispatchers.IO).launch {
            endpoints.forEach { endpoint ->
                testEndpoint("$BASE_URL$endpoint?userId=$userId")
            }

            // Тест WebSocket напрямую
            testWebSocketDirectly(userId)
        }
    }

    private suspend fun testEndpoint(url: String) {
        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            Log.d(TAG, "🔍 $url -> ${response.code} ${response.message}")
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "Empty body"
                Log.d(TAG, "Response body: ${body.take(200)}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ $url -> Error: ${e.message}")
        }
    }

    private fun testWebSocketDirectly(userId: String) {
        val wsManager = SockJSManager.getInstance()

        // Пробуем разные URL
        val urls = listOf(
            "ws://x8tsh9tc-8080.euw.devtunnels.ms/ws",
            "ws://x8tsh9tc-8080.euw.devtunnels.ms/ws/notifications",
            "ws://x8tsh9tc-8080.euw.devtunnels.ms/notifications"
        )

        urls.forEachIndexed { index, url ->
            CoroutineScope(Dispatchers.IO).launch {
                delay((index * 2000).toLong()) // Задержка между попытками

                Log.d(TAG, "🔄 Testing WebSocket: $url")
                wsManager.disconnect()

                // Создаем свой запрос для теста
                val request = Request.Builder()
                    .url("$url?userId=$userId&token=test_token_$userId")
                    .build()

                try {
                    val testClient = OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .build()

                    val webSocket = testClient.newWebSocket(request, object : okhttp3.WebSocketListener() {
                        override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                            Log.d(TAG, "✅ SUCCESS! Connected to: $url")
                            Log.d(TAG, "Response: ${response.code} ${response.message}")
                            webSocket.close(1000, "Test complete")
                        }

                        override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                            Log.d(TAG, "❌ FAILED to connect to: $url")
                            Log.d(TAG, "Error: ${t.message}")
                        }
                    })

                    delay(5000) // Даем время на подключение
                    webSocket.close(1000, "Test timeout")
                } catch (e: Exception) {
                    Log.e(TAG, "Exception testing $url: ${e.message}")
                }
            }
        }
    }
}