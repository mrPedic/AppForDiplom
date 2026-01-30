package com.example.roamly.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.AdminQueryDto
import com.example.roamly.entity.ViewModel.AdminSqlViewModel
import com.example.roamly.ui.screens.sealed.AdminScreens
import com.example.roamly.ui.theme.AppTheme

@Composable
fun SqlListScreen(
    navController: NavController,
    viewModel: AdminSqlViewModel = hiltViewModel()
) {
    val queries by viewModel.savedQueries.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchQueries()
    }

    Scaffold(
        containerColor = AppTheme.colors.MainContainer,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(AdminScreens.SqlDetail.createRoute(0)) },
                containerColor = AppTheme.colors.MainSuccess
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Сохраненные запросы",
                style = MaterialTheme.typography.headlineMedium,
                color = AppTheme.colors.MainText
            )
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp) // Отступ под FAB
            ) {
                items(queries) { query ->
                    SqlListCard(
                        query = query,
                        onClick = {
                            // Переход к деталям
                            navController.navigate(AdminScreens.SqlDetail.createRoute(query.id))
                        },
                        onDelete = { viewModel.deleteQuery(query.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SqlListCard(
    query: AdminQueryDto,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick, // Используем встроенный onClick для лучшей обработки нажатий
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.SecondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // --- ВЕРХНЯЯ ЧАСТЬ: Заголовок и Кнопка удаления ---
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = query.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.MainText,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                // Кнопка удаления (минимальный размер, чтобы не перекрывать клик по карточке)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AppTheme.colors.MainFailure,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // --- ОПИСАНИЕ ---
            if (query.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = query.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.SecondaryText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- SQL КОД (на отдельном фоне) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppTheme.colors.MainContainer.copy(alpha = 0.5f)) // Более темный фон
                    .padding(12.dp)
            ) {
                Text(
                    text = query.sqlQuery,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace, // Шрифт как в коде
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = AppTheme.colors.MainText.copy(alpha = 0.9f),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}