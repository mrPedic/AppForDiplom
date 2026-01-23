package com.example.roamly.ui.screens.sealed

sealed class ProfileScreens(val route: String) {
    object Login: ProfileScreens(route = "login")
    object SingUp:  ProfileScreens(route = "sing_up")

    object EditProfile: ProfileScreens(route = "edit_profile")

}