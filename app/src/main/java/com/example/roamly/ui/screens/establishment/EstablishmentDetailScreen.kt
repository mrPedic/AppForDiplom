package com.example.roamly.ui.screens.establishment

import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.roamly.entity.*
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import java.time.format.DateTimeFormatter
import java.util.Locale


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
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Вкладки
    val tabs = listOf("Описание", "Меню", "Карта", "Отзывы")
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    var selectedTab by remember { mutableIntStateOf(pagerState.currentPage) }

    LaunchedEffect(establishmentId) {
        viewModel.fetchEstablishmentById(establishmentId)
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
            // ⭐ ИСПОЛЬЗУЕМ НОВУЮ ШАПКУ
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
                        // Отображение контента вкладок
                        EstablishmentTabContent(page, establishment!!, navController)
                    }
                }
                else -> Text("Данные не найдены", Modifier.align(Alignment.Center))
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
            val mainPhotoBase64 = establishment?.photoBase64s
                ?.filter { it.isNotBlank() }
                ?.firstOrNull()

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
fun EstablishmentTabContent(page: Int, establishment: EstablishmentDisplayDto, navController: NavController) {
    // Используем LazyColumn/Column в зависимости от содержимого, но для карты нужно fillMaxSize()
    Column(modifier = Modifier.fillMaxSize()) {
        when (page) {
            0 -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Описание:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(establishment.description)
                    Spacer(Modifier.height(8.dp))
                    Text("Адрес: ${establishment.address}")
                    Text("Тип: ${convertTypeToWord(establishment.type)}")
                }
            }
            1 -> Text("Здесь будет отображаться Меню", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            2 -> {
                // ⭐ ИСПОЛЬЗУЕМ РЕАЛИЗАЦИЮ КАРТЫ
                EstablishmentMapTab(
                    name = establishment.name,
                    latitude = establishment.latitude,
                    longitude = establishment.longitude
                )
            }
            3 -> {
                ReviewTabContent(establishment = establishment, navController = navController)
            }
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
                // TODO: Добавить иконки звезд
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