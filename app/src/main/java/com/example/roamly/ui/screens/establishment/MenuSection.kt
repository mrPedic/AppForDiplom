package com.example.roamly.ui.screens.establishment

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.classes.cl_menu.Food
import com.example.roamly.classes.cl_menu.MenuOfEstablishment
import com.example.roamly.ui.theme.AppTheme

@Composable
fun SimpleFoodCard(food: Food, onClick: () -> Unit) {
    val colors = AppTheme.colors

    Card(
        modifier = Modifier
            .width(180.dp)
            .height(120.dp)
            .clickable(onClick = onClick)
            .padding(4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Фото (фиксированная высота)
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(120.dp)
//                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
//            ) {
//                /*if (!food.photoBase64.isNullOrEmpty()) {
//                    val imageBytes = Base64.decode(food.photoBase64, Base64.DEFAULT)
//                    Image(
//                        painter = rememberAsyncImagePainter(
//                            ImageRequest.Builder(LocalContext.current)
//                                .data(imageBytes)
//                                .crossfade(true)
//                                .build()
//                        ),
//                        contentDescription = food.name,
//                        modifier = Modifier.fillMaxSize(),
//                        contentScale = ContentScale.Crop
//                    )
//                } else {*/
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(colors.MainContainer),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        Icons.Default.Build,
//                        contentDescription = null,
//                        tint = colors.SecondaryText,
//                        modifier = Modifier.size(48.dp)
//                    )
//                }
//                //}
//            }

            Spacer(modifier = Modifier.height(8.dp))

            // Название (ограничено 2 строками)
            Text(
                text = food.name ?: "Без названия",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.MainText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .height(48.dp) // фиксированная высота для единообразия
            )

            Spacer(modifier = Modifier.weight(1f))

            // Цена и вес
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${food.cost} р.",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.MainSuccess,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${food.weight} г",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.SecondaryText
                )
            }
        }
    }
}

