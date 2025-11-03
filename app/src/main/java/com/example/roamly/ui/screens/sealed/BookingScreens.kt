package com.example.roamly.ui.screens.sealed

sealed class BookingScreens(val route : String) {
    // ⭐ Добавляем ключ для NavArgument
    companion object {
        const val ESTABLISHMENT_ID_KEY = "establishmentId"
        const val BOOKING_ID_KEY = "bookingId"
    }

    // ⭐ Объект CreateBooking теперь правильно принимает аргумент в route
    object CreateBooking: BookingScreens("booking/create/{$ESTABLISHMENT_ID_KEY}"){
        fun createRoute(establishmentId: Long): String = "booking/create/$establishmentId"
    }

    // ⭐ НОВЫЙ РОУТ: Список бронирований пользователя
    object UserBookings : BookingScreens("bookings/list")

    // ⭐ НОВЫЙ РОУТ: Детали бронирования
    object BookingDetail : BookingScreens("booking/detail/{${BOOKING_ID_KEY}}") {
        fun createRoute(id: Long) = "booking/detail/$id"
    }

}