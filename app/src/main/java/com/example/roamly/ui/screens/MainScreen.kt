@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.roamly.entity.Role
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.navigation.NavGraph
import com.example.roamly.ui.screens.sealed.AdminScreens
import com.example.roamly.ui.screens.sealed.BookingScreens
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import com.example.roamly.ui.screens.sealed.SealedButtonBar
import com.example.roamly.ui.theme.AppTheme
import com.example.roamly.ui.theme.AppThemeConfig

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    userViewModel: UserViewModel = hiltViewModel(),
    establishmentViewModel: EstablishmentViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val safeRoute = currentRoute ?: ""

    var mapRefreshKey by remember { mutableStateOf(false) }

    val hideBottomBarRoutes = listOf(
        LogSinUpScreens.SingUp.route,
        LogSinUpScreens.Login.route,
        EstablishmentScreens.EstablishmentDetail.route,
        EstablishmentScreens.CreateEstablishment.route,
        EstablishmentScreens.UserEstablishments.route,
        EstablishmentScreens.MenuEdit.route,
        EstablishmentScreens.ReviewCreation.route,
        BookingScreens.CreateBooking.route,
        EstablishmentScreens.EstablishmentEdit.route,
        EstablishmentScreens.MapPicker.route,
        AdminScreens.PendingList.route
    )

    val hideBackIcon = listOf(
        SealedButtonBar.Profile.route,
        SealedButtonBar.Booking.route,
        SealedButtonBar.Searching.route,
        SealedButtonBar.Home.route,
        SealedButtonBar.AdminPanel.route
    )

    val isHomeScreen = safeRoute == SealedButtonBar.Home.route
    val showBottomBar = safeRoute !in hideBottomBarRoutes
    val showBackIcon = safeRoute !in hideBackIcon

        Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.MainContainer)) {
            NavGraph(
                navController = navController,
                userViewModel = userViewModel,
                mapRefreshKey = mapRefreshKey,
                onMapRefresh = {
                    establishmentViewModel.fetchEstablishmentMarkers()
                    mapRefreshKey = !mapRefreshKey
                }
            )

            if (showBottomBar) {
                FloatingButtonBar(
                    navController = navController,
                    userViewModel = userViewModel,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                )
            }
        }
    }

@Composable
fun FloatingButtonBar(
    navController: NavHostController,
    userViewModel: UserViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val screens = mutableListOf(
        SealedButtonBar.Home,
        SealedButtonBar.Searching,
        SealedButtonBar.Booking,
    )

    val user by userViewModel.user.collectAsState()

    screens.add(SealedButtonBar.Profile)

    if (user.role == Role.AdminOfApp) {
        screens.add(SealedButtonBar.AdminPanel)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(
        modifier = modifier.border(
            width = 1.dp,
            color = AppTheme.colors.MainBorder,
            shape = MaterialTheme.shapes.extraLarge
        ),
        shape = MaterialTheme.shapes.extraLarge,
        color = AppTheme.colors.MainContainer.copy(alpha = 0.95f), // Основной контейнер (темный фон в темной теме)
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent // Прозрачный фон самой NavigationBar
        ) {
            screens.forEach { screen ->
                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppTheme.colors.MainSuccess,      // Яркий акцент для выбранного (например, синий/фиолетовый успех)
                        unselectedIconColor = AppTheme.colors.SecondaryText, // Вторичный текст для невыбранных

                        selectedTextColor = AppTheme.colors.MainSuccess,
                        unselectedTextColor = AppTheme.colors.SecondaryText,

                        indicatorColor = AppTheme.colors.SecondarySuccess.copy(alpha = 0.3f) // Вторичный успех как индикатор
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
}