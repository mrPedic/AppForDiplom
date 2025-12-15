package com.example.roamly.ui.screens.establishment

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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

/**
 * Утилита для кодирования Uri в Base64.
 */
fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Сжатие изображения для уменьшения размера строки Base64
        val byteArrayOutputStream = ByteArrayOutputStream()
        // 70% качество - это хороший компромисс между размером и качеством для Base64
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        // Кодирование в Base64
        Base64.encodeToString(byteArray, Base64.DEFAULT)
    } catch (e: Exception) {
        e.printStackTrace()
        null
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

    // В Production-приложении getId() обычно возвращает Optional или nullable Long,
    // но мы предполагаем, что он возвращает Long? для безопасности.
    val userId = userViewModel.getId()

    var rating by remember { mutableFloatStateOf(3f) }
    var reviewText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val isLoading by viewModel.isLoading.collectAsState()

    // Лаунчер для выбора изображения из галереи
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // --- Функция отправки ---
    val submitReview: () -> Unit = {
        if (userId == null || userId < 1) {
            Toast.makeText(context, "Ошибка: Пользователь не авторизован.", Toast.LENGTH_LONG).show()
        } else {
            // Кодируем фото в Base64 в фоновом потоке
            coroutineScope.launch {
                val base64String = withContext(Dispatchers.IO) {
                    selectedImageUri?.let { uriToBase64(context, it) }
                }

                viewModel.submitReview(
                    establishmentId = establishmentId,
                    userId = userId,
                    rating = rating,
                    reviewText = reviewText,
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

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Слайдер для оценки
        Text(
            text = "Ваша оценка: ${"%.1f".format(rating)} *",
            style = MaterialTheme.typography.titleMedium,
            color = AppTheme.colors.MainText
        )

        Slider(
            value = rating,
            onValueChange = { rating = it },
            valueRange = 1f..5f,
            steps = 3, // 1, 2, 3, 4, 5
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = AppTheme.colors.SelectedItem,
                activeTrackColor = AppTheme.colors.MainSuccess,
                inactiveTrackColor = AppTheme.colors.SecondaryContainer,
                activeTickColor = AppTheme.colors.MainSuccess,
                inactiveTickColor = AppTheme.colors.SecondaryContainer
            )
        )

        Spacer(Modifier.height(16.dp))

        // 2. Поле для текста отзыва
        OutlinedTextField(
            value = reviewText,
            onValueChange = { reviewText = it },
            label = {
                Text(
                    text = "Текст отзыва *",
                    color = AppTheme.colors.SecondaryText
                )
            },
            placeholder = {
                Text(
                    text = "Опишите свои впечатления...",
                    color = AppTheme.colors.SecondaryText.copy(alpha = 0.6f)
                )
            },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.MainBorder,
                unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                errorBorderColor = AppTheme.colors.MainFailure,
                focusedTextColor = AppTheme.colors.MainText,
                unfocusedTextColor = AppTheme.colors.MainText,
                cursorColor = AppTheme.colors.MainText,
                focusedLabelColor = AppTheme.colors.SecondaryText,
                unfocusedLabelColor = AppTheme.colors.SecondaryText,
                focusedContainerColor = AppTheme.colors.MainContainer,
                unfocusedContainerColor = AppTheme.colors.MainContainer,
                focusedPlaceholderColor = AppTheme.colors.SecondaryText.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = AppTheme.colors.SecondaryText.copy(alpha = 0.6f)
            )
        )

        Spacer(Modifier.height(16.dp))

        // 3. Кнопка выбора фото
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
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
                text = "Выбрать фото (опционально)",
                color = AppTheme.colors.MainText
            )
        }

        Spacer(Modifier.height(8.dp))

        // 4. Отображение выбранного фото
        selectedImageUri?.let { uri ->
            Card(
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colors.SecondaryContainer
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainBorder)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Используем Coil для загрузки изображения из Uri
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Выбранное фото",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 5. Кнопка отправки
        Button(
            onClick = submitReview,
            enabled = !isLoading && reviewText.isNotBlank() && rating > 0f,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.MainSuccess,
                contentColor = AppTheme.colors.MainText,
                disabledContainerColor = AppTheme.colors.MainSuccess.copy(alpha = 0.5f),
                disabledContentColor = AppTheme.colors.MainText.copy(alpha = 0.5f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = AppTheme.colors.MainText,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Отправить отзыв",
                    color = AppTheme.colors.MainText
                )
            }
        }

        // 6. Индикатор обязательных полей
        Text(
            text = "* - обязательные поля",
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.SecondaryText,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}