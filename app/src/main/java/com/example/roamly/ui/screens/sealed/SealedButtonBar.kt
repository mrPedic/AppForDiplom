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
        title = "Карта",
        icon = Icons.Default.Home
    )

    object Profile: SealedButtonBar(
        route = "profile",
        title = "Профиль",
        icon = Icons.Default.Person
    )

    object Searching: SealedButtonBar(
        route = "search",
        title = "Поиск",
        icon = Icons.Default.Search
    )

    object Booking: SealedButtonBar(
        route = "booking",
        title = "Бронь",
        icon = Icons.Default.Menu
    )

    object AdminPanel: SealedButtonBar(
        route = "admin_panel",
        title = "Админ-панель",
        icon = Icons.Default.AccountCircle
    )
}