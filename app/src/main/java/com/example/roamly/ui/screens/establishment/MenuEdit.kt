@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.classes.cl_menu.*
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.ui.screens.sealed.SaveStatus

// --- Вспомогательные классы для UI состояния ---
sealed class EditMode {
    object Idle : EditMode()
    data class GroupName(val groupId: Long?, val isFood: Boolean, val currentName: String) : EditMode()
    data class FoodItem(val groupId: Long?, val item: Food?) : EditMode()
    data class DrinkItem(val groupId: Long?, val item: Drink?) : EditMode()
}

// Генератор временного ID для новых локальных элементов
fun generateTempId() = (100000..999999).random().toLong() * -1


@Composable
fun MenuEditScreen(
    navController: NavController,
    establishmentId: Long,
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    // ⭐ Используем mutableStateListOf для коллекций внутри MenuOfEstablishment
    val menuState = remember {
        mutableStateOf(
            MenuOfEstablishment(
                establishmentId = establishmentId,
                // Используем mutableStateListOf для отслеживания изменений в списках групп
                foodGroups = mutableStateListOf(
                    FoodGroup(id = 101L, establishmentId = establishmentId, name = "Закуски", items = mutableStateListOf(
                        Food(id = 1L, foodGroupId = 101L, name = "Сырники", caloriesPer100g = 200.0, fatPer100g = 10.0, carbohydratesPer100g = 15.0, proteinPer100g = 15.0, ingredients = "Творог, яйцо", cost = 5.0, weight = 250)
                    )),
                    FoodGroup(id = 102L, establishmentId = establishmentId, name = "Основные блюда", items = mutableStateListOf()),
                ),
                drinksGroups = mutableStateListOf(
                    DrinksGroup(id = 201L, establishmentId = establishmentId, name = "Кофе", items = mutableStateListOf(
                        Drink(id = 2L, drinkGroupId = 201L, name = "Латте", caloriesPer100g = 50.0, fatPer100g = 1.5, carbohydratesPer100g = 5.0, proteinPer100g = 2.0, ingredients = "Молоко, кофе", options = mutableStateListOf(
                            DrinkOption(id = 301L, drinkId = 2L, sizeMl = 300, cost = 3.4),
                            DrinkOption(id = 302L, drinkId = 2L, sizeMl = 400, cost = 4.0)
                        ))
                    ))
                )
            )
        )
    }
    var menu by menuState
    var editMode by remember { mutableStateOf<EditMode>(EditMode.Idle) }
    var showConfirmDeleteGroup by remember { mutableStateOf<Pair<Long?, Boolean>?>(null) }

    val saveStatus by viewModel.saveStatus.collectAsState()
    val isLoading = saveStatus is SaveStatus.Loading
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Вспомогательные функции для манипуляции состоянием (CRUD логика) ---

    // ⭐ ЛОГИКА: Сохранение/Обновление названия группы
    val saveGroupName: (Long?, Boolean, String) -> Unit = { groupId, isFood, newName ->
        if (isFood) {
            if (groupId == null) {
                menu.foodGroups.add(FoodGroup(id = generateTempId(), establishmentId = establishmentId, name = newName))
            } else {
                val index = menu.foodGroups.indexOfFirst { it.id == groupId }
                if (index != -1) menu.foodGroups[index] = menu.foodGroups[index].copy(name = newName)
            }
        } else {
            if (groupId == null) {
                menu.drinksGroups.add(DrinksGroup(id = generateTempId(), establishmentId = establishmentId, name = newName))
            } else {
                val index = menu.drinksGroups.indexOfFirst { it.id == groupId }
                if (index != -1) menu.drinksGroups[index] = menu.drinksGroups[index].copy(name = newName)
            }
        }
        editMode = EditMode.Idle
    }

    // ⭐ ЛОГИКА: Удаление группы (Используем локальное удаление для работы, в реальном коде вызываем ViewModel)
    val deleteGroup: (Long?, Boolean) -> Unit = { groupId, isFood ->
        viewModel.trackAndDeleteGroup(groupId, isFood, menu)
        showConfirmDeleteGroup = null
    }

    // ⭐ ЛОГИКА: Сохранение/Обновление компонента (Food/Drink)
    val saveMenuItem: (MenuItem, Long?) -> Unit = saveMenuItem@{ item, groupId ->
        if (groupId == null) return@saveMenuItem

        when (item) {
            is Food -> {
                menu.foodGroups.find { it.id == groupId }?.let { targetGroup ->
                    val updatedFood = item.copy(foodGroupId = groupId)
                    if (item.id == null || item.id!! < 0) {
                        targetGroup.items.add(updatedFood.copy(id = generateTempId()))
                    } else {
                        val index = targetGroup.items.indexOfFirst { it.id == item.id }
                        if (index != -1) targetGroup.items[index] = updatedFood
                        else{ targetGroup.items.add(updatedFood.copy(id = generateTempId())) }
                    }
                }
            }
            is Drink -> {
                menu.drinksGroups.find { it.id == groupId }?.let { targetGroup ->
                    val updatedDrink = item.copy(drinkGroupId = groupId)
                    if (item.id == null || item.id!! < 0) {
                        targetGroup.items.add(updatedDrink.copy(id = generateTempId()))
                    } else {
                        val index = targetGroup.items.indexOfFirst { it.id == item.id }
                        if (index != -1) targetGroup.items[index] = updatedDrink
                        else{ targetGroup.items.add(updatedDrink.copy(id = generateTempId())) }
                    }
                }
            }
            else -> {}
        }
        editMode = EditMode.Idle
    }

    // ⭐ ЛОГИКА: Удаление компонента (Food/Drink) (Используем локальное удаление для работы, в реальном коде вызываем ViewModel)
    val deleteItem: (Long?, Long?, Boolean) -> Unit = { groupId, itemId, isFood ->
        // ⭐ ИСПРАВЛЕНИЕ: Вызываем метод ViewModel для отслеживания удаления компонента
        viewModel.trackAndDeleteItem(groupId, itemId, isFood, menu)
    }

    // ⭐ ЛОГИКА: Сохранение всего меню (теперь только запускает)
    val saveMenu: () -> Unit = {
        // ⭐ ИСПРАВЛЕНИЕ: Вызов метода ViewModel для отправки данных
        viewModel.processMenuChanges(menu)
        println("Запущена отправка изменений меню: $menu")
    }

    // --- 2. SCAFFOLD И BOTTOMBAR С БЛОКИРОВКОЙ ---
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomAppBar(
                actions = {
                    Text("Меню ресторана", modifier = Modifier.padding(start = 16.dp), style = MaterialTheme.typography.titleMedium)
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (!isLoading) {
                                saveMenu()
                            }
                        },
                        icon = {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Filled.Check, contentDescription = "Сохранить")
                            }
                        },
                        text = {
                            if (!isLoading) {
                                Text("Сохранить меню")
                            }
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        // --- 3. LAZYCOLUMN С БЛОКИРОВАННЫМИ КНОПКАМИ ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- БЛОК: Кнопки добавления новых групп ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { editMode = EditMode.GroupName(null, true, "") },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading // Блокировка
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Группа Еды")
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = { editMode = EditMode.GroupName(null, false, "") },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading // Блокировка
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Группа Напитков")
                    }
                }
                Divider()
            }
            // --- Группы Еды ---
            item { Text("Меню Еды", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            items(menu.foodGroups, key = { it.id ?: generateTempId() }) { group ->
                FoodGroupEditor(
                    group = group,
                    // Блокируем вызовы, если идет загрузка
                    onEditGroupName = { if (!isLoading) editMode = EditMode.GroupName(group.id, true, group.name) },
                    onAddItem = { if (!isLoading) editMode = EditMode.FoodItem(group.id, null) },
                    onDeleteItem = { itemId -> if (!isLoading) deleteItem(group.id, itemId, true) },
                    onEditItem = { item -> if (!isLoading) editMode = EditMode.FoodItem(group.id, item) },
                    onDeleteGroup = { if (!isLoading) showConfirmDeleteGroup = Pair(group.id, true) }
                )
            }

            // --- Группы Напитков ---
            item { Spacer(Modifier.height(20.dp)); Text("Меню Напитков", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            items(menu.drinksGroups, key = { it.id ?: generateTempId() }) { group ->
                DrinksGroupEditor(
                    group = group,
                    // Блокируем вызовы, если идет загрузка
                    onEditGroupName = { if (!isLoading) editMode = EditMode.GroupName(group.id, false, group.name) },
                    onAddItem = { if (!isLoading) editMode = EditMode.DrinkItem(group.id, null) },
                    onDeleteItem = { itemId -> if (!isLoading) deleteItem(group.id, itemId, false) },
                    onEditItem = { item -> if (!isLoading) editMode = EditMode.DrinkItem(group.id, item) },
                    onDeleteGroup = { if (!isLoading) showConfirmDeleteGroup = Pair(group.id, false) }
                )
            }
        }
    }

    // --- 4. LaunchedEffect ДЛЯ СТАТУСА СОХРАНЕНИЯ (В реальном коде) ---
    LaunchedEffect(viewModel.saveStatus) {
        when (val status = viewModel.saveStatus.value) {
            is SaveStatus.Success -> {
                snackbarHostState.showSnackbar(message = "Меню успешно сохранено! ✨", withDismissAction = true)
                viewModel.clearSaveStatus()
            }
            is SaveStatus.Error -> {
                snackbarHostState.showSnackbar(message = "Ошибка сохранения: ${status.message} ⚠️", withDismissAction = true, duration = SnackbarDuration.Long)
                viewModel.clearSaveStatus()
            }
            else -> {}
        }
    }

    // --- 5. ДИАЛОГОВЫЕ ОКНА С БЛОКИРОВКОЙ ---

    // 1. Диалог подтверждения удаления группы
    showConfirmDeleteGroup?.let { (groupId, isFood) ->
        AlertDialog(
            onDismissRequest = { if (!isLoading) showConfirmDeleteGroup = null },
            title = { Text("Подтвердите удаление") },
            text = {
                val groupName = if (isFood) menu.foodGroups.find { it.id == groupId }?.name else menu.drinksGroups.find { it.id == groupId }?.name
                Text("Вы уверены, что хотите удалить группу '${groupName ?: "Неизвестная группа"}' вместе со всеми компонентами?")
            },
            confirmButton = {
                Button(
                    onClick = { deleteGroup(groupId, isFood) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isLoading
                ) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDeleteGroup = null }, enabled = !isLoading) { Text("Отмена") } }
        )
    }

    // 2. Диалог редактирования/создания элемента меню
    when (val mode = editMode) {
        is EditMode.FoodItem -> MenuEditDialog(
            isFood = true,
            foodItem = mode.item,
            onDismiss = { if (!isLoading) editMode = EditMode.Idle },
            onSave = { item -> saveMenuItem(item, mode.groupId) },
            isLoading = isLoading
        )
        is EditMode.DrinkItem -> MenuEditDialog(
            isFood = false,
            drinkItem = mode.item,
            onDismiss = { if (!isLoading) editMode = EditMode.Idle },
            onSave = { item -> saveMenuItem(item, mode.groupId) },
            isLoading = isLoading
        )
        is EditMode.GroupName -> GroupNameEditDialog(
            currentName = mode.currentName,
            onDismiss = { if (!isLoading) editMode = EditMode.Idle },
            onSave = { newName -> saveGroupName(mode.groupId, mode.isFood, newName) },
            isLoading = isLoading
        )
        EditMode.Idle -> { /* Ничего не показываем */ }
    }
}

