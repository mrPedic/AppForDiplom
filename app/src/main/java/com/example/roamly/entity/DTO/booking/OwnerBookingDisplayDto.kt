// OwnerBookingDisplayDto.kt
package com.example.roamly.entity.DTO.booking

import com.example.roamly.LocalDateTimeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

/**
 * Специальное DTO для экрана "Одобрение броней" — содержит всю нужную информацию владельцу
 */
data class OwnerBookingDisplayDto(
    val id: Long,
    val establishmentId: Long,
    val establishmentName: String,
    val userId: Long,
    val userName: String,
    val userPhone: String?,
    val tableNumber: Int,
    val numberOfGuests: Int,
    @SerializedName("startTime")
    @JsonAdapter(LocalDateTimeAdapter::class)
    val startTime: LocalDateTime,
    @SerializedName("endTime")
    @JsonAdapter(LocalDateTimeAdapter::class)
    val endTime: LocalDateTime,
    val status: BookingStatus
)