package com.example.roamly.entity.DTO

import com.example.roamly.entity.TypeOfEstablishment

data class EstablishmentFavoriteDto(
    val id: Long,
    val name: String,
    val address: String,
    val rating: Double,
    val type: TypeOfEstablishment,
    val photoBase64: String? // Главное фото (или null)
)