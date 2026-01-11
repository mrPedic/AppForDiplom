package com.example.roamly.entity.DTO

data class OrderNotificationDto(
    val id: Long,
    val orderId: Long,
    val userId: Long,
    val establishmentId: Long,
    val notificationType: String,
    val message: String,
    val createdAt: String,
    val isRead: Boolean
)