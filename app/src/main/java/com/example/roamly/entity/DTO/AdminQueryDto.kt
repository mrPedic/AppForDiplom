package com.example.roamly.entity.DTO

data class AdminQueryDto(
    val id: Long = 0,
    val title: String,
    val description: String,
    val sqlQuery: String
)