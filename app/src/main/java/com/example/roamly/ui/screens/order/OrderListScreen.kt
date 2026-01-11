package com.example.roamly.ui.screens.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.OrderDto
import com.example.roamly.entity.OrderStatus
import com.example.roamly.entity.ViewModel.OrderViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.theme.AppTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    val colors = AppTheme.colors

    LaunchedEffect(user.id) {
        user.id?.let { orderViewModel.loadUserOrders(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои заказы", color = colors.MainText) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.MainContainer.copy(alpha = 0.95f),
                    scrolledContainerColor = colors.MainContainer,
                    navigationIconContentColor = colors.MainText,
                    titleContentColor = colors.MainText,
                    actionIconContentColor = colors.MainText
                )
            )
        },
        containerColor = colors.MainContainer
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.MainSuccess)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Активные заказы
                val activeOrders = orders.filter {
                    it.status == OrderStatus.PENDING ||
                            it.status == OrderStatus.CONFIRMED ||
                            it.status == OrderStatus.IN_PROGRESS ||
                            it.status == OrderStatus.OUT_FOR_DELIVERY
                }

                if (activeOrders.isNotEmpty()) {
                    item {
                        Text(
                            "Активные заказы",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp),
                            color = colors.MainText
                        )
                    }
                    items(activeOrders) { order ->
                        OrderCard(order, navController)
                    }
                }

                // Завершенные заказы
                val completedOrders = orders.filter {
                    it.status == OrderStatus.DELIVERED ||
                            it.status == OrderStatus.CANCELLED ||
                            it.status == OrderStatus.REJECTED
                }

                if (completedOrders.isNotEmpty()) {
                    item {
                        Text(
                            "История заказов",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp),
                            color = colors.MainText
                        )
                    }
                    items(completedOrders) { order ->
                        OrderCard(order, navController)
                    }
                }

                if (orders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = colors.SecondaryText
                                )
                                Text(
                                    "У вас пока нет заказов",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.SecondaryText
                                )
                                Button(
                                    onClick = {
                                        // TODO: Переход к списку заведений
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.MainSuccess,
                                        contentColor = colors.MainText
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Сделать первый заказ")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: OrderDto, navController: NavController) {
    val colors = AppTheme.colors
    val statusColor = when (order.status) {
        OrderStatus.PENDING -> colors.SecondarySuccess.copy(alpha = 0.2f)
        OrderStatus.CONFIRMED -> colors.MainSuccess.copy(alpha = 0.2f)
        OrderStatus.IN_PROGRESS -> colors.MainBorder.copy(alpha = 0.2f)
        OrderStatus.OUT_FOR_DELIVERY -> colors.SecondarySuccess.copy(alpha = 0.2f)
        OrderStatus.DELIVERED -> colors.SecondaryContainer
        OrderStatus.CANCELLED, OrderStatus.REJECTED -> colors.MainFailure.copy(alpha = 0.2f)
    }

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
            containerColor = statusColor,
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
                    "${order.totalPrice} ₽",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.MainText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Badge(
                containerColor = when (order.status) {
                    OrderStatus.PENDING -> colors.SecondarySuccess
                    OrderStatus.CONFIRMED -> colors.MainSuccess
                    OrderStatus.IN_PROGRESS -> colors.MainBorder
                    OrderStatus.OUT_FOR_DELIVERY -> colors.SecondarySuccess
                    OrderStatus.DELIVERED -> colors.SecondaryText
                    OrderStatus.CANCELLED, OrderStatus.REJECTED -> colors.MainFailure
                },
                contentColor = colors.MainText,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    getStatusText(order.status),
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

private fun formatDate(dateString: String): String {
    // TODO: Форматирование даты
    return dateString
}