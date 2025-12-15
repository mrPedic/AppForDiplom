@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.classes.cl_menu.*
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.ui.screens.sealed.SaveStatus
import com.example.roamly.ui.theme.AppTheme

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
    // --- 1. ЗАГРУЗКА ДАННЫХ ---
    val serverMenu by viewModel.menuOfEstablishment.collectAsState()
    val isMenuLoading by viewModel.isMenuLoading.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val isLoading = saveStatus is SaveStatus.Loading

    // --- 2. ИНИЦИАЛИЗАЦИЯ ЛОКАЛЬНОГО СОСТОЯНИЯ (menuState) ---
    val menuState = remember(serverMenu) {
        val initialState = if (serverMenu != null) {
            Log.d("MenuEditScreen", "Инициализация menuState из serverMenu.")
            serverMenu!!.copy(
                foodGroups = serverMenu!!.foodGroups.map { fg ->
                    fg.copy(
                        name = fg.name ?: "",
                        items = fg.items.map { f ->
                            f.copy(
                                name = f.name ?: "",
                                ingredients = f.ingredients ?: ""
                            )
                        }.toMutableStateList()
                    )
                }.toMutableStateList(),

                drinksGroups = serverMenu!!.drinksGroups.map { dg ->
                    dg.copy(
                        name = dg.name ?: "",
                        items = dg.items.map { d ->
                            d.copy(
                                name = d.name ?: "",
                                ingredients = d.ingredients ?: "",
                                options = d.options.toMutableStateList()
                            )
                        }.toMutableStateList()
                    )
                }.toMutableStateList()
            )
        } else {
            Log.d("MenuEditScreen", "Инициализация menuState пустым меню.")
            MenuOfEstablishment(
                establishmentId = establishmentId,
                foodGroups = mutableStateListOf(),
                drinksGroups = mutableStateListOf()
            )
        }
        mutableStateOf(initialState)
    }

    var menu by menuState
    val hasInitialized = remember { mutableStateOf(false) }

    var editMode by remember { mutableStateOf<EditMode>(EditMode.Idle) }
    var showConfirmDeleteGroup by remember { mutableStateOf<Pair<Long?, Boolean>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // --- 3. ЗАГРУЗКА И СИНХРОНИЗАЦИЯ ---
    LaunchedEffect(establishmentId) {
        Log.d("MenuEditScreen", "Запускаем fetchMenuForEstablishment для ID: $establishmentId")
        viewModel.fetchMenuForEstablishment(establishmentId)
    }

    // --- Вспомогательные функции для манипуляции состоянием (CRUD логика) ---
    val saveGroupName: (Long?, Boolean, String) -> Unit = { groupId, isFood, newName ->
        if (isFood) {
            if (groupId == null) {
                // Используем copy для создания нового объекта меню, чтобы вызвать рекомпозицию
                menu = menu.copy(
                    foodGroups = menu.foodGroups.toMutableList().apply {
                        add(FoodGroup(id = generateTempId(), establishmentId = menu.establishmentId, name = newName))
                    }.toMutableStateList()
                )
            } else {
                val index = menu.foodGroups.indexOfFirst { it.id == groupId }
                if (index != -1) {
                    menu = menu.copy(
                        foodGroups = menu.foodGroups.toMutableList().apply {
                            this[index] = this[index].copy(name = newName)
                        }.toMutableStateList()
                    )
                }
            }
        } else {
            if (groupId == null) {
                menu = menu.copy(
                    drinksGroups = menu.drinksGroups.toMutableList().apply {
                        add(DrinksGroup(id = generateTempId(), establishmentId = menu.establishmentId, name = newName))
                    }.toMutableStateList()
                )
            } else {
                val index = menu.drinksGroups.indexOfFirst { it.id == groupId }
                if (index != -1) {
                    menu = menu.copy(
                        drinksGroups = menu.drinksGroups.toMutableList().apply {
                            this[index] = this[index].copy(name = newName)
                        }.toMutableStateList()
                    )
                }
            }
        }
        editMode = EditMode.Idle
    }

    val deleteGroup: (Long?, Boolean) -> Unit = { groupId, isFood ->
        viewModel.trackAndDeleteGroup(groupId, isFood, menu)
        // Создаем новую копию меню для обновления UI
        menu = menu.copy(
            foodGroups = if (isFood) menu.foodGroups.filter { it.id != groupId }.toMutableStateList() else menu.foodGroups,
            drinksGroups = if (!isFood) menu.drinksGroups.filter { it.id != groupId }.toMutableStateList() else menu.drinksGroups
        )
        showConfirmDeleteGroup = null
    }

    val saveMenuItem: (MenuItem, Long?) -> Unit = saveMenuItem@{ item, groupId ->
        if (groupId == null) return@saveMenuItem

        when (item) {
            is Food -> {
                val targetGroupIndex = menu.foodGroups.indexOfFirst { it.id == groupId }
                if (targetGroupIndex != -1) {
                    val targetGroup = menu.foodGroups[targetGroupIndex]
                    val updatedFood = item.copy(foodGroupId = groupId)

                    val newItems = if (item.id == null || item.id!! < 0) {
                        targetGroup.items.toMutableList().apply {
                            add(updatedFood.copy(id = generateTempId()))
                        }
                    } else {
                        targetGroup.items.toMutableList().apply {
                            val index = indexOfFirst { it.id == item.id }
                            if (index != -1) this[index] = updatedFood
                            else add(updatedFood.copy(id = generateTempId()))
                        }
                    }

                    menu = menu.copy(
                        foodGroups = menu.foodGroups.toMutableList().apply {
                            this[targetGroupIndex] = targetGroup.copy(items = newItems.toMutableStateList())
                        }.toMutableStateList()
                    )
                }
            }
            is Drink -> {
                val targetGroupIndex = menu.drinksGroups.indexOfFirst { it.id == groupId }
                if (targetGroupIndex != -1) {
                    val targetGroup = menu.drinksGroups[targetGroupIndex]
                    val updatedDrink = item.copy(drinkGroupId = groupId)

                    val newItems = if (item.id == null || item.id!! < 0) {
                        targetGroup.items.toMutableList().apply {
                            add(updatedDrink.copy(id = generateTempId()))
                        }
                    } else {
                        targetGroup.items.toMutableList().apply {
                            val index = indexOfFirst { it.id == item.id }
                            if (index != -1) this[index] = updatedDrink
                            else add(updatedDrink.copy(id = generateTempId()))
                        }
                    }

                    menu = menu.copy(
                        drinksGroups = menu.drinksGroups.toMutableList().apply {
                            this[targetGroupIndex] = targetGroup.copy(items = newItems.toMutableStateList())
                        }.toMutableStateList()
                    )
                }
            }
            else -> {}
        }
        editMode = EditMode.Idle
    }

    val deleteItem: (Long?, Long?, Boolean) -> Unit = { groupId, itemId, isFood ->
        viewModel.trackAndDeleteItem(groupId, itemId, isFood, menu)
        // Обновляем UI после удаления элемента
        if (isFood) {
            val groupIndex = menu.foodGroups.indexOfFirst { it.id == groupId }
            if (groupIndex != -1) {
                val group = menu.foodGroups[groupIndex]
                val newItems = group.items.filter { it.id != itemId }.toMutableStateList()
                menu = menu.copy(
                    foodGroups = menu.foodGroups.toMutableList().apply {
                        this[groupIndex] = group.copy(items = newItems)
                    }.toMutableStateList()
                )
            }
        } else {
            val groupIndex = menu.drinksGroups.indexOfFirst { it.id == groupId }
            if (groupIndex != -1) {
                val group = menu.drinksGroups[groupIndex]
                val newItems = group.items.filter { it.id != itemId }.toMutableStateList()
                menu = menu.copy(
                    drinksGroups = menu.drinksGroups.toMutableList().apply {
                        this[groupIndex] = group.copy(items = newItems)
                    }.toMutableStateList()
                )
            }
        }
    }

    val saveMenu: () -> Unit = {
        viewModel.processMenuChanges(menu)
        println("Запущена отправка изменений меню: $menu")
    }

    // --- 4. ОТОБРАЖЕНИЕ UI (С УЧЕТОМ ЗАГРУЗКИ) ---
    if (isMenuLoading && serverMenu == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.MainContainer)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = AppTheme.colors.MainText
            )
        }
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.MainContainer),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                BottomAppBar(
                    containerColor = AppTheme.colors.MainContainer,
                    contentColor = AppTheme.colors.MainText,
                    actions = {
                        Text(
                            "Меню ресторана",
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.colors.MainText
                        )
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
                                        color = AppTheme.colors.MainText
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Сохранить",
                                        tint = AppTheme.colors.MainText
                                    )
                                }
                            },
                            text = {
                                if (!isLoading) {
                                    Text(
                                        "Сохранить меню",
                                        color = AppTheme.colors.MainText
                                    )
                                }
                            },
                            containerColor = AppTheme.colors.MainSuccess,
                            contentColor = AppTheme.colors.MainText
                        )
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.MainContainer)
                    .padding(horizontal = 5.dp)
                    .padding(paddingValues),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- БЛОК: Кнопки добавления новых групп ---
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppTheme.colors.SecondaryContainer
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainBorder)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { editMode = EditMode.GroupName(null, true, "") },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppTheme.colors.MainSuccess,
                                        contentColor = AppTheme.colors.MainText
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = AppTheme.colors.MainText
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Группа Еды",
                                        color = AppTheme.colors.MainText
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Button(
                                    onClick = { editMode = EditMode.GroupName(null, false, "") },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppTheme.colors.MainSuccess,
                                        contentColor = AppTheme.colors.MainText
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = AppTheme.colors.MainText
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Группа Напитков",
                                        color = AppTheme.colors.MainText
                                    )
                                }
                            }
                        }
                    }
                }

                // --- Группы Еды ---
                if (menu.foodGroups.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = AppTheme.colors.SecondaryContainer
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainBorder)
                            )
                        ) {
                            Text(
                                "Меню Еды",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.MainText,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    items(menu.foodGroups, key = { it.id ?: generateTempId() }) { group ->
                        FoodGroupEditor(
                            group = group,
                            onEditGroupName = { if (!isLoading) editMode = EditMode.GroupName(group.id, true, group.name ?: "") },
                            onAddItem = { if (!isLoading) editMode = EditMode.FoodItem(group.id, null) },
                            onDeleteItem = { itemId -> if (!isLoading) deleteItem(group.id, itemId, true) },
                            onEditItem = { item -> if (!isLoading) editMode = EditMode.FoodItem(group.id, item) },
                            onDeleteGroup = { if (!isLoading) showConfirmDeleteGroup = Pair(group.id, true) }
                        )
                    }
                }

                // --- Группы Напитков ---
                if (menu.drinksGroups.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = AppTheme.colors.SecondaryContainer
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainBorder)
                            )
                        ) {
                            Text(
                                "Меню Напитков",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.MainText,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    items(menu.drinksGroups, key = { it.id ?: generateTempId() }) { group ->
                        DrinksGroupEditor(
                            group = group,
                            onEditGroupName = { if (!isLoading) editMode = EditMode.GroupName(group.id, false, group.name ?: "") },
                            onAddItem = { if (!isLoading) editMode = EditMode.DrinkItem(group.id, null) },
                            onDeleteItem = { itemId -> if (!isLoading) deleteItem(group.id, itemId, false) },
                            onEditItem = { item -> if (!isLoading) editMode = EditMode.DrinkItem(group.id, item) },
                            onDeleteGroup = { if (!isLoading) showConfirmDeleteGroup = Pair(group.id, false) }
                        )
                    }
                }

                // --- Пустое состояние ---
                if (menu.foodGroups.isEmpty() && menu.drinksGroups.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = AppTheme.colors.SecondaryContainer
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainBorder)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = "Пустое меню",
                                    tint = AppTheme.colors.SecondaryText,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Меню пока пустое",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AppTheme.colors.MainText
                                )
                                Text(
                                    "Добавьте группы и позиции, чтобы заполнить меню",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppTheme.colors.SecondaryText,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 5. LaunchedEffect ДЛЯ СТАТУСА СОХРАНЕНИЯ (Snackbar) ---
    LaunchedEffect(saveStatus) {
        when (val status = saveStatus) {
            is SaveStatus.Success -> {
                snackbarHostState.showSnackbar(message = "Меню успешно сохранено! ✨", withDismissAction = true)
                viewModel.clearSaveStatus()
            }
            is SaveStatus.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Ошибка сохранения: ${status.message} ⚠️",
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )
                viewModel.clearSaveStatus()
            }
            else -> {}
        }
    }

    // --- 6. ДИАЛОГОВЫЕ ОКНА ---
    showConfirmDeleteGroup?.let { (groupId, isFood) ->
        AlertDialog(
            onDismissRequest = { if (!isLoading) showConfirmDeleteGroup = null },
            title = {
                Text(
                    "Подтвердите удаление",
                    color = AppTheme.colors.MainText
                )
            },
            text = {
                val groupName = if (isFood) menu.foodGroups.find { it.id == groupId }?.name else menu.drinksGroups.find { it.id == groupId }?.name
                Text(
                    "Вы уверены, что хотите удалить группу '${groupName ?: "Неизвестная группа"}' вместе со всеми компонентами?",
                    color = AppTheme.colors.SecondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = { deleteGroup(groupId, isFood) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.MainFailure,
                        contentColor = AppTheme.colors.MainText
                    ),
                    enabled = !isLoading
                ) {
                    Text("Удалить", color = AppTheme.colors.MainText)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDeleteGroup = null },
                    enabled = !isLoading
                ) {
                    Text("Отмена", color = AppTheme.colors.SecondaryText)
                }
            },
            containerColor = AppTheme.colors.MainContainer
        )
    }

    when (val mode = editMode) {
        is EditMode.FoodItem -> MenuEditDialog(
            isFood = true, foodItem = mode.item, onDismiss = { if (!isLoading) editMode = EditMode.Idle },
            onSave = { item -> saveMenuItem(item, mode.groupId) }, isLoading = isLoading
        )
        is EditMode.DrinkItem -> MenuEditDialog(
            isFood = false, drinkItem = mode.item, onDismiss = { if (!isLoading) editMode = EditMode.Idle },
            onSave = { item -> saveMenuItem(item, mode.groupId) }, isLoading = isLoading
        )
        is EditMode.GroupName -> GroupNameEditDialog(
            currentName = mode.currentName, onDismiss = { if (!isLoading) editMode = EditMode.Idle },
            onSave = { newName -> saveGroupName(mode.groupId, mode.isFood, newName) }, isLoading = isLoading
        )
        EditMode.Idle -> { /* Ничего не показываем */ }
    }
}

