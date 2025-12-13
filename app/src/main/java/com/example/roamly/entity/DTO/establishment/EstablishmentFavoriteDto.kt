package com.example.roamly.entity.DTO.establishment

import com.example.roamly.entity.classes.TypeOfEstablishment

data class EstablishmentFavoriteDto(
    val id: Long,
    val name: String,
    val address: String,
    val rating: Double,
    val type: TypeOfEstablishment,
    val photoBase64: String? // Главное фото (или null)
)