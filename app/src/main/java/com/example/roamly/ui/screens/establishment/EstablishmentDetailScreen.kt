@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.roamly.ui.screens.establishment

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.*
import com.example.roamly.ui.screens.sealed.EstablishmentScreens

@Composable
fun EstablishmentDetailScreen(
    navController: NavController,
    establishmentId: Long,
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    // Получаем состояние из ViewModel
    val establishment by viewModel.currentEstablishment.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Вкладки
    val tabs = listOf("Описание", "Меню", "Карта", "Отзывы")
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    var selectedTab by remember { mutableIntStateOf(pagerState.currentPage) }

    LaunchedEffect(establishmentId) {
        // Загружаем данные при первом входе
        viewModel.fetchEstablishmentById(establishmentId)
    }

    LaunchedEffect(selectedTab) {
        pagerState.scrollToPage(selectedTab)
    }

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    Scaffold(
        topBar = {
            // ⭐ ИСПОЛЬЗУЕМ COLUMN ДЛЯ ГРУППИРОВКИ TOPAPPBAR И TABROW
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { Text(establishment?.name ?: "Заведение") },
                    actions = {
                        establishment?.let {
                            // Кнопка редактирования
                            IconButton(onClick = {
                                navController.navigate("${EstablishmentScreens.EstablishmentEdit.route}/${it.id}")
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
                            }
                        }
                    },
                    // Убираем параметр 'bottom'
                    // bottom = { ... } <-- ЭТО УБИРАЕМ!

                    // Рекомендуется: добавить цвета для TopAppBar, как вы делали раньше.
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                // ⭐ TabRow теперь находится отдельно, внутри Column, но ДО СЛОТА CONTENT
                TabRow(
                    selectedTabIndex = selectedTab,
                    // Дополнительные стили для TabRow, если нужно
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                errorMessage != null -> Text(
                    text = "Ошибка: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                establishment != null -> {
                    HorizontalPager(state = pagerState) { page ->
                        // Отображение контента вкладок
                        EstablishmentTabContent(page, establishment!!)
                    }
                }
                else -> Text("Данные не найдены", Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun EstablishmentTabContent(page: Int, establishment: EstablishmentDisplayDto) {
    // Временно, для демонстрации
    Column(modifier = Modifier.padding(16.dp)) {
        when (page) {
            0 -> {
                Text("Описание:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(establishment.description)
                Spacer(Modifier.height(8.dp))
                Text("Адрес: ${establishment.address}")
                Text("Тип: ${convertTypeToWord(establishment.type)}")
            }
            1 -> Text("Здесь будет отображаться Меню", style = MaterialTheme.typography.titleMedium)
            2 -> Text("Здесь будет отображаться Карта (Lat: ${establishment.latitude})", style = MaterialTheme.typography.titleMedium)
            3 -> Text("Здесь будут отображаться Отзывы", style = MaterialTheme.typography.titleMedium)
        }
    }
}