package com.example.roamly.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.roamly.ui.screens.AdminPanelScreen
import com.example.roamly.ui.screens.BookingScreen
import com.example.roamly.ui.screens.HomeScreen
import com.example.roamly.ui.screens.SearchScreen
import com.example.roamly.ui.screens.profileFR.SingUpScreen
import com.example.roamly.ui.screens.profileFR.ProfileScreenRegistered
import com.example.roamly.ui.screens.profileFR.ProfileScreenUnRegistered
import com.example.roamly.ui.screens.profileFR.LoginScreen
import com.example.roamly.ui.screens.sealed.ButtonBarScreens
import com.example.roamly.ui.screens.sealed.LogSinUpScreens

@Composable
fun BottomNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = ButtonBarScreens.Home.route,
        modifier = modifier
    ) {
        // Основные вкладки
        composable(ButtonBarScreens.Home.route) {
            HomeScreen(navController)
        }
        composable(ButtonBarScreens.Booking.route) {
            BookingScreen(navController)
        }
        composable(ButtonBarScreens.Searching.route) {
            SearchScreen(navController)
        }

        // Экраны профиля
        composable(ButtonBarScreens.RegisteredProfile.route) {
            ProfileScreenRegistered(navController)
        }
        composable(ButtonBarScreens.UnRegisteredProfile.route) {
            ProfileScreenUnRegistered(navController)
        }

        // Экраны авторизации
        composable(LogSinUpScreens.Login.route) {
            SingUpScreen(navController)
        }
        composable(LogSinUpScreens.SingUp.route) {
            LoginScreen(navController)
        }

        // Админ панель
        composable(ButtonBarScreens.AdminPanel.route) {
            AdminPanelScreen(navController)
        }
    }
}
