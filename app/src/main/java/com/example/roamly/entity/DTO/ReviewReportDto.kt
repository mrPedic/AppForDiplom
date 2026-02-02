package com.example.roamly.entity.DTO

import com.example.roamly.entity.classes.ReviewEntity

data class ReviewReportDto(
    val id: Long? = null,
    val reviewId: Long,
    val userId: Long,
    val establishmentId: Long,
    val reason: String,
    val description: String? = null,

    // Поля для отображения в админке (ReadOnly)
    val reviewText: String? = null,
    val reviewRating: Double? = null,
    val reviewAuthorName: String? = null,
    val reviewPhoto: String? = null, // Base64
    val establishmentName: String? = null,
    val reportDate: String? = null
)