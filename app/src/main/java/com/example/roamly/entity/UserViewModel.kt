package com.example.roamly.entity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.roamly.factory.RetrofitFactory
import com.example.roamly.fetcher.GistFetcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor() : ViewModel() {

    var user by mutableStateOf(User())
        private set

    fun registerUser(name: String, login: String, password: String, onResult: (User?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseUrl = GistFetcher.fetchBaseUrl() ?: run {
                    withContext(Dispatchers.Main) { onResult(null) }
                    return@launch
                }
                val service = RetrofitFactory.create(baseUrl)
                val newUser = User(name = name, login = login, password = password)
                val createdUser = service.createUser(newUser)

                withContext(Dispatchers.Main) {
                    user = createdUser
                    onResult(createdUser)
                }
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
                val baseUrl = GistFetcher.fetchBaseUrl() ?: run {
                    withContext(Dispatchers.Main) { onResult(null) }
                    return@launch
                }
                val service = RetrofitFactory.create(baseUrl)

                val response = service.loginUser(User(login = login, password = password))
                withContext(Dispatchers.Main) {
                    if (response != null) {
                        user = response
                        onResult(response)
                    } else {
                        onResult(null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("UserViewModel", "Ошибка авторизации: ${e.message}")
                    onResult(null)
                }
            }
        }
    }


    fun userIsExists(
        email: String,
    ){

    }

    fun logout() {
        user = User()
    }

    fun login(name: String, login: String, password: String, role: Role = Role.Registered) {
        user = User(name = name, login = login, password = password, role = role)
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