@Composable
fun SimpleDrinkCard(drink: Drink, onClick: () -> Unit) {
    val colors = AppTheme.colors

    Card(
        modifier = Modifier
            .width(180.dp)
            .height(120.dp) // Такая же высота, как у еды
            .clickable(onClick = onClick)
            .padding(4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Фото (фиксированная высота)
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(120.dp)
//                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
//            ) {
//                /*if (!drink.photoBase64.isNullOrEmpty()) {
//                    val imageBytes = Base64.decode(drink.photoBase64, Base64.DEFAULT)
//                    Image(
//                        painter = rememberAsyncImagePainter(
//                            ImageRequest.Builder(LocalContext.current)
//                                .data(imageBytes)
//                                .crossfade(true)
//                                .build()
//                        ),
//                        contentDescription = drink.name,
//                        modifier = Modifier.fillMaxSize(),
//                        contentScale = ContentScale.Crop
//                    )
//                } else {*/
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(colors.MainContainer),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        Icons.Default.Build,
//                        contentDescription = null,
//                        tint = colors.SecondaryText,
//                        modifier = Modifier.size(48.dp)
//                    )
//                }
//                //}
//            }

            Spacer(modifier = Modifier.height(8.dp))

            // Название (ограничено 2 строками)
            Text(
                text = drink.name ?: "Без названия",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.MainText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .height(48.dp) // фиксированная высота для единообразия
            )

            Spacer(modifier = Modifier.weight(1f))

            // Цена (самая низкая из опций)
            val minPrice = drink.options.minOfOrNull { it.cost } ?: 0.0
            Text(
                text = "от ${String.format("%.0f", minPrice)} р.",
                style = MaterialTheme.typography.titleLarge,
                color = colors.MainSuccess,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSection(menu: MenuOfEstablishment) {
    val colors = AppTheme.colors

    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var selectedDrink by remember { mutableStateOf<Drink?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.MainContainer)
            .padding(bottom = 16.dp)
    ) {
        // Раздел еды
        if (menu.foodGroups.isNotEmpty()) {
            Text(
                "Еда",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.MainText,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            menu.foodGroups.forEach { group ->
                if (group.items.isNotEmpty()) {
                    Text(
                        group.name ?: "Группа",
                        fontWeight = FontWeight.Bold,
                        color = colors.MainText,
                        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
                    )

                    LazyRow(
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        items(group.items) { food ->
                            SimpleFoodCard(food = food) {
                                selectedFood = food
                            }
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                    }
                }
            }
        }

        // Раздел напитков
        if (menu.drinksGroups.isNotEmpty()) {
            Text(
                "Напитки",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.MainText,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )

            menu.drinksGroups.forEach { group ->
                if (group.items.isNotEmpty()) {
                    Text(
                        group.name ?: "Группа",
                        fontWeight = FontWeight.Bold,
                        color = colors.MainText,
                        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
                    )

                    LazyRow(
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        items(group.items) { drink ->
                            SimpleDrinkCard(drink = drink) {
                                selectedDrink = drink
                            }
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                    }
                }
            }
        }

        // Если меню пустое
        if (menu.foodGroups.isEmpty() && menu.drinksGroups.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Меню пока не добавлено",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.SecondaryText
                    )
                }
            }
        }
    }

    // BottomSheet для еды
    if (selectedFood != null) {
        ModalBottomSheet(
            containerColor = colors.SecondaryContainer,
            onDismissRequest = { selectedFood = null },
            sheetState = sheetState
        ) {
            FoodDetailBottomSheet(food = selectedFood!!)
        }
    }

    // BottomSheet для напитков
    if (selectedDrink != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedDrink = null },
            sheetState = sheetState
        ) {
            DrinkDetailBottomSheet(drink = selectedDrink!!)
        }
    }
}

@Composable
fun FoodDetailBottomSheet(food: Food) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.SecondaryContainer)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            food.name ?: "Без названия",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.MainText
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Фото можно включить позже
        /*
        if (!food.photoBase64.isNullOrEmpty()) {
            val imageBytes = Base64.decode(food.photoBase64, Base64.DEFAULT)
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(imageBytes)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = food.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        */

        // Информация
        DetailRow("Цена", "${food.cost} р.")
        DetailRow("Вес", "${food.weight} г")
        DetailRow("Калорийность", "${food.caloriesPer100g} ккал/100г")
        DetailRow("Белки", "${food.proteinPer100g} г/100г")
        DetailRow("Жиры", "${food.fatPer100g} г/100г")
        DetailRow("Углеводы", "${food.carbohydratesPer100g} г/100г")

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Ингредиенты:",
            style = MaterialTheme.typography.titleMedium,
            color = colors.MainText
        )
        Text(
            food.ingredients ?: "Не указано",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.SecondaryText
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DrinkDetailBottomSheet(drink: Drink) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.SecondaryContainer)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            drink.name ?: "Без названия",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.MainText
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Фото можно включить позже
        /*
        if (!drink.photoBase64.isNullOrEmpty()) {
            val imageBytes = Base64.decode(drink.photoBase64, Base64.DEFAULT)
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(imageBytes)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = drink.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        */

        // Информация
        DetailRow("Калорийность", "${drink.caloriesPer100g} ккал/100г")
        DetailRow("Белки", "${drink.proteinPer100g} г/100г")
        DetailRow("Жиры", "${drink.fatPer100g} г/100г")
        DetailRow("Углеводы", "${drink.carbohydratesPer100g} г/100г")

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Ингредиенты:",
            style = MaterialTheme.typography.titleMedium,
            color = colors.MainText
        )
        Text(
            drink.ingredients ?: "Не указано",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.SecondaryText
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Опции объёма и цены
        Text(
            "Варианты объёма:",
            style = MaterialTheme.typography.titleMedium,
            color = colors.MainText
        )

        if (drink.options.isEmpty()) {
            Text(
                "Нет доступных объёмов",
                color = colors.SecondaryText,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            drink.options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${option.sizeMl} мл",
                        color = colors.MainText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${option.cost} р.",
                        color = colors.MainSuccess,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.SecondaryText)
        Text(value, color = colors.MainText, fontWeight = FontWeight.Medium)
    }
}
