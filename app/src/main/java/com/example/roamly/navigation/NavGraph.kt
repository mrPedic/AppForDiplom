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
import com.example.roamly.entity.ViewModel.OrderCreationViewModel
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
import com.example.roamly.ui.screens.order.OrderDetailsScreen
import com.example.roamly.ui.screens.order.OrderListScreen
import com.example.roamly.ui.screens.profileFR.NotificationsScreen
import com.example.roamly.ui.screens.profileFR.ProfileScreen
import com.example.roamly.ui.screens.sealed.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel,
    orderViewModel: OrderViewModel,
    mapRefreshKey: Boolean,
    orderCreationViewModel: OrderCreationViewModel,
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

        composable(EstablishmentScreens.ApproveBookings.route) {
            ApproveBookingsScreen(navController = navController, userViewModel = userViewModel)
        }

        composable(
            route = BookingScreens.OwnerBookingsManagement.route,
            arguments = listOf(navArgument(BookingScreens.ESTABLISHMENT_ID_KEY) { type = NavType.StringType })
        ) { backStackEntry ->
            val establishmentIdStr = backStackEntry.arguments?.getString(BookingScreens.ESTABLISHMENT_ID_KEY)
            val establishmentId = establishmentIdStr?.toLongOrNull() ?: 0L

            if (establishmentId == 0L) {
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

        composable(
            route = BookingScreens.OwnerApprovedBookings.route,
            arguments = listOf(navArgument(BookingScreens.ESTABLISHMENT_ID_KEY) { type = NavType.StringType })
        ) { backStackEntry ->
            val establishmentIdStr = backStackEntry.arguments?.getString(BookingScreens.ESTABLISHMENT_ID_KEY)
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

        // =====================================
        // Создание заказа - ИСПОЛЬЗУЕМ SEALED CLASSES
        // =====================================
        composable(
            route = OrderScreens.OrderCreation.route,
            arguments = listOf(
                navArgument(OrderScreens.ESTABLISHMENT_ID_KEY) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getLong(OrderScreens.ESTABLISHMENT_ID_KEY) ?: return@composable
            OrderCreationMenuScreen(
                navController = navController,
                establishmentId = establishmentId,
                viewModel = orderCreationViewModel
            )
        }

        // =====================================
        // Оформление заказа
        // =====================================
        composable(
            route = OrderScreens.OrderCheckout.route,
            arguments = listOf(
                navArgument(OrderScreens.ESTABLISHMENT_ID_KEY) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val establishmentId = backStackEntry.arguments?.getLong(OrderScreens.ESTABLISHMENT_ID_KEY) ?: return@composable
            val userId = userViewModel.getId()

            if (userId != null) {
                OrderCheckoutScreen(
                    navController = navController,
                    establishmentId = establishmentId,
                    userId = userId,
                    orderViewModel = orderViewModel,
                    orderCreationViewModel = orderCreationViewModel
                )
            }
        }

        // =====================================
        // Список заказов
        // =====================================
        composable(OrderScreens.OrderList.route) {
            OrderListScreen(navController, userViewModel)
        }

        // Детали заказа
        composable(
            route = OrderScreens.OrderDetails.route,
            arguments = listOf(
                navArgument(OrderScreens.ORDER_ID_KEY) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong(OrderScreens.ORDER_ID_KEY)
            orderId?.let {
                OrderDetailsScreen(
                    navController = navController,
                    orderId = orderId
                )
            }
        }

        // =====================================
        // Адреса доставки
        // =====================================
        composable(
            route = OrderScreens.DeliveryAddresses.route,
            arguments = listOf(
                navArgument(OrderScreens.USER_ID_KEY) { type = NavType.LongType },
                navArgument("isSelectionMode") {
                    type = NavType.BoolType
                    defaultValue = true
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong(OrderScreens.USER_ID_KEY) ?: return@composable
            val isSelectionMode = backStackEntry.arguments?.getBoolean("isSelectionMode") ?: true

            DeliveryAddressScreen(
                navController = navController,
                userId = userId,
                isSelectionMode = isSelectionMode,
                onAddressSelected = { address ->
                    // Устанавливаем результат и выходим (ОДИН РАЗ)
                    navController.previousBackStackEntry?.savedStateHandle?.set("selectedAddress", address)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = OrderScreens.CreateDeliveryAddress.route,
            arguments = listOf(
                navArgument(OrderScreens.USER_ID_KEY) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong(OrderScreens.USER_ID_KEY) ?: return@composable
            CreateDeliveryAddressScreen(navController, userId)
        }

        composable(
            route = OrderScreens.EditDeliveryAddress.route,
            arguments = listOf(
                navArgument(OrderScreens.USER_ID_KEY) { type = NavType.LongType },
                navArgument(OrderScreens.ADDRESS_ID_KEY) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong(OrderScreens.USER_ID_KEY) ?: return@composable
            val addressId = backStackEntry.arguments?.getLong(OrderScreens.ADDRESS_ID_KEY) ?: return@composable
            EditDeliveryAddressScreen(navController, userId, addressId)
        }
    }
}