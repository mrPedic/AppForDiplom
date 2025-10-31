package com.example.roamly.ui.screens

import android.widget.Toast
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
import com.example.roamly.entity.DTO.EstablishmentDisplayDto
import com.example.roamly.entity.ViewModel.EstablishmentViewModel // Импорт вашего ViewModel
import com.example.roamly.ui.screens.sealed.EstablishmentScreens

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    // Состояние для поля поиска
    var searchQuery by remember { mutableStateOf("") }

    // Получаем результаты поиска из ViewModel
    // NOTE: Вам нужно убедиться, что у вас есть StateFlow в ViewModel для хранения результатов поиска
    // или использовать уже существующий userEstablishments, если он подходит для этой цели.
    val searchResults by viewModel.userEstablishments.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)

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
                // ⭐ Выполняем поиск при каждом изменении (или можно добавить debounce)
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
                modifier = Modifier.fillMaxWidth(),
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
    establishment: EstablishmentDisplayDto,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                // ⭐ НАВИГАЦИЯ НА ЭКРАН ДЕТАЛЕЙ
                navController.navigate(
                    EstablishmentScreens.EstablishmentDetail.createRoute(establishment.id)
                )
            })
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
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
        // Опционально: можно показать статус или рейтинг
        Text(
            text = "Рейтинг: ${String.format("%.1f", establishment.rating)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}