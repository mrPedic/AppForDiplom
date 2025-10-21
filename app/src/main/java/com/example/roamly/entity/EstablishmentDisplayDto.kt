package com.example.roamly.entity

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
    val createdId : Long
)
