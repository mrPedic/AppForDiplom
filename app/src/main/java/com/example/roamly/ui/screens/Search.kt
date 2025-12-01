package com.example.roamly.ui.screens

import android.annotation.SuppressLint
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.EstablishmentSearchResultDto
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.entity.TypeOfEstablishment
import com.example.roamly.entity.convertTypeToWord
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color 
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }

    val selectedTypes by viewModel.selectedTypes.collectAsState(initial = emptySet())

    val searchResults by viewModel.establishmentSearchResults.collectAsState(initial = emptyList())
    val isLoading by viewModel.isSearchLoading.collectAsState(initial = false)

    val recentEstablishments by viewModel.recentEstablishments.collectAsState(initial = emptyList())


    LaunchedEffect(searchQuery) {
        viewModel.searchEstablishments(searchQuery)
    }

    if (showFilterDialog) {
        FilterDialog(
            currentSelections = selectedTypes,
            onDismiss = { showFilterDialog = false },
            onConfirm = { newSelections ->
                viewModel.updateFilters(newSelections)
                showFilterDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // --- 1. Поле поиска и фильтр ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Название или адрес") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Поиск") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { showFilterDialog = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Фильтр")
            }
        }

        // --- 2. Активные фильтры ---
        if (selectedTypes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                selectedTypes.forEach { type ->
                    FilterChip(
                        selected = true,
                        onClick = { /* Удаление фильтра */ },
                        label = { Text(convertTypeToWord(type)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. Отображение результатов/истории ---

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (searchQuery.isEmpty() && recentEstablishments.isNotEmpty()) {
            // Если строка поиска пуста и есть история
            Text(
                text = "Недавние заведения",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // ⭐ ИСПРАВЛЕНИЕ: LazyColumn для Истории и результатов
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Отступ между карточками
            ) {
                items(recentEstablishments) { establishment ->
                    EstablishmentResultItem(
                        establishment = establishment,
                        navController = navController,
                        viewModel = viewModel,
                        isRecent = true
                    )
                    // ⭐ УДАЛЕН HorizontalDivider
                }

                if (searchResults.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Результаты поиска",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        // Оставляем разделитель между заголовком "Результаты поиска" и элементами
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
                    }
                }

                items(searchResults) { establishment ->
                    EstablishmentResultItem(
                        establishment = establishment,
                        navController = navController,
                        viewModel = viewModel,
                        isRecent = false
                    )
                    // ⭐ УДАЛЕН HorizontalDivider
                }
            }

        } else if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
            Text(text = "Заведений не найдено.", modifier = Modifier.padding(8.dp))
        } else {
            // Если история пуста или мы в активном поиске и есть результаты
            // ⭐ ИСПРАВЛЕНИЕ: LazyColumn для обычных результатов
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { // Отступ между карточками
                if (searchQuery.isNotEmpty()) {
                    item {
                        Text(
                            text = "Результаты поиска",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        // Оставляем разделитель после заголовка
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
                    }
                }
                items(searchResults) { establishment ->
                    EstablishmentResultItem(
                        establishment = establishment,
                        navController = navController,
                        viewModel = viewModel,
                        isRecent = false
                    )
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun EstablishmentResultItem(
    establishment: EstablishmentSearchResultDto,
    navController: NavController,
    viewModel: EstablishmentViewModel,
    isRecent: Boolean
) {
    // Выбираем цвет фона: серый (surfaceVariant) для истории, прозрачный для обычных результатов
    val backgroundColor = if (isRecent) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                viewModel.addRecentEstablishment(establishment)
                navController.navigate(
                    EstablishmentScreens.EstablishmentDetail.createRoute(establishment.id)
                )
            }),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Небольшая тень
        colors = CardDefaults.cardColors(containerColor = backgroundColor) // Применяем цвет фона к Card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) { // Занимаем доступное пространство
                Text(
                    text = establishment.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = establishment.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Отображение рейтинга
            Text(
                text = "Рейтинг: ${String.format("%.1f", establishment.rating)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/**
 * Новый Composable для диалога выбора фильтрова
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDialog(
    currentSelections: Set<TypeOfEstablishment>,
    onDismiss: () -> Unit,
    onConfirm: (Set<TypeOfEstablishment>) -> Unit
) {
    // Временное состояние, пока пользователь выбирает в диалоге
    var tempSelections by remember { mutableStateOf(currentSelections) }
    val allTypes = remember { TypeOfEstablishment.entries.toTypedArray() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Фильтр по типу заведения") },
        text = {
            Column {
                // Кнопки "Выбрать все" / "Очистить"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { tempSelections = allTypes.toSet() }) {
                        Text("Выбрать все")
                    }
                    TextButton(onClick = { tempSelections = emptySet() }) {
                        Text("Очистить")
                    }
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                // Список всех типов с чекбоксами
                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(allTypes) { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempSelections = if (type in tempSelections) {
                                        tempSelections - type
                                    } else {
                                        tempSelections + type
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = type in tempSelections,
                                onCheckedChange = { isChecked ->
                                    tempSelections = if (isChecked) {
                                        tempSelections + type
                                    } else {
                                        tempSelections - type
                                    }
                                }
                            )
                            Text(convertTypeToWord(type), modifier = Modifier.padding(start = 8.dp))
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