// --- КОМПОНЕНТЫ ---

@Composable
fun FoodGroupEditor(
    group: FoodGroup, onEditGroupName: () -> Unit, onAddItem: () -> Unit,
    onDeleteItem: (Long?) -> Unit, onEditItem: (Food) -> Unit, onDeleteGroup: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.SecondaryContainer
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.SecondaryBorder)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            GroupHeader(group.name ?: "", onEditGroupName, onAddItem, onDeleteGroup)
            Spacer(Modifier.height(12.dp))
            if (group.items.isNotEmpty()) {
                group.items.forEach { food ->
                    MenuItemCard(food.name ?: "", "Вес: ${food.weight} г, Цена: ${food.cost} р.", { onEditItem(food) }, { onDeleteItem(food.id) })
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                Text(
                    "Нет блюд в этой группе.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.SecondaryText,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun DrinksGroupEditor(
    group: DrinksGroup, onEditGroupName: () -> Unit, onAddItem: () -> Unit,
    onDeleteItem: (Long?) -> Unit, onEditItem: (Drink) -> Unit, onDeleteGroup: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.SecondaryContainer
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.SecondaryBorder)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            GroupHeader(group.name ?: "", onEditGroupName, onAddItem, onDeleteGroup)
            Spacer(Modifier.height(12.dp))
            if (group.items.isNotEmpty()) {
                group.items.forEach { drink ->
                    val optionsText = drink.options.joinToString { "${it.sizeMl} мл / ${it.cost} р." }
                    MenuItemCard(drink.name ?: "", optionsText, { onEditItem(drink) }, { onDeleteItem(drink.id) })
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                Text(
                    "Нет напитков в этой группе.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.SecondaryText,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun GroupHeader(name: String, onEditName: () -> Unit, onAddItem: () -> Unit, onDeleteGroup: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            color = AppTheme.colors.MainText
        )
        Row {
            IconButton(onClick = onEditName) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Редактировать название",
                    tint = AppTheme.colors.MainText
                )
            }
            IconButton(onClick = onAddItem) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Добавить компонент",
                    tint = AppTheme.colors.MainText
                )
            }
            IconButton(onClick = onDeleteGroup) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить группу",
                    tint = AppTheme.colors.MainFailure
                )
            }
        }
    }
}

