package com.example.roamly.entity

import java.time.LocalDate

data class EstablishmentEntity(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val description: String,
    val rating: Double = 0.0,
    val dateOfCreation: String,
    val menuId: Long = -1,
    val createdUserId: Long,
    // ⭐ Добавим поле для статуса заведения
    val status: EstablishmentStatus
) {

}

enum class EstablishmentStatus {
    // На рассмотрении администрации
    PENDING_APPROVAL,
    // Отклонено
    REJECTED,
    // Временно неактивно
    DISABLED,
    APPROVED
}