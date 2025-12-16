package com.example.roamly.ui.screens.establishment

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Утилита для кодирования Uri в Base64.
 */
fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2 // Уменьшение размера в 2 раза для оптимизации
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                ?: throw IOException("Не удалось декодировать изображение")

            // Сжатие изображения для уменьшения размера строки Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            // 70% качество - это хороший компромисс между размером и качеством для Base64
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            // Освобождение памяти
            bitmap.recycle()

            // Кодирование в Base64
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Проверка валидности данных отзыва
 */
fun validateReviewData(rating: Float, reviewText: String): Pair<Boolean, String?> {
    return when {
        rating < 1f -> Pair(false, "Оценка должна быть не менее 1 звезды")
        reviewText.isBlank() -> Pair(false, "Текст отзыва не может быть пустым")
        reviewText.length < 10 -> Pair(false, "Отзыв должен содержать минимум 10 символов")
        reviewText.length > 2000 -> Pair(false, "Отзыв не должен превышать 2000 символов")
        else -> Pair(true, null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewCreationScreen(
    navController: NavController,
    establishmentId: Long,
    viewModel: EstablishmentViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // В Production-приложении getId() обычно возвращает Optional или nullable Long,
    // но мы предполагаем, что он возвращает Long? для безопасности.
    val userId by remember { derivedStateOf { userViewModel.getId() } }

    var rating by remember { mutableFloatStateOf(3f) }
    var reviewText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isEncodingImage by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()

    // Лаунчер для выбора изображения из галереи
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            validationError = null // Сбрасываем ошибку при выборе нового изображения
        }
    }

    // Функция для сброса выбранного изображения
    val clearSelectedImage = {
        selectedImageUri = null
    }

    // --- Функция отправки ---
    val submitReview: () -> Unit = submitReview@{
        // Валидация данных
        val (isValid, errorMessage) = validateReviewData(rating, reviewText)

        if (!isValid) {
            validationError = errorMessage
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            return@submitReview
        }

        if (userId == null || userId!! < 1) {
            Toast.makeText(context, "Ошибка: Пользователь не авторизован.", Toast.LENGTH_LONG).show()
            return@submitReview
        }

        // Кодируем фото в Base64 в фоновом потоке
        coroutineScope.launch {
            isEncodingImage = true
            val base64String = withContext(Dispatchers.IO) {
                selectedImageUri?.let { uriToBase64(context, it) }
            }
            isEncodingImage = false

            viewModel.submitReview(
                establishmentId = establishmentId,
                userId = userId!!,
                rating = rating,
                reviewText = reviewText.trim(),
                photoBase64 = base64String,
                onResult = { success, errorMsg ->
                    if (success) {
                        Toast.makeText(context, "Отзыв успешно отправлен!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack() // Возвращаемся на предыдущий экран
                    } else {
                        Toast.makeText(context, errorMsg ?: "Ошибка отправки отзыва.", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Новый отзыв",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTheme.colors.MainText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = AppTheme.colors.MainText
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppTheme.colors.MainContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.MainContainer)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Слайдер для оценки
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = AppTheme.colors.MainSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Ваша оценка: ${"%.1f".format(rating)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.colors.MainText
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Slider(
                        value = rating,
                        onValueChange = {
                            rating = it
                            validationError = null // Сбрасываем ошибку при изменении
                        },
                        valueRange = 1f..5f,
                        steps = 7, // 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = AppTheme.colors.SelectedItem,
                            activeTrackColor = AppTheme.colors.MainSuccess,
                            inactiveTrackColor = AppTheme.colors.SecondaryContainer,
                            activeTickColor = AppTheme.colors.MainSuccess,
                            inactiveTickColor = AppTheme.colors.SecondaryContainer
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1", color = AppTheme.colors.SecondaryText)
                        Text("5", color = AppTheme.colors.SecondaryText)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 2. Поле для текста отзыва
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Текст отзыва *",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.SecondaryText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = {
                            reviewText = it
                            validationError = null // Сбрасываем ошибку при изменении
                        },
                        placeholder = {
                            Text(
                                text = "Опишите свои впечатления...",
                                color = AppTheme.colors.SecondaryText.copy(alpha = 0.6f)
                            )
                        },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth(),
                        isError = validationError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.colors.MainBorder,
                            unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                            errorBorderColor = AppTheme.colors.MainFailure,
                            focusedTextColor = AppTheme.colors.MainText,
                            unfocusedTextColor = AppTheme.colors.MainText,
                            cursorColor = AppTheme.colors.MainText,
                            focusedContainerColor = AppTheme.colors.SecondaryContainer,
                            unfocusedContainerColor = AppTheme.colors.SecondaryContainer,
                            errorContainerColor = AppTheme.colors.MainContainer,
                            focusedPlaceholderColor = AppTheme.colors.SecondaryText.copy(alpha = 0.6f),
                            unfocusedPlaceholderColor = AppTheme.colors.SecondaryText.copy(alpha = 0.6f)
                        ),
//                        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions.Default.copy(
//                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
//                        )
                    )

                    // Счетчик символов
                    Text(
                        text = "${reviewText.length}/2000",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reviewText.length > 2000) AppTheme.colors.MainFailure
                        else AppTheme.colors.SecondaryText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )

                    // Сообщение об ошибке
                    validationError?.takeIf { reviewText.isNotBlank() }?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.MainFailure,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 3. Блок для фото
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Фотография (опционально)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.SecondaryText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Кнопка выбора фото
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && !isEncodingImage,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = AppTheme.colors.SecondaryContainer,
                            contentColor = AppTheme.colors.MainText,
                            disabledContainerColor = AppTheme.colors.SecondaryContainer.copy(alpha = 0.5f),
                            disabledContentColor = AppTheme.colors.MainText.copy(alpha = 0.5f)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainBorder)
                        )
                    ) {
                        Text(
                            text = "Выбрать фото из галереи",
                            color = AppTheme.colors.MainText
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Отображение выбранного фото
                    selectedImageUri?.let { uri ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = AppTheme.colors.SecondaryContainer
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainBorder)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                // Кнопка удаления фото
                                IconButton(
                                    onClick = clearSelectedImage,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .background(
                                            AppTheme.colors.MainContainer.copy(alpha = 0.8f),
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Удалить фото",
                                        tint = AppTheme.colors.MainText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Изображение
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = "Выбранное фото",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(8.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // 4. Кнопка отправки
                Button(
                    onClick = submitReview,
                    enabled = !isLoading && !isEncodingImage && reviewText.isNotBlank() && rating > 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.MainSuccess,
                        contentColor = AppTheme.colors.MainText,
                        disabledContainerColor = AppTheme.colors.MainSuccess.copy(alpha = 0.5f),
                        disabledContentColor = AppTheme.colors.MainText.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading || isEncodingImage) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = AppTheme.colors.MainText,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = if (isEncodingImage) "Обработка изображения..." else "Отправка...",
                                color = AppTheme.colors.MainText
                            )
                        }
                    } else {
                        Text(
                            text = "Отправить отзыв",
                            style = MaterialTheme.typography.labelLarge,
                            color = AppTheme.colors.MainText
                        )
                    }
                }

                // 5. Индикатор обязательных полей
                Text(
                    text = "* - обязательные поля",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.SecondaryText,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Полупрозрачный оверлей при загрузке
            if (isLoading || isEncodingImage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.MainContainer.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = AppTheme.colors.MainSuccess,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (isEncodingImage) "Обработка изображения..." else "Отправка отзыва...",
                            color = AppTheme.colors.MainText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}