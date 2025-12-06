package com.example.roamly.entity.DTO

data class BookingCreationDto(
    val establishmentId: Long,
    val userId: Long,
    val tableId: Long,
    val startTime: String,
    val durationMinutes: Long,
    val numberOfGuests: Int,
    val comment: String? = null
)