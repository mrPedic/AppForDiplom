package com.example.roamly.entity.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.data.source.UserDataSource // ⭐ ИМПОРТ НОВОГО ИСТОЧНИКА ДАННЫХ
import com.example.roamly.entity.Role
import com.example.roamly.entity.User
import com.example.roamly.factory.RetrofitFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    // ⭐ ВНЕДРЯЕМ UserDataSource ВМЕСТО ПРЯМОЙ РАБОТЫ С SharedPreferences
    private val userDataSource: UserDataSource,
    // Application оставлен только для checkServerConnection, если он нужен для системных сервисов
    private val application: Application
) : ViewModel() {

    // --- State ---

    // ⭐ ДЕЛЕГИРУЕМ: Состояние пользователя берем из UserDataSource
    // UserDataSource сам загружает данные при инициализации
    val user = userDataSource.currentUser

    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected = _isServerConnected.asStateFlow()

    // --- Initialization ---

    init {
        checkServerConnection()
        // ⭐ УДАЛЕНО: loadUser() больше не нужен, его вызывает UserDataSource в своем конструкторе
    }

    // ⭐ УДАЛЕНО: Приватные поля SharedPreferences (prefs и PrefKeys)
    // ⭐ УДАЛЕНО: Приватные функции saveUserToPrefs и loadUser

    // --- Public API ---

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

    fun registerUser(name: String, login: String, password: String, email: String, onResult: (User?) -> Unit) {
        viewModelScope.launch {
            try {
                val newUser = User(name = name, login = login, password = password, email = email)
                val newId = withContext(Dispatchers.IO) {
                    apiService.createUser(newUser)
                }

                val registeredUser = newUser.copy(id = newId, role = Role.Registered, password = "")

                // ⭐ ДЕЛЕГИРУЕМ: Сохраняем и обновляем состояние через DataSource
                userDataSource.saveUserState(registeredUser)

                onResult(registeredUser)
                Log.d("UserViewModel", "Регистрация успешна: ${getAllData()}")

            } catch (e: Exception) {
                Log.e("UserViewModel", "Ошибка регистрации: ${e.message}")
                onResult(null)
            }
        }
    }

    fun loginUser(login: String, password: String, onResult: (User?) -> Unit) {
        viewModelScope.launch {
            try {
                val loginData = User(login = login, password = password)
                val response = withContext(Dispatchers.IO) {
                    apiService.loginUser(loginData)
                }

                if (response != null) {
                    val loggedInUser = response.copy(password = "")

                    // ⭐ ДЕЛЕГИРУЕМ: Сохраняем и обновляем состояние через DataSource
                    userDataSource.saveUserState(loggedInUser)

                    onResult(loggedInUser)
                    Log.d("UserViewModel", "Авторизация успешна: ${getAllData()}")
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Ошибка авторизации: ${e.message}")
                onResult(null)
            }
        }
    }

    fun logout() {
        // ⭐ ДЕЛЕГИРУЕМ: Сброс состояния и очистка SharedPreferences через DataSource
        userDataSource.clearUserState()
    }

    fun updateRole(newRole: Role) {
        // ⭐ ДЕЛЕГИРУЕМ: Обновление состояния через DataSource
        // Предполагается, что UserDataSource имеет метод для обновления роли
        userDataSource.updateRole(newRole)
    }

    // --- State-derived getters ---

    // ⭐ ДЕЛЕГИРУЕМ: Получаем ID из UserDataSource
    fun getId(): Long? = userDataSource.currentUserId

    // Остальные геттеры работают с Flow, который теперь является прокси DataSource
    fun isLoggedIn(): Boolean = user.value.role != Role.UnRegistered
    fun isAdmin(): Boolean = user.value.role == Role.AdminOfApp || user.value.role == Role.AdminOfInstitution
    fun getAllData(): String = user.value.let {
        "Name: ${it.name}, Role: ${it.role}, Login: ${it.login}, Email: ${it.email}, Password: [HIDDEN], Id: ${it.id}"
    }

    // --- Private Helpers ---

    private val apiService by lazy {
        RetrofitFactory.create()
    }
}