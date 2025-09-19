package com.example.roamly.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.roamly.navigation.BottomNavGraph
import com.example.roamly.ui.screens.sealed.ButtonBarScreens

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    Scaffold(
        bottomBar = {
            ButtonBar(navController = navController)
        }
    ) { innerPadding ->
        BottomNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun ButtonBar(
    navController: NavHostController,
    userViewModel: UserViewModel = hiltViewModel()
) {
    val screens = mutableListOf(
        ButtonBarScreens.Home,
        ButtonBarScreens.Searching,
        ButtonBarScreens.Booking,
    )

    val user = userViewModel.user  // <-- это подписка на состояние (mutableStateOf)

    if (user.role == Role.UnRegistered) {
        screens.add(ButtonBarScreens.UnRegisteredProfile)
    } else {
        screens.add(ButtonBarScreens.RegisteredProfile)
    }

    if (user.role == Role.AdminOfApp) {
        screens.add(ButtonBarScreens.AdminPanel)
    }



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
}
