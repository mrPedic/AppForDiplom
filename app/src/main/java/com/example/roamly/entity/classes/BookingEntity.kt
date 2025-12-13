package com.example.roamly.entity.classes

data class BookingEntity(
    val id: Long = 0,
    val establishmentId: Long,
    val userId: Long,
    val tableId: Long,
    val startTime: String, // Время начала
    val durationMinutes: Int = 60 // Длительность
)