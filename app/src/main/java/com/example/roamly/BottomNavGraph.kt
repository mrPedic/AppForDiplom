// BottomNavGraph.kt (или где у вас NavHost)
package com.example.roamly

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.roamly.screens.BottomBarScreen
import com.example.roamly.screens.HomeScreen
import com.example.roamly.screens.ProfileScreen
import com.example.roamly.screens.Screen
import com.example.roamly.screens.SettingsScreen
import com.example.roamly.ui.screens.LoginScreen
import com.example.roamly.ui.screens.SingUpScreen

@Composable
fun BottomNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomBarScreen.Home.route,
        modifier = modifier
    ){
        composable(route = BottomBarScreen.Home.route){
            HomeScreen()
        }
        navigation(
            startDestination = Screen.Login.route,
            route = BottomBarScreen.Profile.route
        ) {
            composable(route = Screen.Login.route) {
                LoginScreen(navController = navController)
            }
            composable(route = Screen.SingUp.route) {
                SingUpScreen(navController = navController)
            }
        }
        composable(route = BottomBarScreen.Settings.route){
            SettingsScreen()
        }
    }
}
