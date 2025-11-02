package com.example.roamly.ui.screens.sealed

sealed class BookingScreens(val route : String) {
    // ⭐ Добавляем ключ для NavArgument
    companion object {
        const val ESTABLISHMENT_ID_KEY = "establishmentId"
    }

    // ⭐ Объект CreateBooking теперь правильно принимает аргумент в route
    object CreateBooking: BookingScreens("booking/create/{$ESTABLISHMENT_ID_KEY}"){
        fun createRoute(establishmentId: Long): String = "booking/create/$establishmentId"
    }
}