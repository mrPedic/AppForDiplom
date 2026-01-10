// UserEstablishments.kt
package com.example.roamly.ui.screens.establishment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.establishment.EstablishmentDisplayDto
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.ViewModel.UserEstablishmentsViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.entity.classes.TypeOfEstablishment
import com.example.roamly.entity.classes.convertTypeToWord
import com.example.roamly.ui.screens.sealed.BookingScreens
import com.example.roamly.ui.theme.AppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEstablishmentsScreen(
    navController: NavController,
    userViewModel: UserViewModel = hiltViewModel(),
    establishmentViewModel: EstablishmentViewModel = hiltViewModel(),
    pinnedViewModel: UserEstablishmentsViewModel = hiltViewModel()
) {
    val user by userViewModel.user.collectAsState()
    val establishments by establishmentViewModel.userEstablishments.collectAsState(emptyList())
    val pinnedIds by pinnedViewModel.pinnedEstablishments.collectAsState()
    val isLoading by establishmentViewModel.isLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Поиск и фильтрация на клиенте
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<TypeOfEstablishment?>(null) }

    // Список типов типов из enum, исключая Error
    val establishmentTypes = TypeOfEstablishment.values().filter { it != TypeOfEstablishment.Error }

    // Фильтрация заведений на клиенте
    val filteredEstablishments = establishments.filter { est ->
        (searchQuery.isEmpty() || est.name.contains(searchQuery, ignoreCase = true)) &&
                (selectedType == null || est.type == selectedType)
    }

    // Разделяем на закрепленные и обычные после фильтрации
    val pinnedEstablishments = filteredEstablishments.filter { it.id in pinnedIds }
    val otherEstablishments = filteredEstablishments.filter { it.id !in pinnedIds }

    // Объединенный список с заголовками (показываем заголовок только если раздел не пуст)
    val displayList = buildList {
        if (pinnedEstablishments.isNotEmpty()) {
            add("Закрепленные")
            addAll(pinnedEstablishments)
        }
        if (otherEstablishments.isNotEmpty()) {
            add("Все заведения")
            addAll(otherEstablishments)
        }
    }

    LaunchedEffect(user.id) {
        user.id?.let { establishmentViewModel.fetchEstablishmentsByUserId(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Мои заведения", color = AppTheme.colors.MainText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = AppTheme.colors.MainText
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppTheme.colors.SecondaryContainer
                ),
                modifier = Modifier.fillMaxHeight(0.08f)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppTheme.colors.MainContainer)
        ) {
            // Поисковая строка
            val focusRequester = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text("Поиск по названию...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppTheme.colors.SecondaryContainer,
                    unfocusedContainerColor = AppTheme.colors.SecondaryContainer,
                    focusedIndicatorColor = AppTheme.colors.MainSuccess,
                    unfocusedIndicatorColor = AppTheme.colors.MainBorder,
                    focusedTextColor = AppTheme.colors.MainText,
                    unfocusedTextColor = AppTheme.colors.MainText,
                    focusedPlaceholderColor = AppTheme.colors.SecondaryText,
                    unfocusedPlaceholderColor = AppTheme.colors.SecondaryText
                )
            )

            // Фильтры по типу (горизонтальный скролл)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding( bottom = 8.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text("Все") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppTheme.colors.MainSuccess.copy(alpha = 0.2f),
                        selectedLabelColor = AppTheme.colors.MainText
                    )
                )
                establishmentTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(convertTypeToWord(type)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppTheme.colors.MainSuccess.copy(alpha = 0.2f),
                            selectedLabelColor = AppTheme.colors.MainText
                        )
                    )
                }
            }

            // Список с анимацией подгрузки
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppTheme.colors.MainSuccess
                    )
                } else if (displayList.isEmpty()) {
                    Text(
                        "Нет заведений",
                        modifier = Modifier.align(Alignment.Center),
                        color = AppTheme.colors.SecondaryText
                    )
                } else {
                    this@Column.AnimatedVisibility(
                        visible = !isLoading,
                        enter = fadeIn(animationSpec = tween(500)),
                        exit = fadeOut(animationSpec = tween(500))
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            displayList.forEach { item ->
                                when (item) {
                                    is String -> {
                                        item {
                                            Text(
                                                text = item,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = AppTheme.colors.MainText,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }

                                    is EstablishmentDisplayDto -> {
                                        item {
                                            EstablishmentItem(
                                                establishment = item,
                                                isPinned = item.id in pinnedIds,
                                                onPinClick = {
                                                    coroutineScope.launch {
                                                        pinnedViewModel.togglePin(item.id)
                                                    }
                                                },
                                                onBookingsClick = {
                                                    navController.navigate(
                                                        BookingScreens.OwnerBookingsManagement.createRoute(item.id)
                                                    )
                                                },
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
        }
    }
}

@Composable
fun EstablishmentItem(
    establishment: EstablishmentDisplayDto,
    isPinned: Boolean,
    onPinClick: () -> Unit,
    onBookingsClick: () -> Unit,
    navController: NavController
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.SecondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = establishment.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.MainText
                )

                IconButton(
                    onClick = onPinClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.Build else Icons.Outlined.Build,
                        contentDescription = if (isPinned) "Открепить" else "Закрепить",
                        tint = if (isPinned) AppTheme.colors.MainSuccess else AppTheme.colors.SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = establishment.address,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.SecondaryText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBookingsClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.MainSuccess
                    )
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Бронирования")
                }

                OutlinedButton(
                    onClick = {
                        // Навигация к деталям заведения
                        navController.navigate("establishment/detail/${establishment.id}")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Детали")
                }
            }
        }
    }
}