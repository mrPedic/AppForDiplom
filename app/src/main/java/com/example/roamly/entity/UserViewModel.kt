package com.example.roamly.entity

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.factory.RetrofitFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow // ✅ ИМПОРТ: Лучшая практика для сокрытия MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    // --- State ---

    // ✅ УЛУЧШЕНИЕ: Приватный MutableStateFlow, публичный StateFlow через asStateFlow()
    private val _user = MutableStateFlow(User())
    val user = _user.asStateFlow() // Публичное, только для чтения, представление

    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected = _isServerConnected.asStateFlow()

    // --- SharedPreferences ---
    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences("user_prefs", Application.MODE_PRIVATE)
    }
    // ✅ УЛУЧШЕНИЕ: Использование object для хранения констант — лучшая организация
    private object PrefKeys {
        const val ID = "user_id"
        const val NAME = "user_name"
        const val LOGIN = "user_login"
        const val ROLE = "user_role"
        const val EMAIL = "user_email"
    }

    // --- Initialization ---

    init {
        checkServerConnection()
        loadUser()
    }

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
        viewModelScope.launch { // ✅ УЛУЧШЕНИЕ: withContext для переключения диспатчеров
            try {
                val newUser = User(name = name, login = login, password = password, email = email)
                // Сетевой вызов в IO потоке
                val newId = withContext(Dispatchers.IO) {
                    apiService.createUser(newUser)
                }

                val registeredUser = newUser.copy(id = newId, role = Role.Registered, password = "")

                _user.value = registeredUser
                saveUserToPrefs(registeredUser)
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
                // Сетевой вызов в IO потоке
                val response = withContext(Dispatchers.IO) {
                    apiService.loginUser(loginData)
                }

                if (response != null) {
                    val loggedInUser = response.copy(password = "")
                    _user.value = loggedInUser
                    saveUserToPrefs(loggedInUser)
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
        _user.value = User() // Сброс состояния
        prefs.edit().clear().apply()
    }

    fun updateRole(newRole: Role) {
        _user.update { currentUser ->
            currentUser.copy(role = newRole).also { updatedUser ->
                saveUserToPrefs(updatedUser) // ✅ ИСПРАВЛЕНИЕ: Сохраняем обновленного пользователя
            }
        }
    }

    // --- State-derived getters ---

    fun getId(): Long? = user.value.id.takeIf { it != -1L }
    fun isLoggedIn(): Boolean = user.value.role != Role.UnRegistered
    fun isAdmin(): Boolean = user.value.role == Role.AdminOfApp || user.value.role == Role.AdminOfInstitution
    fun getAllData(): String = user.value.let {
        "Name: ${it.name}, Role: ${it.role}, Login: ${it.login}, Email: ${it.email}, Password: [HIDDEN], Id: ${it.id}"
    }

    // --- Private Helpers ---

    private fun saveUserToPrefs(user: User) {
        prefs.edit().apply {
            putLong(PrefKeys.ID, user.id ?: -1L)
            putString(PrefKeys.NAME, user.name)
            putString(PrefKeys.LOGIN, user.login)
            putString(PrefKeys.ROLE, user.role.name)
            putString(PrefKeys.EMAIL, user.email)
            apply()
        }
    }

    private fun loadUser() {
        // ✅ УЛУЧШЕНИЕ: Блок `runCatching` для безопасной загрузки
        runCatching {
            val id = prefs.getLong(PrefKeys.ID, -1L)
            if (id == -1L) {
                _user.value = User() // Пользователь не сохранен
                return
            }

            val roleString = prefs.getString(PrefKeys.ROLE, Role.UnRegistered.name)!!
            _user.value = User(
                id = id,
                name = prefs.getString(PrefKeys.NAME, "")!!,
                login = prefs.getString(PrefKeys.LOGIN, "")!!,
                email = prefs.getString(PrefKeys.EMAIL, "")!!,
                password = "", // Пароль никогда не загружаем
                role = Role.valueOf(roleString)
            )
        }.onFailure { e ->
            Log.e("UserViewModel", "Ошибка загрузки пользователя из SharedPreferences: ${e.message}")
            _user.value = User() // В случае ошибки сбрасываем до состояния по умолчанию
            prefs.edit().clear().apply() // Очищаем некорректные данные
        }
    }

    // ✅ УЛУЧШЕНИЕ: Ленивая инициализация apiService
    private val apiService by lazy {
        RetrofitFactory.create()
    }
}
