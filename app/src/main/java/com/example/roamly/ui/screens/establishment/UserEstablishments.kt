package com.example.roamly.ui.screens.establishment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.establishment.EstablishmentDisplayDto
import com.example.roamly.entity.classes.EstablishmentStatus
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.sealed.EstablishmentScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEstablishmentsScreen(
    navController: NavController,
    userViewModel: UserViewModel = hiltViewModel(), // Используем hiltViewModel по умолчанию, если не передан
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    val user by userViewModel.user.collectAsState()

    val establishments by viewModel.userEstablishments.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val errorMessage by viewModel.errorMessage.collectAsState(initial = null)

    val userId = user.id

    // Запуск загрузки данных при входе на экран
    LaunchedEffect(userId) {
        userId?.let {
            // Вызываем функцию ViewModel для загрузки заведений пользователя
            viewModel.fetchEstablishmentsByUserId(it)
        }
    }

    // Новое: Состояние для поиска и фильтрации
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilters by remember { mutableStateOf(setOf<EstablishmentStatus>()) } // Фильтры по статусу
    var showFilterDialog by remember { mutableStateOf(false) }

    // Фильтрованные и отфильтрованные по поиску заведения
    val filteredEstablishments = establishments
        .filter { est ->
            (selectedStatusFilters.isEmpty() || est.status in selectedStatusFilters) &&
                    (searchQuery.isEmpty() ||
                            est.name.contains(searchQuery, ignoreCase = true) ||
                            est.address.contains(searchQuery, ignoreCase = true))
        }

    // Диалог для выбора фильтров
    if (showFilterDialog) {
        FilterDialog(
            currentSelections = selectedStatusFilters,
            onDismiss = { showFilterDialog = false },
            onConfirm = { newSelections ->
                selectedStatusFilters = newSelections
                showFilterDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои заведения") },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Фильтры")
                    }
                }
            )
        },
        bottomBar = {
            BottomButtons(navController = navController)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column {
                // Поле поиска
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск по имени или адресу") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    singleLine = true
                )

                when {
                    isLoading -> {
                        // Индикатор загрузки
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    errorMessage != null -> {
                        // Сообщение об ошибке
                        Text(
                            text = "Ошибка загрузки: $errorMessage",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp)
                        )
                        // Кнопка для повторной попытки
                        Button(
                            onClick = { userId?.let { viewModel.fetchEstablishmentsByUserId(it) } },
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(24.dp)
                        ) {
                            Text("Повторить")
                        }
                    }
                    filteredEstablishments.isEmpty() && !isLoading -> { // Добавлена проверка !isLoading для избежания мерцания
                        // Пустой список
                        Text(
                            text = "У вас пока нет созданных заведений или ничего не найдено.",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    else -> {
                        // Отображение списка
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredEstablishments) { establishment ->
                                // ⭐ ПЕРЕДАЕМ ССЫЛКУ НА VM В EstablishmentItem
                                EstablishmentItem(
                                    establishment = establishment,
                                    viewModel = viewModel,
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomButtons(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = {
            // Переход на экран одобрения броней (предполагаем, что есть маршрут EstablishmentScreens.ApproveBookings)
            navController.navigate(EstablishmentScreens.ApproveBookings.route)
        }) {
            Text("Одобрение броней")
        }

        Button(onClick = {
            // TODO: Реализовать кнопку для просмотра броней
            // navController.navigate(EstablishmentScreens.ViewBookings.route)
        }) {
            Text("Просмотр броней")
        }
    }
}

@Composable
fun EstablishmentItem(
    establishment: EstablishmentDisplayDto,
    viewModel: EstablishmentViewModel,
    navController: NavController
) {
    val showResubmitButton = establishment.status == EstablishmentStatus.REJECTED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate(
                    EstablishmentScreens.EstablishmentDetail.createRoute(establishment.id)
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = establishment.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = establishment.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Статус: ${formatStatus(establishment.status)}",
                        color = getStatusColor(establishment.status),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Создано: ${establishment.dateOfCreation}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = "Рейтинг: ${String.format("%.1f", establishment.rating)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ⭐ НОВЫЙ БЛОК: Кнопка повторной отправки
            if (showResubmitButton) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        // Здесь мы вызываем повторную отправку
                        viewModel.resubmitEstablishmentForReview(establishment.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Отправить на повторное рассмотрение")
                }
            }
        }
    }
}

/**
 * Вспомогательная функция для форматирования статуса (для UI).
 */
@Composable
private fun formatStatus(status: EstablishmentStatus): String {
    return when (status) {
        EstablishmentStatus.PENDING_APPROVAL -> "Ожидает одобрения"
        EstablishmentStatus.ACTIVE -> "Одобрено"
        EstablishmentStatus.REJECTED -> "Отклонено"
        else -> "Неизвестный"
    }
}

/**
 * Вспомогательная функция для определения цвета статуса.
 */
@Composable
private fun getStatusColor(status: EstablishmentStatus): Color {
    return when (status) {
        EstablishmentStatus.PENDING_APPROVAL -> MaterialTheme.colorScheme.tertiary
        EstablishmentStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        EstablishmentStatus.REJECTED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.error
    }
}

/**
 * Диалог для выбора фильтров по статусу
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDialog(
    currentSelections: Set<EstablishmentStatus>,
    onDismiss: () -> Unit,
    onConfirm: (Set<EstablishmentStatus>) -> Unit
) {
    // Временное состояние для выбора в диалоге
    var tempSelections by remember { mutableStateOf(currentSelections) }
    val allStatuses = remember { EstablishmentStatus.entries.toTypedArray() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Фильтр по статусу") },
        text = {
            Column {
                // Кнопки "Выбрать все" / "Очистить"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { tempSelections = allStatuses.toSet() }) {
                        Text("Выбрать все")
                    }
                    TextButton(onClick = { tempSelections = emptySet() }) {
                        Text("Очистить")
                    }
                }
                HorizontalDivider()
                // Список статусов с чекбоксами
                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(allStatuses) { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempSelections = if (status in tempSelections) {
                                        tempSelections - status
                                    } else {
                                        tempSelections + status
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = status in tempSelections,
                                onCheckedChange = { isChecked ->
                                    tempSelections = if (isChecked) {
                                        tempSelections + status
                                    } else {
                                        tempSelections - status
                                    }
                                }
                            )
                            Text(formatStatus(status), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempSelections) }) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}