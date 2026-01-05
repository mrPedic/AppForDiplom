package com.example.roamly.ui.screens.sealed

sealed class NotificationScreens(val route: String) {
    object Notifications : NotificationScreens(route = "notifications")

    // Если нужно передавать параметры в будущем
    companion object {
        fun createRoute() = Notifications.route
    }
}