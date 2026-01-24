// OwnerOrdersViewModel.kt
package com.example.roamly.entity.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.entity.DTO.order.OrderDto
import com.example.roamly.entity.DTO.order.OrderStatus
import com.example.roamly.entity.DTO.order.UpdateOrderStatusRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OwnerOrdersViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _orders = MutableStateFlow<List<OrderDto>>(emptyList())
    val orders: StateFlow<List<OrderDto>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus: StateFlow<String?> = _selectedStatus.asStateFlow()

    fun fetchEstablishmentOrders(establishmentId: Long, status: String? = null) {
        _isLoading.value = true
        _errorMessage.value = null
        _selectedStatus.value = status

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ordersList = apiService.getEstablishmentOrders(establishmentId, status)
                withContext(Dispatchers.Main) {
                    _orders.value = ordersList
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Ошибка загрузки заказов: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun updateOrderStatus(orderId: Long, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val statusEnum = OrderStatus.valueOf(newStatus)
                val request = UpdateOrderStatusRequest(statusEnum)
                val updatedOrder = apiService.updateOrderStatus(orderId, request)

                // Обновляем локальный список
                withContext(Dispatchers.Main) {
                    _orders.value = _orders.value.map { order ->
                        if (order.id == orderId) updatedOrder else order
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Ошибка обновления статуса: ${e.message}"
                }
            }
        }
    }

    fun setStatusFilter(status: String?) {
        _selectedStatus.value = status
    }
}