// --- КОМПОНЕНТЫ (FoodGroupEditor, DrinksGroupEditor, GroupHeader, MenuItemCard, GroupNameEditDialog - остаются без изменений) ---

@Composable
fun FoodGroupEditor(
    group: FoodGroup, onEditGroupName: () -> Unit, onAddItem: () -> Unit,
    onDeleteItem: (Long?) -> Unit, onEditItem: (Food) -> Unit, onDeleteGroup: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        GroupHeader(group.name, onEditGroupName, onAddItem, onDeleteGroup)
        Spacer(Modifier.height(4.dp))
        group.items.forEach { food ->
            MenuItemCard(food.name, "Вес: ${food.weight} г, Цена: ${food.cost} р.", { onEditItem(food) }, { onDeleteItem(food.id) })
            Spacer(Modifier.height(4.dp))
        }
        if (group.items.isEmpty()) Text("Нет блюд в этой группе.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun DrinksGroupEditor(
    group: DrinksGroup, onEditGroupName: () -> Unit, onAddItem: () -> Unit,
    onDeleteItem: (Long?) -> Unit, onEditItem: (Drink) -> Unit, onDeleteGroup: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        GroupHeader(group.name, onEditGroupName, onAddItem, onDeleteGroup)
        Spacer(Modifier.height(4.dp))
        group.items.forEach { drink ->
            val optionsText = drink.options.joinToString { "${it.sizeMl} мл / ${it.cost} р." }
            MenuItemCard(drink.name, optionsText, { onEditItem(drink) }, { onDeleteItem(drink.id) })
            Spacer(Modifier.height(4.dp))
        }
        if (group.items.isEmpty()) Text("Нет напитков в этой группе.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun GroupHeader(name: String, onEditName: () -> Unit, onAddItem: () -> Unit, onDeleteGroup: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Row {
            IconButton(onClick = onEditName) { Icon(Icons.Default.Edit, contentDescription = "Редактировать название") }
            IconButton(onClick = onAddItem) { Icon(Icons.Default.Add, contentDescription = "Добавить компонент") }
            IconButton(onClick = onDeleteGroup) { Icon(Icons.Default.Delete, contentDescription = "Удалить группу", tint = MaterialTheme.colorScheme.error) }
        }
    }
    Divider()
}

