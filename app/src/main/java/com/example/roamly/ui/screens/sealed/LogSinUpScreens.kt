package com.example.roamly.ui.screens.sealed

sealed class LogSinUpScreens(val route: String) {
    object Login: LogSinUpScreens(route = "login")
    object SingUp:  LogSinUpScreens(route = "sing_up")
    object CreateEstablishment: LogSinUpScreens(route = "create_establishment")
    object MapPicker: LogSinUpScreens(route = "map_picker_route")
    object UserEstablishments: LogSinUpScreens(route = "user_establishments")
}