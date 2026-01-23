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
import com.example.roamly.ui.screens.sealed.ProfileScreens
import com.example.roamly.ui.theme.AppTheme

@Composable
fun SingUpScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    var username by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var serverError by remember { mutableStateOf<String?>(null) }

    // Ensure background consistency
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
                text = "Регистрация",
                style = MaterialTheme.typography.headlineMedium,
                color = AppTheme.colors.MainText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // --- Fields ---
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.MainSuccess,
                unfocusedBorderColor = AppTheme.colors.MainBorder,
                errorBorderColor = AppTheme.colors.MainFailure,
                focusedLabelColor = AppTheme.colors.MainSuccess,
                unfocusedLabelColor = AppTheme.colors.SecondaryText,
                cursorColor = AppTheme.colors.MainText,
                focusedTextColor = AppTheme.colors.MainText,
                unfocusedTextColor = AppTheme.colors.MainText
            )

            OutlinedTextField(
                value = username,
                isError = usernameError != null,
                onValueChange = {
                    username = it
                    usernameError = null
                    serverError = null
                },
                label = { Text("Введите имя пользователя") },
                supportingText = {
                    if (usernameError != null) {
                        Text(text = usernameError!!, color = AppTheme.colors.MainFailure)
                    }
                },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = login,
                isError = loginError != null,
                onValueChange = {
                    login = it
                    loginError = null
                    serverError = null
                },
                label = { Text("Введите логин") },
                supportingText = {
                    if (loginError != null) {
                        Text(text = loginError!!, color = AppTheme.colors.MainFailure)
                    }
                },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                isError = passwordError != null,
                onValueChange = {
                    password = it
                    passwordError = null
                    serverError = null
                },
                label = { Text("Введите пароль") },
                supportingText = {
                    if (passwordError != null) {
                        Text(text = passwordError!!, color = AppTheme.colors.MainFailure)
                    }
                },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                modifier = Modifier.fillMaxWidth(0.7f),
                onClick = {
                    usernameError = null
                    loginError = null
                    passwordError = null
                    serverError = null

                    if (username.length <= 3) {
                        usernameError = "Имя должно быть длиннее 3 символов"
                    }
                    if (login.length <= 3) {
                        loginError = "Логин должен быть длиннее 3 символов"
                    }
                    if (password.length <= 3) {
                        passwordError = "Пароль должен быть длиннее 3 символов"
                    }

                    if (usernameError == null && loginError == null && passwordError == null) {
                        userViewModel.registerUser(username, login, password) { createdUser ->
                            if (createdUser != null) {
                                Log.i("SingUpScreen", "Пользователь создан с id: ${createdUser.id}")
                                navController.popBackStack()
                            } else {
                                Log.e("SingUpScreen", "Ошибка при регистрации")
                                serverError = "Ошибка регистрации. Возможно, логин занят."
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.MainSuccess,
                    contentColor = AppTheme.colors.MainText
                )
            ) {
                Text("Зарегистрироваться")
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
                    navController.navigate(route = ProfileScreens.Login.route)
                },
                text = "Войти в существующий аккаунт",
                color = AppTheme.colors.MainBorder,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun SingUpScreenPreview() {
    SingUpScreen(navController = rememberNavController(), userViewModel = hiltViewModel())
}