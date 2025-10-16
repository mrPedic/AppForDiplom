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
    val dateOfCreation: LocalDate,
    val menuId: Long = -1,
    val createUserId: Long,
    // ⭐ Добавим поле для статуса заведения
    val status: EstablishmentStatus
) {

}

enum class EstablishmentStatus {
    // На рассмотрении администрации
    PENDING_APPROVAL,
    // Активно, одобрено
    ACTIVE,
    // Отклонено
    REJECTED,
    // Временно неактивно
    DISABLED
}