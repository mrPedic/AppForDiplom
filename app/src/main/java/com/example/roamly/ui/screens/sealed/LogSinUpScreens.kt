package com.example.roamly.ui.screens.sealed

sealed class LogSinUpScreens(val route: String) {
    object Login: LogSinUpScreens(route = "login")
    object SingUp:  LogSinUpScreens(route = "sing_up")

}