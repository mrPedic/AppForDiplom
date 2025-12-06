package com.example.roamly.ui.screens.sealed

import com.example.roamly.entity.DTO.EstablishmentDisplayDto

// Внутри EstablishmentViewModel.kt или в отдельном файле
sealed class EstablishmentLoadState {
    object Idle : EstablishmentLoadState()
    object Loading : EstablishmentLoadState()
    data class Success(val establishment: EstablishmentDisplayDto) : EstablishmentLoadState()
    data class Error(val message: String) : EstablishmentLoadState()
    object NotFound : EstablishmentLoadState() // Специально для 404
}