@Composable
fun MenuItemCard(name: String, description: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Редактировать компонент") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Close, contentDescription = "Удалить компонент", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

// --- GroupNameEditDialog (ОБНОВЛЕНО: Добавлен isLoading) ---
@Composable
fun GroupNameEditDialog(currentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit, isLoading: Boolean) {
    var newName by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать название группы") },
        text = { OutlinedTextField(value = newName, onValueChange = { if (!isLoading) newName = it }, label = { Text("Название") }, enabled = !isLoading) },
        confirmButton = { Button(onClick = { onSave(newName) }, enabled = !isLoading) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Отмена") } }
    )
}


// --- 3. ИСПРАВЛЕННЫЙ MenuEditDialog с полями ввода ---
@Composable
fun MenuEditDialog(
    isFood: Boolean,
    foodItem: Food? = null,
    drinkItem: Drink? = null,
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit,
    isLoading: Boolean // Параметр для блокировки UI
) {
    val isNew = foodItem == null && drinkItem == null
    val initialName = foodItem?.name ?: drinkItem?.name ?: ""
    val initialIngredients = foodItem?.ingredients ?: drinkItem?.ingredients ?: ""

    // Общие поля
    var name by remember { mutableStateOf(initialName) }
    var ingredients by remember { mutableStateOf(initialIngredients) }

    // КБЖУ
    var calories by remember { mutableStateOf((foodItem?.caloriesPer100g ?: drinkItem?.caloriesPer100g ?: 0.0).toString()) }
    var fat by remember { mutableStateOf((foodItem?.fatPer100g ?: drinkItem?.fatPer100g ?: 0.0).toString()) }
    var carbs by remember { mutableStateOf((foodItem?.carbohydratesPer100g ?: drinkItem?.carbohydratesPer100g ?: 0.0).toString()) }
    var protein by remember { mutableStateOf((foodItem?.proteinPer100g ?: drinkItem?.proteinPer100g ?: 0.0).toString()) }

    // Специфические поля Food
    var foodCost by remember { mutableStateOf(foodItem?.cost?.toString() ?: "0.0") }
    var foodWeight by remember { mutableStateOf(foodItem?.weight?.toString() ?: "0") }

    // Специфические поля Drink
    val drinkOptions = remember {
        mutableStateListOf<DrinkOption>().apply {
            addAll(drinkItem?.options ?: listOf(DrinkOption(sizeMl = 0, cost = 0.0)))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${if (isNew) "Добавить" else "Редактировать"} ${if (isFood) "Блюдо" else "Напиток"}") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxHeight(0.7f)) {
                // --- Общие поля ---
                item {
                    Text("Основная информация", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = ingredients, onValueChange = { ingredients = it }, label = { Text("Ингредиенты (через запятую)") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                    Spacer(Modifier.height(16.dp))
                }

                // --- КБЖУ ---
                item {
                    Text("КБЖУ на 100 г", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = calories, onValueChange = { calories = it.filter { it.isDigit() || it == '.' } }, label = { Text("Ккал") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), enabled = !isLoading)
                        OutlinedTextField(value = protein, onValueChange = { protein = it.filter { it.isDigit() || it == '.' } }, label = { Text("Белки") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), enabled = !isLoading)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fat, onValueChange = { fat = it.filter { it.isDigit() || it == '.' } }, label = { Text("Жиры") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), enabled = !isLoading)
                        OutlinedTextField(value = carbs, onValueChange = { carbs = it.filter { it.isDigit() || it == '.' } }, label = { Text("Углеводы") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), enabled = !isLoading)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // --- Специфические поля ---
                if (isFood) {
                    item {
                        Text("Цена и Вес (для Блюда)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = foodCost, onValueChange = { foodCost = it.filter { it.isDigit() || it == '.' } }, label = { Text("Цена (р.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), enabled = !isLoading)
                        OutlinedTextField(value = foodWeight, onValueChange = { foodWeight = it.filter { it.isDigit() } }, label = { Text("Вес порции (г)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
                    }
                } else {
                    item {
                        Text("Опции (для Напитка)", style = MaterialTheme.typography.titleMedium)
                    }
                    items(drinkOptions.indices.toList()) { index ->
                        val option = drinkOptions[index]
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = option.sizeMl.toString(),
                                onValueChange = { if (!isLoading) drinkOptions[index] = option.copy(sizeMl = it.filter { it.isDigit() }.toIntOrNull() ?: 0) },
                                label = { Text("Объем (мл)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).padding(end = 4.dp),
                                enabled = !isLoading
                            )
                            OutlinedTextField(
                                value = option.cost.toString(),
                                onValueChange = { if (!isLoading) drinkOptions[index] = option.copy(cost = it.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0) },
                                label = { Text("Цена (р.)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).padding(start = 4.dp),
                                enabled = !isLoading
                            )
                            IconButton(
                                onClick = { if (!isLoading && drinkOptions.size > 1) drinkOptions.removeAt(index) },
                                enabled = !isLoading
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Удалить опцию")
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = { if (!isLoading) drinkOptions.add(DrinkOption(sizeMl = 0, cost = 0.0)) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            enabled = !isLoading
                        ) {
                            Text("Добавить опцию")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val caloriesVal = calories.toDoubleOrNull() ?: 0.0
                    val fatVal = fat.toDoubleOrNull() ?: 0.0
                    val carbsVal = carbs.toDoubleOrNull() ?: 0.0
                    val proteinVal = protein.toDoubleOrNull() ?: 0.0

                    val itemToSave: MenuItem = if (isFood) {
                        foodItem?.copy(
                            name = name, caloriesPer100g = caloriesVal, fatPer100g = fatVal, carbohydratesPer100g = carbsVal, proteinPer100g = proteinVal, ingredients = ingredients, cost = foodCost.toDoubleOrNull() ?: 0.0, weight = foodWeight.toIntOrNull() ?: 0
                        ) ?: Food(
                            id = null, foodGroupId = foodItem?.foodGroupId, name = name, caloriesPer100g = caloriesVal, fatPer100g = fatVal, carbohydratesPer100g = carbsVal, proteinPer100g = proteinVal, ingredients = ingredients, cost = foodCost.toDoubleOrNull() ?: 0.0, weight = foodWeight.toIntOrNull() ?: 0
                        )
                    } else {
                        drinkItem?.copy(
                            name = name, caloriesPer100g = caloriesVal, fatPer100g = fatVal, carbohydratesPer100g = carbsVal, proteinPer100g = proteinVal, ingredients = ingredients, options = drinkOptions.filter { it.sizeMl > 0 }.toMutableList()
                        ) ?: Drink(
                            id = null, drinkGroupId = drinkItem?.drinkGroupId, name = name, caloriesPer100g = caloriesVal, fatPer100g = fatVal, carbohydratesPer100g = carbsVal, proteinPer100g = proteinVal, ingredients = ingredients, options = drinkOptions.filter { it.sizeMl > 0 }.toMutableList()
                        )
                    }
                    onSave(itemToSave)
                },
                enabled = !isLoading
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Отмена") }
        }
    )
}