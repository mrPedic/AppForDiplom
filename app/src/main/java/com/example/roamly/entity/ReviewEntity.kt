package com.example.roamly.entity

import com.google.gson.annotations.SerializedName

/**
 * Сущность для отправки нового отзыва на сервер.
 * Сервер должен будет сам проставить id, dateOfCreation и, возможно, фото (если вы будете загружать их отдельно).
 */
data class ReviewEntity(
    val id: Long = 0,
    val establishmentId: Long,
    val createdUserId: Long,
    val rating: Float, // Оценка 1.0 - 5.0
    val reviewText: String,
    val photoBase64: String? = null, // Фото в Base64 (опционально)
    val dateOfCreation: String = ""
)

// DTO для отображения (если нужно)
// data class ReviewDisplayDto(...) // Можем использовать ReviewEntity для простоты

/**
 * DTO для отображения отзыва в приложении
 */
data class ReviewDisplayDto(
    val id: Long,
    val userName: String, // Имя пользователя, оставившего отзыв
    val establishmentId: Long,
    val rating: Float,
    val reviewText: String,
    val photoUrl: String? = null,
    val dateOfCreation: String
)