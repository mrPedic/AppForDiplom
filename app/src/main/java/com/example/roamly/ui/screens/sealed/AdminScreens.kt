package com.example.roamly.ui.screens.sealed

sealed class AdminScreens(val route: String) {
    object PendingList : AdminScreens("admin/pending")
    object AllEstablishments : AdminScreens("admin/establishments")
    object UsersManagement : AdminScreens("admin/users")
    object Statistics : AdminScreens("admin/statistics")
    object CategoriesManagement : AdminScreens("admin/categories")
    object AllBookings : AdminScreens("admin/bookings")
    object ReviewsModeration : AdminScreens("admin/reviews")
    object SystemSettings : AdminScreens("admin/settings")
    object Promotions : AdminScreens("admin/promotions")
    object Reports : AdminScreens("admin/reports")
    object Notifications : AdminScreens("admin/notifications")
    object Backup : AdminScreens("admin/backup")

    object SqlList : AdminScreens("admin/sql_list")
    object SqlDetail : AdminScreens("admin/sql_detail/{queryId}") {
        fun createRoute(queryId: Long) = "admin/sql_detail/$queryId"
    }
}