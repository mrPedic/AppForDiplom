package com.example.roamly.entity.DTO.establishment

import com.example.roamly.entity.classes.TypeOfEstablishment

/**
 * Облегченный DTO для отображения заведений на карте (маркеры).
 * Содержит только минимально необходимые данные для отображения маркера и
 * краткой информации при нажатии на него, оптимизируя загрузку.
 */
data class EstablishmentMarkerDto(
    val id: Long,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val type: TypeOfEstablishment,
    val rating: Double,
    val operatingHoursString: String? = null
)