package com.example.roamly.entity

data class BookingEntity(
    val id: Long = 0,
    val establishmentId: Long,
    val userId: Long,
    val tableId: Long,
    val dateTime: String, // Время начала
    val durationMinutes: Int = 60 // Длительность
)

data class BookingCreationDto(
    val establishmentId: Long,
    val userId: Long,
    val tableId: Long,
    val startTime: String, // Время начала в формате ISO 8601
    val durationMinutes: Long // Продолжительность
)