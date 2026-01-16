package com.example.roamly.entity.DTO.order

import java.time.LocalDateTime

data class OrderNotificationDto(
    val id: Long? = null,
    val orderId: Long? = null,
    val userId: Long? = null,
    val establishmentId: Long? = null,
    val type: OrderNotificationType? = null,
    val message: String? = null,
    val isRead: Boolean = false,
    val createdAt: LocalDateTime? = null
)

enum class OrderNotificationType {
    ORDER_CREATED,
    ORDER_STATUS_CHANGED
}