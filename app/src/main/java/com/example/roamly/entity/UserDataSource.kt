package com.example.roamly.data.source

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import com.example.roamly.entity.Role
import com.example.roamly.entity.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Класс-источник данных (DataSource) для управления состоянием сессии пользователя.
 *
 * Является Singleton, что позволяет безопасно внедрять его в любые ViewModels
 * (например, BookingViewModel) для получения текущего ID пользователя,
 * не вызывая ошибку Hilt, связанную с внедрением ViewModel в ViewModel.
 */
@Singleton
class UserDataSource @Inject constructor(
    application: Application
) {
    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences("user_prefs", Application.MODE_PRIVATE)
    }

    private object PrefKeys {
        const val ID = "user_id"
        const val NAME = "user_name"
        const val LOGIN = "user_login"
        const val ROLE = "user_role"
        const val EMAIL = "user_email"
    }

    private val TAG = "UserDataSource"

    private val _currentUser = MutableStateFlow(loadUserFromPrefs())
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()


    /**
     * Возвращает текущий ID пользователя, если он авторизован (ID != -1L).
     */
    val currentUserId: Long?
        get() = _currentUser.value.id.takeIf { it != -1L }

    /**
     * Сохраняет данные пользователя в SharedPreferences и обновляет Flow.
     * Должен вызываться UserViewModel после успешного логина/регистрации.
     */
    fun saveUserState(user: User) {
        prefs.edit().apply {
            putLong(PrefKeys.ID, user.id ?: -1L)
            putString(PrefKeys.NAME, user.name)
            putString(PrefKeys.LOGIN, user.login)
            putString(PrefKeys.ROLE, user.role.name)
            apply()
        }
        // Обновляем Flow, скрывая пароль
        _currentUser.value = user.copy(password = "")
        Log.d(TAG, "Состояние пользователя сохранено и обновлено. ID: ${user.id}")
    }

    /**
     * Очищает данные пользователя из SharedPreferences и сбрасывает Flow.
     * Должен вызываться UserViewModel при выходе из системы.
     */
    fun clearUserState() {
        prefs.edit().clear().apply()
        _currentUser.value = User() // Сброс до неавторизованного состояния
        Log.d(TAG, "Состояние пользователя очищено.")
    }

    /**
     * ⭐ НОВЫЙ МЕТОД: Обновляет роль текущего пользователя и сохраняет состояние.
     * Необходим для устранения ошибки Unresolved reference в UserViewModel.
     */
    fun updateRole(newRole: Role) {
        // Создаем обновленный объект User
        val updatedUser = _currentUser.value.copy(role = newRole)

        // Сохраняем только роль в SharedPreferences (остальные данные не меняются)
        prefs.edit().apply {
            putString(PrefKeys.ROLE, newRole.name)
            apply()
        }

        // Обновляем Flow
        _currentUser.value = updatedUser
        Log.d(TAG, "Роль пользователя обновлена до: ${newRole}")
    }

    // ------------------------------------------------------------------
    // ⭐ Private Helpers
    // ------------------------------------------------------------------

    /**
     * Загружает данные пользователя при инициализации класса.
     */
    private fun loadUserFromPrefs(): User {
        return runCatching {
            val id = prefs.getLong(PrefKeys.ID, -1L)
            if (id == -1L) {
                return User() // Неавторизованный пользователь
            }

            val roleString = prefs.getString(PrefKeys.ROLE, Role.UnRegistered.name)!!

            // Загружаем данные из SharedPreferences
            User(
                id = id,
                name = prefs.getString(PrefKeys.NAME, "")!!,
                login = prefs.getString(PrefKeys.LOGIN, "")!!,
                password = "", // Пароль не хранится и не загружается
                role = Role.valueOf(roleString)
            )
        }.onFailure { e ->
            Log.e(TAG, "Ошибка загрузки пользователя из SharedPreferences: ${e.message}")
            prefs.edit().clear().apply() // Очищаем некорректные данные
        }.getOrDefault(User())
    }
}