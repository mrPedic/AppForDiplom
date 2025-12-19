package com.example.roamly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.roamly.ui.screens.sealed.AdminScreens
import com.example.roamly.ui.theme.AppTheme

@Composable
fun AdminPanelScreen(
    navController: NavController
) {

    val adminActions = listOf(
        AdminAction(
            title = "Управление заявками",
            description = "Рассмотрение новых заявок на регистрацию заведений",
            route = AdminScreens.PendingList.route
        ),
        AdminAction(
            title = "Все заведения",
            description = "Просмотр, редактирование и удаление всех заведений",
            route = AdminScreens.AllEstablishments.route
        ),
        AdminAction(
            title = "Управление пользователями",
            description = "Просмотр пользователей, изменение ролей, блокировка",
            route = AdminScreens.UsersManagement.route
        ),
        AdminAction(
            title = "Бронирования",
            description = "Просмотр и управление всеми бронированиями системы",
            route = AdminScreens.AllBookings.route
        ),
        AdminAction(
            title = "Модерация отзывов",
            description = "Проверка и управление пользовательскими отзывами",
            route = AdminScreens.ReviewsModeration.route
        ),
        AdminAction(
            title = "Настройки системы",
            description = "Общие настройки приложения и параметры работы",
            route = AdminScreens.SystemSettings.route
        ),
        AdminAction(
            title = "Уведомления",
            description = "Рассылка уведомлений пользователям и заведениям",
            route = AdminScreens.Notifications.route
        ),

    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(17.dp))
            Text(
                text = "Панель администратора",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.MainText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Управление всеми аспектами системы",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.SecondaryText,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(adminActions.chunked(2)) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { action ->
                    AdminActionCard(
                        action = action,
                        navController = navController,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Заполнение пустого места, если в ряду только один элемент
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AdminActionCard(
    action: AdminAction,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable {
                navController.navigate(action.route)
            },
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.SecondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.MainText,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = action.description,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.SecondaryText,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2
            )
        }
    }
}

data class AdminAction(
    val title: String,
    val description: String,
    val route: String
)