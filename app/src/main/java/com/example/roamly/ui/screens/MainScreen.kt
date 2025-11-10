@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh // ⭐ Импорт иконки обновления
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState // ⭐ Импорт для сбора StateFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.roamly.entity.Role
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.navigation.NavGraph
import com.example.roamly.ui.screens.sealed.AdminScreens
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import com.example.roamly.ui.screens.sealed.SealedButtonBar

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val safeRoute = currentRoute ?: ""

    // ⭐ ВОССТАНОВЛЕНО: Состояние для принудительного обновления карты
    var mapRefreshKey by remember { mutableStateOf(false) }

    val hideBottomBarRoutes = listOf(
        LogSinUpScreens.SingUp.route,
        LogSinUpScreens.Login.route
    )

    val hideBackIcon = listOf(
        SealedButtonBar.Profile.route,
        SealedButtonBar.Booking.route,
        SealedButtonBar.Searching.route,
        SealedButtonBar.Home.route,
        SealedButtonBar.AdminPanel.route
    )

    val isHomeScreen = safeRoute == SealedButtonBar.Home.route // ⭐ Проверка на главный экран
    val showBottomBar = safeRoute !in hideBottomBarRoutes
    val showBackIcon = safeRoute !in hideBackIcon

    Scaffold(
        topBar = {
            TopAppBar(
                title = {Text(text = getCurrentTopAppBarTitle(currentRoute = currentRoute))},
                navigationIcon = {
                    if(showBackIcon){
                        IconButton(
                            onClick = {
                                navController.popBackStack()
                            },
                            content = { Icon(Icons.Filled.KeyboardArrowLeft, "Назад") }
                        )
                    }
                },
                actions = { // ⭐ ВОССТАНОВЛЕНО: Блок для иконки Обновить
                    if(isHomeScreen) {
                        IconButton(
                            onClick = {
                                // Тогглим ключ для принудительного обновления AndroidView
                                mapRefreshKey = !mapRefreshKey
                            },
                            content = { Icon(Icons.Filled.Refresh, "Обновить карту") }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            if(showBottomBar){
                ButtonBar(navController = navController, userViewModel = userViewModel) // ⭐ ПЕРЕДАН userViewModel
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            userViewModel = userViewModel,
            mapRefreshKey = mapRefreshKey
        )
    }
}

fun getCurrentTopAppBarTitle(currentRoute: String?): String {
    // ⭐ Шаблон для маршрутов с ID
    val detailRoutePattern = EstablishmentScreens.EstablishmentDetail.route.substringBefore("/{")
    val editRoutePattern = EstablishmentScreens.EstablishmentEdit.route.substringBefore("/{")

    return when {
        // --- Анализ маршрутов с аргументами ---
        currentRoute?.startsWith(detailRoutePattern) == true -> {
            // Внимание: Здесь мы не можем легко получить название заведения без ViewModel.
            // Вместо этого покажем общий заголовок.
            "Детали заведения"
        }
        currentRoute?.startsWith(editRoutePattern) == true -> "Редактирование заведения"


        // --- Маршруты без аргументов ---
        currentRoute ==  LogSinUpScreens.SingUp.route -> "Регистрация"
        currentRoute == LogSinUpScreens.Login.route -> "Вход в аккаунт"
        currentRoute == EstablishmentScreens.UserEstablishments.route -> "Мои заведения"
        currentRoute == EstablishmentScreens.CreateEstablishment.route -> "Создание заведения"
        currentRoute == EstablishmentScreens.MapPicker.route -> "Выбор места заведения"
        currentRoute == SealedButtonBar.AdminPanel.route -> "（￣︶￣）↗　"
        currentRoute == AdminScreens.PendingList.route -> "Заявки на одобрение"

        // Если маршрут не определен или null
        else -> "Roamly"
    }
}

@Composable
fun ButtonBar(
    navController: NavHostController,
    userViewModel: UserViewModel = hiltViewModel()
) {
    val screens = mutableListOf(
        SealedButtonBar.Home,
        SealedButtonBar.Searching,
        SealedButtonBar.Booking,
    )

    // ⭐ ИСПРАВЛЕНО: Сборка StateFlow из ViewModel в Compose State
    val user by userViewModel.user.collectAsState()

    screens.add(SealedButtonBar.Profile)

    if (user.role == Role.AdminOfApp) {
        screens.add(SealedButtonBar.AdminPanel)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // NavigationBar предоставляет RowScope в своей лямбде, так что внутри здесь можно вызывать NavigationBarItem
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        screens.forEach { screen ->
            NavigationBarItem(
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = MaterialTheme.colorScheme.background
                ),
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title
                    )
                },
                label = { Text(text = screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
