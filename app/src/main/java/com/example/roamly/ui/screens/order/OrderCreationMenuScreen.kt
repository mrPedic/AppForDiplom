package com.example.roamly.ui.screens.order

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.classes.cl_menu.Food
import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.entity.DTO.order.CreateOrderItem
import com.example.roamly.entity.DTO.order.MenuItemType
import com.example.roamly.entity.ViewModel.OrderCreationViewModel
import com.example.roamly.ui.theme.AppTheme

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

    LaunchedEffect(establishmentId) {
        Log.d("OrderCreationMenuScreen", "Загружаем меню для заведения $establishmentId")
        viewModel.loadMenu(establishmentId)
    }

    // Получаем общую стоимость заказа
    val totalPrice by remember(cartItems) {
        derivedStateOf {
            val total = viewModel.calculateTotal()
            Log.d("OrderCreationMenuScreen", "Корзина: ${cartItems.size} позиций, сумма: $total")
            total
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Выберите блюда", color = colors.MainText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = colors.MainText)
                    }
                },
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
                                "Итого: ${String.format("%.2f", totalPrice)} р.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.MainText
                            )
                        }
                        Button(
                            onClick = {
                                Log.d("OrderCreationMenuScreen", "Переход к оформлению. Корзина: ${cartItems.size} позиций")
                                navController.navigate("order/checkout/${establishmentId}")
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
                            FoodItemCard(food = food, viewModel = viewModel, cartItems = cartItems)
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
                            DrinkItemCard(drink = drink, viewModel = viewModel, cartItems = cartItems)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoodItemCard(
    food: Food,
    viewModel: OrderCreationViewModel,
    cartItems: List<CreateOrderItem>
) {
    val colors = AppTheme.colors

    // Находим текущее количество этого продукта в корзине
    val quantity = cartItems
        .filter { it.menuItemId == food.id && it.menuItemType == MenuItemType.FOOD }
        .sumOf { it.quantity }

    var showEditDialog by remember { mutableStateOf(false) }
    var tempQuantity by remember { mutableStateOf(quantity.toString()) }
    val focusRequester = remember { FocusRequester() }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Изменить количество", color = colors.MainText) },
            text = {
                Column {
                    Text("${food.name ?: "Блюдо"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.MainText)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempQuantity,
                        onValueChange = {
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                tempQuantity = it
                            }
                        },
                        label = { Text("Количество", color = colors.SecondaryText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.MainBorder,
                            unfocusedBorderColor = colors.SecondaryBorder,
                            focusedTextColor = colors.MainText,
                            unfocusedTextColor = colors.MainText
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newQuantity = tempQuantity.toIntOrNull() ?: 0
                        if (newQuantity > 0) {
                            // Удаляем все текущие записи этого продукта
                            cartItems.filter { it.menuItemId == food.id && it.menuItemType == MenuItemType.FOOD }
                                .forEach { viewModel.removeCartItem(it) }

                            // Добавляем новую запись с нужным количеством
                            viewModel.addToCart(CreateOrderItem(
                                menuItemId = food.id ?: 0,
                                menuItemType = MenuItemType.FOOD,
                                quantity = newQuantity
                            ))
                        } else {
                            // Удаляем все записи этого продукта
                            cartItems.filter { it.menuItemId == food.id && it.menuItemType == MenuItemType.FOOD }
                                .forEach { viewModel.removeCartItem(it) }
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.MainSuccess,
                        contentColor = colors.MainText
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showEditDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.SecondaryContainer,
                        contentColor = colors.MainText
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Отмена")
                }
            },
            containerColor = colors.MainContainer,
            titleContentColor = colors.MainText,
            textContentColor = colors.MainText
        )

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
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
                    food.name ?: "Блюдо",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.MainText
                )
                Text(
                    "${food.weight}г • ${food.cost} р.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.SecondaryText
                )
            }

            if (quantity > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            // Быстрое уменьшение количества на 1
                            if (quantity > 1) {
                                viewModel.removeFromCart(CreateOrderItem(
                                    menuItemId = food.id ?: 0,
                                    menuItemType = MenuItemType.FOOD,
                                    quantity = 1
                                ))
                            } else {
                                // Если количество 1, полностью удаляем
                                cartItems.filter { it.menuItemId == food.id && it.menuItemType == MenuItemType.FOOD }
                                    .forEach { viewModel.removeCartItem(it) }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Уменьшить", tint = colors.MainText)
                    }

                    // Кликабельное количество для редактирования
                    Text(
                        text = quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                tempQuantity = quantity.toString()
                                showEditDialog = true
                            },
                        color = colors.MainText
                    )

                    IconButton(
                        onClick = {
                            // Быстрое увеличение количества на 1
                            viewModel.addToCart(CreateOrderItem(
                                menuItemId = food.id ?: 0,
                                menuItemType = MenuItemType.FOOD,
                                quantity = 1
                            ))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Увеличить", tint = colors.MainText)
                    }

                    // Кнопка удаления всего блюда
                    IconButton(
                        onClick = {
                            cartItems.filter { it.menuItemId == food.id && it.menuItemType == MenuItemType.FOOD }
                                .forEach { viewModel.removeCartItem(it) }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Удалить", tint = colors.MainFailure)
                    }
                }
            } else {
                Button(
                    onClick = {
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
fun DrinkItemCard(
    drink: Drink,
    viewModel: OrderCreationViewModel,
    cartItems: List<CreateOrderItem>
) {
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val colors = AppTheme.colors

    // Находим все элементы этого напитка в корзине (с учетом выбранного размера)
    val drinkCartItems = cartItems.filter {
        it.menuItemId == drink.id && it.menuItemType == MenuItemType.DRINK
    }

    // Определяем, есть ли хотя бы один выбранный размер в корзине
    val hasSelectedDrink = drinkCartItems.isNotEmpty()
    val totalQuantity = drinkCartItems.sumOf { it.quantity }

    // Если есть выбранный напиток, получаем выбранный размер из первого элемента
    val currentSelectedOption = if (hasSelectedDrink) {
        drinkCartItems.firstOrNull()?.selectedOptions?.get("size")?.let { sizeStr ->
            drink.options.find { it.sizeMl.toString() == sizeStr }
        }
    } else {
        null
    }

    var tempQuantity by remember { mutableStateOf(totalQuantity.toString()) }
    val focusRequester = remember { FocusRequester() }

    // Диалог редактирования количества
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Изменить количество", color = colors.MainText) },
            text = {
                Column {
                    Text("${drink.name ?: "Напиток"} ${currentSelectedOption?.let { "(${it.sizeMl} мл)" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.MainText)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempQuantity,
                        onValueChange = {
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                tempQuantity = it
                            }
                        },
                        label = { Text("Количество", color = colors.SecondaryText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.MainBorder,
                            unfocusedBorderColor = colors.SecondaryBorder,
                            focusedTextColor = colors.MainText,
                            unfocusedTextColor = colors.MainText
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newQuantity = tempQuantity.toIntOrNull() ?: 0
                        if (newQuantity > 0 && currentSelectedOption != null) {
                            // Удаляем все текущие записи этого напитка
                            drinkCartItems.forEach { viewModel.removeCartItem(it) }

                            // Добавляем новую запись с нужным количеством
                            viewModel.addToCart(CreateOrderItem(
                                menuItemId = drink.id ?: 0,
                                menuItemType = MenuItemType.DRINK,
                                quantity = newQuantity,
                                selectedOptions = mapOf(
                                    "size" to currentSelectedOption.sizeMl.toString(),
                                    "price" to currentSelectedOption.cost.toString()
                                )
                            ))
                        } else {
                            // Удаляем все записи этого напитка
                            drinkCartItems.forEach { viewModel.removeCartItem(it) }
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.MainSuccess,
                        contentColor = colors.MainText
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showEditDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.SecondaryContainer,
                        contentColor = colors.MainText
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Отмена")
                }
            },
            containerColor = colors.MainContainer,
            titleContentColor = colors.MainText,
            textContentColor = colors.MainText
        )

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    // Диалог выбора размера напитка
    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Выберите объем", color = colors.MainText) },
            text = {
                Column {
                    drink.options.forEach { option ->
                        Card(
                            onClick = {
                                // Удаляем все старые варианты этого напитка
                                drinkCartItems.forEach { cartItem ->
                                    repeat(cartItem.quantity) {
                                        viewModel.removeFromCart(cartItem.copy(quantity = 1))
                                    }
                                }

                                // Добавляем новый с выбранным размером
                                viewModel.addToCart(CreateOrderItem(
                                    menuItemId = drink.id ?: 0,
                                    menuItemType = MenuItemType.DRINK,
                                    quantity = 1,
                                    selectedOptions = mapOf(
                                        "size" to option.sizeMl.toString(),
                                        "price" to option.cost.toString()
                                    )
                                ))
                                showOptionsDialog = false
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

                                // ✅ Добавьте этот Spacer для отступа
                                Spacer(modifier = Modifier.width(16.dp))

                                Text("${option.cost} р.", color = colors.MainText)
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
                    "от ${drink.options.minByOrNull { it.cost }?.cost ?: 0.0} р.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.SecondaryText
                )

                // Показываем выбранный размер, если есть
                currentSelectedOption?.let { option ->
                    Text(
                        "Выбран объем: ${option.sizeMl} мл",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.SecondarySuccess
                    )
                }
            }

            if (hasSelectedDrink && totalQuantity > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            // Быстрое уменьшение количества на 1
                            if (totalQuantity > 1) {
                                val cartItemToRemove = drinkCartItems.firstOrNull()?.copy(quantity = 1)
                                cartItemToRemove?.let {
                                    viewModel.removeFromCart(it)
                                }
                            } else {
                                // Если количество 1, полностью удаляем
                                drinkCartItems.forEach { viewModel.removeCartItem(it) }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Уменьшить", tint = colors.MainText)
                    }

                    // Кликабельное количество для редактирования
                    Text(
                        text = totalQuantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                tempQuantity = totalQuantity.toString()
                                showEditDialog = true
                            },
                        color = colors.MainText
                    )

                    IconButton(
                        onClick = {
                            // Быстрое увеличение количества на 1
                            val cartItemToAdd = drinkCartItems.firstOrNull()?.copy(quantity = 1)
                            cartItemToAdd?.let {
                                viewModel.addToCart(it)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Увеличить", tint = colors.MainText)
                    }

                    // Кнопка удаления всего напитка
                    IconButton(
                        onClick = {
                            drinkCartItems.forEach { viewModel.removeCartItem(it) }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Удалить", tint = colors.MainFailure)
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