@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.preference.PreferenceManager
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.roamly.classes.cl_menu.MenuOfEstablishment
import com.example.roamly.entity.DTO.forDispalyEstablishmentDetails.DescriptionDTO
import com.example.roamly.entity.DTO.forDispalyEstablishmentDetails.MapDTO
import com.example.roamly.entity.LoadState
import com.example.roamly.entity.classes.ReviewEntity
import com.example.roamly.entity.ViewModel.EstablishmentDetailViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.entity.classes.convertTypeToWord
import com.example.roamly.ui.screens.sealed.BookingScreens
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.screens.sealed.OrderScreens
import com.example.roamly.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EstablishmentDetailScreen(establishmentId: Long, navController: NavController) {
    val viewModel: EstablishmentDetailViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()

    val descriptionState by viewModel.descriptionState.collectAsState()
    val reviewsState by viewModel.reviewsState.collectAsState()
    val mapState by viewModel.mapState.collectAsState()
    val menuState by viewModel.menuState.collectAsState()
    val photosState by viewModel.photosState.collectAsState()
    val favoriteState by viewModel.favoriteState.collectAsState()
    val establishmentState by viewModel.establishmentState.collectAsState()

    val user by userViewModel.user.collectAsState()
    val userId = user.id ?: 0L

    val isCreator = (establishmentState as? LoadState.Success)?.data?.createdUserId == userId

    LaunchedEffect(Unit) {
        viewModel.fetchAllDetails(establishmentId, user.id)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(
            initialHeightOffset = 0f,
            initialContentOffset = 0f
        ),
        flingAnimationSpec = null
    )

    val pagerState = rememberPagerState(pageCount = { 4 })
    val tabs = listOf("Описание", "Меню", "Отзывы", "Карта")

    LaunchedEffect(Unit) {
        scrollBehavior.state.heightOffset = 0f
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 3) {
            scrollBehavior.state.heightOffset = 0f
        }
    }

    val colors = AppTheme.colors

    Scaffold(
        topBar = {
            Column {
                Spacer(modifier = Modifier.fillMaxWidth().height(17.dp))
                TopAppBar(
                    title = {
                        (descriptionState as? LoadState.Success)?.data?.let { desc ->
                            Text(desc.name, color = colors.MainText)
                        } ?: Text("Заведение", color = colors.MainText)
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Назад", tint = colors.MainText)
                        }
                    },
                    actions = {
                        val isFavorite = (favoriteState as? LoadState.Success)?.data ?: false
                        if (user.id != null) {
                            IconButton(onClick = {
                                if (isFavorite) {
                                    viewModel.removeFavoriteEstablishment(userId, establishmentId)
                                } else {
                                    viewModel.addFavoriteEstablishment(userId, establishmentId)
                                }
                            }) {
                                Icon(
                                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Избранное",
                                    tint = if (isFavorite) AppTheme.colors.MainFailure else colors.MainText
                                )
                            }
                        }
                        if (isCreator) {
                            IconButton(onClick = {
                                navController.navigate(EstablishmentScreens.EstablishmentEdit.route.replace("{id}", establishmentId.toString()))
                            }) {
                                Icon(
                                    Icons.Filled.Create,
                                    contentDescription = "Редактировать заведение",
                                    tint = colors.MainText
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets(top = 0.dp),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.MainContainer.copy(alpha = 0.95f),
                        scrolledContainerColor = colors.MainContainer,
                        navigationIconContentColor = colors.MainText,
                        titleContentColor = colors.MainText,
                        actionIconContentColor = colors.MainText
                    )
                )
            }
        },
        containerColor = colors.MainContainer,
        contentWindowInsets = WindowInsets(top = 0.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val photoList = (photosState as? LoadState.Success)?.data ?: emptyList<ByteArray>()

            val collapsedFraction = scrollBehavior.state.collapsedFraction
            val animatedHeight by animateFloatAsState(targetValue = 250f * (1f - collapsedFraction))
            val animatedAlpha by animateFloatAsState(targetValue = 1f - collapsedFraction)

            // Правильная логика фотографий
            if (photoList.isNotEmpty() && animatedHeight > 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(animatedHeight.dp)
                        .graphicsLayer { alpha = animatedAlpha }
                ) {
                    val photoPagerState = rememberPagerState(pageCount = { photoList.size })
                    HorizontalPager(
                        state = photoPagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val imageBytes = photoList[page]
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current)
                                    .data(imageBytes)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Вставьте этот код вместо обычного TabRow
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = AppTheme.colors.MainContainer.copy(alpha = 0.95f), // Полупрозрачный основной контейнер как в нижней панели
                divider = { }, // Убираем стандартный divider, если не нужен
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = AppTheme.colors.SecondarySuccess.copy(alpha = 0.3f), // Полупрозрачный индикатор как в NavigationBar
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                title,
                                color = if (pagerState.currentPage == index)
                                    AppTheme.colors.MainSuccess
                                else
                                    AppTheme.colors.SecondaryText
                            )
                        },
                        selectedContentColor = AppTheme.colors.MainSuccess,
                        unselectedContentColor = AppTheme.colors.SecondaryText
                    )
                }
            }

            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> DescriptionTab(
                        descriptionState,
                        { viewModel.descriptionState },
                        scrollBehavior,
                        isCreator, // Добавлено
                        establishmentId, // Добавлено
                        navController // Добавлено
                    )
                    1 -> MenuTab(
                        menuState,
                        { viewModel.menuState },
                        scrollBehavior,
                        isCreator,
                        establishmentId,
                        navController
                    )
                    2 -> ReviewsTab(
                        reviewsState,
                        { viewModel.reviewsState },
                        scrollBehavior,
                        isCreator, // Добавлено
                        establishmentId, // Добавлено
                        navController // Добавлено
                    )
                    3 -> MapTab(
                        mapState,
                        { viewModel.mapState },
                        scrollBehavior
                    )
                }
            }
        }
    }
}

