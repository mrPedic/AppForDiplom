package com.example.roamly.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.establishment.EstablishmentSearchResultDto
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.entity.classes.TypeOfEstablishment
import com.example.roamly.entity.classes.convertTypeToWord
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontWeight
import com.example.roamly.ui.theme.AppTheme
import com.example.roamly.icons_image_vector.FilterList

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
            .padding(horizontal = 5.dp)
            .background(AppTheme.colors.MainContainer)
    ) {
        Spacer(modifier = Modifier.fillMaxWidth().height(13.dp))
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
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.MainBorder,
                    unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                    focusedLabelColor = AppTheme.colors.MainText,
                    unfocusedLabelColor = AppTheme.colors.SecondaryText,
                    cursorColor = AppTheme.colors.MainBorder,
                    focusedLeadingIconColor = AppTheme.colors.MainBorder,
                    unfocusedLeadingIconColor = AppTheme.colors.SecondaryText,
                    focusedContainerColor = AppTheme.colors.MainContainer,
                    unfocusedContainerColor = AppTheme.colors.MainContainer,
                    unfocusedTextColor = AppTheme.colors.MainText,
                    focusedTextColor = AppTheme.colors.MainText,
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { showFilterDialog = true },
                colors = IconButtonDefaults.iconButtonColors(contentColor = AppTheme.colors.MainBorder)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Фильтр")
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
                        enabled = true,
                        selected = true,
                        onClick = { /* Удаление фильтра */ },
                        label = { Text(convertTypeToWord(type)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppTheme.colors.SelectedItem,
                            selectedLabelColor = AppTheme.colors.MainContainer,
                            selectedLeadingIconColor = AppTheme.colors.MainContainer,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            selectedBorderColor = AppTheme.colors.SelectedItem,
                            borderColor = AppTheme.colors.SecondaryBorder,
                            enabled = true,
                            selected = true
                        ),
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
                CircularProgressIndicator(color = AppTheme.colors.MainBorder)
            }
        } else if (searchQuery.isEmpty() && recentEstablishments.isNotEmpty()) {
            // Если строка поиска пуста и есть история
            Text(
                text = "Недавние заведения",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.MainText,
                modifier = Modifier.padding(bottom = 8.dp)
            )

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
                }

                if (searchResults.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Результаты поиска",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.MainText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        // Оставляем разделитель между заголовком "Результаты поиска" и элементами
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            AppTheme.colors.SecondaryBorder
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

        } else if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
            Text(
                text = "Заведений не найдено.",
                color = AppTheme.colors.SecondaryText,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            // Если история пуста или мы в активном поиске и есть результаты
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { // Отступ между карточками
                if (searchQuery.isNotEmpty()) {
                    item {
                        Text(
                            text = "Результаты поиска",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.MainText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        // Оставляем разделитель после заголовка
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            AppTheme.colors.SecondaryBorder
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
    val backgroundColor = AppTheme.colors.SecondaryContainer

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
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = AppTheme.colors.MainText
        )
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
                    color = AppTheme.colors.MainText
                )
                Text(
                    text = establishment.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.SecondaryText
                )
            }
            // Отображение рейтинга
            Text(
                text = "Рейтинг: ${String.format("%.1f", establishment.rating)}",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.SecondaryText
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
        containerColor = AppTheme.colors.SecondaryContainer,
        titleContentColor = AppTheme.colors.MainText,
        textContentColor = AppTheme.colors.SecondaryText,
        title = { Text("Фильтр по типу заведения") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { tempSelections = allTypes.toSet() },
                        colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.MainText)
                    ) {
                        Text("Выбрать все")
                    }
                    TextButton(
                        onClick = { tempSelections = emptySet() },
                        colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.MainText)
                    ) {
                        Text("Очистить")
                    }
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, AppTheme.colors.SecondaryBorder)
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
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AppTheme.colors.SelectedItem,
                                    uncheckedColor = AppTheme.colors.SecondaryText,
                                    checkmarkColor = AppTheme.colors.SecondaryContainer
                                )
                            )
                            Text(
                                convertTypeToWord(type),
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
                    contentColor = AppTheme.colors.MainContainer
                )
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.MainText)
            ) {
                Text("Отмена")
            }
        }
    )
}