package com.example.roamly.entity.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.data.source.UserDataSource // ⭐ Новый импорт
import com.example.roamly.entity.DTO.BookingDisplayDto
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
}