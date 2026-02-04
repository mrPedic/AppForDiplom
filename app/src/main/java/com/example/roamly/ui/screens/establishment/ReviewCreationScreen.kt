// Updated ReviewCreationScreen.kt (ensure delete button is present, no changes needed as it's already there)
package com.example.roamly.ui.screens.establishment

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
    reviewId: Long? = null,
    initialRating: Float = 3f,
    initialText: String = "",
    viewModel: EstablishmentViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val colors = AppTheme.colors

    // В Production-приложении getId() обычно возвращает Optional или nullable Long,
    // но мы предполагаем, что он возвращает Long? для безопасности.
    val userId by remember { derivedStateOf { userViewModel.getId() } }

    var rating by remember { mutableFloatStateOf(initialRating) }
    var reviewText by remember { mutableStateOf(initialText) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var existingPhotoBase64 by remember { mutableStateOf<String?>(null) }
    var isPhotoRemoved by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isEncodingImage by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val selectedReview by viewModel.selectedReview.collectAsState()

    // Лаунчер для выбора изображения из галереи
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            isPhotoRemoved = false
            validationError = null // Сбрасываем ошибку при выборе нового изображения
        }
    }

    // Функция для сброса выбранного изображения
    val clearSelectedImage = {
        if (selectedImageUri != null) {
            selectedImageUri = null
        } else {
            isPhotoRemoved = true
        }
    }

    LaunchedEffect(reviewId) {
        if (reviewId != null) {
            viewModel.loadReview(reviewId)
        }
    }

    LaunchedEffect(selectedReview) {
        selectedReview?.let { review ->
            rating = review.rating
            reviewText = review.reviewText
            existingPhotoBase64 = review.photoBase64
        }
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
                when {
                    selectedImageUri != null -> uriToBase64(context, selectedImageUri!!)
                    isPhotoRemoved -> null
                    else -> existingPhotoBase64
                }
            }
            isEncodingImage = false

            if (reviewId != null) {
                viewModel.updateReview(
                    reviewId = reviewId,
                    establishmentId = establishmentId,
                    userId = userId!!,
                    rating = rating,
                    reviewText = reviewText.trim(),
                    photoBase64 = base64String,
                    onResult = { success, errorMsg ->
                        if (success) {
                            Toast.makeText(context, "Отзыв успешно обновлен!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack() // Возвращаемся на предыдущий экран
                        } else {
                            Toast.makeText(context, errorMsg ?: "Ошибка обновления отзыва.", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } else {
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
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (reviewId != null) "Редактировать отзыв" else "Новый отзыв",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.MainText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = colors.MainText
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.MainContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.MainContainer)
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
                            tint = colors.MainSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Ваша оценка: ${"%.1f".format(rating)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.MainText
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
                        steps = 7, // Исправлено на 7 для точного шага 0.5
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            activeTrackColor = colors.MainSuccess,
                            inactiveTrackColor = colors.SecondaryBorder,
                            activeTickColor = colors.MainSuccess,
                            inactiveTickColor = colors.SecondaryBorder,
                            thumbColor = colors.MainSuccess
                        )
                    )

                    validationError?.takeIf { rating < 1f }?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.MainFailure,
                            modifier = Modifier.padding(top = 4.dp)
                        )
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
                        color = colors.SecondaryText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = {
                            if (it.length <= 2000) {
                                reviewText = it
                                validationError = null
                            }
                        },
                        placeholder = {
                            Text(
                                text = "Напишите отзыв (минимум 10 символов)...",
                                color = colors.SecondaryText
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        maxLines = 10,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.MainSuccess,
                            unfocusedBorderColor = colors.SecondaryBorder,
                            focusedTextColor = colors.MainText,
                            unfocusedTextColor = colors.MainText,
                            cursorColor = colors.MainSuccess,
                            focusedContainerColor = colors.SecondaryContainer,
                            unfocusedContainerColor = colors.SecondaryContainer
                        ),
                        shape = MaterialTheme.shapes.medium,
                        // ── Современный способ ────────────────────────────────
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { /* можно добавить скрытие клавиатуры или submit */ }
                        )
                    )

                    // Счетчик символов
                    Text(
                        text = "${reviewText.length}/2000",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reviewText.length > 2000) colors.MainFailure
                        else colors.SecondaryText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        textAlign = TextAlign.End
                    )

                    // Сообщение об ошибке
                    validationError?.takeIf { reviewText.isNotBlank() }?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.MainFailure,
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
                        color = colors.SecondaryText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Кнопка выбора фото
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && !isEncodingImage,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.SecondaryContainer,
                            contentColor = colors.MainText,
                            disabledContainerColor = colors.SecondaryContainer.copy(alpha = 0.5f),
                            disabledContentColor = colors.MainText.copy(alpha = 0.5f)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(colors.MainBorder)
                        )
                    ) {
                        Text(
                            text = "Выбрать фото из галереи",
                            color = colors.MainText
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Отображение выбранного или существующего фото
                    val showPhoto = selectedImageUri != null || (existingPhotoBase64 != null && !isPhotoRemoved)
                    if (showPhoto) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = colors.SecondaryContainer
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(colors.MainBorder)
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
                                            colors.MainContainer.copy(alpha = 0.8f),
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Удалить фото",
                                        tint = colors.MainText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Изображение
                                val imageData: Any? = if (selectedImageUri != null) {
                                    selectedImageUri
                                } else {
                                    try {
                                        val clean = if (existingPhotoBase64!!.contains(",")) {
                                            existingPhotoBase64!!.split(",")[1]
                                        } else {
                                            existingPhotoBase64!!
                                        }
                                        Base64.decode(clean, Base64.DEFAULT)
                                    } catch (e: Exception) {
                                        Log.e("ReviewCreationScreen", "Error decoding base64", e)
                                        null
                                    }
                                }
                                if (imageData != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(imageData),
                                        contentDescription = "Фото отзыва",
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
                        containerColor = colors.MainSuccess,
                        contentColor = colors.MainText,
                        disabledContainerColor = colors.MainSuccess.copy(alpha = 0.5f),
                        disabledContentColor = colors.MainText.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading || isEncodingImage) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = colors.MainText,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = if (isEncodingImage) "Обработка изображения..." else "Отправка...",
                                color = colors.MainText
                            )
                        }
                    } else {
                        Text(
                            text = if (reviewId != null) "Сохранить изменения" else "Отправить отзыв",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.MainText
                        )
                    }
                }

                // 5. Индикатор обязательных полей
                Text(
                    text = "* - обязательные поля",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.SecondaryText,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Полупрозрачный оверлей при загрузке
            if (isLoading || isEncodingImage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.MainContainer.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = colors.MainSuccess,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (isEncodingImage) "Обработка изображения..." else "Отправка отзыва...",
                            color = colors.MainText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}