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
import androidx.navigation.NavController
import com.example.roamly.entity.UserViewModel
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.screens.sealed.SealedButtonBar

@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val isLoggedIn =  userViewModel.isLoggedIn()
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

            // ⭐ НОВАЯ КНОПКА: Просмотр моих заведений
            Button(
                onClick = {
                    // Предполагаем, что этот маршрут существует
                    navController.navigate(EstablishmentScreens.UserEstablishments.route)
                },
                modifier = Modifier.padding(bottom = 12.dp)
            ){
                Text(text = "Мои Заведения")
            }

            Button(
                onClick = {
                    navController.navigate(EstablishmentScreens.CreateEstablishment.route)
                },
                modifier = Modifier.padding(bottom = 12.dp) // Добавлен отступ для лучшего разделения кнопок
            ){
                Text(text = "Создать свое заведение")
            }

            Button(
                onClick = {
                    userViewModel.logout()
                }
            ) {
                Text(text = "Выйти из аккаунта")
            }

            Spacer(modifier = Modifier.height(12.dp))

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
    // Состояние для индикации загрузки/проверки
    var isCheckingConnection by remember { mutableStateOf(true) }

    // Запускаем проверку подключения при первой композиции
    LaunchedEffect(Unit) {
        userViewModel.checkServerConnection()
        isCheckingConnection = false
    }

    // Читаем реактивное состояние из ViewModel
    val isConnected = userViewModel.isServerConnected

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

            // ✅ БЛОК ОТОБРАЖЕНИЯ СТАТУСА СЕРВЕРА
            if (isCheckingConnection) {
                Text(
                    text = "Проверка подключения...",
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                val statusText = if (isConnected) "Сервер доступен ✅" else "Сервер недоступен ❌"
                val statusColor = if (isConnected) Color.Green else Color.Red

                Text(
                    text = statusText,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            // ----------------------------------------

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(bottom = 16.dp),
                    onClick = {
                        navController.navigate(route = LogSinUpScreens.SingUp.route)
                    },
                    // Кнопка доступна только при наличии подключения
//                    enabled = isConnected
                ) {
                    Text(text = "Создать аккаунт")
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth(0.7f),
                    onClick = {
                        navController.navigate(route = LogSinUpScreens.Login.route)
                    },
                    // Кнопка доступна только при наличии подключения
//                    enabled = isConnected
                ) {
                    Text(text = "Войти в аккаунт")
                }
            }
        }
    }
}
