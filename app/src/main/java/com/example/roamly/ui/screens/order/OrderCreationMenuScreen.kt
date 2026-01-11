package com.example.roamly.ui.screens.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.classes.cl_menu.Food
import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.classes.cl_menu.DrinkOption
import com.example.roamly.entity.CreateOrderItem
import com.example.roamly.entity.MenuItemType
import com.example.roamly.entity.ViewModel.OrderCreationViewModel
import com.example.roamly.ui.theme.AppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCreationMenuScreen(
    navController: NavController,
    establishmentId: Long,
    viewModel: OrderCreationViewModel = hiltViewModel()
) {
    val menu by viewModel.menu.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFoodGroup by viewModel.selectedFoodGroup.collectAsState()
    val selectedDrinkGroup by viewModel.selectedDrinkGroup.collectAsState()
    val colors = AppTheme.colors

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(establishmentId) {
        viewModel.loadMenu(establishmentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Выберите блюда", color = colors.MainText) },
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Товаров: ${cartItems.sumOf { it.quantity }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.SecondaryText
                            )
                            Text(
                                "Итого: ${viewModel.calculateTotal()} ₽",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.MainText
                            )
                        }
                        Button(
                            onClick = {
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
                // Раздел еды
                if (menu?.foodGroups?.isNotEmpty() == true) {
                    item {
                        Text(
                            "Еда",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp, 8.dp),
                            color = colors.MainText
                        )
                    }

                    menu!!.foodGroups.forEach { group ->
                        item {
                            Text(
                                group.name ?: "Без категории",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp),
                                color = colors.MainText
                            )
                        }

                        items(group.items) { food ->
                            FoodItemCard(food, viewModel)
                        }
                    }
                }

                // Раздел напитков
                if (menu?.drinksGroups?.isNotEmpty() == true) {
                    item {
                        Text(
                            "Напитки",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                            color = colors.MainText
                        )
                    }

                    menu!!.drinksGroups.forEach { group ->
                        item {
                            Text(
                                group.name ?: "Без категории",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp),
                                color = colors.MainText
                            )
                        }

                        items(group.items) { drink ->
                            DrinkItemCard(drink, viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoodItemCard(food: Food, viewModel: OrderCreationViewModel) {
    val quantity = remember { mutableStateOf(0) }
    val colors = AppTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.SecondaryContainer,
            contentColor = colors.MainText
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    food.name ?: "Блюдо",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.MainText
                )
                Text(
                    "${food.weight}г • ${food.cost} ₽",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.SecondaryText
                )
            }

            if (quantity.value > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            quantity.value--
                            viewModel.removeFromCart(CreateOrderItem(
                                menuItemId = food.id ?: 0,
                                menuItemType = MenuItemType.FOOD,
                                quantity = 1
                            ))
                        }
                    ) {
                        Icon(Icons.Default.Build, contentDescription = "Уменьшить", tint = colors.MainText)
                    }

                    Text(
                        text = quantity.value.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = colors.MainText
                    )

                    IconButton(
                        onClick = {
                            quantity.value++
                            viewModel.addToCart(CreateOrderItem(
                                menuItemId = food.id ?: 0,
                                menuItemType = MenuItemType.FOOD,
                                quantity = 1
                            ))
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Увеличить", tint = colors.MainText)
                    }
                }
            } else {
                Button(
                    onClick = {
                        quantity.value = 1
                        viewModel.addToCart(CreateOrderItem(
                            menuItemId = food.id ?: 0,
                            menuItemType = MenuItemType.FOOD,
                            quantity = 1
                        ))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.MainSuccess,
                        contentColor = colors.MainText
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Добавить")
                }
            }
        }
    }
}

@Composable
fun DrinkItemCard(drink: Drink, viewModel: OrderCreationViewModel) {
    val quantity = remember { mutableStateOf(0) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    val selectedOption = remember { mutableStateOf<DrinkOption?>(null) }
    val colors = AppTheme.colors

    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Выберите размер", color = colors.MainText) },
            text = {
                Column {
                    drink.options.forEach { option ->
                        Card(
                            onClick = {
                                selectedOption.value = option
                                showOptionsDialog = false
                                quantity.value = 1
                                viewModel.addToCart(CreateOrderItem(
                                    menuItemId = drink.id ?: 0,
                                    menuItemType = MenuItemType.DRINK,
                                    quantity = 1,
                                    selectedOptions = mapOf(
                                        "size" to option.sizeMl.toString(),
                                        "price" to option.cost.toString()
                                    )
                                ))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colors.SecondaryContainer,
                                contentColor = colors.MainText
                            ),
                            shape = MaterialTheme.shapes.small,
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${option.sizeMl} мл", color = colors.MainText)
                                Text("${option.cost} ₽", color = colors.MainText)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showOptionsDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.MainSuccess,
                        contentColor = colors.MainText
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Отмена", color = colors.MainText)
                }
            },
            containerColor = colors.MainContainer,
            titleContentColor = colors.MainText,
            textContentColor = colors.MainText
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.SecondaryContainer,
            contentColor = colors.MainText
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    drink.name ?: "Напиток",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.MainText
                )
                Text(
                    "от ${drink.options.minByOrNull { it.cost }?.cost ?: 0.0} ₽",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.SecondaryText
                )
            }

            if (quantity.value > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            quantity.value--
                            viewModel.removeFromCart(CreateOrderItem(
                                menuItemId = drink.id ?: 0,
                                menuItemType = MenuItemType.DRINK,
                                quantity = 1,
                                selectedOptions = mapOf(
                                    "size" to selectedOption.value?.sizeMl.toString(),
                                    "price" to selectedOption.value?.cost.toString()
                                )
                            ))
                        }
                    ) {
                        Icon(Icons.Default.Build, contentDescription = "Уменьшить", tint = colors.MainText)
                    }

                    Text(
                        text = quantity.value.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = colors.MainText
                    )

                    IconButton(
                        onClick = {
                            quantity.value++
                            viewModel.addToCart(CreateOrderItem(
                                menuItemId = drink.id ?: 0,
                                menuItemType = MenuItemType.DRINK,
                                quantity = 1,
                                selectedOptions = mapOf(
                                    "size" to selectedOption.value?.sizeMl.toString(),
                                    "price" to selectedOption.value?.cost.toString()
                                )
                            ))
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Увеличить", tint = colors.MainText)
                    }
                }
            } else {
                Button(
                    onClick = { showOptionsDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.MainSuccess,
                        contentColor = colors.MainText
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Выбрать")
                }
            }
        }
    }
}