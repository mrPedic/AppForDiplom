package com.example.roamly.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.roamly.ui.screens.AdminPanelScreen
import com.example.roamly.ui.screens.BookingScreen
import com.example.roamly.ui.screens.HomeScreen
import com.example.roamly.ui.screens.SearchScreen
import com.example.roamly.ui.screens.profileFR.SingUpScreen
import com.example.roamly.ui.screens.profileFR.LoginScreen
import com.example.roamly.ui.screens.sealed.SealedButtonBar
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.admin.PendingListScreen
import com.example.roamly.ui.screens.booking.CreateBooking
import com.example.roamly.ui.screens.establishment.CreateEstablishmentScreen
import com.example.roamly.ui.screens.establishment.EstablishmentDetailScreen
import com.example.roamly.ui.screens.establishment.EstablishmentEditScreen
import com.example.roamly.ui.screens.establishment.MapPickerScreen
import com.example.roamly.ui.screens.establishment.ReviewCreationScreen
import com.example.roamly.ui.screens.establishment.UserEstablishmentsScreen
import com.example.roamly.ui.screens.profileFR.ProfileScreen
import com.example.roamly.ui.screens.sealed.AdminScreens
import com.example.roamly.ui.screens.sealed.BookingScreens
import com.example.roamly.ui.screens.sealed.EstablishmentScreens

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel,
    mapRefreshKey: Boolean // ⭐ ДОБАВЛЕНО: Ключ для принудительного обновления карты

) {
    NavHost(
        navController = navController,
        startDestination = SealedButtonBar.Home.route,
        modifier = modifier
    ) {
        // --- Основные вкладки ---
        composable(SealedButtonBar.Home.route) {
            HomeScreen(navController,mapRefreshKey )
        }
        composable(SealedButtonBar.Booking.route) {
            BookingScreen(navController)
        }
        composable(SealedButtonBar.Searching.route) {
            SearchScreen(navController)
        }

        // --- ЕДИНЫЙ ЭКРАН ПРОФИЛЯ ---
        // Предполагаем, что этот роут используется в BottomBar
        composable(SealedButtonBar.Profile.route) {
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
        composable(SealedButtonBar.AdminPanel.route) {
            AdminPanelScreen(navController)
        }

        // --- Создание заведения ---
        composable(EstablishmentScreens.CreateEstablishment.route){
            CreateEstablishmentScreen(navController,userViewModel)
        }
        composable(EstablishmentScreens.MapPicker.route){
            MapPickerScreen(navController)
        }
        composable(EstablishmentScreens.UserEstablishments.route){
            UserEstablishmentsScreen(navController, userViewModel)
        }

        // Просмотре и редактирование заведения

        composable(
            route = EstablishmentScreens.EstablishmentDetail.route, // "establishment/detail/{establishmentId}"
            arguments = listOf(navArgument("establishmentId") { type = NavType.LongType })
        ) { backStackEntry ->
            // Also update the argument retrieval for clarity, though 'id' works here if you use the correct name below
            val establishmentId = backStackEntry.arguments?.getLong("establishmentId") ?: return@composable
            EstablishmentDetailScreen(navController, establishmentId) // Assuming the function expects a Long
        }

        composable(
            route = EstablishmentScreens.EstablishmentEdit.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            EstablishmentEditScreen(navController, id)
        }

        composable(
            route = EstablishmentScreens.ReviewCreation.route, // "establishment/review/{establishmentId}"
            arguments = listOf(
                navArgument(EstablishmentScreens.ReviewCreation.ESTABLISHMENT_ID_KEY) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getLong(EstablishmentScreens.ReviewCreation.ESTABLISHMENT_ID_KEY)

            // Защита, если establishmentId не найден (хотя вряд ли)
            if (establishmentId != null) {
                ReviewCreationScreen(
                    navController = navController,
                    establishmentId = establishmentId
                )
            } else {
                // Обработка ошибки или возврат назад
                navController.popBackStack()
            }
        }

        // Админские фишки
        composable(AdminScreens.PendingList.route){
            PendingListScreen()
        }

        // =====================================
        // ⭐ НОВЫЙ ЭКРАН: Создание бронирования
        // =====================================
        composable(
            route = BookingScreens.CreateBooking.route,
            arguments = listOf(
                navArgument(BookingScreens.ESTABLISHMENT_ID_KEY) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getLong(BookingScreens.ESTABLISHMENT_ID_KEY)

            if (establishmentId != null) {
                // ⭐ Вызываем композабл для создания бронирования
                CreateBooking(
                    navController = navController,
                    establishmentId = establishmentId
                )
            } else {
                // Обработка ошибки
                navController.popBackStack()
            }
        }
    }
}