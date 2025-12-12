// BookingViewModel.kt - Updated with new methods and states
package com.example.roamly.entity.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.entity.DTO.booking.BookingCreationDto
import com.example.roamly.entity.DTO.booking.BookingDisplayDto
import com.example.roamly.entity.DTO.establishment.EstablishmentDisplayDto
import com.example.roamly.entity.EstablishmentLoadState
import com.example.roamly.entity.TableEntity
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

    private val _establishmentDetailState = MutableStateFlow<EstablishmentLoadState>(EstablishmentLoadState.Idle)
    val establishmentDetailState: StateFlow<EstablishmentLoadState> = _establishmentDetailState.asStateFlow()

    internal val _availableTables = MutableStateFlow<List<TableEntity>>(emptyList())
    val availableTables: StateFlow<List<TableEntity>> = _availableTables.asStateFlow()

    private val _bookings = MutableStateFlow<List<BookingDisplayDto>>(emptyList())
    val bookings: StateFlow<List<BookingDisplayDto>> = _bookings.asStateFlow()

    private val _cancellationStatus = MutableStateFlow<Boolean?>(null)
    val cancellationStatus: StateFlow<Boolean?> = _cancellationStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun fetchEstablishmentDetails(establishmentId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dto = withContext(Dispatchers.IO) {
                    apiService.getEstablishmentById(establishmentId)
                }
                _establishmentDetailState.value = EstablishmentLoadState.Success(dto)
            } catch (e: Exception) {
                _establishmentDetailState.value = EstablishmentLoadState.Error(e.message ?: "Unknown error")
                _errorMessage.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAvailableTables(establishmentId: Long, dateTime: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tables = withContext(Dispatchers.IO) {
                    apiService.getAvailableTables(establishmentId, dateTime)
                }
                _availableTables.value = tables
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createBooking(booking: BookingCreationDto, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    apiService.createBooking(booking)
                }
                onResult(true)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchUserBookings(userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userBookings = withContext(Dispatchers.IO) {
                    apiService.getUserBookings(userId)
                }
                _bookings.value = userBookings
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getBookingById(bookingId: Long): BookingDisplayDto? {
        return _bookings.value.find { it.id == bookingId }
    }

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
                _errorMessage.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearCancellationStatus() {
        _cancellationStatus.value = null
    }
}