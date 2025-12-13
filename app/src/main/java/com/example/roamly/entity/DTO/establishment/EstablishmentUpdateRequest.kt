package com.example.roamly.entity.DTO.establishment

import com.example.roamly.entity.classes.TypeOfEstablishment

data class EstablishmentUpdateRequest(
    val name: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val type: TypeOfEstablishment,
    val photoBase64s: List<String>, // Список Base64 строк
    val operatingHoursString: String? // Строка расписания
)