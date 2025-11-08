package com.example.roamly.ui.screens

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
import com.example.roamly.entity.DTO.EstablishmentSearchResultDto // ⭐ ИСПРАВЛЕНИЕ: Используем новый DTO
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.entity.DTO.EstablishmentDisplayDto // Оставляем, если он используется в другом месте

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }

    // ⭐ ИСПРАВЛЕНИЕ 1: Используем правильные StateFlow для поиска
    val searchResults by viewModel.establishmentSearchResults.collectAsState(initial = emptyList())
    val isLoading by viewModel.isSearchLoading.collectAsState(initial = false) // Используем правильный флаг загрузки

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Поле поиска
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { newValue ->
                searchQuery = newValue
                // ⭐ ВЫЗОВ VM: Логика предотвращения HTTP 400 уже в ViewModel
                viewModel.searchEstablishments(newValue)
            },
            label = { Text("Название или адрес") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Поиск") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

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
                    // ⭐ ИСПРАВЛЕНИЕ 2: Передаем EstablishmentSearchResultDto
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

// ⭐ ИСПРАВЛЕНИЕ: ОБНОВЛЕНИЕ EstablishmentResultItem для приема EstablishmentSearchResultDto
@Composable
fun EstablishmentResultItem(
    establishment: EstablishmentSearchResultDto, // Используем EstablishmentSearchResultDto
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