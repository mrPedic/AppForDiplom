package com.example.roamly.ui.screens.sealed

sealed class AdminScreens(val route: String) {
    object PendingList: AdminScreens(route = "admin/pending-list")
}