@Composable
private fun DescriptionTab(
    descriptionState: LoadState<DescriptionDTO>,
    onRetry: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    isCreator: Boolean, // Добавлено
    establishmentId: Long, // Добавлено
    navController: NavController // Добавлено
) {
    val state = rememberLazyListState()
    LazyContent(state, scrollBehavior, true) {
        item {
            when (descriptionState) {
                is LoadState.Loading -> DelayedLoadingIndicator(150)
                is LoadState.Error -> ErrorItem(descriptionState.message, onRetry)
                is LoadState.Success -> DescriptionSection(
                    desc = descriptionState.data,
                    isCreator = isCreator, // Добавлено
                    establishmentId = establishmentId, // Добавлено
                    navController = navController // Добавлено
                )
            }
        }
    }
}

@Composable
private fun MenuTab(
    menuState: LoadState<MenuOfEstablishment>,
    onRetry: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    isCreator: Boolean,
    establishmentId: Long,
    navController: NavController
) {
    val state = rememberLazyListState()
    LazyContent(state, scrollBehavior, true) {
        item {
            when (menuState) {
                is LoadState.Loading -> DelayedLoadingIndicator(150)
                is LoadState.Error -> ErrorItem(menuState.message, onRetry)
                is LoadState.Success -> {
                    if (isCreator) {
                        Button(
                            onClick = {
                                navController.navigate(EstablishmentScreens.MenuEdit.route.replace("{id}", establishmentId.toString()))
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.SecondaryContainer,
                                contentColor = AppTheme.colors.SecondaryText
                            )
                        ) {
                            Text("Редактировать меню")
                        }
                    }
                    MenuSection(menuState.data)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ReviewsTab(
    reviewsState: LoadState<List<ReviewEntity>>,
    onRetry: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    isCreator: Boolean, // Добавлено
    establishmentId: Long, // Добавлено
    navController: NavController // Добавлено
) {
    val state = rememberLazyListState()
    LazyContent(state, scrollBehavior, true) {
        item {
            when (reviewsState) {
                is LoadState.Loading -> DelayedLoadingIndicator(150)
                is LoadState.Error -> ErrorItem(reviewsState.message, onRetry)
                is LoadState.Success -> ReviewsSection(
                    reviews = reviewsState.data,
                    isCreator = isCreator, // Добавлено
                    establishmentId = establishmentId, // Добавлено
                    navController = navController // Добавлено
                )
            }
        }
    }
}

@Composable
private fun MapTab(
    mapState: LoadState<MapDTO>,
    onRetry: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val state = rememberLazyListState()
    LazyContent(state, scrollBehavior, enableNestedScroll = false) {
        item {
            when (mapState) {
                is LoadState.Loading -> DelayedLoadingIndicator(150)
                is LoadState.Error -> ErrorItem(mapState.message, onRetry)
                is LoadState.Success -> MapSection(mapState.data)
            }
        }
    }
}

@Composable
private fun DelayedLoadingIndicator(delayMillis: Long = 150L) {
    var showIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMillis)
        showIndicator = true
    }

    if (showIndicator) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun LazyContent(
    state: LazyListState,
    scrollBehavior: TopAppBarScrollBehavior,
    enableNestedScroll: Boolean,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .then(if (enableNestedScroll) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        content = content
    )
}

@Composable
private fun ErrorItem(message: String, onRetry: () -> Unit) {
    val colors = AppTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Ошибка: $message", color = colors.MainFailure)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = colors.MainSuccess)
        ) { Text("Повторить", color = colors.MainText) }
    }
}

@Composable
fun DescriptionSection(
    desc: DescriptionDTO,
    isCreator: Boolean, // Добавлено
    establishmentId: Long, // Добавлено
    navController: NavController // Добавлено
) {
    val colors = AppTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer)
        ) {
            Column(modifier = Modifier.padding(5.dp)) {
                Text(desc.description, style = MaterialTheme.typography.bodyLarge, color = colors.MainText)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Адрес: ${desc.address}", style = MaterialTheme.typography.bodyMedium, color = colors.SecondaryText)
                Text("Рейтинг: ${desc.rating}", style = MaterialTheme.typography.bodyMedium, color = colors.SecondaryText)
                Text("Тип: ${convertTypeToWord(desc.type)}", style = MaterialTheme.typography.bodyMedium, color = colors.SecondaryText)

                // Улучшенное отображение времени работы
                desc.operatingHoursString?.let { operatingHours ->
                    Column {
                        Text("Часы работы:", style = MaterialTheme.typography.bodyMedium, color = colors.SecondaryText)

                        // Парсим расписание
                        val scheduleMap = parseCompactSchedule(operatingHours)

                        // Если не удалось распарсить как компактный формат, пробуем как JSON массив
                        val hoursMap = if (scheduleMap.isEmpty()) {
                            operatingHours.toMap()
                        } else {
                            scheduleMap
                        }

                        // Порядок дней недели
                        val daysOfWeek = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")

                        daysOfWeek.forEach { day ->
                            hoursMap[day]?.let { hours ->
                                Text(
                                    text = "  • $day: $hours",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.SecondaryText
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text("Дата создания: ${desc.dateOfCreation}", style = MaterialTheme.typography.bodyMedium, color = colors.SecondaryText)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка для создания бронирования
        Button(
            onClick = {
                navController.navigate(BookingScreens.CreateBooking.createRoute(establishmentId))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.SecondaryContainer,
                contentColor = colors.MainText
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange, // Нужно добавить этот иконку в импорты
                contentDescription = "Забронировать",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Забронировать столик")
        }

        // В DescriptionSection добавьте кнопку рядом с кнопкой бронирования

        Button(
                onClick = {
                    navController.navigate(OrderScreens.OrderCreation.createRoute(establishmentId))
                },
            modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.SecondaryContainer,
                    contentColor = colors.MainText
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Заказать",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Заказать с доставкой")
            }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReviewsSection(
    reviews: List<ReviewEntity>,
    isCreator: Boolean,
    establishmentId: Long,
    navController: NavController
) {
    val colors = AppTheme.colors
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // Показываем кнопку создания отзыва только если пользователь не владелец
        if (!isCreator) {
            OutlinedButton(
                onClick = {
                    navController.navigate(EstablishmentScreens.ReviewCreation.createRoute(establishmentId))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 3.dp, vertical = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.MainSuccess.copy(alpha = 0.1f),
                    contentColor = colors.MainSuccess
                ),
                border = BorderStroke(1.dp, colors.MainSuccess.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Написать отзыв",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Написать отзыв", fontWeight = FontWeight.Medium)
            }
        }

        if (reviews.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = colors.SecondaryText,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Пока нет отзывов",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.MainText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Будьте первым, кто оставит отзыв!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.SecondaryText
                    )
                }
            }
        } else {
            // Статистика отзывов
            val averageRating = reviews.map { it.rating }.average()
            val ratingCount = reviews.size

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.SecondaryContainer,
                    contentColor = colors.MainText
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Рейтинг заведения",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.SecondaryText
                        )
                        Text(
                            String.format("%.1f", averageRating),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.MainText
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(5) { index ->
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (index < averageRating.toInt())
                                        colors.MainSuccess
                                    else
                                        colors.SecondaryBorder,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Всего отзывов",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.SecondaryText
                        )
                        Text(
                            ratingCount.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.MainText
                        )
                        Text(
                            "на основе отзывов",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.SecondaryText
                        )
                    }
                }
            }

            // Список отзывов
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                reviews.forEach { review ->
                    ReviewCard(
                        review = review,
                        modifier = Modifier.padding(vertical = 8.dp),
                        establishmentId = establishmentId
                    )
                }
            }
        }
    }
}

