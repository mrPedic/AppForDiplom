package com.example.roamly.ui.screens.establishment

import android.util.Base64
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.classes.cl_menu.Food
import com.example.roamly.classes.cl_menu.MenuOfEstablishment
import com.example.roamly.entity.DTO.EstablishmentDisplayDto
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.sealed.BookingScreens
import java.util.Calendar

// Используем заглушки для отсутствующих частей
val convertTypeToWord: (String) -> String = { it }
val EstablishmentMapTab: @Composable (name: String, latitude: Double, longitude: Double) -> Unit =
    { name, lat, lon -> Text("Карта для $name ($lat, $lon)", Modifier.fillMaxSize().padding(16.dp)) }


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EstablishmentDetailScreen(
    navController: NavController,
    establishmentId: Long,
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    // Получаем состояние из ViewModel
    val establishment by viewModel.currentEstablishment.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val menuState by viewModel.menuOfEstablishment.collectAsState()
    val isMenuLoading by viewModel.isMenuLoading.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()

    // Вкладки
    val tabs = listOf("Описание", "Меню", "Карта", "Отзывы")
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    var selectedTab by remember { mutableIntStateOf(pagerState.currentPage) }

    LaunchedEffect(establishmentId) {
        viewModel.fetchEstablishmentById(establishmentId)
    }

    LaunchedEffect(establishment) {
        establishment?.let {
            // ⭐ ИСПРАВЛЕНИЕ: Загрузка меню
            viewModel.fetchMenuForEstablishment(it.id)
        }
    }

    LaunchedEffect(selectedTab) {
        pagerState.scrollToPage(selectedTab)
    }

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage

        if (pagerState.currentPage == 3 && establishment != null) {
            viewModel.fetchReviewsForEstablishment(establishment!!.id)
        }
    }

    Scaffold(
        topBar = {
            EstablishmentHeader(
                establishment = establishment,
                selectedTab = selectedTab,
                tabs = tabs,
                onTabSelected = { selectedTab = it },
                navController = navController
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading && establishment == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                errorMessage != null -> Text(
                    text = "Ошибка: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                establishment != null -> {
                    HorizontalPager(state = pagerState) { page ->
                        // ⭐ ИСПРАВЛЕНИЕ: Передача состояния меню
                        EstablishmentTabContent(
                            page = page,
                            establishment = establishment!!,
                            navController = navController,
                            menuState = menuState,
                            isMenuLoading = isMenuLoading
                        )
                    }
                }
                else -> Text("Данные не найдены", Modifier.align(Alignment.Center))
            }
        }
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

// --------------------------------------------------------------------------------------------------
// ⭐ НОВЫЙ КОМПОНЕНТ ДЛЯ ШАПКИ
// --------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstablishmentHeader(
    establishment: EstablishmentDisplayDto?,
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    navController: NavController
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val mainPhotoBase64 = establishment?.photoBase64s?.firstOrNull { it.isNotBlank() }

            // Добавим логгирование для отладки
            LaunchedEffect(mainPhotoBase64) {
                if (mainPhotoBase64 != null) {
                    Log.d("EstablishmentHeader", "Найдено Base64 фото. Длина: ${mainPhotoBase64.length}")
                    // Можно залогировать первые 50 символов для проверки
                    Log.d("EstablishmentHeader", "Начало Base64: ${mainPhotoBase64.take(50)}...")
                } else {
                    Log.d("EstablishmentHeader", "Фото Base64 не найдено или пусто.")
                }
            }


            if (mainPhotoBase64 != null) {
                val imageBytes = remember(mainPhotoBase64) { base64ToByteArray(mainPhotoBase64) }

                LaunchedEffect(imageBytes) {
                    if (imageBytes != null) {
                        Log.d("EstablishmentHeader", "Base64 успешно конвертирован в ${imageBytes.size} байт.")
                    } else {
                        Log.e("EstablishmentHeader", "Ошибка конвертации Base64 в байты.")
                    }
                }

                if (imageBytes != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageBytes),
                        contentDescription = "Основное фото заведения",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Заглушка, если Base64 некорректен
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray.copy(alpha = 0.5f)) // Простой серый фон
                    ) {
                        Text(
                            "Не удалось загрузить фото",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            } else {
                // Заглушка, если фото нет вообще
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        "Фото заведения отсутствует",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // ⭐ Сплошная заливка на фоне текста
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)) // Сплошная заливка, более темная
            )

            // ⭐ Заголовок и кнопка Edit (прикреплены к нижней части Box)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Название заведения
                    Text(
                        text = establishment?.name ?: "Загрузка...",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        // Ограничиваем, чтобы название не наезжало на кнопку
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    // Кнопка редактирования
                    establishment?.let {
                        IconButton(onClick = {
                            navController.navigate(EstablishmentScreens.EstablishmentEdit.createRoute(it.id))
                        }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Редактировать",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ⭐ 2. TabRow (всегда под шапкой)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title) }
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
    userViewModel: UserViewModel = hiltViewModel()
) {
    // Получаем текущего пользователя и его ID
    val currentUser by userViewModel.user.collectAsState()
    val currentUserId = currentUser.id ?: -1L

    // ⭐ Проверка, является ли текущий пользователь владельцем заведения
    val isOwner = establishment.createdUserId == currentUserId

    when (page) {
        0 -> {
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
                        navController.navigate(
                            BookingScreens.CreateBooking.createRoute(establishment.id)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Забронировать столик (Шаг 10 мин)")
                }

                Spacer(Modifier.height(16.dp))
                OperatingStatusDisplay(establishment.operatingHoursString)

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Полное расписание:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                OperatingHoursDisplay(convertHoursStringToMap(establishment.operatingHoursString))
            }
        }
        // ⭐ ИЗМЕНЕНИЕ: Вкладка "Меню"
        1 -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp) // Общий горизонтальный отступ
        ) {
            if (isMenuLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (menuState == null || (menuState.foodGroups.isEmpty() && menuState.drinksGroups.isEmpty())) {
                Column(Modifier.align(Alignment.Center)) {
                    Text("Меню отсутствует.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                // ⭐ ПЕРЕДАЕМ isOwner И НАЧИНАЕМ С MenuDisplayContent
                MenuDisplayContent(
                    menu = menuState,
                    isOwner = isOwner,
                    establishmentId = establishment.id,
                    navController = navController
                )
            }
        }
        2 -> {
            // Карта
            EstablishmentMapTab(
                name = establishment.name,
                latitude = establishment.latitude,
                longitude = establishment.longitude
            )
        }
        3 -> {
            // Отзывы
            ReviewTabContent(establishment = establishment, navController = navController)
        }
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

    // ⭐ Используем Box для размещения списка и закрепленной кнопки
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Spacer(Modifier.height(8.dp))

            // --- Логика отображения сообщения (перенесена наверх) ---
            when {
                !isLoggedIn -> {
                    Text("Войдите в систему, чтобы оставить отзыв.", color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                }
                isOwner -> {
                    Text("Вы являетесь владельцем этого заведения и не можете оставить отзыв.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
                // Для canReview теперь будет только кнопка внизу.
            }

            // --- Отображение списка отзывов ---
            if (isReviewsLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (reviews.isEmpty()) {
                Text("Отзывов пока нет. Будьте первыми!", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Список отзывов (${reviews.size}):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                // ⭐ LazyColumn занимает место до закрепленной кнопки
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f), // Занимаем все доступное пространство
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reviews) { review ->
                        // ⭐ Логика выбора карточки
                        if (review.photoBase64.isNullOrBlank()) {
                            ReviewItem(review = review)
                        } else {
                            ReviewItemWithPhoto(review = review)
                        }
                        Divider()
                    }
                }
            }
        }

        // ⭐ ЗАКРЕПЛЕННАЯ КНОПКА ВНИЗУ
        if (canReview) {
            Button(
                onClick = {
                    navController.navigate(EstablishmentScreens.ReviewCreation.createRoute(establishment.id))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.BottomCenter) // Прикрепляем к низу Box
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

// ⭐ ОБЩИЙ КОМПОНЕНТ ДЛЯ ПОВТОРЯЮЩЕГОСЯ КОНТЕНТА
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
            review.dateOfCreation?.let {
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
    // ⭐ ВЫЗОВ ФУНКЦИИ СТАТУСА
    val status = remember(hoursString) { getOperatingStatus(hoursString) }

    val color = if (status.isOpen) Color(0xFF4CAF50) /* Green */ else MaterialTheme.colorScheme.error

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

// ⭐ НОВЫЙ СПИСОК ДНЕЙ ДЛЯ СОПОСТАВЛЕНИЯ С Calendar.DAY_OF_WEEK
// Calendar.SUNDAY = 1, Calendar.MONDAY = 2, ..., Calendar.SATURDAY = 7
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

// ⭐ ИЗМЕНЕНА: Использует Calendar API для определения текущего дня и времени + ЛОГИ
fun getOperatingStatus(hoursString: String?): OperatingStatus {
    // ВАЖНО: Предполагаем, что convertHoursStringToMap возвращает Map<String, String>,
    // где ключи - это русские названия дней: "Понедельник", "Вторник" и т.д.
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
        // ... (Функция parseTime остается прежней)
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
                    // ⭐ УПРОЩЕНО: Просто говорим "Открыто" и часы на сегодня
                    Log.d("OpStatus", "Статус: ОТКРЫТО. Часы: $todayHours")
                    return OperatingStatus(
                        true,
                        "Открыто. Работает сегодня до ${parts[1]}.", // Просто конечная точка
                    )
                } else if (nowTotalMinutes < startMinutes && !isOvernight) {
                    // ⭐ УПРОЩЕНО: Говорим "Закрыто, откроется сегодня" (без точного времени)
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

// ⭐ ИЗМЕНЕНА: Использует Calendar API для поиска следующего рабочего дня + ЛОГИ
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

                // ⭐ УПРОЩЕНО: Только день
                Log.d("OpStatus", "Найдено: Откроется $dayLabel.")
                return OperatingStatus(
                    false,
                    "Закрыто. Откроется $dayLabel.",
                    // parts[0]
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
    isOwner: Boolean, // ⭐ НОВЫЙ ПАРАМЕТР
    establishmentId: Long, // ⭐ НОВЫЙ ПАРАМЕТР
    navController: NavController // ⭐ НОВЫЙ ПАРАМЕТР
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // contentPadding убираем или уменьшаем, так как внешний Box уже имеет отступ.
        // Оставим только вертикальный, чтобы разгрузить внешний Box.
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
                    text = "🍽️ Блюда",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                )
            }

            items(menu.foodGroups) { group ->
                // ⭐ Обработка null (group.name) с использованием оператора Elvis
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
                            FoodCard(food = food)
                            Spacer(Modifier.width(16.dp)) // Отступ между горизонтальными карточками
                        }
                    }
                }
            }
        }

        // --- Разделитель между едой и напитками ---
        item {
            Spacer(Modifier.height(24.dp))
            Divider()
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
                // ⭐ Обработка null (group.name)
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
                            DrinkCard(drink = drink)
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
            // ⭐ Обработка null (food.name)
            Text(
                text = food.name ?: "Блюдо без имени",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${food.cost ?: 0} р. | ${food.weight ?: 0} г.", // Предполагаем, что cost и weight могут быть null
                style = MaterialTheme.typography.bodySmall
            )
            // Ингредиенты (безопасная проверка, оставлена как у вас)
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
            // ⭐ Обработка null (drink.name)
            Text(
                text = drink.name ?: "Напиток без имени",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            // Обработка опций (предполагаем, что options не null, но может быть пустым)
            val optionsText = drink.options?.joinToString("\n") {
                "${it.sizeMl ?: 0} мл / ${"%.2f".format(it.cost ?: 0f)} р."
            } ?: "Нет опций"

            Text(
                text = optionsText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}