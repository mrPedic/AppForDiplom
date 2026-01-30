package com.example.roamly.ui.screens.admin

import android.widget.ScrollView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.ViewModel.AdminSqlViewModel
import com.example.roamly.ui.theme.AppTheme

@Composable
fun SqlDetailScreen(
    navController: NavController,
    queryId: Long,
    viewModel: AdminSqlViewModel = hiltViewModel()
) {
    val currentQuery by viewModel.currentQuery.collectAsState()
    val queryResult by viewModel.queryResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Локальное состояние для редактирования SQL (чтобы не сохранять каждый чих)
    var localSql by remember { mutableStateOf("") }

    // Состояние диалога редактирования информации
    var showInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(queryId) {
        viewModel.loadQuery(queryId)
    }

    // Синхронизируем локальный SQL с загруженным
    LaunchedEffect(currentQuery) {
        currentQuery?.let {
            if (localSql.isEmpty()) localSql = it.sqlQuery
        }
    }

    if (currentQuery == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Scaffold(
        containerColor = AppTheme.colors.MainContainer,
        topBar = {
            // Можно добавить стандартный TopAppBar с кнопкой назад
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // --- ПАНЕЛЬ УПРАВЛЕНИЯ ---
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.SecondaryContainer),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.Top // Выравнивание по верху, так как поле многострочное
                ) {
                    // 1. Кнопка редактирования ИНФО (Слева)
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Info",
                            tint = AppTheme.colors.MainText
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // 2. Поле ввода SQL (По центру)
                    OutlinedTextField(
                        value = localSql,
                        onValueChange = { localSql = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 100.dp, max = 200.dp), // Ограничение высоты
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            color = AppTheme.colors.MainText,
                            fontSize = 14.sp
                        ),
                        placeholder = { Text("SELECT * FROM table") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.colors.MainBorder,
                            unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                            cursorColor = AppTheme.colors.MainSuccess
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    // 3. Кнопка ЗАПУСТИТЬ (Справа)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        IconButton(
                            onClick = { viewModel.executeSql(localSql) },
                            modifier = Modifier
                                .background(AppTheme.colors.MainSuccess, shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Run",
                                tint = Color.White
                            )
                        }

                        // Кнопка сохранения самого SQL, если нужно обновить запрос
                        Spacer(Modifier.height(8.dp))
                        IconButton(
                            onClick = {
                                viewModel.saveQuery(currentQuery!!.copy(sqlQuery = localSql))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Save SQL",
                                tint = AppTheme.colors.SecondaryText
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- ТАБЛИЦА РЕЗУЛЬТАТОВ ---
            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                queryResult?.let { results ->
                    if (results.isEmpty()) {
                        Text(
                            "Запрос выполнен успешно. Данных нет.",
                            color = AppTheme.colors.SecondaryText,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        DynamicSqlTable(data = results)
                    }
                }
            }
        }
    }

    // --- ДИАЛОГ РЕДАКТИРОВАНИЯ ИНФО ---
    if (showInfoDialog) {
        var tempTitle by remember { mutableStateOf(currentQuery!!.title) }
        var tempDesc by remember { mutableStateOf(currentQuery!!.description) }

        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = AppTheme.colors.SecondaryContainer,
            title = { Text("Информация о запросе", color = AppTheme.colors.MainText) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Название") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTheme.colors.MainText,
                            unfocusedTextColor = AppTheme.colors.MainText,
                            cursorColor = AppTheme.colors.MainSuccess,
                            focusedBorderColor = AppTheme.colors.MainBorder,
                            unfocusedBorderColor = AppTheme.colors.SecondaryBorder
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempDesc,
                        onValueChange = { tempDesc = it },
                        label = { Text("Описание") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTheme.colors.MainText,
                            unfocusedTextColor = AppTheme.colors.MainText,
                            cursorColor = AppTheme.colors.MainSuccess,
                            focusedBorderColor = AppTheme.colors.MainBorder,
                            unfocusedBorderColor = AppTheme.colors.SecondaryBorder
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updated = currentQuery!!.copy(
                        title = tempTitle,
                        description = tempDesc,
                        sqlQuery = localSql // Сохраняем и текущий SQL тоже
                    )
                    viewModel.saveQuery(updated)
                    showInfoDialog = false
                }) {
                    Text("Сохранить", color = AppTheme.colors.MainSuccess)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Отмена", color = AppTheme.colors.SecondaryText)
                }
            }
        )
    }
}



@Composable
fun DynamicSqlTable(data: List<Map<String, Any?>>) {
    val scrollState = rememberScrollState()
    val headers = data.firstOrNull()?.keys?.toList() ?: emptyList()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppTheme.colors.SecondaryBorder, RoundedCornerShape(8.dp))
            .background(AppTheme.colors.SecondaryContainer)
    ) {
        // Горизонтальный скролл для всей таблицы
        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            Column {
                // --- Заголовки ---
                Row(
                    modifier = Modifier
                        .background(AppTheme.colors.MainBorder.copy(alpha = 0.1f))
                        .padding(vertical = 12.dp)
                ) {
                    headers.forEach { header ->
                        Text(
                            text = header,
                            modifier = Modifier
                                .width(150.dp)
                                .padding(horizontal = 12.dp),
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.MainText,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Divider(color = AppTheme.colors.SecondaryBorder)

                // --- Строки данных ---
                LazyColumn(modifier = Modifier.heightIn(max = 500.dp)) {
                    itemsIndexed(data) { index, row ->
                        val bgColor = if (index % 2 == 0) Color.Transparent else AppTheme.colors.MainContainer.copy(alpha = 0.2f)
                        Row(
                            modifier = Modifier
                                .background(bgColor)
                                .padding(vertical = 10.dp)
                        ) {
                            headers.forEach { header ->
                                val cellValue = row[header]?.toString() ?: "NULL"
                                Text(
                                    text = cellValue,
                                    modifier = Modifier
                                        .width(150.dp)
                                        .padding(horizontal = 12.dp),
                                    color = if (cellValue == "NULL") AppTheme.colors.SecondaryText else AppTheme.colors.MainText,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Divider(color = AppTheme.colors.SecondaryBorder.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}