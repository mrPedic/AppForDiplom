package com.example.roamly.ui.screens.order

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.order.OrderDto
import com.example.roamly.entity.DTO.order.OrderStatus
import com.example.roamly.entity.ViewModel.OrderViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.sealed.OrderScreens
import com.example.roamly.ui.theme.AppTheme
import com.example.roamly.ui.theme.toColor
import com.example.roamly.ui.theme.toRussianText
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val user by userViewModel.user.collectAsState()
    val orders by orderViewModel.userOrders.collectAsState()
    val isLoading by orderViewModel.isLoading.collectAsState()
    val error by orderViewModel.error.collectAsState()  // Добавлено для отладки
    val colors = AppTheme.colors

    // Автоматическая загрузка заказов при открытии экрана
    LaunchedEffect(user.id) {
        user.id?.let { orderViewModel.loadUserOrders(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои заказы", color = colors.MainText) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.MainContainer),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = colors.MainText)
                    }
                },
            )
        },
        modifier = Modifier.background(AppTheme.colors.MainContainer)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.MainContainer)) {
            // Если ошибка, показать её
            error?.let {
                Text(
                    text = "Ошибка: $it",
                    color = AppTheme.colors.MainFailure,
                    modifier = Modifier.padding(16.dp)
                )
                LaunchedEffect(Unit) {
                    orderViewModel.clearError()
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.MainSuccess)
                }
            } else if (orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет заказов", color = colors.SecondaryText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(orders, key = { it.id ?: 0 }) { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate(OrderScreens.OrderDetails.createRoute(orderId = order.id ?: -1)) },
                            colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer),
                            border = BorderStroke(1.dp, colors.SecondaryBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Цветная полоска статуса
//                                Box(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .height(8.dp)
//                                        .background(order.status.toColor())
//                                )
//                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Заказ #${order.id}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.MainText
                                    )
                                    Text(
                                        order.status.toRussianText(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = order.status.toColor(),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    "Сумма: ${String.format("%.2f", order.totalPrice)} р.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.MainText
                                )

                                order.createdAt?.let {
                                    Text(
                                        "Создан: ${formatDate(it)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.SecondaryText
                                    )
                                }

                                order.deliveryAddress?.let { address ->
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Доставка: ${address.street}, д. ${address.house}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.SecondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OrderCard(order: OrderDto, navController: NavController) {
    val colors = AppTheme.colors

    // Получаем цвет статуса из расширения
    val statusColor = order.status.toColor()

    // Получаем текст статуса на русском
    val statusText = order.status.toRussianText()

    Card(
        onClick = {
            order.id?.let {
                navController.navigate("order/details/$it")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            // Используем цвет статуса с прозрачностью для фона
            containerColor = statusColor.copy(alpha = 0.15f),
            contentColor = colors.MainText
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Заказ #${order.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.MainText
                )
                Text(
                    "${order.totalPrice} р.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.MainText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badge с цветом статуса
            Badge(
                containerColor = statusColor,
                contentColor = colors.MainText,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Количество позиций: ${order.items.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.SecondaryText
            )

            order.createdAt?.let {
                Text(
                    "Дата: ${formatDate(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.SecondaryText
                )
            }

            // Информация о доставке
            order.deliveryAddress?.let { address ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${address.street}, д. ${address.house}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.SecondaryText
                )
            }
        }
    }
}

private fun getStatusText(status: OrderStatus): String {
    return when (status) {
        OrderStatus.PENDING -> "На рассмотрении"
        OrderStatus.CONFIRMED -> "Подтвержден"
        OrderStatus.IN_PROGRESS -> "Готовится"
        OrderStatus.OUT_FOR_DELIVERY -> "В доставке"
        OrderStatus.DELIVERED -> "Доставлен"
        OrderStatus.CANCELLED -> "Отменен"
        OrderStatus.REJECTED -> "Отклонен"
    }
}



// Добавьте эту функцию в файл или в утилитарный класс
@RequiresApi(Build.VERSION_CODES.O)
private fun formatDate(dateString: String): String {
    return try {
        val dateTime = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("ru", "RU"))
        dateTime.format(formatter)
    } catch (e: Exception) {
        dateString // Вернуть исходную строку, если произошла ошибка форматирования
    }
}