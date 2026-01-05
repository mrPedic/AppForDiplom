package com.example.roamly.websocket

import com.example.roamly.data.source.UserDataSource
import com.example.roamly.entity.Role
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Singleton
class WebSocketService @Inject constructor(
    private val userDataSource: UserDataSource
) {
    private val sockJSManager = SockJSManager.getInstance()

    private var connectionJob: Job? = null

    fun initialize() {
        connectionJob?.cancel()

        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            userDataSource.currentUser
                .onEach { user ->
                    if (user.id != null && user.role != Role.UnRegistered) {
                        // Подключаемся немедленно
                        sockJSManager.connectWithUser(user.id.toString())
                    } else {
                        sockJSManager.disconnect()
                    }
                }
                .launchIn(this)
        }
    }

    fun cleanup() {
        connectionJob?.cancel()
        sockJSManager.disconnect()
    }
}
