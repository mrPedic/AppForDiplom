package com.example.roamly.ui.screens.profileFR

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer // <-- Добавлен импорт
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height // <-- Добавлен импорт
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
import kotlin.math.log

@Composable
fun LoginScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // ⭐ 1. ИЗМЕНЕНИЕ: Используем String? для хранения текста ошибки
    var loginError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var serverError by remember { mutableStateOf<String?>(null) }


    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 200.dp, bottom = 200.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = login,
            // ⭐ 2. ИЗМЕНЕНИЕ: isError и supportingText
            isError = loginError != null,
            onValueChange = {
                login = it
                // Сбрасываем ошибку, как только пользователь начинает печатать
                if (loginError != null) loginError = null
                if (serverError != null) serverError = null
            },
            label = { Text(text = "Введите логин") },
            supportingText = {
                if (loginError != null) {
                    Text(text = loginError!!, color = MaterialTheme.colorScheme.error)
                }
            }
        )

        OutlinedTextField(
            value = password,
            // ⭐ 2. ИЗМЕНЕНИЕ: isError и supportingText
            isError = passwordError != null,
            onValueChange = {
                password = it
                // Сбрасываем ошибку, как только пользователь начинает печатать
                if (passwordError != null) passwordError = null
                if (serverError != null) serverError = null
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
                // ⭐ 3. ИЗМЕНЕНИЕ: Улучшенная логика валидации
                // Сначала сбрасываем ошибки
                loginError = null
                passwordError = null
                serverError = null

                // Проверяем
                if (login.length <= 3 ) {
                    loginError = "Логин должен быть длиннее 3 символов"
                }
                if (password.length <= 3 ) {
                    passwordError = "Пароль должен быть длиннее 3 символов"
                }

                // Если локальных ошибок нет, пробуем войти
                if (loginError == null && passwordError == null){
                    userViewModel.loginUser(login = login, password = password) { createdUser ->
                        if (createdUser != null) {
                            Log.i("LoginScreen", "Пользователь вошел с id: ${createdUser.id}")
                            navController.popBackStack()
                        } else {
                            // Ошибка от сервера
                            serverError = "Неверный логин или пароль"
                        }
                    }
                }
            }
        ) {
            Text(text = "Войти в аккаунт")
        }

        // ⭐ 4. ДОБАВЛЕНО: Отображение ошибки сервера
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
                navController.navigate(route = LogSinUpScreens.SingUp.route)
            },
            text = "Созать новый аккаунт",
            color = MaterialTheme.colorScheme.primary,
            fontSize = MaterialTheme.typography.bodySmall.fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
@Preview (showBackground = true)
fun LoginScreenPreview(){
    LoginScreen(
        navController = rememberNavController(),
        hiltViewModel()
    )
}