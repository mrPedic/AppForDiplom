package com.example.roamly.entity

/**
 * Data class, представляющий профиль пользователя.
 * Пароль не сохраняется в SharedPreferences и используется только для API-запросов.
 */
data class User(
    val id: Long? = null,
    val name: String = "",
    val login: String = "",
    val password: String = "", // Используется для API-запросов
    var role: Role = Role.UnRegistered,
    var email: String = "" // ⭐ ДОБАВЛЕНО: Поле электронной почты
)

/**
 * Enum класс, определяющий возможные роли пользователя в приложении.
 */
enum class Role{
    Registered,
    UnRegistered,
    AdminOfInstitution,
    AdminOfApp
}
