// EditProfileScreen.kt
package com.example.roamly.ui.screens.profileFR

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.theme.AppTheme

@Composable
fun EditProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel = hiltViewModel()
) {
    val user by userViewModel.user.collectAsState()

    var name by remember { mutableStateOf(user.name) }
    var login by remember { mutableStateOf(user.login) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var serverError by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppTheme.colors.MainContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Редактировать профиль",
                style = MaterialTheme.typography.headlineMedium,
                color = AppTheme.colors.MainText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

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
                value = name,
                isError = nameError != null,
                onValueChange = {
                    name = it
                    nameError = null
                    serverError = null
                    successMessage = null
                },
                label = { Text("Имя пользователя") },
                supportingText = {
                    if (nameError != null) {
                        Text(text = nameError!!, color = AppTheme.colors.MainFailure)
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
                    successMessage = null
                },
                label = { Text("Логин") },
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
                    successMessage = null
                },
                label = { Text("Новый пароль (оставьте пустым, если не меняете)") },
                supportingText = {
                    if (passwordError != null) {
                        Text(text = passwordError!!, color = AppTheme.colors.MainFailure)
                    }
                },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                isError = confirmPasswordError != null,
                onValueChange = {
                    confirmPassword = it
                    confirmPasswordError = null
                    serverError = null
                    successMessage = null
                },
                label = { Text("Подтвердите пароль") },
                supportingText = {
                    if (confirmPasswordError != null) {
                        Text(text = confirmPasswordError!!, color = AppTheme.colors.MainFailure)
                    }
                },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                modifier = Modifier.fillMaxWidth(0.7f),
                onClick = {
                    nameError = null
                    loginError = null
                    passwordError = null
                    confirmPasswordError = null
                    serverError = null
                    successMessage = null

                    var hasChanges = false

                    if (name != user.name) {
                        if (name.length <= 3) {
                            nameError = "Имя должно быть длиннее 3 символов"
                        } else {
                            hasChanges = true
                        }
                    }

                    if (login != user.login) {
                        if (login.length <= 3) {
                            loginError = "Логин должен быть длиннее 3 символов"
                        } else {
                            hasChanges = true
                        }
                    }

                    if (password.isNotEmpty()) {
                        if (password.length <= 3) {
                            passwordError = "Пароль должен быть длиннее 3 символов"
                        } else if (password != confirmPassword) {
                            confirmPasswordError = "Пароли не совпадают"
                        } else {
                            hasChanges = true
                        }
                    }

                    if (nameError == null && loginError == null && passwordError == null && confirmPasswordError == null && hasChanges) {
                        userViewModel.updateUserProfile(
                            newName = if (name != user.name) name else null,
                            newLogin = if (login != user.login) login else null,
                            newPassword = if (password.isNotEmpty()) password else null,
                            onSuccess = {
                                successMessage = "Профиль успешно обновлен"
                            },
                            onError = { error ->
                                serverError = error ?: "Ошибка обновления профиля"
                            }
                        )
                    } else if (!hasChanges) {
                        serverError = "Нет изменений для сохранения"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.MainSuccess,
                    contentColor = AppTheme.colors.MainText
                )
            ) {
                Text("Сохранить изменения")
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

            if (successMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = successMessage!!,
                    color = AppTheme.colors.MainSuccess,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))

            TextButton(
                onClick = { navController.popBackStack() }
            ) {
                Text(
                    text = "Отмена",
                    color = AppTheme.colors.MainBorder
                )
            }
        }
    }
}