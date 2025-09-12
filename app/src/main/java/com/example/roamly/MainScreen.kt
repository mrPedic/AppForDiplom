package com.example.roamly

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.roamly.screens.BottomBarScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            ButtomBar(navController = navController)
        }
    ) { innerPadding ->
        BottomNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun ButtomBar(
    navController: NavHostController
) {
    val screens = listOf(
        BottomBarScreen.Home,
        BottomBarScreen.Profile,
        BottomBarScreen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // NavigationBar предоставляет RowScope в своей лямбде, так что внутри здесь можно вызывать NavigationBarItem
    NavigationBar {
        screens.forEach { screen ->
            NavigationBarItem(
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
                        // чтобы не плодить экраны в back stack
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

    @Composable
    fun RowScope.AddItem(
        screen: BottomBarScreen,
        currentDestination: NavDestination?,
        navController: NavHostController
    ) {
        NavigationBarItem(
            label = {
                Text(text = screen.title)
            },
            icon = {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = "Navigation Icon"
                )
            },
            selected = currentDestination?.hierarchy?.any {
                it.route == screen.route
            } == true,
            onClick = {
                navController.navigate(screen.route) {
                    // Чтобы не создавать копии экранов в стеке
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
    }
}