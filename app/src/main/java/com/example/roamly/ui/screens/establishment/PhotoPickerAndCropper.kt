package com.example.roamly.ui.screens.establishment

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@Composable
fun PhotoPickerAndCropper(
    photoUris: List<Uri>,
    onUrisChange: (List<Uri>) -> Unit,
    navController: NavController
) {
    val context = LocalContext.current

    // Launcher для выбора одного фото из галереи
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                // ⭐ Вместо добавления напрямую, переходим к экрану обрезки.
                // ВАЖНО: Здесь предполагается, что у вас есть экран обрезки с URI в аргументах.
                // Поскольку у нас нет определения MapPicker, используем заглушку навигации:
                // navController.navigate(EstablishmentScreens.ImageCropper.createRoute(uri.toString()))

                // Для простоты примера, пока добавим без обрезки. В реальном проекте используйте экран обрезки.
                if (photoUris.size < MAX_PHOTOS) {
                    onUrisChange(photoUris + uri)
                }
            }
        }
    )

    // --- Логика обработки обрезанного URI (если используется отдельный экран) ---
    /* val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(Unit) {
        val croppedUriString = savedStateHandle?.get<String>(CROPPED_IMAGE_URI_KEY)
        if (croppedUriString != null) {
            val croppedUri = Uri.parse(croppedUriString)
            if (photoUris.size < MAX_PHOTOS) {
                onUrisChange(photoUris + croppedUri)
            }
            savedStateHandle.remove<String>(CROPPED_IMAGE_URI_KEY)
        }
    }
    */

    Column(Modifier.fillMaxWidth()) {
        Text("Фотографии заведения (до $MAX_PHOTOS шт.)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // 1. Список миниатюр
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            photoUris.forEachIndexed { index, uri ->
                PhotoThumbnail(
                    uri = uri,
                    onRemove = {
                        onUrisChange(photoUris.toMutableList().apply { removeAt(index) })
                    }
                )
            }

            // 2. Кнопка добавления (только если лимит не достигнут)
            if (photoUris.size < MAX_PHOTOS) {
                AddPhotoButton(
                    onClick = { imagePickerLauncher.launch("image/*") }
                )
            }
        }
    }
}

@Composable
fun PhotoThumbnail(uri: Uri, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.size(80.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(model = uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Кнопка удаления
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .clip(RoundedCornerShape(bottomStart = 8.dp))
                    .background(MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Удалить фото",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AddPhotoButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Edit, // Используем Edit как иконку для выбора
                contentDescription = "Добавить фото",
                modifier = Modifier.size(32.dp),
                tint = LocalContentColor.current
            )
        }
    }
}

// --------------------------------------------------------------------------
// ВАЖНО: Функция конвертации URI в Base64
// --------------------------------------------------------------------------
fun convertUriToBase64(context: Context, uri: Uri): String? {
    // ВАЖНО: ЭТА РЕАЛИЗАЦИЯ ТРЕБУЕТ БОЛЬШЕГО КОДА
    // В реальном проекте вы должны:
    // 1. Открыть InputStream через context.contentResolver.openInputStream(uri)
    // 2. Считать байты в ByteArray.
    // 3. Кодировать ByteArray в Base64 строку (android.util.Base64.encodeToString).

    // В целях демонстрации здесь используется заглушка:
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bytes = inputStream.readBytes()
            return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        }
    } catch (e: Exception) {
        Log.e("Base64Converter", "Error converting URI to Base64", e)
        return null
    }
    return null
}