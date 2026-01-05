package com.example.roamly.ui.screens.profileFR

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.ViewModel.NotificationViewModel
import com.example.roamly.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    // 🔥 НОВОЕ: Автоматическое обновление при каждом открытии экрана
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // 🔥 ДЛЯ ОТЛАДКИ
    LaunchedEffect(notifications, unreadCount) {
        Log.d("NotificationsScreen",
            "📊 Уведомлений: ${notifications.size}, Непрочитанных: $unreadCount, " +
                    "ID всех уведомлений: ${notifications.map { it.id }}")
    }

    Scaffold(
        containerColor = AppTheme.colors.MainContainer,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Уведомления" + if (unreadCount > 0) " ($unreadCount)" else "",
                        color = AppTheme.colors.MainText,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = AppTheme.colors.MainText
                        )
                    }
                },
                actions = {
                    // 🔥 Добавляем кнопку обновления
                    IconButton(onClick = {
                        viewModel.refresh()
                        // Показать Snackbar или другой индикатор обновления
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить уведомления",
                            tint = AppTheme.colors.MainText
                        )
                    }

                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAll() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Очистить все",
                                tint = AppTheme.colors.MainText
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppTheme.colors.MainContainer
                )
            )
        }
    ) { paddingValues ->
        when {
            notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет уведомлений",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTheme.colors.SecondaryText
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(5.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = notifications.distinctBy { it.id }, // Добавляем distinctBy здесь
                        key = { it.id }
                    ) { notification ->
                        NotificationItemCard(
                            notification = notification,
                            onClick = {
                                viewModel.markAsRead(notification.id)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(103.dp)) // Отступ под нижнюю панель
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    notification: NotificationViewModel.Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                AppTheme.colors.SecondaryContainer.copy(alpha = 0.6f)
            else
                AppTheme.colors.SecondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка слева
            Icon(
                imageVector = when (notification.type) {
                    "NEW_BOOKING" -> Icons.Default.DateRange
                    "BOOKING_STATUS_UPDATE" -> Icons.Default.Build
                    "TEST_NOTIFICATION", "TEST", "TEST_CHANNEL_NOTIFICATION" -> Icons.Default.CheckCircle
                    else -> Icons.Default.Notifications
                },
                contentDescription = null,
                tint = when (notification.type) {
                    "NEW_BOOKING" -> AppTheme.colors.MainSuccess
                    "BOOKING_STATUS_UPDATE" -> AppTheme.colors.SecondarySuccess
                    "TEST_NOTIFICATION", "TEST", "TEST_CHANNEL_NOTIFICATION" -> AppTheme.colors.MainSuccess
                    else -> AppTheme.colors.MainBorder
                },
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Основная информация
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    color = AppTheme.colors.MainText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (notification.isRead)
                        AppTheme.colors.SecondaryText
                    else
                        AppTheme.colors.MainText.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.SecondaryText
                )
            }

            // Стрелка справа + точка непрочитанного
            Row {
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(AppTheme.colors.MainSuccess, shape = androidx.compose.foundation.shape.CircleShape)
                            .align(Alignment.Top)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Подробнее",
                    tint = AppTheme.colors.MainBorder,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    // 🔥 ДОПОЛНИТЕЛЬНАЯ ПРОВЕРКА: Конвертируем из секунд в миллисекунды если нужно
    val timestampMillis = if (timestamp.toString().length == 10) {
        timestamp * 1000
    } else {
        timestamp
    }

    val date = Date(timestampMillis)
    val now = Date()
    val diff = now.time - date.time

    return when {
        diff < 60_000 -> "только что"
        diff < 3_600_000 -> "${diff / 60_000} мин назад"
        diff < 86_400_000 -> "${diff / 3_600_000} ч назад"
        else -> {
            val formatter = SimpleDateFormat("dd MMM HH:mm", Locale("ru"))
            formatter.format(date).replaceFirstChar { it.uppercase() }
        }
    }
}