package com.example.roamly.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.roamly.screens.AUTHENTICATION_ROUTE
import com.example.roamly.screens.Screen
import com.example.roamly.ui.screens.LoginScreen
import com.example.roamly.ui.screens.SingUpScreen

fun NavGraphBuilder.authNavGraph(
    navController: NavHostController
){
    navigation(
        startDestination = Screen.Login.route,
        route = AUTHENTICATION_ROUTE
    ){
        composable(
            route = Screen.Login.route
        ){
            LoginScreen(navController = navController)
        }

        composable (
            route = Screen.SingUp.route
        ){
            SingUpScreen(navController = navController)
        }
    }
}