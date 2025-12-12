package com.example.roamly.entity.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.data.source.UserDataSource // ⭐ Новый импорт
import com.example.roamly.entity.DTO.booking.BookingDisplayDto
import com.example.roamly.factory.RetrofitFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    // ⭐ ИСПРАВЛЕНИЕ: Внедряем UserDataSource вместо UserViewModel
    private val userDataSource: UserDataSource
) : ViewModel() {

    private val apiService by lazy { RetrofitFactory.create() }

    private val _userBookings = MutableStateFlow<List<BookingDisplayDto>>(emptyList())
    val userBookings: StateFlow<List<BookingDisplayDto>> = _userBookings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _cancellationStatus = MutableStateFlow<Boolean?>(null)
    val cancellationStatus: StateFlow<Boolean?> = _cancellationStatus.asStateFlow()

    private val TAG = "BookingViewModel"

    init {
        // ⭐ Используем Flow из UserDataSource для автоматического обновления
        viewModelScope.launch {
            userDataSource.currentUser.collectLatest { user ->
                user.id.takeIf { it != -1L }?.let { userId ->
                    fetchUserBookings(userId)
                } ?: run {
                    _userBookings.value = emptyList()
                }
            }
        }
    }

    fun fetchUserBookings(userId: Long) {
        // Проверка на -1L уже не так критична из-за collectLatest выше, но оставим для безопасности
        if (userId == -1L) {
            _userBookings.value = emptyList()
            Log.w(TAG, "Cannot fetch bookings: User ID is invalid (-1).")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Fetching bookings for User ID: $userId")
                val bookings = withContext(Dispatchers.IO) {
                    apiService.getUserBookings(userId)
                }
                _userBookings.value = bookings
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user bookings: ${e.message}", e)
                _userBookings.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Возвращает конкретную бронь по ID из текущего списка.
     */
    fun getBookingById(bookingId: Long): BookingDisplayDto? {
        return _userBookings.value.find { it.id == bookingId }
    }

    /**
     * Отправляет запрос на сервер для отмены бронирования.
     */
    fun cancelBooking(bookingId: Long) {
        // Сброс статуса перед началом
        _cancellationStatus.value = null

        viewModelScope.launch {
            try {
                // Вызываем эндпоинт API
                val response = apiService.cancelBooking(bookingId)

                if (response.isSuccessful) {
                    _cancellationStatus.value = true // Успешно отменено
                    // Опционально: Обновить список бронирований в ViewModel
                    // Например, удалить отмененную бронь из списка, который отображается в UserBookingsScreen
                } else {
                    // Обработка ошибок (например, 404 Not Found или 400 Bad Request)
                    Log.e("BookingViewModel", "Cancellation failed: ${response.code()}")
                    _cancellationStatus.value = false
                }
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Cancellation exception", e)
                _cancellationStatus.value = false
            }
        }
    }

    fun clearCancellationStatus() {
        _cancellationStatus.value = null
    }
}