@Composable
fun MenuItemCard(name: String, description: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.MainContainer
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.SecondaryBorder)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.MainText
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.SecondaryText
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Редактировать компонент",
                    tint = AppTheme.colors.MainText
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Удалить компонент",
                    tint = AppTheme.colors.MainFailure
                )
            }
        }
    }
}

// --- GroupNameEditDialog ---
@Composable
fun GroupNameEditDialog(currentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit, isLoading: Boolean) {
    var newName by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Редактировать название группы",
                color = AppTheme.colors.MainText
            )
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { if (!isLoading) newName = it },
                label = {
                    Text(
                        "Название",
                        color = AppTheme.colors.SecondaryText
                    )
                },
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.MainBorder,
                    unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                    focusedTextColor = AppTheme.colors.MainText,
                    unfocusedTextColor = AppTheme.colors.MainText,
                    focusedLabelColor = AppTheme.colors.SecondaryText,
                    unfocusedLabelColor = AppTheme.colors.SecondaryText,
                    cursorColor = AppTheme.colors.MainText
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(newName) },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.MainSuccess,
                    contentColor = AppTheme.colors.MainText
                )
            ) {
                Text("Сохранить", color = AppTheme.colors.MainText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Отмена", color = AppTheme.colors.SecondaryText)
            }
        },
        containerColor = AppTheme.colors.MainContainer
    )
}



