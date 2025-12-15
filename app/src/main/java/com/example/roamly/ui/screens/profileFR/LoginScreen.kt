package com.example.roamly.ui.screens.profileFR

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import com.example.roamly.ui.theme.AppTheme

@Composable
fun LoginScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var loginError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var serverError by remember { mutableStateOf<String?>(null) }

    // Surface ensures consistent background color
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppTheme.colors.MainContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Вход",
                style = MaterialTheme.typography.headlineMedium,
                color = AppTheme.colors.MainText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = login,
                isError = loginError != null,
                onValueChange = {
                    login = it
                    if (loginError != null) loginError = null
                    if (serverError != null) serverError = null
                },
                label = { Text(text = "Введите логин") },
                supportingText = {
                    if (loginError != null) {
                        Text(text = loginError!!, color = AppTheme.colors.MainFailure)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.MainSuccess,
                    unfocusedBorderColor = AppTheme.colors.MainBorder,
                    errorBorderColor = AppTheme.colors.MainFailure,
                    focusedLabelColor = AppTheme.colors.MainSuccess,
                    unfocusedLabelColor = AppTheme.colors.SecondaryText,
                    cursorColor = AppTheme.colors.MainText,
                    focusedTextColor = AppTheme.colors.MainText,
                    unfocusedTextColor = AppTheme.colors.MainText
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                isError = passwordError != null,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                    if (serverError != null) serverError = null
                },
                label = { Text(text = "Введите пароль") },
                supportingText = {
                    if (passwordError != null) {
                        Text(text = passwordError!!, color = AppTheme.colors.MainFailure)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.MainSuccess,
                    unfocusedBorderColor = AppTheme.colors.MainBorder,
                    errorBorderColor = AppTheme.colors.MainFailure,
                    focusedLabelColor = AppTheme.colors.MainSuccess,
                    unfocusedLabelColor = AppTheme.colors.SecondaryText,
                    cursorColor = AppTheme.colors.MainText,
                    focusedTextColor = AppTheme.colors.MainText,
                    unfocusedTextColor = AppTheme.colors.MainText
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                modifier = Modifier.fillMaxWidth(0.7f),
                onClick = {
                    loginError = null
                    passwordError = null
                    serverError = null

                    if (login.length <= 3) {
                        loginError = "Логин должен быть длиннее 3 символов"
                    }
                    if (password.length <= 3) {
                        passwordError = "Пароль должен быть длиннее 3 символов"
                    }

                    if (loginError == null && passwordError == null) {
                        userViewModel.loginUser(login = login, password = password) { createdUser ->
                            if (createdUser != null) {
                                Log.i("LoginScreen", "Пользователь вошел с id: ${createdUser.id}")
                                navController.popBackStack()
                            } else {
                                serverError = "Неверный логин или пароль"
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.MainSuccess,
                    contentColor = AppTheme.colors.MainText
                )
            ) {
                Text(text = "Войти в аккаунт")
            }

            if (serverError != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = serverError!!,
                    color = AppTheme.colors.MainFailure,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                modifier = Modifier.clickable {
                    navController.popBackStack()
                    navController.navigate(route = LogSinUpScreens.SingUp.route)
                },
                text = "Создать новый аккаунт",
                color = AppTheme.colors.MainBorder, // Accent link color
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun LoginScreenPreview() {
    LoginScreen(
        navController = rememberNavController(),
        hiltViewModel()
    )
}