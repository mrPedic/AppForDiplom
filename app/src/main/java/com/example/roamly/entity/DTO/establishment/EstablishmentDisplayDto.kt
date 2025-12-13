package com.example.roamly.entity.DTO.establishment

import com.example.roamly.entity.classes.EstablishmentStatus
import com.example.roamly.entity.classes.TypeOfEstablishment

data class EstablishmentDisplayDto(
    val id: Long,
    val name: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val status: EstablishmentStatus,
    val rating: Double,
    val dateOfCreation: String,
    val type: TypeOfEstablishment,
    val menuId: Long,
    val createdUserId: Long,
    val photoBase64s: List<String> = emptyList(),
    val operatingHoursString: String? = null
)