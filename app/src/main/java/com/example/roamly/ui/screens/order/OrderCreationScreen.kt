package com.example.roamly.ui.screens.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.CreateOrderItem
import com.example.roamly.entity.ViewModel.OrderViewModel
import com.example.roamly.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCreationScreen(
    navController: NavController,
    establishmentId: Long,
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val cartItems = remember { mutableStateListOf<CreateOrderItem>() }
    val colors = AppTheme.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создание заказа", color = colors.MainText) },
                actions = {
                    if (cartItems.isNotEmpty()) {
                        Badge(
                            containerColor = colors.MainSuccess,
                            contentColor = colors.MainText,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(cartItems.sumOf { it.quantity }.toString())
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.MainContainer.copy(alpha = 0.95f),
                    scrolledContainerColor = colors.MainContainer,
                    navigationIconContentColor = colors.MainText,
                    titleContentColor = colors.MainText,
                    actionIconContentColor = colors.MainText
                )
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.SecondaryContainer,
                        contentColor = colors.MainText
                    ),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Товаров: ${cartItems.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.SecondaryText
                            )
                            Text(
                                "Итого: ${calculateTotal(cartItems)} ₽",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.MainText
                            )
                        }
                        Button(
                            onClick = {
                                // Переходим к оформлению
                                navController.navigate("order/checkout/$establishmentId")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.MainSuccess,
                                contentColor = colors.MainText
                            ),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("Далее")
                        }
                    }
                }
            }
        },
        containerColor = colors.MainContainer
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Здесь будет отображение меню заведения
            // с возможностью добавления в корзину
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Корзина",
                        modifier = Modifier.size(64.dp),
                        tint = colors.SecondaryText
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Выберите блюда из меню",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.MainText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Перейдите в меню для выбора товаров",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.SecondaryText
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            navController.navigate("order/menu/$establishmentId")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.MainSuccess,
                            contentColor = colors.MainText
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Перейти в меню")
                    }
                }
            }
        }
    }
}

@Composable
private fun Alignment.CenterHorizontally(): Alignment.Horizontal = Alignment.CenterHorizontally

@Composable
private fun Arrangement.Center(): Arrangement.Vertical = Arrangement.Center

private fun calculateTotal(items: List<CreateOrderItem>): Double {
    // TODO: Реализовать расчет суммы на основе цен из меню
    return 0.0
}