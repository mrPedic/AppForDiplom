// Updated AdminScreens.kt
package com.example.roamly.ui.screens.sealed

sealed class AdminScreens(val route: String) {
    object PendingList : AdminScreens("admin/pending")
    object Notifications : AdminScreens("admin/notifications")
    object Reports : AdminScreens("admin/reports")

    object SqlList : AdminScreens("admin/sql_list")
    object SqlDetail : AdminScreens("admin/sql_detail/{queryId}") {
        fun createRoute(queryId: Long) = "admin/sql_detail/$queryId"
    }
}