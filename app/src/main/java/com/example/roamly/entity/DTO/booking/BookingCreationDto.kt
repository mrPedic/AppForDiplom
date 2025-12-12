package com.example.roamly.entity.DTO.booking

import com.google.gson.annotations.SerializedName

data class BookingCreationDto(
    val establishmentId: Long,
    val userId: Long,
    val tableId: Long,
    val startTime: String,  // Формат: "YYYY-MM-DDTHH:MM:SS" (ISO_LOCAL_DATE_TIME)
    val durationMinutes: Long,
    val numPeople: Int,     // Унифицировано с сервером (было numberOfGuests)
    val notes: String? = null  // Унифицировано с сервером (было comment)
)