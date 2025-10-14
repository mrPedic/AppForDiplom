package com.example.roamly.ui.screens.sealed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector

sealed class ButtonBarScreens (
    val route: String,
    val title: String,
    val icon: ImageVector
){
    object Home: ButtonBarScreens(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )

    object Profile: ButtonBarScreens(
        route = "profile",
        title = "Profile",
        icon = Icons.Default.Person
    )

    object Searching: ButtonBarScreens(
        route = "search",
        title = "Search",
        icon = Icons.Default.Search
    )

    object Booking: ButtonBarScreens(
        route = "booking",
        title = "Booking",
        icon = Icons.Default.Menu
    )

    object AdminPanel: ButtonBarScreens(
        route = "admin_panel",
        title = "AdminPanel",
        icon = Icons.Default.AccountCircle
    )
}