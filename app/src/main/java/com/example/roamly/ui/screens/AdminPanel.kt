package com.example.roamly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.roamly.ui.screens.sealed.AdminScreens

@Composable
fun AdminPanelScreen(
    navController: NavController
) {
    val adminActions = listOf(
        "Управление заявками" to AdminScreens.PendingList.route,
//        "Управление пользователями" to "admin/users" // Пример
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        items(adminActions) { (title, route) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        navController.navigate(route)
                    }
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}