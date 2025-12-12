package com.example.roamly.entity.DTO.booking

enum class BookingStatus {
    PENDING,    // Ожидает подтверждения
    CONFIRMED,  // Подтверждено
    CANCELLED,  // Отменено
    COMPLETED,  // Завершено (после того, как клиент посетил заведение)
    NO_SHOW     // Клиент не пришел
}