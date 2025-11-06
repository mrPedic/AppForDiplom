package com.example.roamly.ui.screens.sealed
sealed class SaveStatus {
    object Idle : SaveStatus()      // Ожидание
    object Loading : SaveStatus()   // Идет сохранение
    object Success : SaveStatus()   // Успешно
    data class Error(val message: String) : SaveStatus() // Ошибка
}