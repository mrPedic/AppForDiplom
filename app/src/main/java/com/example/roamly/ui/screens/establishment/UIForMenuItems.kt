package com.example.roamly.ui.screens.establishment

import android.os.Build
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.classes.cl_menu.Food
import com.example.roamly.entity.DTO.ReviewReportDto
import com.example.roamly.entity.ViewModel.EstablishmentDetailViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.entity.classes.ReviewEntity
import com.example.roamly.ui.theme.AppTheme
import java.time.format.DateTimeFormatter

@Composable
fun SimpleFoodCard(food: Food) { // Замените Food на ваш реальный класс
    val colors = AppTheme.colors

    Card(
        modifier = Modifier
            .width(150.dp)
            .height(100.dp), // Фиксированная высота
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer),
        border = BorderStroke(1.dp, colors.SecondaryBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Название блюда
            Text(
                text = food.name ?: "Название",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colors.MainText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Информация о блюде
            Column {
                // Вес
                if (food.weight != null) {
                    Text(
                        text = "${food.weight}г",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.SecondaryText
                    )
                }

                // Калории (если есть)
                if (food.caloriesPer100g != null) {
                    Text(
                        text = "${food.caloriesPer100g} ккал/100г",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.SecondaryText
                    )
                }

                // Цена
                if (food.cost != null) {
                    Text(
                        text = "${String.format("%.2f", food.cost)} BYN",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.MainSuccess,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleDrinkCard(drink: Drink) { // Замените Drink на ваш реальный класс
    val colors = AppTheme.colors

    Card(
        modifier = Modifier
            .width(150.dp)
            .height(100.dp), // Фиксированная высота
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer),
        border = BorderStroke(1.dp, colors.SecondaryBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Название напитка
            Text(
                text = drink.name ?: "Напиток",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colors.MainText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Информация о напитке
            Column {
                // Состав (кратко)
                drink.ingredients?.let { ingredients ->
                    if (ingredients.length > 30) {
                        Text(
                            text = "${ingredients.substring(0, 30)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.SecondaryText
                        )
                    } else {
                        Text(
                            text = ingredients,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.SecondaryText
                        )
                    }
                }

                // Цена (берем минимальную из опций)
                val minPrice = drink.options?.minOfOrNull { it.cost ?: 0.0 }
                if (minPrice != null && minPrice > 0) {
                    Text(
                        text = "от ${String.format("%.2f", minPrice)} BYN",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.MainSuccess,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReviewCard(
    review: ReviewEntity,
    modifier: Modifier = Modifier,
    establishmentId: Long
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val viewModel: EstablishmentDetailViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val user by userViewModel.user.collectAsState()

    val imageBytes by produceState<ByteArray?>(initialValue = null, key1 = review.photoBase64) {
        review.photoBase64?.let { photo ->
            if (photo.isNotBlank() && !photo.startsWith("PLACEHOLDER") && !photo.startsWith("ERROR")) {
                try {
                    val cleanPhoto = if (photo.contains(",")) {
                        photo.substring(photo.indexOf(",") + 1)
                    } else {
                        photo
                    }
                    value = Base64.decode(cleanPhoto, Base64.DEFAULT)
                } catch (e: Exception) {
                    Log.e("ReviewCard", "Error decoding photo: ${e.message}")
                    value = null
                }
            } else {
                value = null
            }
        }
    }

    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf("") }
    var otherDescription by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.SecondaryContainer,
            contentColor = colors.MainText
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, colors.SecondaryBorder.copy(alpha = 0.5f)) // Сделал границу чуть заметнее для консистентности
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = colors.MainSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        review.rating.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.MainText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row {
                        repeat(5) { index ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < review.rating.toInt())
                                    colors.MainSuccess.copy(alpha = 0.6f)
                                else
                                    colors.SecondaryBorder.copy(alpha = 0.3f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    review.dateOfCreation?.let {
                        Text(
                            it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.SecondaryText
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Пожаловаться",
                            tint = colors.SecondaryFailure.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                review.reviewText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.MainText,
                lineHeight = 20.sp
            )

            if (imageBytes != null) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageBytes)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.MainSuccess.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = colors.MainSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Пользователь #${review.createdUserId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.SecondaryText
                )
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = colors.MainContainer, // Установка фона диалога
            title = {
                Text(
                    "Пожаловаться на отзыв",
                    color = colors.MainText,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    val reasons = listOf("Spam", "Offensive Content", "Fake Review", "Other")
                    val russianReasons = listOf("Спам", "Оскорбительный контент", "Ложный отзыв", "Другое" )
                    russianReasons.forEach { reason ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedReason = reason },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.MainSuccess,
                                    unselectedColor = colors.SecondaryBorder
                                )
                            )
                            Text(reason, color = colors.MainText)
                        }
                    }
                    if (selectedReason == "Other") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = otherDescription,
                            onValueChange = { otherDescription = it },
                            placeholder = { Text("Опишите проблему...", color = colors.SecondaryText) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.MainSuccess,
                                unfocusedBorderColor = colors.SecondaryBorder,
                                focusedTextColor = colors.MainText,
                                unfocusedTextColor = colors.MainText,
                                cursorColor = colors.MainSuccess
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedReason.isNotEmpty()) {
                            // ИЗМЕНЕНИЕ: Передаем context и callback
                            viewModel.reportReview(
                                context = context,
                                reviewId = review.id,
                                userId = user.id ?: 0L,
                                establishmentId = establishmentId,
                                reason = selectedReason,
                                description = if (selectedReason == "Другое") otherDescription else null,
                                onSuccess = {
                                    showReportDialog = false
                                    selectedReason = ""
                                    otherDescription = ""
                                }
                            )
                        } else {
                            Toast.makeText(context, "Выберите причину", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.MainSuccess, contentColor = colors.MainContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Отправить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showReportDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.SecondaryText // Делаем кнопку "Отмена" менее акцентной
                    )
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}