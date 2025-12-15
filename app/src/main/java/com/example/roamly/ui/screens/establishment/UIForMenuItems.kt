// UIForMenuItems.kt
package com.example.roamly.ui.screens.establishment

import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.classes.cl_menu.Food
import com.example.roamly.ui.theme.AppTheme

@Composable
fun FoodCard(food: Food, onClick: () -> Unit) {
    val colors = AppTheme.colors

    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer),
        border = BorderStroke(1.dp, AppTheme.colors.SecondaryBorder ) // Опционально тонкий бордер для акцента
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            food.photoBase64?.let { photo ->
                Image(
                    painter = rememberAsyncImagePainter(Base64.decode(photo, Base64.DEFAULT)),
                    contentDescription = null,
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = food.name ?: "Без названия",
                fontWeight = FontWeight.Bold,
                color = colors.MainText
            )
            Text(
                text = "Цена: ${food.cost}",
                color = colors.SecondaryText
            )
            Text(
                text = "Вес: ${food.weight} г",
                color = colors.SecondaryText
            )
        }
    }
}

@Composable
fun DrinkCard(drink: Drink, onClick: () -> Unit) {
    val colors = AppTheme.colors

    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer),
        border = BorderStroke(1.dp, AppTheme.colors.SecondaryBorder ) // Опционально тонкий бордер для акцента
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            drink.photoBase64?.let { photo ->
                Image(
                    painter = rememberAsyncImagePainter(Base64.decode(photo, Base64.DEFAULT)),
                    contentDescription = null,
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = drink.name ?: "Без названия",
                fontWeight = FontWeight.Bold,
                color = colors.MainText
            )
            drink.options.forEach { option ->
                Text(
                    text = "${option.sizeMl} мл - ${option.cost}",
                    color = colors.SecondaryText
                )
            }
        }
    }
}