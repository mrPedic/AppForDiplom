package com.example.roamly.ui.screens.profileFR

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer // <-- 1. Добавлен импорт
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height // <-- 2. Добавлен импорт
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import com.example.roamly.ui.screens.sealed.SealedButtonBar

@Composable
fun SingUpScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    var username by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // ⭐ 3. ИЗМЕНЕНИЕ: Используем String? для хранения текста ошибки
    var usernameError by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var serverError by remember { mutableStateOf<String?>(null) } // Ошибка от сервера

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Регистрация",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // --- Поле Имени ---
        OutlinedTextField(
            value = username,
            isError = usernameError != null, // Проверяем наличие ошибки
            onValueChange = {
                username = it
                usernameError = null // Сбрасываем ошибку при вводе
                serverError = null
            },
            label = { Text(text = "Введите имя пользователя") },
            // Добавляем supportingText для отображения ошибки
            supportingText = {
                if (usernameError != null) {
                    Text(text = usernameError!!, color = MaterialTheme.colorScheme.error)
                }
            }
        )

        // --- Поле Логина ---
        OutlinedTextField(
            value = login,
            isError = loginError != null,
            onValueChange = {
                login = it
                loginError = null // Сбрасываем ошибку при вводе
                serverError = null
            },
            label = { Text(text = "Введите логин") },
            supportingText = {
                if (loginError != null) {
                    Text(text = loginError!!, color = MaterialTheme.colorScheme.error)
                }
            }
        )

        // --- Поле Пароля ---
        OutlinedTextField(
            value = password,
            isError = passwordError != null,
            onValueChange = {
                password = it
                passwordError = null // Сбрасываем ошибку при вводе
                serverError = null
            },
            label = { Text(text = "Введите пароль") },
            supportingText = {
                if (passwordError != null) {
                    Text(text = passwordError!!, color = MaterialTheme.colorScheme.error)
                }
            }
        )

        Button(
            modifier = Modifier.fillMaxWidth(0.7f),
            onClick = {
                // ⭐ 4. ИЗМЕНЕНИЕ: Улучшенная логика валидации
                // Сначала сбрасываем ошибки
                usernameError = null
                loginError = null
                passwordError = null
                serverError = null

                // Валидация полей
                if (username.length <= 3) {
                    usernameError = "Имя должно быть длиннее 3 символов"
                }
                if (login.length <= 3) {
                    loginError = "Логин должен быть длиннее 3 символов"
                }
                if (password.length <= 3) {
                    passwordError = "Пароль должен быть длиннее 3 символов"
                }

                // Если локальных ошибок нет, отправляем запрос
                if (usernameError == null && loginError == null && passwordError == null) {
                    userViewModel.registerUser(username, login, password) { createdUser ->
                        if (createdUser != null) {
                            Log.i("SingUpScreen", "✅ Пользователь создан с id: ${createdUser.id}")
                            navController.popBackStack()
                        } else {
                            Log.e("SingUpScreen", "Ошибка при регистрации")
                            // Отображаем ошибку сервера
                            serverError = "Ошибка регистрации. Возможно, логин занят."
                        }
                    }
                } else {
                    Log.w("SingUpScreen", "Ошибка: Локальная валидация не пройдена.")
                }
            }
        ) {
            Text(text = "Зарегистрироваться")
        }

        // ⭐ 5. ДОБАВЛЕНО: Отображение ошибки сервера
        if (serverError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = serverError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            modifier = Modifier.clickable {
                navController.popBackStack()
                navController.navigate(route = LogSinUpScreens.Login.route)
            },
            text = "Войти в существующий аккаунт",
            color = MaterialTheme.colorScheme.primary,
            fontSize = MaterialTheme.typography.bodySmall.fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
@Preview(showBackground = true)
fun SingUpScreenPreview() {
    SingUpScreen(navController = rememberNavController(), userViewModel = hiltViewModel())
}