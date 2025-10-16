package com.example.roamly.entity

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.roamly.factory.RetrofitFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import android.content.SharedPreferences;

@HiltViewModel
class UserViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private var _user = mutableStateOf(User())
    var user: User
        get() = _user.value
        private set(value) { _user.value = value }

    private val PREFS_NAME = "user_prefs"
    // Используем Application для получения SharedPreferences
    private val prefs: SharedPreferences = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)
    private val KEY_ID = "user_id"
    private val KEY_NAME = "user_name"
    private val KEY_LOGIN = "user_login"
    private val KEY_ROLE = "user_role"

    private fun saveUser(user: User) {
        prefs.edit().apply {
            putLong(KEY_ID, user.id ?: -1L)
            putString(KEY_NAME, user.name)
            putString(KEY_LOGIN, user.login)
            putString(KEY_ROLE, user.role.name)
            apply()
        }
    }
    init{
        loadUser()
    }
    private fun loadUser() {
        val id = prefs.getLong(KEY_ID, -1L)

        // Проверяем, есть ли сохраненный пользователь (например, по ID)
        if (id != -1L) {
            val name = prefs.getString(KEY_NAME, "") ?: ""
            val login = prefs.getString(KEY_LOGIN, "") ?: ""
            // Пароль обычно не сохраняется в SharedPreferences!
            val password = ""

            // Получаем роль и конвертируем из строки в Enum (Role)
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
                password = password, // Пароль пуст, но это безопасно
                role = role
            )
        }
        else{
            _user.value = User()
        }
    }

    // Предполагаем, что RetrofitFactory.create() доступен и создает ApiService
    private val apiService = RetrofitFactory.create()


    fun registerUser(name: String, login: String, password: String, onResult: (User?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newUser = User(name = name, login = login, password = password)
                val newId: Long = apiService.createUser(newUser)
                val registeredUser = newUser.copy(id = newId, role = Role.Registered)

                withContext(Dispatchers.Main) {
                    user = registeredUser
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
                val response = apiService.loginUser(User(login = login, password = password))

                withContext(Dispatchers.Main) {
                    if (response != null) {
                        user = response
                        saveUser(response)
                        onResult(response)
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

    fun userIsExists(email: String){}

    fun getId(): Long?{
        return user.id.takeIf { it != null && it != -1L }
    }

    fun logout() {
        user = User()
        prefs.edit().clear().apply()
    }

    fun updateRole(newRole: Role) {
        user = user.copy(role = newRole)
    }

    fun isLoggedIn(): Boolean = user.role != Role.UnRegistered
    fun isAdmin(): Boolean = user.role == Role.AdminOfApp || user.role == Role.AdminOfInstitution

    fun getAllData(): String{
        return "Name : ${user.name}, Role : ${user.role}, Login : ${user.login}, Password : ${user.password}, Id : ${user.id}"
    }
}
