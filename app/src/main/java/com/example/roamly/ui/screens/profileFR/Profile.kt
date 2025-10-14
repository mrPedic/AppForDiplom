package com.example.roamly.ui.screens.profileFR

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.UserViewModel
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.remember

@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val isLoggedIn =  userViewModel.isLoggedIn()

    Log.d("ProfileScreen", userViewModel.isLoggedIn().toString())

    if (isLoggedIn) {
        RegisteredProfileContent(navController, userViewModel)
    } else {
        UnRegisteredProfileContent(navController, userViewModel)
    }
}

// ----------------------------------------------------
// КОМПОНЕНТ ДЛЯ АВТОРИЗОВАННОГО ПОЛЬЗОВАТЕЛЯ
// ----------------------------------------------------
@Composable
private fun RegisteredProfileContent(
    navController: NavController,
    userViewModel: UserViewModel
) {
    // Читаем реактивное состояние. Это тоже вызовет перекомпоновку при изменении.
    val currentUser = userViewModel.user

    val userDataDisplay = "ID: ${currentUser.id ?: "Нет ID"}\n" +
            "Login: ${currentUser.login}\n" +
            "Name: ${currentUser.name}\n" +
            "Role: ${currentUser.role}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Ваш Профиль",
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ⚠️ ВЫВОД ДАННЫХ
            Text(
                text = userDataDisplay,
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    userViewModel.logout()
                    // При вызове logout() userViewModel.user меняется,
                    // что вызывает перекомпоновку ProfileScreen
                }
            ) {
                Text(text = "Выйти из аккаунта")
            }
        }
    }
}

// ----------------------------------------------------
// КОМПОНЕНТ ДЛЯ НЕАВТОРИЗОВАННОГО ПОЛЬЗОВАТЕЛЯ
// ----------------------------------------------------
@Composable
private fun UnRegisteredProfileContent(
    navController: NavController,
    userViewModel: UserViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Blue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PROFILE",
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Сообщение, если пользователь не авторизован
            Text(text = "Пользователь не авторизован", color = Color.LightGray)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(bottom = 16.dp),
                    onClick = {
                        navController.navigate(route = LogSinUpScreens.SingUp.route) // Создать аккаунт -> Регистрация
                    },
                ) {
                    Text(text = "Создать аккаунт")
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth(0.7f),
                    onClick = {
                        navController.navigate(route = LogSinUpScreens.Login.route) // Перейти на авторизацию -> Вход
                    },

                    ) {
                    Text(text = "Войти в аккаунт")
                }
            }
        }
    }
}