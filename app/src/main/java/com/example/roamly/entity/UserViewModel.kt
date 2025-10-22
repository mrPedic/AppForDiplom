package com.example.roamly.entity

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.factory.RetrofitFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow // ⭐ ИМПОРТ: Для реактивного состояния
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update // ⭐ ИМПОРТ: Для безопасного обновления StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import android.content.SharedPreferences

@HiltViewModel
class UserViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    // ⭐ ИСПРАВЛЕНИЕ: Используем StateFlow для асинхронного наблюдения за состоянием.
    private val _user = MutableStateFlow(User())
    val user: StateFlow<User> = _user // Публичное неизменяемое представление

    private val apiService = RetrofitFactory.create()

    // StateFlow для статуса подключения к серверу
    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected: StateFlow<Boolean> = _isServerConnected

    fun checkServerConnection() = viewModelScope.launch(Dispatchers.IO) {
        val isConnected = try {
            val response = apiService.pingServer()
            response.trim().equals("pong", ignoreCase = true)
        } catch (e: Exception) {
            Log.e("ConnectionCheck", "Ошибка подключения к серверу: ${e.message}")
            false
        }
        _isServerConnected.value = isConnected
    }

    // --- SharedPreferences Logic ---
    private val PREFS_NAME = "user_prefs"
    private val prefs: SharedPreferences = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)
    private val KEY_ID = "user_id"
    private val KEY_NAME = "user_name"
    private val KEY_LOGIN = "user_login"
    private val KEY_ROLE = "user_role"
    private val KEY_EMAIL = "user_email" // ⭐ НОВЫЙ КЛЮЧ

    private fun saveUser(user: User) {
        prefs.edit().apply {
            putLong(KEY_ID, user.id ?: -1L)
            putString(KEY_NAME, user.name)
            putString(KEY_LOGIN, user.login)
            putString(KEY_ROLE, user.role.name)
            putString(KEY_EMAIL, user.email) // ⭐ СОХРАНЕНИЕ EMAIL
            apply()
        }
    }

    init{
        loadUser()
    }

    private fun loadUser() {
        val id = prefs.getLong(KEY_ID, -1L)

        if (id != -1L) {
            val name = prefs.getString(KEY_NAME, "") ?: ""
            val login = prefs.getString(KEY_LOGIN, "") ?: ""
            val email = prefs.getString(KEY_EMAIL, "") ?: "" // ⭐ ЗАГРУЗКА EMAIL
            val password = ""

            val roleString = prefs.getString(KEY_ROLE, Role.UnRegistered.name) ?: Role.UnRegistered.name
            val role = try {
                Role.valueOf(roleString)
            } catch (e: IllegalArgumentException) {
                Role.UnRegistered
            }

            _user.value = User(
                id = id,
                name = name,
                login = login,
                password = password,
                role = role,
                email = email // ⭐ УСТАНОВКА EMAIL
            )
        }
        else{
            _user.value = User()
        }
    }


    fun registerUser(name: String, login: String, password: String, email: String, onResult: (User?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Создаем объект с email и паролем для API
                val newUser = User(name = name, login = login, password = password, email = email)
                val newId: Long = apiService.createUser(newUser)

                val registeredUser = newUser.copy(
                    id = newId,
                    role = Role.Registered,
                    password = "" // Очищаем пароль для StateFlow
                )

                withContext(Dispatchers.Main) {
                    _user.value = registeredUser // Обновляем StateFlow
                    saveUser(registeredUser)
                    onResult(registeredUser)
                }
                Log.e("UserViewModelCorrect", getAllData())
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("UserViewModel", "Ошибка регистрации: ${e.message}")
                    onResult(null)
                }
            }
        }
    }

    fun loginUser(login: String, password: String, onResult: (User?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loginData = User(login = login, password = password)
                val response = apiService.loginUser(loginData)

                withContext(Dispatchers.Main) {
                    if (response != null) {
                        // Очищаем пароль в объекте, который идет в StateFlow и SharedPreferences
                        val loggedInUser = response.copy(password = "")
                        _user.value = loggedInUser // Обновляем StateFlow
                        saveUser(loggedInUser)
                        onResult(loggedInUser)
                    } else {
                        onResult(null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.d("UserViewModel", "Ошибка авторизации: ${e.message}")
                    onResult(null)
                }
            }
        }
    }

    // Функция должна принимать email как параметр, если она нужна для проверки
    fun userIsExists(email: String){}

    // ⭐ ОБНОВЛЕНО: Получение данных из StateFlow
    fun getId(): Long?{
        return user.value.id.takeIf { it != null && it != -1L }
    }

    fun logout() {
        _user.value = User() // Обновляем StateFlow
        prefs.edit().clear().apply()
    }

    fun updateRole(newRole: Role) {
        // Безопасно обновляем StateFlow
        _user.update { current ->
            current.copy(role = newRole)
        }
        // ⭐ ДОБАВЛЕНО: Обновляем SharedPreferences для сохранения новой роли
        saveUser(_user.value)
    }

    // ⭐ ОБНОВЛЕНО: Получение данных из StateFlow
    fun isLoggedIn(): Boolean = user.value.role != Role.UnRegistered
    fun isAdmin(): Boolean = user.value.role == Role.AdminOfApp || user.value.role == Role.AdminOfInstitution

    // ⭐ ОБНОВЛЕНО: Получение данных из StateFlow и включение email
    fun getAllData(): String{
        val currentUser = user.value
        return "Name : ${currentUser.name}, Role : ${currentUser.role}, Login : ${currentUser.login}, Email : ${currentUser.email}, Password : ${currentUser.password}, Id : ${currentUser.id}"
    }
}
