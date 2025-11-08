package com.example.roamly.ui.screens.profileFR

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row // <-- 1. Добавлен импорт
import androidx.compose.foundation.layout.Spacer // <-- 2. Добавлен импорт
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height // <-- 3. Добавлен импорт
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults // <-- 4. Добавлен импорт
import androidx.compose.material3.Card // <-- 5. Добавлен импорт
import androidx.compose.material3.CardDefaults // <-- 6. Добавлен импорт
import androidx.compose.material3.CircularProgressIndicator // <-- 7. Добавлен импорт
import androidx.compose.material3.Divider // <-- 8. Добавлен импорт
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.example.roamly.entity.Role
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.screens.sealed.SealedButtonBar

@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    // Логика не изменилась: по-прежнему проверяем статус авторизации
    val user by userViewModel.user.collectAsState()
    val isLoggedIn = user.role != Role.UnRegistered

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
    val currentUser by userViewModel.user.collectAsState()

    // ⭐ ИСПРАВЛЕНО: Убран Box с серым фоном. Используем Column с отступами.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), // Общий отступ для всего экрана
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Ваш Профиль",
            style = MaterialTheme.typography.headlineLarge, // Увеличен стиль
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, // Используем цвет темы
            modifier = Modifier.padding(top = 32.dp, bottom = 24.dp)
        )

        // ⭐ ИСПРАВЛЕНО: Данные пользователя обернуты в Card для лучшего вида
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Информация о пользователе",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                // Используем вспомогательный Composable для красивого вывода
                InfoRow(label = "Имя:", value = currentUser.name ?: "Не указано")
                InfoRow(label = "Логин:", value = currentUser.login)
                InfoRow(label = "Роль:", value = currentUser.role.toString())
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Кнопки для владельца заведения
        Button(
            onClick = {
                navController.navigate(EstablishmentScreens.UserEstablishments.route)
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ){
            Text(text = "Мои Заведения")
        }

        Button(
            onClick = {
                navController.navigate(EstablishmentScreens.CreateEstablishment.route)
            },
            modifier = Modifier.fillMaxWidth() // Убран нижний отступ
        ){
            Text(text = "Создать свое заведение")
        }

        // ⭐ ИСПРАВЛЕНО: Spacer с weight(1f) прижимает кнопку "Выйти" к низу
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                userViewModel.logout()
            },
            // ⭐ ИСПРАВЛЕНО: Кнопка выхода сделана "опасной" (красной)
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
        ) {
            Text(text = "Выйти из аккаунта")
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
    // ⭐ ИСПРАВЛЕНО: Убран Box с синим фоном.
    // Используем центрированный Column со стандартным фоном.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center, // Центрируем по вертикали
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Профиль",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Вы не авторизованы",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Мягкий цвет
        )

        Spacer(modifier = Modifier.height(48.dp))

        // ✅ Выносим логику статуса сервера в отдельный Composable
        ServerStatus(userViewModel = userViewModel)

        Spacer(modifier = Modifier.height(48.dp))

        // --- Кнопки входа/регистрации ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Немного шире
                    .padding(bottom = 16.dp),
                onClick = {
                    navController.navigate(route = LogSinUpScreens.SingUp.route)
                },
            ) {
                Text(text = "Создать аккаунт")
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth(0.8f),
                onClick = {
                    navController.navigate(route = LogSinUpScreens.Login.route)
                },
            ) {
                Text(text = "Войти в аккаунт")
            }
        }
    }
}

// ----------------------------------------------------
// Вспомогательные компоненты
// ----------------------------------------------------

/**
 * Вспомогательный Composable для отображения статуса сервера
 */
@Composable
private fun ServerStatus(userViewModel: UserViewModel) {
    var isCheckingConnection by remember { mutableStateOf(true) }
    val isConnected by userViewModel.isServerConnected.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        userViewModel.checkServerConnection()
        isCheckingConnection = false
    }

    if (isCheckingConnection) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Проверка подключения...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        // ⭐ ИСПРАВЛЕНО: Используем цвета темы вместо Color.Green/Red
        val statusText = if (isConnected) "Сервер доступен ✅" else "Сервер недоступен ❌"
        val statusColor = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

        Text(
            text = statusText,
            color = statusColor,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Вспомогательный Composable для отображения строки "Метка: Значение"
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}