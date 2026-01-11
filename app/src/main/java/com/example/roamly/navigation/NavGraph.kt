package com.example.roamly.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.roamly.entity.ViewModel.OrderViewModel
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
import com.example.roamly.ui.screens.booking.CreateBookingScreen
import com.example.roamly.ui.screens.booking.ApproveBookingsScreen
import com.example.roamly.ui.screens.booking.OwnerApprovedBookingsScreen
import com.example.roamly.ui.screens.booking.OwnerBookingsManagementScreen
import com.example.roamly.ui.screens.establishment.CreateEstablishmentScreen
import com.example.roamly.ui.screens.establishment.EstablishmentDetailScreen
import com.example.roamly.ui.screens.establishment.EstablishmentEditScreen
import com.example.roamly.ui.screens.establishment.MapPickerScreen
import com.example.roamly.ui.screens.establishment.MenuEditScreen
import com.example.roamly.ui.screens.establishment.ReviewCreationScreen
import com.example.roamly.ui.screens.establishment.UserEstablishmentsScreen
import com.example.roamly.ui.screens.order.CreateDeliveryAddressScreen
import com.example.roamly.ui.screens.order.DeliveryAddressScreen
import com.example.roamly.ui.screens.order.EditDeliveryAddressScreen
import com.example.roamly.ui.screens.order.OrderCheckoutScreen
import com.example.roamly.ui.screens.order.OrderCreationMenuScreen
import com.example.roamly.ui.screens.order.OrderCreationScreen
import com.example.roamly.ui.screens.order.OrderListScreen
import com.example.roamly.ui.screens.profileFR.NotificationsScreen
import com.example.roamly.ui.screens.profileFR.ProfileScreen
import com.example.roamly.ui.screens.sealed.AdminScreens
import com.example.roamly.ui.screens.sealed.BookingScreens
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.screens.sealed.NotificationScreens
import com.example.roamly.ui.screens.sealed.OrderScreens

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel,
    orderViewModel: OrderViewModel,
    mapRefreshKey: Boolean,
    onMapRefresh: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = SealedButtonBar.Home.route,
        modifier = modifier
    ) {
        // --- Основные вкладки ---
        composable(SealedButtonBar.Home.route) {
            HomeScreen(navController, mapRefreshKey, onMapRefresh)
        }
        composable(SealedButtonBar.Searching.route) {
            SearchScreen(navController)
        }

        composable(SealedButtonBar.Booking.route) {
            UserBookingsScreen(navController = navController, userViewModel = userViewModel)
        }

        composable(SealedButtonBar.Profile.route) {
            ProfileScreen(navController, userViewModel, orderViewModel)
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

        composable(route = NotificationScreens.Notifications.route) {
            NotificationsScreen(navController = navController)
        }

        // Просмотр и редактирование заведения

        composable(
            route = EstablishmentScreens.EstablishmentDetail.route,
            arguments = listOf(
                navArgument(EstablishmentScreens.EstablishmentDetail.ESTABLISHMENT_ID_KEY) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getLong(
                EstablishmentScreens.EstablishmentDetail.ESTABLISHMENT_ID_KEY
            ) ?: return@composable
            EstablishmentDetailScreen(
                navController = navController,
                establishmentId = establishmentId
            )
        }

        composable(
            route = EstablishmentScreens.EstablishmentEdit.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            EstablishmentEditScreen(navController, id)
        }

        composable(
            route = EstablishmentScreens.MenuEdit.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            MenuEditScreen(navController, establishmentId = id)
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
                CreateBookingScreen(
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

        // Внутри NavHost, где остальные composable
        composable(EstablishmentScreens.ApproveBookings.route) {
            ApproveBookingsScreen(navController = navController, userViewModel = userViewModel)
        }

        // Добавим в NavHost компонуемый файл
        composable(BookingScreens.OwnerBookingsManagement.route) { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getLong("establishmentId") ?: 0L
            OwnerBookingsManagementScreen(
                navController = navController,
                establishmentId = establishmentId
            )
        }

        composable(BookingScreens.OwnerApprovedBookings.route) { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getLong("establishmentId") ?: 0L
            OwnerApprovedBookingsScreen(
                navController = navController,
                establishmentId = establishmentId
            )
        }// Добавим в NavHost компонуемый файл
        composable(BookingScreens.OwnerBookingsManagement.route) { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getLong("establishmentId") ?: 0L
            OwnerBookingsManagementScreen(
                navController = navController,
                establishmentId = establishmentId
            )
        }

        composable(BookingScreens.OwnerApprovedBookings.route) { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getLong("establishmentId") ?: 0L
            OwnerApprovedBookingsScreen(
                navController = navController,
                establishmentId = establishmentId
            )
        }

        // В NavGraph.kt измените обработку параметров:
        composable(BookingScreens.OwnerBookingsManagement.route) { backStackEntry ->
            val establishmentIdStr = backStackEntry.arguments?.getString("establishmentId")
            val establishmentId = establishmentIdStr?.toLongOrNull() ?: 0L

            if (establishmentId == 0L) {
                // Обработка ошибки
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ошибка: неверный ID заведения")
                }
            } else {
                OwnerBookingsManagementScreen(navController = navController, establishmentId = establishmentId)
            }
        }

        composable(BookingScreens.OwnerApprovedBookings.route) { backStackEntry ->
            val establishmentIdStr = backStackEntry.arguments?.getString("establishmentId")
            val establishmentId = establishmentIdStr?.toLongOrNull() ?: 0L

            if (establishmentId == 0L) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ошибка: неверный ID заведения")
                }
            } else {
                OwnerApprovedBookingsScreen(navController = navController, establishmentId = establishmentId)
            }
        }

        // Добавить в навигацию
        composable(OrderScreens.OrderCreation.route) { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getString("establishmentId")?.toLongOrNull()
            establishmentId?.let {
                OrderCreationScreen(navController, it)
            }
        }

        composable("order/checkout/{establishmentId}") { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getString("establishmentId")?.toLongOrNull()
            val userId = userViewModel.getId()

            if (establishmentId != null && userId != null) {
                OrderCheckoutScreen(navController, establishmentId, userId)
            }
        }

        composable(OrderScreens.OrderList.route) {
            OrderListScreen(navController, userViewModel)
        }

        composable(OrderScreens.OrderDetails.route) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")?.toLongOrNull()
            orderId?.let {
                // TODO: Создать OrderDetailScreen
            }
        }

        // Добавьте эти маршруты в NavGraph

        composable(OrderScreens.OrderCreation.route) { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getString("establishmentId")?.toLongOrNull()
            establishmentId?.let {
                OrderCreationMenuScreen(navController, it)
            }
        }

        composable("order/checkout/{establishmentId}") { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getString("establishmentId")?.toLongOrNull()
            val userId = userViewModel.getId()

            if (establishmentId != null && userId != null) {
                OrderCheckoutScreen(navController, establishmentId, userId)
            }
        }

        composable("orders/delivery-addresses/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull()
            userId?.let {
                DeliveryAddressScreen(
                    navController = navController,
                    userId = it,
                    onAddressSelected = { address ->
                        // Сохраняем выбранный адрес и возвращаемся назад
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "selectedAddress",
                            address
                        )
                        navController.popBackStack()
                    }
                )
            }
        }

        composable("order/create-address/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull()
            userId?.let {
                CreateDeliveryAddressScreen(navController, it)
            }
        }

        composable("order/edit-address/{userId}/{addressId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull()
            val addressId = backStackEntry.arguments?.getString("addressId")?.toLongOrNull()
            if (userId != null && addressId != null) {
                EditDeliveryAddressScreen(navController, userId, addressId)
            }
        }
    }
}