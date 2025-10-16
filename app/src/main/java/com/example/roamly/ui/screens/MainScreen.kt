@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.roamly.entity.Role
import com.example.roamly.entity.UserViewModel
import com.example.roamly.navigation.NavGraph
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import com.example.roamly.ui.screens.sealed.SealedButtonBar

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hideBottomBarRoutes = listOf(
        LogSinUpScreens.SingUp.route,
        LogSinUpScreens.Login.route
        // Добавьте другие полноэкранные маршруты, если они есть
    )

    val hideBackIcon = listOf(
        SealedButtonBar.Profile.route,
        SealedButtonBar.Booking.route,
        SealedButtonBar.Searching.route,
        SealedButtonBar.Home.route,
    )

    val showBottomBar = currentRoute !in hideBottomBarRoutes
    val showBackIcon = currentRoute !in hideBackIcon

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Roamly") },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            if(showBottomBar){
                ButtonBar(navController = navController)

            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            userViewModel = userViewModel
        )
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

    val user = userViewModel.user

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

