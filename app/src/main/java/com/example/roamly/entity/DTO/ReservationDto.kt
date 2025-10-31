package com.example.roamly.entity.DTO

import java.time.LocalDate
import java.time.LocalTime

data class ReservationDto(
    val id: Long? = null,
    val tableId: Long,
    val userId: Long,
    val reservationDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val numberOfGuests: Int,
    val comment: String? = null
)