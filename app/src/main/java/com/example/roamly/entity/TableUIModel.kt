package com.example.roamly.entity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


data class TableUIModel(
    val tempId: Long,
    val name: String,
    val description: String,
    val maxCapacity: Int
)

@Composable
fun TableEditorList(
    tables: List<TableUIModel>,
    onTablesChange: (List<TableUIModel>) -> Unit
) {
    var isDialogOpen by remember { mutableStateOf(false) }
    var tableToEdit by remember { mutableStateOf<TableUIModel?>(null) }

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Столики заведения",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Список текущих столиков
        if (tables.isEmpty()) {
            Text(
                text = "Нажмите 'Добавить столик', чтобы начать.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        } else {
            tables.forEach { table ->
                TableListItem(
                    table = table,
                    onEditClick = {
                        tableToEdit = table
                        isDialogOpen = true
                    },
                    onDeleteClick = {
                        onTablesChange(tables.filter { it.tempId != table.tempId })
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Кнопка добавления нового столика
        OutlinedButton(
            onClick = {
                val newTable = TableUIModel(
                    tempId = System.currentTimeMillis(), // Простой временный ID
                    name = "Столик №${tables.size + 1}",
                    description = "Обычный столик",
                    maxCapacity = 4
                )
                onTablesChange(tables + newTable)
            },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = ButtonDefaults.ContentPadding
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить столик")
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Добавить столик")
        }
    }

    // ⭐ Диалог редактирования
    if (isDialogOpen && tableToEdit != null) {
        TableEditDialog(
            table = tableToEdit!!,
            onDismiss = { isDialogOpen = false; tableToEdit = null },
            onSave = { updatedTable ->
                onTablesChange(tables.map {
                    if (it.tempId == updatedTable.tempId) updatedTable else it
                })
                isDialogOpen = false
                tableToEdit = null
            }
        )
    }
}

// Компонент одного элемента списка столов
@Composable
fun TableListItem(
    table: TableUIModel,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(table.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Мест: ${table.maxCapacity}, ${table.description}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun TableEditDialog(
    table: TableUIModel,
    onDismiss: () -> Unit,
    onSave: (TableUIModel) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(table.name) }
    var description by rememberSaveable { mutableStateOf(table.description) }
    var capacityText by rememberSaveable { mutableStateOf(table.maxCapacity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать столик") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название / Номер") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (У окна, VIP и т.д.)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it.filter { char -> char.isDigit() } }, // Только цифры
                    label = { Text("Макс. количество людей") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val isCapacityValid = capacityText.toIntOrNull() != null && capacityText.toInt() > 0

            TextButton(
                onClick = {
                    if (isCapacityValid) {
                        val updatedTable = table.copy(
                            name = name,
                            description = description,
                            maxCapacity = capacityText.toInt()
                        )
                        onSave(updatedTable)
                    }
                },
                enabled = name.isNotBlank() && isCapacityValid
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}