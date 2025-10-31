package com.example.roamly.ui.screens.profileFR

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
    var email by remember { mutableStateOf("") } // ⭐ ДОБАВЛЕНО: Состояние для email
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp, bottom = 100.dp), // Увеличен отступ для размещения всех полей
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ⭐ ДОБАВЛЕНО: Заголовок
        Text(
            text = "Регистрация",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(text = "Введите имя пользователя") }
        )

        // ⭐ ДОБАВЛЕНО: Поле для Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(text = "Введите Email") }
        )

        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = { Text(text = "Введите логин") }
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Введите пароль") }
        )

        Button(
            modifier = Modifier.fillMaxWidth(0.7f),
            onClick = {
                // ⭐ ОБНОВЛЕНО: Добавлена проверка email
                if (username.length > 3 && login.length > 3 && password.length > 3 && email.length > 5) {
                    // ⭐ ОБНОВЛЕНО: Передаем email в registerUser
                    userViewModel.registerUser(username, login, password, email) { createdUser ->
                        if (createdUser != null) {
                            Log.i("SingUpScreen", "✅ Пользователь создан с id: ${createdUser.id}")
                            navController.popBackStack()
                        } else {
                            Log.e("SingUpScreen", "Ошибка при регистрации")
                        }
                    }
                } else {
                    Log.w("SingUpScreen", "Ошибка: Все поля должны быть заполнены и иметь достаточную длину.")
                    // В реальном приложении здесь было бы Toast или Snackbar
                }
            }
        ) {
            Text(text = "Зарегистрироваться")
        }

        Text(
            modifier = Modifier.clickable {
                navController.popBackStack()
                navController.navigate(route = LogSinUpScreens.Login.route)
            },
            text = "Войти в существующий аккаунт",
            color = MaterialTheme.colorScheme.primary, // Используем основную тему вместо Magenta
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


/**
 * Column:                    Row:                    Box:
 *  ↑ Top                     → Start                 +-----------+
 *  │                         │                       | TopStart  |
 *  │                         │                       |           |
 *  │                         │                       |   Center  |
 *  │                         │                       |           |
 *  ↓ Bottom                  ← End                   | BottomEnd |
 * */