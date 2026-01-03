package com.example.roamly.notification

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.websocket.WebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor() : ViewModel() {
    private val wsManager = WebSocketManager.getInstance()

    val messages = mutableStateListOf<String>()
    val connectionState = mutableStateOf<WebSocketManager.ConnectionState>(WebSocketManager.ConnectionState.Disconnected)

    init {
        viewModelScope.launch {
            wsManager.messages.collect { message ->
                messages.add(message)
            }
        }

        viewModelScope.launch {
            wsManager.connectionState.collect { state ->
                connectionState.value = state
            }
        }
    }

    fun connect() = wsManager.connect()

    fun subscribeToChannel(channel: String) = wsManager.subscribe(channel)

    fun unsubscribeFromChannel(channel: String) = wsManager.unsubscribe(channel)

    fun disconnect() = wsManager.disconnect()

    override fun onCleared() {
        super.onCleared()
        wsManager.disconnect()
    }
}