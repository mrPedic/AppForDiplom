package com.example.roamly.entity.DTO.booking

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.example.roamly.LocalDateTimeAdapter // Импортируем наш кастомный адаптер
import java.time.LocalDateTime

/**
 * DTO для отображения информации о бронировании на экране пользователя.
 */
data class BookingDisplayDto(
    val id: Long,
    val establishmentName: String,
    val establishmentAddress: String,
    val establishmentLatitude: Double, // Для карты
    val establishmentLongitude: Double, // Для карты
    val tableName: String,
    val tableMaxCapacity: Int,
    @SerializedName("startTime") // Убедитесь, что имя соответствует бэкенду
    @JsonAdapter(LocalDateTimeAdapter::class)
    val startTime: LocalDateTime,
    val durationMinutes: Long,
    val status: String // Например, "PENDING", "CONFIRMED", "CANCELLED"
)