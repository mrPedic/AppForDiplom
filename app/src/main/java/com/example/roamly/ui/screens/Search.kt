package com.example.roamly.ui.screens

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


    // Изменение фильтров (selectedTypes) будет обработано функцией updateFilters
    // и объединено (combine) в ViewModel.
    LaunchedEffect(searchQuery) {
        // Вызываем метод VM, который просто обновляет _searchQueryFlow.
        viewModel.searchEstablishments(searchQuery)
    }

    // ⭐ Показываем диалог фильтра
    if (showFilterDialog) {
        FilterDialog(
            currentSelections = selectedTypes, // Используем состояние из VM
            onDismiss = { showFilterDialog = false },
            onConfirm = { newSelections ->
                viewModel.updateFilters(newSelections)
                showFilterDialog = false
                // ViewModel автоматически запустит новый поиск через combine
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it }, // Просто обновляем состояние
                label = { Text("Название или адрес") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Поиск") },
                modifier = Modifier.weight(1f), // Занимает все место
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Кнопка фильтра
            IconButton(onClick = { showFilterDialog = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Фильтр")
            }
        }

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
                        onClick = { /* Можно удалить по клику, если хотите */ },
                        label = { Text(convertTypeToWord(type)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Отображение результатов
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
            Text(text = "Заведений не найдено.", modifier = Modifier.padding(8.dp))
        } else {
            LazyColumn {
                items(searchResults) { establishment ->
                    EstablishmentResultItem(
                        establishment = establishment,
                        navController = navController
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun EstablishmentResultItem(
    establishment: EstablishmentSearchResultDto,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                navController.navigate(
                    EstablishmentScreens.EstablishmentDetail.createRoute(establishment.id)
                )
            })
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
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
            // Rating - это Double в новом DTO
            text = "Рейтинг: ${String.format("%.1f", establishment.rating)}",
            style = MaterialTheme.typography.bodyMedium, // Немного увеличен размер
            color = MaterialTheme.colorScheme.secondary
        )
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
    val allTypes = remember { TypeOfEstablishment.values() }

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
                Divider()
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