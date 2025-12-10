// UIForMenuItems.kt
package com.example.roamly.ui.screens.establishment

import android.util.Base64
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

@Composable
fun FoodCard(food: Food, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
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
            Text(food.name ?: "Без названия", fontWeight = FontWeight.Bold)
            Text("Цена: ${food.cost}")
            Text("Вес: ${food.weight} г")
        }
    }
}

@Composable
fun DrinkCard(drink: Drink, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
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
            Text(drink.name ?: "Без названия", fontWeight = FontWeight.Bold)
            drink.options.forEach { option ->
                Text("${option.sizeMl} мл - ${option.cost}")
            }
        }
    }
}