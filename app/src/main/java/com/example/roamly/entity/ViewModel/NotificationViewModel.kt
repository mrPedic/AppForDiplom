package com.example.roamly.entity.ViewModel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.entity.DTO.booking.OwnerBookingDisplayDto
import com.example.roamly.entity.DTO.order.OrderStatus
import com.example.roamly.entity.DTO.order.toDisplayString
import com.example.roamly.websocket.NotificationHelper
import com.example.roamly.websocket.SockJSManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationHelper: NotificationHelper,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {
    private val sockJSManager = SockJSManager.getInstance()
    private val gson = Gson()

    private val NOTIFICATIONS_KEY = stringPreferencesKey("notifications_json")
    private val MAX_NOTIFICATIONS = 50

    data class Notification(
        val id: String,
        val type: String,
        val title: String,
        val message: String,
        val data: Map<String, Any>? = null,
        val timestamp: Long = System.currentTimeMillis(),
        val isRead: Boolean = false
    )

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _connectionState = MutableStateFlow<SockJSManager.ConnectionState>(
        SockJSManager.ConnectionState.Disconnected
    )
    val connectionState: StateFlow<SockJSManager.ConnectionState> = _connectionState.asStateFlow()

    val unreadCount: StateFlow<Int> = _notifications
        .map { notifications -> notifications.count { !it.isRead } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    var lastMessage by mutableStateOf<String?>(null)
        private set

    var connectionDebug by mutableStateOf("")
        private set

    private var isLoadingFromDataStore = false
    private var isSavingToDataStore = false
    private var hasLoadedFromDataStore = false

    init {
        viewModelScope.launch {
            if (!hasLoadedFromDataStore) {
                loadNotificationsFromDataStore()
            }
        }

        viewModelScope.launch {
            sockJSManager.messages
                .distinctUntilChanged()
                .collect { message ->
                    processIncomingMessage(message)
                }
        }

        viewModelScope.launch {
            sockJSManager.connectionState.collect { state ->
                _connectionState.value = state
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (!isLoadingFromDataStore) {
                hasLoadedFromDataStore = false
                loadNotificationsFromDataStore()
            }
        }
    }

    private fun cleanOldNotifications() {
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)

        _notifications.update { currentList ->
            val filtered = currentList.filter {
                val timestampMillis = if (it.timestamp.toString().length == 10) {
                    it.timestamp * 1000
                } else {
                    it.timestamp
                }
                timestampMillis > oneWeekAgo
            }

            if (filtered.size != currentList.size) {
                Log.d("NotificationViewModel",
                    "🧹 Удалено ${currentList.size - filtered.size} старых уведомлений")
            }

            filtered
        }

        saveNotificationsToDataStore()
    }

    private fun processIncomingMessage(message: String) {
        viewModelScope.launch {
            try {
                val json = JSONObject(message)
                val type = json.optString("type", "UNKNOWN")

                Log.d("NotificationViewModel", "📥 Получено сообщение типа: $type")

                when (type) {
                    "TEST_NOTIFICATION", "TEST_CHANNEL_NOTIFICATION" -> {
                        handleTestNotification(json, type)
                    }

                    "NEW_BOOKING" -> {
                        handleBookingNotification(json, "NEW_BOOKING", "Новое бронирование")
                    }

                    "BOOKING_STATUS_UPDATE" -> {
                        handleBookingNotification(json, "BOOKING_STATUS_UPDATE", "Статус бронирования")
                    }

                    "ping", "pong", "connected", "subscribed", "error" -> {
                        Log.d("NotificationViewModel", "📨 Служебное сообщение: $type")
                    }

                    // Внутри processIncomingMessage -> when (type)
                    "ORDER_NOTIFICATION" -> {
                        val data = json.optJSONObject("data")
                        if (data != null) {
                            val msgText = data.optString("message", "Обновление по заказу")
                            val orderId = data.optLong("orderId")

                            // ПРИМЕНЯЕМ ПЕРЕВОД ЗДЕСЬ
                            val translatedMessage = formatMessageText(msgText)

                            val notificationId = data.optLong("id").let {
                                if (it != 0L) it.toString() else UUID.randomUUID().toString()
                            }

                            val newNotification = Notification(
                                id = notificationId,
                                title = "Заказ #$orderId",
                                message = translatedMessage, // Сохраняем уже переведенный текст
                                timestamp = System.currentTimeMillis(),
                                type = "ORDER",
                                data = parseJsonToMap(data)
                            )
                            addNotificationIfNotExists(newNotification)
                        }
                    }

                    else -> {
                        Log.d("NotificationViewModel", "❓ Неизвестный тип сообщения: $type")
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Ошибка обработки сообщения: ${e.message}", e)
            }
        }
    }

    private suspend fun loadNotificationsFromDataStore() {
        if (isLoadingFromDataStore || hasLoadedFromDataStore) {
            return
        }

        isLoadingFromDataStore = true

        try {
            val json = dataStore.data.map { prefs ->
                prefs[NOTIFICATIONS_KEY] ?: "[]"
            }.first()

            if (json.isNotEmpty() && json != "[]") {
                val type = object : TypeToken<List<Notification>>() {}.type
                val loadedNotifications: List<Notification> = gson.fromJson(json, type)

                val convertedNotifications = loadedNotifications.map { notification ->
                    if (notification.timestamp.toString().length == 10) {
                        notification.copy(timestamp = notification.timestamp * 1000)
                    } else {
                        notification
                    }
                }

                val uniqueNotifications = convertedNotifications
                    .groupBy { it.id }
                    .map { (_, notifications) ->
                        notifications.maxByOrNull { it.timestamp } ?: notifications.first()
                    }
                    .sortedByDescending { it.timestamp }
                    .take(MAX_NOTIFICATIONS)

                val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                val recentNotifications = uniqueNotifications.filter { it.timestamp > oneWeekAgo }

                _notifications.value = recentNotifications
            } else {
                _notifications.value = emptyList()
            }

            hasLoadedFromDataStore = true
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "❌ Ошибка загрузки уведомлений: ${e.message}")
            _notifications.value = emptyList()
        } finally {
            isLoadingFromDataStore = false
        }
    }

    private fun handleTestNotification(json: JSONObject, type: String) {
        val data = json.optJSONObject("data") ?: JSONObject()
        val testId = data.optString("testId", UUID.randomUUID().toString())

        val timestampSeconds = data.optLong("timestamp", System.currentTimeMillis() / 1000)
        val timestampMillis = if (timestampSeconds.toString().length == 10) {
            timestampSeconds * 1000
        } else {
            timestampSeconds
        }

        val notificationId = "${type}_${testId}_${timestampSeconds}"

        val notification = Notification(
            id = notificationId,
            type = type,
            title = "Тестовое уведомление",
            message = data.optString("message", "Тестовое сообщение от сервера"),
            data = parseJsonToMap(data),
            timestamp = timestampMillis,
            isRead = false
        )

        addNotificationIfNotExists(notification)
    }

    private fun handleBookingNotification(json: JSONObject, type: String, titlePrefix: String) {
        val data = json.optJSONObject("data") ?: JSONObject()
        val bookingId = data.optLong("bookingId", 0)

        val timestampSeconds = data.optLong("timestamp", System.currentTimeMillis() / 1000)
        val timestampMillis = if (timestampSeconds.toString().length == 10) {
            timestampSeconds * 1000
        } else {
            timestampSeconds
        }

        val notificationId = "${type}_${bookingId}_${timestampSeconds}"

        val notification = Notification(
            id = notificationId,
            type = type,
            title = "$titlePrefix",
            message = data.optString("message", "Новое уведомление"),
            data = parseJsonToMap(data),
            timestamp = timestampMillis,
            isRead = false
        )

        addNotificationIfNotExists(notification)
    }

    private fun addNotificationIfNotExists(notification: Notification) {
        val existingNotification = _notifications.value.firstOrNull { it.id == notification.id }
        if (existingNotification == null) {
            addNotification(notification)

            notificationHelper.showNotification(
                title = notification.title,
                message = notification.message,
                notificationId = notification.id
            )
        }
    }

    private fun addNotification(notification: Notification) {
        _notifications.update { currentList ->
            if (currentList.any { it.id == notification.id }) {
                return@update currentList
            }
            (listOf(notification) + currentList).take(MAX_NOTIFICATIONS)
        }

        saveNotificationsToDataStore()
    }

    fun markAsRead(notificationId: String) {
        _notifications.update { currentList ->
            currentList.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(isRead = true)
                } else {
                    notification
                }
            }
        }
        saveNotificationsToDataStore()
    }

    private fun saveNotificationsToDataStore() {
        if (isSavingToDataStore) return
        isSavingToDataStore = true

        viewModelScope.launch {
            try {
                val currentNotifications = _notifications.value
                val uniqueNotifications = currentNotifications
                    .groupBy { it.id }
                    .map { (_, notifications) ->
                        notifications.maxByOrNull { it.timestamp } ?: notifications.first()
                    }

                val json = gson.toJson(uniqueNotifications)
                dataStore.edit { prefs ->
                    prefs[NOTIFICATIONS_KEY] = json
                }
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Ошибка сохранения уведомлений: ${e.message}")
            } finally {
                isSavingToDataStore = false
            }
        }
    }

    fun clearAll() {
        _notifications.value = emptyList()
        notificationHelper.dismissAllNotifications()

        viewModelScope.launch {
            try {
                dataStore.edit { it.clear() }
                hasLoadedFromDataStore = false
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Ошибка очистки DataStore: ${e.message}")
            }
        }
    }

    fun markAllAsRead() {
        _notifications.update { currentList ->
            currentList.map { it.copy(isRead = true) }
        }
        saveNotificationsToDataStore()
    }

    fun removeNotification(notificationId: String) {
        _notifications.update { currentList ->
            currentList.filter { it.id != notificationId }
        }
        saveNotificationsToDataStore()
    }

    fun sendTestMessage(trigger: String = "manual") {
        viewModelScope.launch {
            try {
                if (sockJSManager.isConnected()) {
                    sockJSManager.sendTestMessage(trigger)
                }
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Ошибка отправки: ${e.message}")
            }
        }
    }

    fun subscribeToChannel(channel: String) {
        sockJSManager.subscribe(channel)
    }

    fun getStats(): String {
        return "Уведомлений: ${_notifications.value.size}, " +
                "Непрочитанных: ${_notifications.value.count { !it.isRead }}, " +
                "Состояние: ${_connectionState.value}"
    }

    private fun parseJsonToMap(json: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = json.get(key)
        }
        return map
    }

    fun disconnect() = sockJSManager.disconnect()

    override fun onCleared() {
        super.onCleared()
    }

    private val _showBookingDialog = MutableStateFlow(false)
    val showBookingDialog: StateFlow<Boolean> = _showBookingDialog.asStateFlow()

    private val _selectedBooking = MutableStateFlow<OwnerBookingDisplayDto?>(null)
    val selectedBooking: StateFlow<OwnerBookingDisplayDto?> = _selectedBooking.asStateFlow()

    fun showBookingApprovalDialog(booking: OwnerBookingDisplayDto) {
        _selectedBooking.value = booking
        _showBookingDialog.value = true
    }

    fun dismissBookingDialog() {
        _showBookingDialog.value = false
        _selectedBooking.value = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun handleNotificationClick(notificationId: String, data: Map<String, String>) {
        viewModelScope.launch {
            markAsRead(notificationId)

            val bookingId = data["bookingId"]?.toLongOrNull()
            val establishmentId = data["establishmentId"]?.toLongOrNull()

            if (bookingId != null && establishmentId != null) {
                val booking = OwnerBookingDisplayDto(
                    id = bookingId,
                    establishmentId = establishmentId,
                    establishmentName = data["establishmentName"] ?: "Заведение",
                    userId = data["userId"]?.toLongOrNull() ?: 0,
                    userName = data["userName"] ?: "Гость",
                    userPhone = data["userPhone"],
                    tableNumber = data["tableNumber"]?.toIntOrNull() ?: 1,
                    numberOfGuests = data["numberOfGuests"]?.toIntOrNull() ?: 2,
                    startTime = parseDateTime(data["startTime"]),
                    endTime = parseDateTime(data["endTime"]),
                    status = com.example.roamly.entity.DTO.booking.BookingStatus.PENDING
                )

                showBookingApprovalDialog(booking)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseDateTime(dateTimeStr: String?): java.time.LocalDateTime {
        return try {
            java.time.LocalDateTime.parse(dateTimeStr)
        } catch (e: Exception) {
            java.time.LocalDateTime.now()
        }
    }

    // Словарь для перевода конкретных статусов
    // В файле NotificationViewModel.kt

    /**
     * Основной метод перевода, использующий Enum OrderStatus.
     * Мы сохраняем метод translateStatus(OrderStatus), как ты и просил.
     */
    private fun translateStatus(status: OrderStatus): String {
        return status.toDisplayString().lowercase() // Используем метод из OrderModels.kt
    }

    /**
     * Вспомогательный метод для перевода строк, которые не входят в OrderStatus
     */
    private fun translateTechnicalType(type: String): String {
        return when (type.uppercase()) {
            "ORDER_CREATED" -> "создан"
            "ORDER_STATUS_CHANGED" -> "изменен"
            "PAID" -> "оплачен"
            "READY" -> "готов к выдаче"
            else -> type
        }
    }

    /**
     * Сканирует текст и заменяет английские термины на русские через Enum
     */
    private fun formatMessageText(rawMessage: String): String {
        var formatted = rawMessage
        // Список всех слов, которые могут встретиться в тексте сообщения
        val wordsToTranslate = listOf(
            "PENDING", "CONFIRMED", "IN_PROGRESS", "OUT_FOR_DELIVERY",
            "DELIVERED", "CANCELLED", "REJECTED",
            "ORDER_CREATED", "ORDER_STATUS_CHANGED", "PAID", "READY"
        )

        wordsToTranslate.forEach { word ->
            val regex = Regex("\\b$word\\b", RegexOption.IGNORE_CASE)
            if (formatted.contains(regex)) {
                val replacement = try {
                    // Пробуем найти соответствие в OrderStatus
                    val statusEnum = OrderStatus.valueOf(word.uppercase())
                    translateStatus(statusEnum)
                } catch (e: Exception) {
                    // Если это не статус заказа, переводим как технический тип
                    translateTechnicalType(word)
                }
                formatted = formatted.replace(regex, replacement)
            }
        }
        return formatted
    }
}