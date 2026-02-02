package com.example.roamly.entity.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SendResult(val success: Boolean, val error: String? = null)

data class GlobalNotificationDto(
    val title: String,
    val message: String,
    val target: String, // "all_users", "all_establishments", "specific_user:{id}", etc.
)

@HiltViewModel
class AdminNotificationsViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()

    private val _sendResult = MutableStateFlow<SendResult?>(null)
    val sendResult = _sendResult.asStateFlow()

    fun sendGlobalNotification(
        title: String,
        message: String,
        target: String,
        specificId: Long? = null
    ) {
        viewModelScope.launch {
            _isSending.value = true
            _sendResult.value = null

            // Добавляем ID только если target начинается на specific_
            val effectiveTarget = if (target.startsWith("specific_") && specificId != null) {
                "$target:$specificId"
            } else {
                target
            }
            // -----------------------

            try {
                Log.d("AdminVM", "Sending notification to: $effectiveTarget") // Лог для проверки
                apiService.sendGlobalNotification(
                    GlobalNotificationDto(
                        title = title,
                        message = message,
                        target = effectiveTarget
                    )
                )
                _sendResult.value = SendResult(true)
            } catch (e: Exception) {
                Log.e("NotificationsViewModel", "Error sending notification", e)
                _sendResult.value = SendResult(false, e.message)
            } finally {
                _isSending.value = false
            }
        }
    }
}