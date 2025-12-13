// BookingCreationDto.kt
package com.example.roamly.entity.DTO.booking

import com.google.gson.annotations.SerializedName

data class BookingCreationDto(
    val establishmentId: Long,
    val userId: Long,
    val tableId: Long,
    val startTime: String,           // ISO: "2025-04-05T18:30:00"
    val durationMinutes: Long,
    val numPeople: Int,
    val notes: String? = null,

    // Новое поле — телефон, который гость указывает при бронировании
    @SerializedName("guestPhone")
    val guestPhone: String
)