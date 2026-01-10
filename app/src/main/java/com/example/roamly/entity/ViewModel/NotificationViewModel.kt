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
import com.example.roamly.websocket.NotificationHelper
import com.example.roamly.websocket.SockJSManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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

    // 🔥 ИСПРАВЛЕНО: Используем единственный источник истины
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _connectionState = MutableStateFlow<SockJSManager.ConnectionState>(
        SockJSManager.ConnectionState.Disconnected
    )
    val connectionState: StateFlow<SockJSManager.ConnectionState> = _connectionState.asStateFlow()

    // 🔥 ИСПРАВЛЕНО: Вычисляемое свойство для непрочитанных
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

    // 🔥 ДЛЯ ОТЛАДКИ: Отслеживаем состояние загрузки
    private var isLoadingFromDataStore = false
    private var isSavingToDataStore = false
    private var hasLoadedFromDataStore = false

    init {
        // Загружаем уведомления из DataStore только один раз при создании ViewModel
        viewModelScope.launch {
            if (!hasLoadedFromDataStore) {
                loadNotificationsFromDataStore()
            }
        }

        // 🔥 ИСПРАВЛЕНИЕ: Убрал бесконечный цикл (может вызывать проблемы)
        // Вместо этого будем обновлять только при явном вызове refresh()

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

    // 🔥 ИСПРАВЛЕННЫЙ МЕТОД: Обновление уведомлений
    fun refresh() {
        viewModelScope.launch {
            if (!isLoadingFromDataStore) {
                // Сбрасываем флаг загрузки, чтобы можно было загрузить снова
                hasLoadedFromDataStore = false
                loadNotificationsFromDataStore()
            }
        }
    }

    // 🔥 НОВЫЙ МЕТОД: Обновление уведомлений
    private suspend fun refreshNotifications() {
        if (!hasLoadedFromDataStore) {
            loadNotificationsFromDataStore()
        }
    }

    // 🔥 ИСПРАВЛЕННЫЙ МЕТОД: Проверка и очистка старых уведомлений
    private fun cleanOldNotifications() {
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // 7 дней назад

        _notifications.update { currentList ->
            // 🔥 ИСПРАВЛЕНИЕ: Убедимся, что timestamp уже в миллисекундах
            val filtered = currentList.filter {
                // Если timestamp в секундах, конвертируем на лету
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

    // 🔥 ИСПРАВЛЕННЫЙ МЕТОД: Обработка входящих сообщений
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
                        // Игнорируем служебные сообщения
                        Log.d("NotificationViewModel", "📨 Служебное сообщение: $type")
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

    // 🔥 ИСПРАВЛЕННЫЙ МЕТОД: Загрузка уведомлений с корректной обработкой временных меток
    private suspend fun loadNotificationsFromDataStore() {
        if (isLoadingFromDataStore || hasLoadedFromDataStore) {
            Log.d("NotificationViewModel", "⚠️ Уже загружаем или уже загрузили, пропускаем")
            return
        }

        isLoadingFromDataStore = true

        try {
            val json = dataStore.data.map { prefs ->
                prefs[NOTIFICATIONS_KEY] ?: "[]"
            }.first()

            Log.d("NotificationViewModel", "📥 Загружаем из DataStore, длина JSON: ${json.length}")

            if (json.isNotEmpty() && json != "[]") {
                val type = object : TypeToken<List<Notification>>() {}.type
                val loadedNotifications: List<Notification> = gson.fromJson(json, type)

                // 🔥 ИСПРАВЛЕНИЕ: КОНВЕРТИРУЕМ TIMESTAMP ИЗ СЕКУНД В МИЛЛИСЕКУНДЫ
                val convertedNotifications = loadedNotifications.map { notification ->
                    // Проверяем, является ли timestamp 10-значным (секунды)
                    if (notification.timestamp.toString().length == 10) {
                        notification.copy(timestamp = notification.timestamp * 1000)
                    } else {
                        notification
                    }
                }

                // Удаляем дубликаты по ID
                val uniqueNotifications = convertedNotifications
                    .groupBy { it.id }
                    .map { (_, notifications) ->
                        notifications.maxByOrNull { it.timestamp } ?: notifications.first()
                    }
                    .sortedByDescending { it.timestamp }
                    .take(MAX_NOTIFICATIONS)

                Log.d("NotificationViewModel",
                    "📦 Загружено из DataStore: ${loadedNotifications.size}, " +
                            "уникальных: ${uniqueNotifications.size}, " +
                            "ID: ${uniqueNotifications.map { it.id }}"
                )

                // 🔥 ОЧИСТКА СТАРЫХ УВЕДОМЛЕНИЙ (но теперь timestamp в миллисекундах)
                val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                val recentNotifications = uniqueNotifications.filter { it.timestamp > oneWeekAgo }

                if (recentNotifications.size != uniqueNotifications.size) {
                    Log.d("NotificationViewModel",
                        "🧹 Автоматически удалено ${uniqueNotifications.size - recentNotifications.size} старых уведомлений")
                }

                // Устанавливаем загруженные уведомления
                _notifications.value = recentNotifications

                Log.d("NotificationViewModel",
                    "✅ Загружено ${recentNotifications.size} уведомлений из DataStore, " +
                            "непрочитанных: ${recentNotifications.count { !it.isRead }}"
                )
            } else {
                Log.d("NotificationViewModel", "📭 DataStore пуст")
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

    // 🔥 ИСПРАВЛЕННЫЙ МЕТОД: Обработка тестовых уведомлений
    private fun handleTestNotification(json: JSONObject, type: String) {
        val data = json.optJSONObject("data") ?: JSONObject()
        val testId = data.optString("testId", UUID.randomUUID().toString())

        // 🔥 ИСПРАВЛЕНИЕ: Конвертируем timestamp из секунд в миллисекунды
        val timestampSeconds = data.optLong("timestamp", System.currentTimeMillis() / 1000)
        val timestampMillis = if (timestampSeconds.toString().length == 10) {
            timestampSeconds * 1000
        } else {
            timestampSeconds
        }

        val trigger = data.optString("trigger", "unknown")

        // 🔥 УНИКАЛЬНЫЙ ID для предотвращения дубликатов
        val notificationId = "${type}_${testId}_${timestampSeconds}"

        val notification = Notification(
            id = notificationId,
            type = type,
            title = "Тестовое уведомление",
            message = data.optString("message", "Тестовое сообщение от сервера"),
            data = parseJsonToMap(data),
            timestamp = timestampMillis, // 🔥 Теперь в миллисекундах
            isRead = false
        )

        // 🔥 ДОБАВЛЯЕМ ТОЛЬКО ЕСЛИ ЕЩЁ НЕТ
        addNotificationIfNotExists(notification)
    }

    // 🔥 ИСПРАВЛЕННЫЙ МЕТОД: Обработка бронирований
    private fun handleBookingNotification(json: JSONObject, type: String, titlePrefix: String) {
        val data = json.optJSONObject("data") ?: JSONObject()
        val bookingId = data.optLong("bookingId", 0)

        // 🔥 ИСПРАВЛЕНИЕ: Конвертируем timestamp из секунд в миллисекунды
        val timestampSeconds = data.optLong("timestamp", System.currentTimeMillis() / 1000)
        val timestampMillis = if (timestampSeconds.toString().length == 10) {
            timestampSeconds * 1000
        } else {
            timestampSeconds
        }

        // 🔥 УНИКАЛЬНЫЙ ID
        val notificationId = "${type}_${bookingId}_${timestampSeconds}"

        val notification = Notification(
            id = notificationId,
            type = type,
            title = "$titlePrefix",
            message = data.optString("message", "Новое уведомление"),
            data = parseJsonToMap(data),
            timestamp = timestampMillis, // 🔥 Теперь в миллисекундах
            isRead = false
        )

        // 🔥 ДОБАВЛЯЕМ ТОЛЬКО ЕСЛИ ЕЩЁ НЕТ
        addNotificationIfNotExists(notification)
    }

    // 🔥 НОВЫЙ МЕТОД: Добавление уведомления если не существует
    private fun addNotificationIfNotExists(notification: Notification) {
        val existingNotification = _notifications.value.firstOrNull { it.id == notification.id }
        if (existingNotification == null) {
            addNotification(notification)

            // Показываем системное уведомление
            notificationHelper.showNotification(
                title = notification.title,
                message = notification.message,
                notificationId = notification.id
            )
        } else {
            Log.d("NotificationViewModel", "🔄 Уведомление уже существует: ${notification.id}")
        }
    }

    // 🔥 ИСПРАВЛЕННЫЙ МЕТОД: Добавление уведомления
    private fun addNotification(notification: Notification) {
        _notifications.update { currentList ->
            // Проверяем, нет ли уже уведомления с таким ID
            if (currentList.any { it.id == notification.id }) {
                Log.d("NotificationViewModel", "🔄 Уведомление ${notification.id} уже существует")
                return@update currentList
            }

            // Добавляем новое уведомление в начало
            (listOf(notification) + currentList).take(MAX_NOTIFICATIONS)
        }

        // 🔥 Сохраняем в DataStore
        saveNotificationsToDataStore()

        Log.d("NotificationViewModel",
            "📝 Добавлено уведомление [${notification.type}], " +
                    "всего: ${_notifications.value.size}, " +
                    "непрочитанных: ${_notifications.value.count { !it.isRead }}"
        )
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

        Log.d("NotificationViewModel",
            "✅ Уведомление $notificationId помечено как прочитанное, " +
                    "непрочитанных: ${_notifications.value.count { !it.isRead }}"
        )
    }

    // 🔥 ИСПРАВЛЕННЫЙ МЕТОД: Сохранение в DataStore
    private fun saveNotificationsToDataStore() {
        if (isSavingToDataStore) {
            Log.d("NotificationViewModel", "⚠️ Уже сохраняем в DataStore, пропускаем")
            return
        }

        isSavingToDataStore = true

        viewModelScope.launch {
            try {
                val currentNotifications = _notifications.value

                Log.d("NotificationViewModel",
                    "💾 Сохраняем в DataStore: ${currentNotifications.size} уведомлений, " +
                            "непрочитанных: ${currentNotifications.count { !it.isRead }}"
                )

                // 🔥 УДАЛЯЕМ ДУБЛИКАТЫ ПЕРЕД СОХРАНЕНИЕМ
                val uniqueNotifications = currentNotifications
                    .groupBy { it.id }
                    .map { (_, notifications) ->
                        notifications.maxByOrNull { it.timestamp } ?: notifications.first()
                    }

                val json = gson.toJson(uniqueNotifications)

                dataStore.edit { prefs ->
                    prefs[NOTIFICATIONS_KEY] = json
                }

                Log.d("NotificationViewModel",
                    "✅ Сохранено ${uniqueNotifications.size} уникальных уведомлений в DataStore"
                )
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Ошибка сохранения уведомлений: ${e.message}")
            } finally {
                isSavingToDataStore = false
            }
        }
    }

    // 🔥 ИСПРАВЛЕННЫЙ МЕТОД: Очистка всех уведомлений
    fun clearAll() {
        Log.d("NotificationViewModel", "🧹 Очищаем все уведомления")

        _notifications.value = emptyList()
        notificationHelper.dismissAllNotifications()

        viewModelScope.launch {
            try {
                dataStore.edit { it.clear() }
                hasLoadedFromDataStore = false // 🔥 СБРАСЫВАЕМ ФЛАГ
                Log.d("NotificationViewModel", "✅ DataStore очищен")
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Ошибка очистки DataStore: ${e.message}")
            }
        }
    }

    // 🔥 ИСПРАВЛЕННЫЙ МЕТОД: Пометить все как прочитанные
    fun markAllAsRead() {
        _notifications.update { currentList ->
            currentList.map { it.copy(isRead = true) }
        }

        saveNotificationsToDataStore()

        Log.d("NotificationViewModel", "✅ Все уведомления помечены как прочитанные")
    }

    // 🔥 ИСПРАВЛЕННЫЙ МЕТОД: Удаление уведомления
    fun removeNotification(notificationId: String) {
        _notifications.update { currentList ->
            currentList.filter { it.id != notificationId }
        }

        saveNotificationsToDataStore()

        Log.d("NotificationViewModel", "🗑️ Уведомление $notificationId удалено")
    }

    fun sendTestMessage(trigger: String = "manual") {
        Log.d("NotificationViewModel", "🚀 Отправка тестового сообщения на сервер")
        viewModelScope.launch {
            try {
                if (sockJSManager.isConnected()) {
                    sockJSManager.sendTestMessage(trigger)
                }
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Ошибка отправки тестового сообщения: ${e.message}")
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
        Log.d("NotificationViewModel", "🛑 NotificationViewModel уничтожается")
    }

    // Новые состояния для диалога подтверждения брони
    private val _showBookingDialog = MutableStateFlow(false)
    val showBookingDialog: StateFlow<Boolean> = _showBookingDialog.asStateFlow()

    private val _selectedBooking = MutableStateFlow<OwnerBookingDisplayDto?>(null)
    val selectedBooking: StateFlow<OwnerBookingDisplayDto?> = _selectedBooking.asStateFlow()

    // Функция для открытия диалога с бронированием
    fun showBookingApprovalDialog(booking: OwnerBookingDisplayDto) {
        _selectedBooking.value = booking
        _showBookingDialog.value = true
    }

    // Функция для закрытия диалога
    fun dismissBookingDialog() {
        _showBookingDialog.value = false
        _selectedBooking.value = null
    }

    // Функция для обработки уведомления
    @RequiresApi(Build.VERSION_CODES.O)
    fun handleNotificationClick(notificationId: String, data: Map<String, String>) {
        viewModelScope.launch {
            // Пометим уведомление как прочитанное
            markAsRead(notificationId)

            // Если в данных есть информация о бронировании, показываем диалог
            val bookingId = data["bookingId"]?.toLongOrNull()
            val establishmentId = data["establishmentId"]?.toLongOrNull()

            if (bookingId != null && establishmentId != null) {
                // Здесь нужно загрузить детали бронирования
                // Временная заглушка - в реальном приложении нужно загрузить из API
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
}
