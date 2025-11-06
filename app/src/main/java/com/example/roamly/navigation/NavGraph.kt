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
import com.example.roamly.ui.screens.HomeScreen
import com.example.roamly.ui.screens.SearchScreen
import com.example.roamly.ui.screens.profileFR.SingUpScreen
import com.example.roamly.ui.screens.profileFR.LoginScreen
import com.example.roamly.ui.screens.sealed.SealedButtonBar
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.BookingDetailScreen
import com.example.roamly.ui.screens.UserBookingsScreen
import com.example.roamly.ui.screens.admin.PendingListScreen
import com.example.roamly.ui.screens.booking.CreateBooking
import com.example.roamly.ui.screens.establishment.CreateEstablishmentScreen
import com.example.roamly.ui.screens.establishment.EstablishmentDetailScreen
import com.example.roamly.ui.screens.establishment.EstablishmentEditScreen
import com.example.roamly.ui.screens.establishment.MapPickerScreen
// ⭐ ИМПОРТ ЗАГЛУШКИ ДЛЯ НОВОГО ЭКРАНА
import com.example.roamly.ui.screens.establishment.MenuEditScreen
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
            HomeScreen(navController, mapRefreshKey)
        }
        composable(SealedButtonBar.Searching.route) {
            SearchScreen(navController)
        }

        composable(SealedButtonBar.Booking.route) {
            UserBookingsScreen(navController = navController, userViewModel = userViewModel)
        }

        composable(SealedButtonBar.Profile.route) {
            ProfileScreen(navController, userViewModel)
        }

        // --- Экраны авторизации ---
        composable(LogSinUpScreens.SingUp.route) {
            SingUpScreen(navController, userViewModel)
        }
        composable(LogSinUpScreens.Login.route) {
            LoginScreen(navController, userViewModel)
        }

        // --- Админ панель ---
        composable(SealedButtonBar.AdminPanel.route) {
            AdminPanelScreen(navController)
        }

        // --- Создание заведения ---
        composable(EstablishmentScreens.CreateEstablishment.route) {
            CreateEstablishmentScreen(navController, userViewModel)
        }
        composable(EstablishmentScreens.MapPicker.route) {
            MapPickerScreen(navController)
        }
        composable(EstablishmentScreens.UserEstablishments.route) {
            UserEstablishmentsScreen(navController, userViewModel)
        }

        // Просмотр и редактирование заведения

        composable(
            route = EstablishmentScreens.EstablishmentDetail.route,
            arguments = listOf(navArgument("establishmentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getLong("establishmentId") ?: return@composable
            EstablishmentDetailScreen(navController, establishmentId)
        }

        composable(
            route = EstablishmentScreens.EstablishmentEdit.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            EstablishmentEditScreen(navController, id)
        }

        // ⭐ НОВОЕ: ЭКРАН РЕДАКТИРОВАНИЯ МЕНЮ
        composable(
            route = EstablishmentScreens.MenuEdit.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            // Предполагаем, что у вас есть композабл MenuEditScreen
            MenuEditScreen(navController, establishmentId = id) // ⭐ Вызываем новый композабл
        }

        composable(
            route = EstablishmentScreens.ReviewCreation.route,
            arguments = listOf(
                navArgument(EstablishmentScreens.ReviewCreation.ESTABLISHMENT_ID_KEY) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getLong(EstablishmentScreens.ReviewCreation.ESTABLISHMENT_ID_KEY)

            if (establishmentId != null) {
                ReviewCreationScreen(
                    navController = navController,
                    establishmentId = establishmentId
                )
            } else {
                navController.popBackStack()
            }
        }

        // Админские фишки
        composable(AdminScreens.PendingList.route) {
            PendingListScreen()
        }

        // =====================================
        // Создание и просмотр бронирования
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
                CreateBooking(
                    navController = navController,
                    establishmentId = establishmentId
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(
            route = BookingScreens.BookingDetail.route,
            arguments = listOf(
                navArgument(BookingScreens.BOOKING_ID_KEY) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getLong(BookingScreens.BOOKING_ID_KEY)

            if (bookingId != null) {
                BookingDetailScreen(
                    navController = navController,
                    bookingId = bookingId
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}