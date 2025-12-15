package com.example.roamly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.roamly.ui.screens.sealed.AdminScreens
import com.example.roamly.ui.theme.AppTheme

@Composable
fun AdminPanelScreen(
    navController: NavController
) {
    val adminActions = listOf(
        "Управление заявками" to AdminScreens.PendingList.route,
//        "Управление пользователями" to "admin/users" // Пример
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
    ) {
        item{
            Spacer(Modifier.fillMaxWidth().height(13.dp))
        }
        items(adminActions) { (title, route) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        navController.navigate(route)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colors.SecondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(16.dp),
                    color = AppTheme.colors.MainText,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}