@Composable
fun MapSection(mapData: MapDTO) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
            .clipToBounds()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .clip(RoundedCornerShape(12.dp)) // красивые углы (опционально)
        ) {
            // Сама карта
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance()
                        .load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(false)
                        setBuiltInZoomControls(false)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(mapData.latitude, mapData.longitude))

                        overlays.add(Marker(this).apply {
                            position = GeoPoint(mapData.latitude, mapData.longitude)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        })

                        invalidate()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Невидимый слой, который перехватывает ВСЕ касания
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { /* ничего не делаем */ }
                        detectDragGestures { _, _ -> /* блокируем драг */ }
                    }
            )
        }
    }
}

private fun parseCompactSchedule(schedule: String): Map<String, String> {
    val result = mutableMapOf<String, String>()

    // Маппинг сокращений дней к полным именам
    val dayMap = mapOf(
        "Пн" to "Понедельник",
        "Вт" to "Вторник",
        "Ср" to "Среда",
        "Чт" to "Четверг",
        "Пт" to "Пятница",
        "Сб" to "Суббота",
        "Вс" to "Воскресенье"
    )

    // Порядок дней для диапазонов
    val daysOrder = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")

    // Разделяем по запятым
    val parts = schedule.split(",").map { it.trim() }.filter { it.isNotBlank() }

    parts.forEach { part ->
        // Разделяем диапазон дней и время (ожидаем формат "Диапазон Время")
        val spaceIndex = part.indexOf(' ')
        if (spaceIndex == -1) {
            return@forEach // Пропускаем некорректные части
        }

        val dayRange = part.substring(0, spaceIndex).trim()
        val time = part.substring(spaceIndex + 1).trim()

        if (time.isBlank()) {
            return@forEach
        }

        // Парсим диапазон дней
        val days: List<String> = if (dayRange.contains("-")) {
            val rangeSplit = dayRange.split("-")
            if (rangeSplit.size != 2) {
                return@forEach
            }

            val fromShort = rangeSplit[0].trim()
            val toShort = rangeSplit[1].trim()

            val fromFull = dayMap[fromShort] ?: return@forEach
            val toFull = dayMap[toShort] ?: return@forEach

            val fromIndex = daysOrder.indexOf(fromFull)
            val toIndex = daysOrder.indexOf(toFull)

            if (fromIndex == -1 || toIndex == -1 || fromIndex > toIndex) {
                return@forEach
            }

            daysOrder.subList(fromIndex, toIndex + 1)
        } else {
            val singleShort = dayRange.trim()
            val singleFull = dayMap[singleShort] ?: return@forEach
            listOf(singleFull)
        }

        // Присваиваем время для каждого дня
        days.forEach { day ->
            result[day] = time
        }
    }
    return result.filterValues { it.isNotBlank() }
}
