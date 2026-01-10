// BookingViewModel.kt — финальная рабочая версия
package com.example.roamly.entity.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.entity.DTO.booking.BookingCreationDto
import com.example.roamly.entity.DTO.booking.BookingDisplayDto
import com.example.roamly.entity.DTO.booking.OwnerBookingDisplayDto
import com.example.roamly.entity.EstablishmentLoadState
import com.example.roamly.entity.classes.TableEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    // Состояние загрузки деталей заведения
    private val _establishmentDetailState = MutableStateFlow<EstablishmentLoadState>(EstablishmentLoadState.Idle)
    val establishmentDetailState: StateFlow<EstablishmentLoadState> = _establishmentDetailState.asStateFlow()

    // Доступные столики для выбранного времени
    internal val _availableTables = MutableStateFlow<List<TableEntity>>(emptyList())
    val availableTables: StateFlow<List<TableEntity>> = _availableTables.asStateFlow()

    // Список броней текущего пользователя
    private val _bookings = MutableStateFlow<List<BookingDisplayDto>>(emptyList())
    val bookings: StateFlow<List<BookingDisplayDto>> = _bookings.asStateFlow()

    // Состояние создания/отмены брони
    private val _cancellationStatus = MutableStateFlow<Boolean?>(null)
    val cancellationStatus: StateFlow<Boolean?> = _cancellationStatus.asStateFlow()

    // Состояние создания брони
    private val _bookingCreationStatus = MutableStateFlow<Boolean?>(null)
    val bookingCreationStatus: StateFlow<Boolean?> = _bookingCreationStatus.asStateFlow()

    // === НОВОЕ: для владельца заведения ===
    private val _ownerPendingBookings = MutableStateFlow<List<OwnerBookingDisplayDto>>(emptyList())
    val ownerPendingBookings: StateFlow<List<OwnerBookingDisplayDto>> = _ownerPendingBookings.asStateFlow()

    // Список approved броней для владельца
    private val _ownerApprovedBookings = MutableStateFlow<List<OwnerBookingDisplayDto>>(emptyList())
    val ownerApprovedBookings: StateFlow<List<OwnerBookingDisplayDto>> = _ownerApprovedBookings.asStateFlow()

    // Общие состояния
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Загрузка деталей заведения
    fun fetchEstablishmentDetails(establishmentId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val establishment = withContext(Dispatchers.IO) {
                    apiService.getEstablishmentById(establishmentId)
                }
                _establishmentDetailState.value = EstablishmentLoadState.Success(establishment)
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки заведения: ${e.message}"
                _establishmentDetailState.value = EstablishmentLoadState.Error(e.message ?: "Неизвестная ошибка")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Загрузка доступных столиков
    fun fetchAvailableTables(establishmentId: Long, startTimeIso: String) {
        viewModelScope.launch {
            try {
                val tables = withContext(Dispatchers.IO) {
                    apiService.getAvailableTables(establishmentId, startTimeIso)
                }
                _availableTables.value = tables
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки столиков: ${e.message}"
            }
        }
    }

    // Загрузка броней пользователя
    fun fetchUserBookings(userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userBookings = withContext(Dispatchers.IO) {
                    apiService.getUserBookings(userId)
                }
                _bookings.value = userBookings
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки броней: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Создание брони
    fun createBooking(booking: BookingCreationDto, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    apiService.createBooking(booking)
                }
                onResult(true)
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка создания брони: ${e.message}"
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearBookingCreationStatus() {
        _bookingCreationStatus.value = null
    }

    // Отмена брони
    fun cancelBooking(bookingId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    apiService.cancelBooking(bookingId)
                }
                _cancellationStatus.value = true
                _bookings.update { it.filter { booking -> booking.id != bookingId } }
            } catch (e: Exception) {
                _cancellationStatus.value = false
                _errorMessage.value = e.message ?: "Ошибка отмены"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearCancellationStatus() {
        _cancellationStatus.value = null
    }

    // === МЕТОДЫ ДЛЯ ВЛАДЕЛЬЦА ЗАВЕДЕНИЯ ===

    fun fetchPendingBookingsForOwner(ownerId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val bookings = withContext(Dispatchers.IO) {
                    apiService.getPendingBookingsForOwner(ownerId)
                }
                _ownerPendingBookings.value = bookings
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось загрузить брони: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchApprovedBookingsForOwner(ownerId: Long, establishmentId: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val bookings = withContext(Dispatchers.IO) {
                    apiService.getApprovedBookingsForOwner(ownerId, establishmentId)
                }
                _ownerApprovedBookings.value = bookings
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось загрузить брони: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveBooking(bookingId: Long, ownerId: Long) {
        updateBookingStatus(bookingId, "CONFIRMED", ownerId)
    }

    fun rejectBooking(bookingId: Long, ownerId: Long) {
        updateBookingStatus(bookingId, "REJECTED", ownerId)
    }

    private fun updateBookingStatus(bookingId: Long, status: String, ownerId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    apiService.updateBookingStatus(bookingId, status, ownerId)
                }
                // Удаляем из списка — больше не pending
                _ownerPendingBookings.update { list ->
                    list.filterNot { it.id == bookingId }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось изменить статус: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun fetchBookingDetails(bookingId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val booking = withContext(Dispatchers.IO) {
                    apiService.getBookingDetails(bookingId)
                }
                // Можно сохранить в отдельном StateFlow для диалога
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось загрузить детали брони: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}