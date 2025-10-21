package com.example.roamly.ui.screens.sealed

sealed class EstablishmentScreens(val route: String) {
    object CreateEstablishment: EstablishmentScreens(route = "create_establishment")
    object MapPicker: EstablishmentScreens(route = "map_picker_route")
    object UserEstablishments: EstablishmentScreens(route = "user_establishments")
    object EstablishmentDetail : EstablishmentScreens("establishment_detail/{id}") {
        fun createRoute(id: Long) = "establishment_detail/$id"
    }
    object EstablishmentEdit : EstablishmentScreens("establishment_edit/{id}") {
        fun createRoute(id: Long) = "establishment_edit/$id"
    }
}