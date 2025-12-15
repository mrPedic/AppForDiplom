package com.example.roamly.ui.screens.establishment

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.establishment.EstablishmentDisplayDto
import com.example.roamly.entity.classes.EstablishmentStatus
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEstablishmentsScreen(
    navController: NavController,
    userViewModel: UserViewModel = hiltViewModel(),
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
            viewModel.fetchEstablishmentsByUserId(it)
        }
    }

    // Новое: Состояние для поиска и фильтрации
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilters by remember { mutableStateOf(setOf<EstablishmentStatus>()) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Фильтрованные заведения
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
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Мои заведения",
                        color = AppTheme.colors.MainText
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.SecondaryContainer,
                    navigationIconContentColor = AppTheme.colors.MainText,
                    titleContentColor = AppTheme.colors.MainText,
                    actionIconContentColor = AppTheme.colors.MainText
                ),
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Фильтры",
                            tint = AppTheme.colors.MainText
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomButtons(navController = navController)
        },
        containerColor = AppTheme.colors.MainContainer
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.MainContainer)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Поле поиска
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = {
                        Text(
                            "Поиск по имени или адресу",
                            color = AppTheme.colors.SecondaryText
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = AppTheme.colors.MainText
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 5.dp, end = 5.dp, top = 16.dp, bottom = 16.dp),
                    singleLine = true,
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

                when {
                    isLoading -> {
                        // Индикатор загрузки
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AppTheme.colors.MainText
                            )
                        }
                    }
                    errorMessage != null -> {
                        // Сообщение об ошибке
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Ошибка загрузки: $errorMessage",
                                color = AppTheme.colors.MainFailure,
                                modifier = Modifier.padding(16.dp)
                            )
                            // Кнопка для повторной попытки
                            Button(
                                onClick = { userId?.let { viewModel.fetchEstablishmentsByUserId(it) } },
                                modifier = Modifier.padding(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppTheme.colors.MainSuccess,
                                    contentColor = AppTheme.colors.MainText
                                )
                            ) {
                                Text("Повторить")
                            }
                        }
                    }
                    filteredEstablishments.isEmpty() && !isLoading -> {
                        // Пустой список
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "У вас пока нет созданных заведений или ничего не найдено.",
                                style = MaterialTheme.typography.titleMedium,
                                color = AppTheme.colors.MainText
                            )
                        }
                    }
                    else -> {
                        // Отображение списка
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 5.dp, end = 5.dp, top = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredEstablishments) { establishment ->
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
            .padding(16.dp)
            .background(AppTheme.colors.MainContainer),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                navController.navigate(EstablishmentScreens.ApproveBookings.route)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.MainSuccess,
                contentColor = AppTheme.colors.MainText
            )
        ) {
            Text("Одобрение броней")
        }

        Button(
            onClick = {
                // TODO: Реализовать кнопку для просмотра броней
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.MainSuccess,
                contentColor = AppTheme.colors.MainText
            )
        ) {
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
            containerColor = AppTheme.colors.SecondaryContainer
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.SecondaryBorder)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = establishment.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.MainText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = establishment.address,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.SecondaryText
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
                        modifier = Modifier.padding(top = 4.dp),
                        color = AppTheme.colors.SecondaryText
                    )
                }

                Text(
                    text = "Рейтинг: ${String.format("%.1f", establishment.rating)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.MainText
                )
            }

            if (showResubmitButton) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.resubmitEstablishmentForReview(establishment.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.MainSuccess,
                        contentColor = AppTheme.colors.MainText
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
private fun getStatusColor(status: EstablishmentStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        EstablishmentStatus.PENDING_APPROVAL -> AppTheme.colors.SecondarySuccess
        EstablishmentStatus.ACTIVE -> AppTheme.colors.MainSuccess
        EstablishmentStatus.REJECTED -> AppTheme.colors.MainFailure
        else -> AppTheme.colors.MainFailure
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
    var tempSelections by remember { mutableStateOf(currentSelections) }
    val allStatuses = remember { EstablishmentStatus.entries.toTypedArray() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Фильтр по статусу",
                color = AppTheme.colors.MainText
            )
        },
        text = {
            Column {
                // Кнопки "Выбрать все" / "Очистить"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { tempSelections = allStatuses.toSet() }
                    ) {
                        Text(
                            "Выбрать все",
                            color = AppTheme.colors.MainText
                        )
                    }
                    TextButton(
                        onClick = { tempSelections = emptySet() }
                    ) {
                        Text(
                            "Очистить",
                            color = AppTheme.colors.MainText
                        )
                    }
                }
                HorizontalDivider(
                    color = AppTheme.colors.SecondaryBorder
                )
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
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AppTheme.colors.MainSuccess,
                                    uncheckedColor = AppTheme.colors.SecondaryBorder,
                                    checkmarkColor = AppTheme.colors.MainText
                                )
                            )
                            Text(
                                formatStatus(status),
                                modifier = Modifier.padding(start = 8.dp),
                                color = AppTheme.colors.MainText
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tempSelections) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.MainSuccess,
                    contentColor = AppTheme.colors.MainText
                )
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Отмена",
                    color = AppTheme.colors.SecondaryText
                )
            }
        },
        containerColor = AppTheme.colors.MainContainer
    )
}