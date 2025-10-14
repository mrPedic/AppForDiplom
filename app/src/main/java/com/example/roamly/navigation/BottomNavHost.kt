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
import com.example.roamly.ui.screens.profileFR.LoginScreen
import com.example.roamly.ui.screens.sealed.ButtonBarScreens
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import com.example.roamly.entity.UserViewModel
import com.example.roamly.ui.screens.profileFR.ProfileScreen

@Composable
fun BottomNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel
) {
    NavHost(
        navController = navController,
        startDestination = ButtonBarScreens.Home.route,
        modifier = modifier
    ) {
        // --- Основные вкладки ---
        composable(ButtonBarScreens.Home.route) {
            HomeScreen(navController)
        }
        composable(ButtonBarScreens.Booking.route) {
            BookingScreen(navController)
        }
        composable(ButtonBarScreens.Searching.route) {
            SearchScreen(navController)
        }

        // --- ЕДИНЫЙ ЭКРАН ПРОФИЛЯ ---
        // Предполагаем, что этот роут используется в BottomBar
        composable(ButtonBarScreens.Profile.route) {
            ProfileScreen(navController,userViewModel)
        }

        // --- Экраны авторизации ---
        // ИСПРАВЛЕНО: Теперь SingUp (регистрация) и Login (вход) правильно связаны с роутами
        composable(LogSinUpScreens.SingUp.route) { // SingUp = Регистрация
            SingUpScreen(navController,userViewModel)
        }
        composable(LogSinUpScreens.Login.route) { // Login = Вход
            LoginScreen(navController,userViewModel)
        }

        // --- Админ панель ---
        composable(ButtonBarScreens.AdminPanel.route) {
            AdminPanelScreen(navController)
        }
    }
}