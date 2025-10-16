package com.example.roamly.ui.screens.sealed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class SealedButtonBar (
    val route: String,
    val title: String,
    val icon: ImageVector
){
    object Home: SealedButtonBar(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )

    object Profile: SealedButtonBar(
        route = "profile",
        title = "Profile",
        icon = Icons.Default.Person
    )

    object Searching: SealedButtonBar(
        route = "search",
        title = "Search",
        icon = Icons.Default.Search
    )

    object Booking: SealedButtonBar(
        route = "booking",
        title = "Booking",
        icon = Icons.Default.Menu
    )

    object AdminPanel: SealedButtonBar(
        route = "admin_panel",
        title = "AdminPanel",
        icon = Icons.Default.AccountCircle
    )
}