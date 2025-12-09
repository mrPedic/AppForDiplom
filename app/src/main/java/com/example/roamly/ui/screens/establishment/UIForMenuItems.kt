package com.example.roamly.ui.screens.establishment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.classes.cl_menu.Food
import com.example.roamly.classes.cl_menu.MenuItem

// ==========================================
// 1. Карточки для ленты (LazyRow)
// ==========================================


@Composable
fun FoodCard(food: Food, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(200.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = food.name ?: "Блюдо без имени",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${food.cost} р. | ${food.weight} г.",
                style = MaterialTheme.typography.bodySmall
            )
            food.ingredients?.takeIf { it.isNotBlank() }?.let { ingredients ->
                Text(
                    text = ingredients,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DrinkCard(drink: Drink, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(180.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = drink.name ?: "Напиток без имени",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            val optionsText = drink.options.joinToString("\n") {
                "${it.sizeMl} мл / ${"%.2f".format(it.cost)} р."
            }

            Text(
                text = optionsText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==========================================
// 2. Всплывающие диалоги с деталями
// ==========================================

@Composable
fun MenuItemDetailDialog(item: Any, onDismiss: () -> Unit) {
    when (item) {
        is Food -> FoodDetailContent(food = item, onDismiss = onDismiss)
        is Drink -> DrinkDetailContent(drink = item, onDismiss = onDismiss)
        else -> onDismiss()
    }
}

@Composable
fun FoodDetailContent(food: Food, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = food.name ?: "Детали блюда",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Основная информация
                Text(
                    text = "${"%.0f".format(food.cost)} р.",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Вес: ${food.weight} г", style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(16.dp))

                // Ингредиенты
                if (!food.ingredients.isNullOrBlank()) {
                    Text("Состав:", fontWeight = FontWeight.Bold)
                    Text(food.ingredients!!, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                }

                // КБЖУ (Helper function below)
                NutritionalInfoTable(food)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
fun DrinkDetailContent(drink: Drink, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = drink.name ?: "Детали напитка",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Опции (Объем - Цена)
                if (drink.options.isNotEmpty()) {
                    Text("Варианты:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    drink.options.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${option.sizeMl} мл")
                            Text(
                                text = "${"%.0f".format(option.cost)} р.",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Ингредиенты
                if (!drink.ingredients.isNullOrBlank()) {
                    Text("Состав:", fontWeight = FontWeight.Bold)
                    Text(drink.ingredients!!, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                }

                // КБЖУ
                NutritionalInfoTable(drink)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

// ==========================================
// 3. Вспомогательный компонент для КБЖУ
// ==========================================

@Composable
fun NutritionalInfoTable(item: MenuItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = MaterialTheme.shapes.small)
            .padding(8.dp)
    ) {
        Text(
            text = "Пищевая ценность (на 100г):",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            NutritionalItem("Ккал", item.caloriesPer100g)
            NutritionalItem("Белки", item.proteinPer100g)
            NutritionalItem("Жиры", item.fatPer100g)
            NutritionalItem("Углев.", item.carbohydratesPer100g)
        }
    }
}

@Composable
fun NutritionalItem(label: String, value: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.caption, color = MaterialTheme.colorScheme.secondary)
        Text(text = "%.1f".format(value), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

// Добавьте этот extension если typography.caption недоступен в Material3:
val androidx.compose.material3.Typography.caption: androidx.compose.ui.text.TextStyle
    get() = this.bodySmall