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

@Composable
fun LoginScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 200.dp, bottom = 200.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                if (login.length > 3 && password.length > 3){
                    userViewModel.loginUser(login = login, password = password, { createdUser ->
                        if (createdUser != null) {
                            Log.i("LoginScreen", "Пользователь вошел с id: ${createdUser.id}")
                            navController.popBackStack()
                        }
                    })
                }
            }
        ) {
            Text(text = "Войти в аккаунт")
        }

        Text(
            modifier = Modifier.clickable {
                navController.popBackStack()
                navController.navigate(route = LogSinUpScreens.SingUp.route)
            },
            text = "Созать новый аккаунт",
            color = Color.Magenta,
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

