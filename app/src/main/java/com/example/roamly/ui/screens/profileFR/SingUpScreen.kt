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
import com.example.roamly.entity.UserViewModel
import com.example.roamly.ui.screens.sealed.LogSinUpScreens

@Composable
fun SingUpScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    var username by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 200.dp, bottom = 200.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(text = "Введите имя пользователя") }
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
                if (username.length > 3 && login.length > 3 && password.length > 3) {
                    userViewModel.registerUser(username, login, password) { createdUser ->
                        if (createdUser != null) {
                            Log.i("LoginScreen", "✅ Пользователь создан с id: ${createdUser.id}")
                            navController.popBackStack()
                        } else {
                            Log.e("LoginScreen", "Ошибка при регистрации")
                        }
                    }
                }
            }
        ) {
            Text(text = "Зарегистрироваться")
        }

        Text(
            modifier = Modifier.clickable {
                navController.popBackStack()
                navController.navigate(route = LogSinUpScreens.SingUp.route)
            },
            text = "Войти в существующий аккаунт",
            color = Color.Magenta,
            fontSize = MaterialTheme.typography.bodySmall.fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
@Preview(showBackground = true)
fun SingUpScreenPreview() {
    SingUpScreen(navController = rememberNavController(), hiltViewModel())
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