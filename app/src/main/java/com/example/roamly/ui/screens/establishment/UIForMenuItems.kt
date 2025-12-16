// UIForMenuItems.kt
package com.example.roamly.ui.screens.establishment

import android.os.Build
import android.util.Base64
import android.util.Log
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.classes.cl_menu.Food
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
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val context = LocalContext.current

    // Создаем состояние для хранения декодированного изображения
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
                    // Игнорируем ошибки декодирования
                    Log.e("ReviewCard", "Error decoding photo: ${e.message}")
                    value = null
                }
            } else {
                value = null
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.SecondaryContainer,
            contentColor = colors.MainText
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, colors.SecondaryBorder.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок с рейтингом и датой
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Рейтинг
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    // Дополнительные звезды для визуализации
                    Row {
                        repeat(5) { index ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < review.rating.toInt())
                                    colors.MainSuccess.copy(alpha = 0.5f)
                                else
                                    colors.SecondaryBorder.copy(alpha = 0.3f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Дата
                review.dateOfCreation?.let {
                    Text(
                        it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Текст отзыва
            Text(
                review.reviewText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.MainText,
                lineHeight = 20.sp
            )

            // Фото отзыва (если есть)
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

            Spacer(modifier = Modifier.height(8.dp))

            // Футер с информацией о пользователе
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Аватар пользователя
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.MainSuccess.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = colors.MainSuccess,
                        modifier = Modifier.size(18.dp)
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
}