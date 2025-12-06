@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

import android.util.Base64
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.roamly.entity.*
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import java.time.format.DateTimeFormatter
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.classes.cl_menu.Food
import com.example.roamly.classes.cl_menu.MenuOfEstablishment
import com.example.roamly.entity.DTO.EstablishmentDisplayDto
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.sealed.BookingScreens
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EstablishmentDetailScreen(
    navController: NavController,
    establishmentId: Long,
    viewModel: EstablishmentViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val establishment by viewModel.currentEstablishment.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val menuState by viewModel.menuOfEstablishment.collectAsState()
    val isMenuLoading by viewModel.isMenuLoading.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userState by userViewModel.user.collectAsState()
    val currentUserId = userState.id ?: -1L

    val density = LocalDensity.current

    val tabs = listOf("Описание", "Меню", "Карта", "Отзывы")
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    var selectedTab by remember { mutableIntStateOf(pagerState.currentPage) }

    val coroutineScope = rememberCoroutineScope()
    var showMenuItemDetailDialog by remember { mutableStateOf(false) }
    var selectedMenuItem: Any? by remember { mutableStateOf(null) }

    val onMenuItemClick: (Any) -> Unit = { item ->
        selectedMenuItem = item
        showMenuItemDetailDialog = true
    }


    // --- ЛОГИКА COLLAPSING HEADER ---

    val headerHeight = 250.dp
    val headerHeightPx = with(LocalDensity.current) { headerHeight.toPx() }
    val topBarHeightPx = with(LocalDensity.current) { 56.dp.toPx() } // <--- НОВАЯ ПЕРЕМЕННАЯ
    var headerOffsetHeightPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = headerOffsetHeightPx + delta
                headerOffsetHeightPx = newOffset.coerceIn(-(headerHeightPx - topBarHeightPx), 0f)
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(establishmentId, currentUserId) { // <--- ИЗМЕНЕНИЕ
        if (establishmentId > 0) {
            viewModel.fetchEstablishmentById(establishmentId) // <--- ИЗМЕНЕНИЕ
            viewModel.fetchMenuForEstablishment(establishmentId)
        }
    }

    LaunchedEffect(establishmentId) { viewModel.fetchEstablishmentById(establishmentId) }
    LaunchedEffect(establishment) { establishment?.let { viewModel.fetchMenuForEstablishment(it.id) } }
    LaunchedEffect(currentUserId) { if (currentUserId > 0) viewModel.fetchFavoriteEstablishmentsList(currentUserId) }
    LaunchedEffect(selectedTab) { pagerState.scrollToPage(selectedTab) }
    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
        if (pagerState.currentPage == 3 && establishment != null) {
            viewModel.fetchReviewsForEstablishment(establishment!!.id)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        val coroutineScope = rememberCoroutineScope()
        val density = LocalDensity.current


        val (operatingHoursMap, operatingStatus) = remember(establishment?.operatingHoursString) {
            if (establishment?.operatingHoursString == null) {
                // Возвращаем пустые значения на время загрузки
                Pair(emptyMap<String, String>(), OperatingStatus(false, "Загрузка..."))
            } else {
                val map = convertHoursStringToMap(establishment!!.operatingHoursString)
                val status = getOperatingStatus(establishment!!.operatingHoursString)
                Pair(map, status)
            }
        }

        when {
            isLoading && establishment == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            errorMessage != null -> Text("Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            establishment != null -> {

                // --- 1. Хедер (Фото + Табы) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight + 48.dp)
                        .offset { IntOffset(x = 0, y = headerOffsetHeightPx.roundToInt()) }
                        .zIndex(1f) // Хедер должен быть выше контента (Pager), но ниже TopBar
                ) {
                    Column {
                        EstablishmentHeaderImage(establishment!!, Modifier.height(headerHeight))
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(48.dp)
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = { Text(title) }
                                )
                            }
                        }
                    }
                }

                // --- 2. Контент (Pager) ---
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = headerHeight + 48.dp)
                        .offset { IntOffset(x = 0, y = headerOffsetHeightPx.roundToInt()) }
                ) { page ->
                    EstablishmentTabContent(
                        page = page,
                        establishment = establishment!!,
                        navController = navController,
                        menuState = menuState,
                        isMenuLoading = isMenuLoading,
                        operatingHoursMap = operatingHoursMap,
                        operatingStatus = operatingStatus,
                        onMenuItemClick = onMenuItemClick
                    )
                }
            }
        }

        // --- 3. TOP BAR (Исправлено: всегда сверху) ---
        if (establishment != null) {
            val scrollPercentage = (-headerOffsetHeightPx / headerHeightPx).coerceIn(0f, 1f)
            val topBarColor = MaterialTheme.colorScheme.surface.copy(alpha = scrollPercentage)
            val contentColor = if (scrollPercentage > 0.5f) MaterialTheme.colorScheme.onSurface else Color.White

            EstablishmentTopBar(
                navController = navController,
                establishmentName = establishment!!.name,
                establishmentId = establishment!!.id,
                userId = currentUserId,
                establishmentViewModel = viewModel,
                isOwner = establishment!!.createdUserId == currentUserId,
                containerColor = topBarColor,
                contentColor = contentColor,
                modifier = Modifier.zIndex(2f).align(Alignment.TopCenter),
                establishment = establishment!!
            )
        }

        if (showMenuItemDetailDialog && selectedMenuItem != null) {
            MenuItemDetailDialog(
                item = selectedMenuItem!!,
                onDismiss = {
                    showMenuItemDetailDialog = false
                    selectedMenuItem = null
                }
            )
        }
    }
}

