package com.example.roamly.ui.screens.sealed

sealed class BookingScreens(val route : String) {
    companion object {
        const val ESTABLISHMENT_ID_KEY = "establishmentId"
        const val BOOKING_ID_KEY = "bookingId"
    }

    object CreateBooking: BookingScreens("booking/create/{$ESTABLISHMENT_ID_KEY}"){
        fun createRoute(establishmentId: Long): String = "booking/create/$establishmentId"
    }

    object UserBookings : BookingScreens("bookings/list")

    object BookingDetail : BookingScreens("booking/detail/{${BOOKING_ID_KEY}}") {
        fun createRoute(id: Long) = "booking/detail/$id"
    }

}