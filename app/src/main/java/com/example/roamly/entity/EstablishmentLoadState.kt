package com.example.roamly.entity

import com.example.roamly.entity.DTO.establishment.EstablishmentDisplayDto

sealed interface EstablishmentLoadState {
    object Idle : EstablishmentLoadState
    object Loading : EstablishmentLoadState
    data class Success(
        val data: EstablishmentDisplayDto,
        val photos: List<String> = emptyList(),
        val photosLoading: Boolean = false
    ) : EstablishmentLoadState
    data class Error(val message: String) : EstablishmentLoadState
}