@Composable
fun EstablishmentHeaderImage(establishment: EstablishmentDisplayDto, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        val mainPhotoBase64 = establishment.photoBase64s.firstOrNull { it.isNotBlank() }
        if (mainPhotoBase64 != null) {
            val imageBytes = remember(mainPhotoBase64) { base64ToByteArray(mainPhotoBase64) }
            if (imageBytes != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = imageBytes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color.Gray))
            }
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary))
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
    }
}

@Composable
fun OperatingHoursDisplay(operatingHours: Map<String, String>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Время работы:",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        if (operatingHours.isEmpty()) {
            Text("Информация о времени работы отсутствует.", style = MaterialTheme.typography.bodyMedium)
            return
        }

        // Используем DAYS_OF_WEEK для гарантированного порядка
        DAYS_OF_WEEK.forEach { day ->
            val hours = operatingHours[day]

            // Отображаем только те дни, для которых есть данные
            if (!hours.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = day, modifier = Modifier.weight(1f))
                    Text(
                        text = hours,
                        fontWeight = if (hours.contains("Закрыто", ignoreCase = true)) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (hours.contains("Закрыто", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun EstablishmentTopBar(
    navController: NavController,
    establishmentName: String,
    establishmentId: Long,
    userId: Long,
    establishment: EstablishmentDisplayDto,
    establishmentViewModel: EstablishmentViewModel,
    isOwner: Boolean,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val favoriteIds by establishmentViewModel.favoriteEstablishmentIds.collectAsState()
    val isFavorite = establishmentViewModel.checkIfFavorite(establishmentId)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            establishmentName,
            color = contentColor,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Center)
        )

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterStart)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Назад", tint = contentColor)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isOwner) {
                IconButton(onClick = { navController.navigate(EstablishmentScreens.EstablishmentEdit.createRoute(establishmentId)) }) {
                    Icon(Icons.Filled.Edit, "Редактировать", tint = contentColor)
                }
            }
            IconButton(onClick = { establishmentViewModel.toggleFavorite(establishment, userId) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else contentColor
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EstablishmentTabContent(
    page: Int,
    establishment: EstablishmentDisplayDto,
    navController: NavController,
    menuState: MenuOfEstablishment?,
    isMenuLoading: Boolean,
    operatingHoursMap: Map<String, String>,
    operatingStatus: OperatingStatus,
    userViewModel: UserViewModel = hiltViewModel(),
    onMenuItemClick: (Any) -> Unit
) {
    val currentUser by userViewModel.user.collectAsState()
    val currentUserId = currentUser.id ?: -1L
    val isOwner = establishment.createdUserId == currentUserId
    val context = LocalContext.current

    when (page) {
        0 -> {
            // Описание
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Описание:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(establishment.description)
                Spacer(Modifier.height(8.dp))
                Text("Адрес: ${establishment.address}")
                Text("Тип: ${convertTypeToWord(establishment.type)}")

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        val currentUserId = userViewModel.getId()
                        if (currentUserId != null) {
                            navController.navigate(
                                BookingScreens.CreateBooking.createRoute(establishment.id)
                            )
                        } else {
                            Toast.makeText(context, "Пожалуйста, авторизуйтесь для бронирования.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Забронировать столик")
                }

                Spacer(Modifier.height(16.dp))
                OperatingStatusDisplayInternal(operatingStatus)

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Полное расписание:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                // Передаем уже вычисленную карту
                OperatingHoursDisplay(operatingHoursMap)
            }
        }
        1 -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Меню
            if (isMenuLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (menuState == null || (menuState.foodGroups.isEmpty() && menuState.drinksGroups.isEmpty())) {
                Column(Modifier.align(Alignment.Center)) {
                    Text("Меню отсутствует.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                MenuDisplayContent(
                    menu = menuState,
                    isOwner = (establishment.createdUserId == userViewModel.getId()), // Или ваша логика isOwner
                    establishmentId = establishment.id,
                    navController = navController,
                    onMenuItemClick = onMenuItemClick
                )
            }
        }
        2 -> {
            // Карта (статичная)
            Box(modifier = Modifier.fillMaxSize()) {
                EstablishmentMapTab(
                    name = establishment.name,
                    latitude = establishment.latitude,
                    longitude = establishment.longitude
                )

                // Блокировка взаимодействия с картой
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Transparent)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                )
            }
        }
        3 -> {
            ReviewTabContent(establishment = establishment, navController = navController)
        }
    }
}

// Новая функция для отображения статуса (принимает готовый объект, чтобы не пересчитывать)
@Composable
fun OperatingStatusDisplayInternal(status: OperatingStatus) {
    val color = if (status.isOpen) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = color.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = status.statusText,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReviewTabContent(
    establishment: EstablishmentDisplayDto,
    navController: NavController,
    userViewModel: UserViewModel = hiltViewModel(),
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    val currentUser by userViewModel.user.collectAsState()
    val currentUserId = currentUser.id ?: -1L
    val isLoggedIn = userViewModel.isLoggedIn()

    val isOwner = establishment.createdUserId == currentUserId

    val reviews by viewModel.reviews.collectAsState()
    val isReviewsLoading by viewModel.isReviewsLoading.collectAsState()

    val canReview = isLoggedIn && !isOwner

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Spacer(Modifier.height(8.dp))

            when {
                !isLoggedIn -> {
                    Text("Войдите в систему, чтобы оставить отзыв.", color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                }
                isOwner -> {
                    Text("Вы являетесь владельцем этого заведения и не можете оставить отзыв.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (isReviewsLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (reviews.isEmpty()) {
                Text("Отзывов пока нет. Будьте первыми!", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Список отзывов (${reviews.size}):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reviews) { review ->
                        if (review.photoBase64.isNullOrBlank()) {
                            ReviewItem(review = review)
                        } else {
                            ReviewItemWithPhoto(review = review)
                        }
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
                    }
                }
            }
        }

        if (canReview) {
            Button(
                onClick = {

                    navController.navigate(EstablishmentScreens.ReviewCreation.createRoute(establishment.id))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Text("Оставить отзыв")
            }
        }
    }
}

fun base64ToByteArray(base64String: String): ByteArray? {
    return try {
        val cleanBase64 = base64String.substringAfter(",", base64String)

        Base64.decode(cleanBase64, Base64.DEFAULT)
    } catch (e: IllegalArgumentException) {
        Log.e("Base64", "Ошибка декодирования Base64: ${e.message}. Строка начинается с: ${base64String.take(50)}")
        null
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReviewItem(review: ReviewEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        ReviewContent(review = review, hasPhoto = false)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReviewItemWithPhoto(review: ReviewEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            ReviewContent(review = review, hasPhoto = true)

            // --- Отображение фото ---
            review.photoBase64?.takeIf { it.isNotBlank() }?.let { base64 ->
                val imageBytes = remember(base64) { base64ToByteArray(base64) }

                imageBytes?.let { bytes ->
                    Spacer(Modifier.height(8.dp))
                    Image(
                        // Coil может принимать массив байтов
                        painter = rememberAsyncImagePainter(model = bytes),
                        contentDescription = "Фото отзыва",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp) // Фиксированная высота для фото
                            .padding(horizontal = 12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                } ?: Text("Ошибка загрузки фото", color = Color.Red, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ReviewContent(review: ReviewEntity, hasPhoto: Boolean) {
    Column(modifier = Modifier.padding(12.dp)) {
        // Заголовок: Оценка и Дата
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Оценка
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Оценка: ${"%.1f".format(review.rating)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
            }

            // Дата создания
            review.dateOfCreation.let {
                val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
                Text(
                    text = it.format(formatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Текст отзыва
        Text(review.reviewText, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(8.dp))

        // Индикация фото
        if (hasPhoto) {
            Text("Приложено фото", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
        }

        // Информация о пользователе
        Text(
            text = "Пользователь ID: ${review.createdUserId}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun OperatingStatusDisplay(hoursString: String?) {
    val status = remember(hoursString) { getOperatingStatus(hoursString) }

    val color = if (status.isOpen) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = color.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            )
            .padding(8.dp)
    ) {
        // Индикатор (кружок)
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(Modifier.width(8.dp))

        // Текст статуса
        Text(
            text = status.statusText,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

val CALENDAR_DAYS_MAP = mapOf(
    Calendar.MONDAY to "Понедельник",
    Calendar.TUESDAY to "Вторник",
    Calendar.WEDNESDAY to "Среда",
    Calendar.THURSDAY to "Четверг",
    Calendar.FRIDAY to "Пятница",
    Calendar.SATURDAY to "Суббота",
    Calendar.SUNDAY to "Воскресенье"
)

// Вспомогательный класс для хранения статуса (остается прежним)
data class OperatingStatus(
    val isOpen: Boolean,
    val statusText: String,
)

fun getOperatingStatus(hoursString: String?): OperatingStatus {
    val operatingHoursMap = convertHoursStringToMap(hoursString)
    Log.d("OpStatusMap", "Полученное расписание: $operatingHoursMap")

    if (operatingHoursMap.isEmpty()) {
        Log.d("OpStatus", "Расписание отсутствует.")
        return OperatingStatus(false, "Информация о расписании отсутствует.")
    }

    val calendar = Calendar.getInstance()
    val todayDayInt = calendar.get(Calendar.DAY_OF_WEEK)
    val nowHour = calendar.get(Calendar.HOUR_OF_DAY)
    val nowMinute = calendar.get(Calendar.MINUTE)

    val todayDayName = CALENDAR_DAYS_MAP[todayDayInt] ?: "Неизвестный день"

    fun parseTime(time: String): Int? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        return try {
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            Log.e("OpStatus", "Ошибка парсинга времени: $time", e)
            null
        }
    }

    val nowTotalMinutes = nowHour * 60 + nowMinute
    Log.d("OpStatus", "Текущее время: $todayDayName ($todayDayInt), ${String.format("%02d:%02d", nowHour, nowMinute)} ($nowTotalMinutes мин)")

    // 1. Проверяем сегодня
    val todayHours = operatingHoursMap[todayDayName]

    if (todayHours.isNullOrBlank() || todayHours.contains("Закрыто", ignoreCase = true)) {
        Log.d("OpStatus", "Сегодня ($todayDayName) закрыто. Ищем следующий день.")
        return findNextOpenDay(operatingHoursMap, todayDayInt)

    } else {
        // Сегодня открыто, проверяем временной интервал
        val parts = todayHours.split(" - ")
        if (parts.size == 2) {
            val startMinutes = parseTime(parts[0])
            val endMinutes = parseTime(parts[1])

            Log.d("OpStatus", "Сегодня: $todayHours ($startMinutes - $endMinutes мин)")

            if (startMinutes != null && endMinutes != null) {
                val isOvernight = endMinutes < startMinutes

                val isOpen = if (isOvernight) {
                    nowTotalMinutes >= startMinutes || nowTotalMinutes < endMinutes
                } else {
                    nowTotalMinutes >= startMinutes && nowTotalMinutes < endMinutes
                }

                if (isOpen) {
                    Log.d("OpStatus", "Статус: ОТКРЫТО. Часы: $todayHours")
                    return OperatingStatus(
                        true,
                        "Открыто. Работает сегодня до ${parts[1]}.", // Просто конечная точка
                    )
                } else if (nowTotalMinutes < startMinutes && !isOvernight) {
                    Log.d("OpStatus", "Статус: ЗАКРЫТО, откроется сегодня.")
                    return OperatingStatus(
                        false,
                        "Закрыто. Откроется сегодня.",
                    )
                } else {
                    // ЗАКРЫТО (после закрытия сегодня)
                    Log.d("OpStatus", "Статус: ЗАКРЫТО (время прошло). Ищем следующий день.")
                    return findNextOpenDay(operatingHoursMap, todayDayInt)
                }
            }
        }
    }

    Log.w("OpStatus", "Не удалось определить статус работы (Ошибка парсинга).")
    return OperatingStatus(false, "Не удалось определить статус работы.")
}

private fun findNextOpenDay(operatingHoursMap: Map<String, String>, startDayInt: Int): OperatingStatus {
    // Начинаем поиск со следующего дня
    for (i in 1..7) {
        val nextDayInt = if (startDayInt + i > 7) (startDayInt + i) % 7 else startDayInt + i

        val nextDayName = CALENDAR_DAYS_MAP[nextDayInt]

        if (nextDayName != null) {
            val nextHours = operatingHoursMap[nextDayName]
            Log.d("OpStatus", "Проверка дня ${i}: $nextDayName ($nextDayInt). Часы: $nextHours")

            if (!nextHours.isNullOrBlank() && !nextHours.contains("Закрыто", ignoreCase = true)) {
                val dayLabel = if (i == 1) "завтра" else "в $nextDayName"

                Log.d("OpStatus", "Найдено: Откроется $dayLabel.")
                return OperatingStatus(
                    false,
                    "Закрыто. Откроется $dayLabel.",
                )
            }
        }
    }
    Log.d("OpStatus", "Все дни закрыты.")
    return OperatingStatus(false, "Временно закрыто на неопределенный срок.")
}

@Composable
fun MenuDisplayContent(
    menu: MenuOfEstablishment,
    isOwner: Boolean,
    establishmentId: Long,
    navController: NavController,
    onMenuItemClick: (Any) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp)
    ) {
        // --- 1. Заголовок ---
        item {
            Text(
                "Меню заведения",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp) // Горизонтальный отступ здесь
            )
            Spacer(Modifier.height(16.dp))
        }

        // --- 2. Кнопка редактирования меню (только для владельца) ---
        if (isOwner) {
            item {
                Button(
                    onClick = {
                        navController.navigate(
                            EstablishmentScreens.MenuEdit.createRoute(establishmentId)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp) // Горизонтальный отступ здесь
                ) {
                    Text("Редактировать меню")
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // --- Группы Еды ---
        if (menu.foodGroups.isNotEmpty()) {
            item {
                Text(
                    text = "Блюда",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                )
            }

            items(menu.foodGroups) { group ->
                Text(
                    text = group.name ?: "Название группы не указано",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

                if (group.items.isEmpty()) {
                    Text("Нет блюд в этой группе.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                } else {
                    LazyRow(contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(group.items) { food ->
                            FoodCard(
                                food = food,
                                onClick = { onMenuItemClick(food) }
                            )
                            Spacer(Modifier.width(16.dp))
                        }
                    }
                }
            }
        }

        // --- Разделитель между едой и напитками ---
        item {
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "🍹 Напитки",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // --- Группы Напитков ---
        if (menu.drinksGroups.isNotEmpty()) {
            items(menu.drinksGroups) { group ->
                Text(
                    text = group.name ?: "Название группы не указано",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

                if (group.items.isEmpty()) {
                    Text("Нет напитков в этой группе.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                } else {
                    LazyRow(contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(group.items) { drink ->
                            DrinkCard(
                                drink = drink,
                                onClick = { onMenuItemClick(drink) }
                            )
                            Spacer(Modifier.width(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// Вспомогательные Composable для отображения карточек (нужно доработать стиль)

@Composable
fun FoodCard(food: Food) {
    Card(
        modifier = Modifier.width(200.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = food.name ?: "Блюдо без имени",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${food.cost} р. | ${food.weight} г.",
                style = MaterialTheme.typography.bodySmall
            )
            food.ingredients?.takeIf { it.isNotBlank() }?.let { ingredients ->
                Text(
                    text = ingredients,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DrinkCard(drink: Drink) {
    Card(
        modifier = Modifier.width(180.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = drink.name ?: "Напиток без имени",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            val optionsText = drink.options.joinToString("\n") {
                "${it.sizeMl} мл / ${"%.2f".format(it.cost)} р."
            }

            Text(
                text = optionsText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}