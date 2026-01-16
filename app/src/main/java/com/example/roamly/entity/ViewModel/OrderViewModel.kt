// OrderViewModel.kt
package com.example.roamly.entity.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.entity.DTO.order.CreateOrderRequest
import com.example.roamly.entity.DTO.order.DeliveryAddressDto
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
class OrderViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _userOrders = MutableStateFlow<List<OrderDto>>(emptyList())
    val userOrders: StateFlow<List<OrderDto>> = _userOrders.asStateFlow()

    private val _establishmentOrders = MutableStateFlow<List<OrderDto>>(emptyList())
    val establishmentOrders: StateFlow<List<OrderDto>> = _establishmentOrders.asStateFlow()

    private val _deliveryAddresses = MutableStateFlow<List<DeliveryAddressDto>>(emptyList())
    val deliveryAddresses: StateFlow<List<DeliveryAddressDto>> = _deliveryAddresses.asStateFlow()

    private val _currentOrder = MutableStateFlow<OrderDto?>(null)
    val currentOrder: StateFlow<OrderDto?> = _currentOrder.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Загрузка заказов пользователя
    fun loadUserOrders(userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val orders = withContext(Dispatchers.IO) {
                    apiService.getUserOrders(userId)
                }
                _userOrders.value = orders
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки заказов: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Загрузка заказов заведения
    fun loadEstablishmentOrders(establishmentId: Long, status: OrderStatus? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val orders = withContext(Dispatchers.IO) {
                    apiService.getEstablishmentOrders(
                        establishmentId,
                        status?.name
                    )
                }
                _establishmentOrders.value = orders
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки заказов: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Обновление статуса заказа
    fun updateOrderStatus(orderId: Long, status: OrderStatus, rejectionReason: String? = null) {
        viewModelScope.launch {
            try {
                val request = UpdateOrderStatusRequest(status, rejectionReason)
                val updatedOrder = withContext(Dispatchers.IO) {
                    apiService.updateOrderStatus(orderId, request)
                }

                // Обновляем локальный список
                _userOrders.value = _userOrders.value.map {
                    if (it.id == orderId) updatedOrder else it
                }

                // Отправка уведомления пользователю
                val statusMessage = when (status) {
                    OrderStatus.CONFIRMED -> "Заказ подтвержден"
                    OrderStatus.REJECTED -> "Заказ отклонен: $rejectionReason"
                    else -> "Статус заказа изменен на: ${status.name}"
                }

                sendOrderNotification(
                    orderId = orderId,
                    userId = updatedOrder.userId,
                    establishmentId = updatedOrder.establishmentId,
                    message = statusMessage
                )
            } catch (e: Exception) {
                _error.value = "Ошибка обновления статуса: ${e.message}"
            }
        }
    }

    private suspend fun sendOrderNotification(
        orderId: Long,
        userId: Long,
        establishmentId: Long,
        message: String
    ) {
        try {
            val notification = ApiService.OrderNotification(
                orderId = orderId,
                userId = userId,
                establishmentId = establishmentId,
                notificationType = "ORDER_STATUS_CHANGED",
                message = message
            )
            withContext(Dispatchers.IO) {
                apiService.sendOrderNotification(notification)
            }
        } catch (e: Exception) {
            // Логируем ошибку, но не прерываем основной поток
            println("Ошибка отправки уведомления: ${e.message}")
        }
    }

    fun clearError() {
        _error.value = null
    }

    private suspend fun sendOrderNotification(
        orderId: Long,
        userId: Long,
        establishmentId: Long,
        notificationType: String,
        message: String
    ) {
        try {
            val notification = ApiService.OrderNotification(
                orderId = orderId,
                userId = userId,
                establishmentId = establishmentId,
                notificationType = notificationType,
                message = message
            )
            withContext(Dispatchers.IO) {
                apiService.sendOrderNotification(notification)
            }
        } catch (e: Exception) {
            println("Ошибка отправки уведомления: ${e.message}")
        }
    }

    fun createOrder(request: CreateOrderRequest, onSuccess: (OrderDto) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val order = withContext(Dispatchers.IO) {
                    apiService.createOrder(request)
                }
                _currentOrder.value = order

                // Отправляем уведомление администратору заведения
                sendOrderNotification(
                    orderId = order.id!!,
                    userId = order.userId,
                    establishmentId = order.establishmentId,
                    notificationType = "ORDER_CREATED",
                    message = "Создан новый заказ №${order.id}"
                )

                // Добавляем в список заказов пользователя
                _userOrders.value = _userOrders.value + order

                onSuccess(order)
            } catch (e: Exception) {
                _error.value = "Ошибка создания заказа: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

