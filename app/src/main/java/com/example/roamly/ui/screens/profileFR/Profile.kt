package com.example.roamly.ui.screens.profileFR

import android.R
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
fun ProfileScreenUnRegistered(
    navController: NavController,
    userViewModel: UserViewModel = hiltViewModel()
) {
    val userData = remember{
        mutableStateOf(userViewModel.getAllData())
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Blue),
        contentAlignment = Alignment.Center
    ){

        Column (
            modifier = Modifier
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Text(
                text = "PROFILE",
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(text = userData.value, color = Color.LightGray)

            Button(
                modifier = Modifier.fillMaxWidth(0.7f),
                onClick = {
                    userData.value = userViewModel.getAllData()
                }
            ) {
                Text(text = "обновить данные пользователя ")
            }

            Column () {


                Button(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .padding( bottom = 50.dp),
                    onClick = {
                        navController.navigate(route = LogSinUpScreens.Login.route)
                    },

                ) {
                    Text(text = "Перейти на авторизацию")
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth(0.45f),
                    onClick = {
                        navController.navigate(route = LogSinUpScreens.SingUp.route)
                    },
                ) {
                    Text(text = "Создать аккаунт")
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun ProfileScreenPreview(){
    ProfileScreenUnRegistered(navController = rememberNavController())
}

@Composable
fun ProfileScreenRegistered (
    navController: NavController,
    userViewModel: UserViewModel = hiltViewModel()
){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray),
        contentAlignment = Alignment.BottomCenter
    ){
        Button(
            modifier = Modifier.padding(bottom = 20.dp),
            onClick = {
                userViewModel.logout()
                navController.popBackStack()
            }
        ) {
            Text(text = "Выйти из аккаунта")
        }
    }
}

@Composable
@Preview(showBackground = true)
fun ProfileScreenRegisteredPreview (){
    ProfileScreenRegistered(
        navController = rememberNavController()
    )
}