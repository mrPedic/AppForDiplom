package com.example.roamly.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.roamly.entity.DTO.EstablishmentDisplayDto
import com.example.roamly.entity.EstablishmentStatus // Импорт статуса
import com.example.roamly.entity.ViewModel.EstablishmentViewModel

@Composable
fun PendingListScreen(
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    val pendingList by viewModel.pendingEstablishments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Загружаем список при первом входе на экран
    LaunchedEffect(Unit) {
        viewModel.fetchPendingEstablishments()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (pendingList.isEmpty()) {
                Text("Нет ожидающих заявок.", modifier = Modifier.padding(top = 16.dp))
            } else {
                LazyColumn(contentPadding = PaddingValues(top = 8.dp)) {
                    items(pendingList, key = { it.id }) { establishment ->
                        PendingItemCard(
                            establishment = establishment,
                            onApprove = { viewModel.updateEstablishmentStatus(it.id, EstablishmentStatus.ACTIVE) },
                            onReject = { viewModel.updateEstablishmentStatus(it.id, EstablishmentStatus.REJECTED) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PendingItemCard(
    establishment: EstablishmentDisplayDto,
    onApprove: (EstablishmentDisplayDto) -> Unit,
    onReject: (EstablishmentDisplayDto) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = establishment.name, style = MaterialTheme.typography.titleMedium)
            Text(text = establishment.address, style = MaterialTheme.typography.bodyMedium)
            Text(text = "Тип: ${establishment.type}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Создал: ID ${establishment.createdUserId}", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onReject(establishment) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отклонить")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onApprove(establishment) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Одобрить")
                }
            }
        }
    }
}