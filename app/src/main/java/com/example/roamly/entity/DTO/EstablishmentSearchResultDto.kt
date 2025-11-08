package com.example.roamly.entity.DTO

/**
 * Облегченный DTO для отображения заведений в результатах поиска.
 * Содержит только минимально необходимые данные для списка поиска.
 */
data class EstablishmentSearchResultDto(
    val id: Long,
    val name: String,
    val address: String,
    val rating: Double,
)