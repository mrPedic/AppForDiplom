package com.example.roamly.entity

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class UserViewModel : ViewModel() {
    var user by mutableStateOf(User())
        private set

    fun logout() {
        user = User() // сбросить состояние
    }
}