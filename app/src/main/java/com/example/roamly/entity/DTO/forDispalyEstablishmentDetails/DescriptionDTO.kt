package com.example.roamly.entity.DTO.forDispalyEstablishmentDetails

import com.example.roamly.entity.classes.TypeOfEstablishment

data class DescriptionDTO(
    val name: String,
    val description: String,
    val address: String,
    val rating: Double,
    val type: TypeOfEstablishment,
    val operatingHoursString: String?,
    val dateOfCreation: String
)