// --- MenuEditDialog ---
@Composable
fun MenuEditDialog(
    isFood: Boolean,
    foodItem: Food? = null,
    drinkItem: Drink? = null,
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit,
    isLoading: Boolean
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
        title = {
            Text(
                "${if (isNew) "Добавить" else "Редактировать"} ${if (isFood) "Блюдо" else "Напиток"}",
                color = AppTheme.colors.MainText
            )
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxHeight(0.7f)) {
                // --- Общие поля ---
                item {
                    Text(
                        "Основная информация",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTheme.colors.MainText
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = {
                            Text(
                                "Название",
                                color = AppTheme.colors.SecondaryText
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.colors.MainBorder,
                            unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                            focusedTextColor = AppTheme.colors.MainText,
                            unfocusedTextColor = AppTheme.colors.MainText,
                            focusedLabelColor = AppTheme.colors.SecondaryText,
                            unfocusedLabelColor = AppTheme.colors.SecondaryText,
                            cursorColor = AppTheme.colors.MainText
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ingredients,
                        onValueChange = { ingredients = it },
                        label = {
                            Text(
                                "Ингредиенты (через запятую)",
                                color = AppTheme.colors.SecondaryText
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.colors.MainBorder,
                            unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                            focusedTextColor = AppTheme.colors.MainText,
                            unfocusedTextColor = AppTheme.colors.MainText,
                            focusedLabelColor = AppTheme.colors.SecondaryText,
                            unfocusedLabelColor = AppTheme.colors.SecondaryText,
                            cursorColor = AppTheme.colors.MainText
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // --- КБЖУ ---
                item {
                    Text(
                        "КБЖУ на 100 г",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTheme.colors.MainText
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = calories,
                            onValueChange = { calories = it.filter { it.isDigit() || it == '.' } },
                            label = {
                                Text(
                                    "Ккал",
                                    color = AppTheme.colors.SecondaryText
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colors.MainBorder,
                                unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                                focusedTextColor = AppTheme.colors.MainText,
                                unfocusedTextColor = AppTheme.colors.MainText,
                                focusedLabelColor = AppTheme.colors.SecondaryText,
                                unfocusedLabelColor = AppTheme.colors.SecondaryText,
                                cursorColor = AppTheme.colors.MainText
                            )
                        )
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { protein = it.filter { it.isDigit() || it == '.' } },
                            label = {
                                Text(
                                    "Белки",
                                    color = AppTheme.colors.SecondaryText
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colors.MainBorder,
                                unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                                focusedTextColor = AppTheme.colors.MainText,
                                unfocusedTextColor = AppTheme.colors.MainText,
                                focusedLabelColor = AppTheme.colors.SecondaryText,
                                unfocusedLabelColor = AppTheme.colors.SecondaryText,
                                cursorColor = AppTheme.colors.MainText
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fat,
                            onValueChange = { fat = it.filter { it.isDigit() || it == '.' } },
                            label = {
                                Text(
                                    "Жиры",
                                    color = AppTheme.colors.SecondaryText
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colors.MainBorder,
                                unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                                focusedTextColor = AppTheme.colors.MainText,
                                unfocusedTextColor = AppTheme.colors.MainText,
                                focusedLabelColor = AppTheme.colors.SecondaryText,
                                unfocusedLabelColor = AppTheme.colors.SecondaryText,
                                cursorColor = AppTheme.colors.MainText
                            )
                        )
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { carbs = it.filter { it.isDigit() || it == '.' } },
                            label = {
                                Text(
                                    "Углеводы",
                                    color = AppTheme.colors.SecondaryText
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colors.MainBorder,
                                unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                                focusedTextColor = AppTheme.colors.MainText,
                                unfocusedTextColor = AppTheme.colors.MainText,
                                focusedLabelColor = AppTheme.colors.SecondaryText,
                                unfocusedLabelColor = AppTheme.colors.SecondaryText,
                                cursorColor = AppTheme.colors.MainText
                            )
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // --- Специфические поля ---
                if (isFood) {
                    item {
                        Text(
                            "Цена и Вес (для Блюда)",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.colors.MainText
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = foodCost,
                            onValueChange = { foodCost = it.filter { it.isDigit() || it == '.' } },
                            label = {
                                Text(
                                    "Цена (р.)",
                                    color = AppTheme.colors.SecondaryText
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colors.MainBorder,
                                unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                                focusedTextColor = AppTheme.colors.MainText,
                                unfocusedTextColor = AppTheme.colors.MainText,
                                focusedLabelColor = AppTheme.colors.SecondaryText,
                                unfocusedLabelColor = AppTheme.colors.SecondaryText,
                                cursorColor = AppTheme.colors.MainText
                            )
                        )
                        OutlinedTextField(
                            value = foodWeight,
                            onValueChange = { foodWeight = it.filter { it.isDigit() } },
                            label = {
                                Text(
                                    "Вес порции (г)",
                                    color = AppTheme.colors.SecondaryText
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colors.MainBorder,
                                unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                                focusedTextColor = AppTheme.colors.MainText,
                                unfocusedTextColor = AppTheme.colors.MainText,
                                focusedLabelColor = AppTheme.colors.SecondaryText,
                                unfocusedLabelColor = AppTheme.colors.SecondaryText,
                                cursorColor = AppTheme.colors.MainText
                            )
                        )
                    }
                } else {
                    item {
                        Text(
                            "Опции (для Напитка)",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.colors.MainText
                        )
                    }
                    items(drinkOptions.indices.toList()) { index ->
                        val option = drinkOptions[index]
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = option.sizeMl.toString(),
                                onValueChange = { if (!isLoading) drinkOptions[index] = option.copy(sizeMl = it.filter { it.isDigit() }.toIntOrNull() ?: 0) },
                                label = {
                                    Text(
                                        "Объем (мл)",
                                        color = AppTheme.colors.SecondaryText
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).padding(end = 4.dp),
                                enabled = !isLoading,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppTheme.colors.MainBorder,
                                    unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                                    focusedTextColor = AppTheme.colors.MainText,
                                    unfocusedTextColor = AppTheme.colors.MainText,
                                    focusedLabelColor = AppTheme.colors.SecondaryText,
                                    unfocusedLabelColor = AppTheme.colors.SecondaryText,
                                    cursorColor = AppTheme.colors.MainText
                                )
                            )
                            OutlinedTextField(
                                value = option.cost.toString(),
                                onValueChange = { if (!isLoading) drinkOptions[index] = option.copy(cost = it.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0) },
                                label = {
                                    Text(
                                        "Цена (р.)",
                                        color = AppTheme.colors.SecondaryText
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).padding(start = 4.dp),
                                enabled = !isLoading,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppTheme.colors.MainBorder,
                                    unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                                    focusedTextColor = AppTheme.colors.MainText,
                                    unfocusedTextColor = AppTheme.colors.MainText,
                                    focusedLabelColor = AppTheme.colors.SecondaryText,
                                    unfocusedLabelColor = AppTheme.colors.SecondaryText,
                                    cursorColor = AppTheme.colors.MainText
                                )
                            )
                            IconButton(
                                onClick = { if (!isLoading && drinkOptions.size > 1) drinkOptions.removeAt(index) },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Удалить опцию",
                                    tint = AppTheme.colors.MainFailure
                                )
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = { if (!isLoading) drinkOptions.add(DrinkOption(sizeMl = 0, cost = 0.0)) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.SecondaryContainer,
                                contentColor = AppTheme.colors.MainText
                            )
                        ) {
                            Text(
                                "Добавить опцию",
                                color = AppTheme.colors.MainText
                            )
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
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.MainSuccess,
                    contentColor = AppTheme.colors.MainText
                )
            ) {
                Text("Сохранить", color = AppTheme.colors.MainText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Отмена", color = AppTheme.colors.SecondaryText)
            }
        },
        containerColor = AppTheme.colors.MainContainer
    )
}