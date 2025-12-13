package com.example.roamly.ui.screens.sealed

sealed class EstablishmentScreens(val route: String) {
    object CreateEstablishment: EstablishmentScreens(route = "create/establishment")
    object MapPicker: EstablishmentScreens(route = "map/picker_route")
    object UserEstablishments: EstablishmentScreens(route = "user/establishments")

    object EstablishmentEdit : EstablishmentScreens("establishment/edit/{id}") {
        fun createRoute(id: Long) = "establishment/edit/$id"
    }

    object MenuEdit: EstablishmentScreens(route = "establishment/menu/edit/{id}"){

        fun createRoute(id: Long) = "establishment/menu/edit/$id"

    }

    object EstablishmentDetail : EstablishmentScreens("establishment/detail/{establishmentId}") {
        fun createRoute(id: Long?) = "establishment/detail/$id"
    }

    object ReviewCreation : EstablishmentScreens("establishment/review/{establishmentId}") {
        const val ESTABLISHMENT_ID_KEY = "establishmentId"
        fun createRoute(establishmentId: Long): String = "establishment/review/$establishmentId"
    }

    object ApproveBookings : EstablishmentScreens("establishment/approve_bookings")}