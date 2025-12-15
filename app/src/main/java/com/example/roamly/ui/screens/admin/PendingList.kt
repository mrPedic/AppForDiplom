package com.example.roamly.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.roamly.entity.DTO.establishment.EstablishmentDisplayDto
import com.example.roamly.entity.classes.EstablishmentStatus
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.ui.theme.AppTheme

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
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.MainContainer)
    ) { padding ->
        Column(
            modifier = Modifier
                .background(AppTheme.colors.MainContainer)
                .padding(padding)
                .padding(5.dp)
                .fillMaxSize()
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppTheme.colors.MainSuccess,
                    trackColor = AppTheme.colors.SecondaryContainer
                )
            } else if (pendingList.isEmpty()) {
                Text(
                    "Нет ожидающих заявок.",
                    modifier = Modifier.padding(top = 16.dp),
                    color = AppTheme.colors.SecondaryText
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pendingList, key = { it.id }) { establishment ->
                        PendingItemCard(
                            establishment = establishment,
                            onApprove = { viewModel.updateEstablishmentStatus(it.id, EstablishmentStatus.ACTIVE) },
                            onReject = { viewModel.updateEstablishmentStatus(it.id, EstablishmentStatus.REJECTED) }
                        )
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.SecondaryContainer
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainBorder)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = establishment.name,
                style = MaterialTheme.typography.titleMedium,
                color = AppTheme.colors.MainText
            )
            Text(
                text = establishment.address,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.SecondaryText
            )
            Text(
                text = "Тип: ${establishment.type}",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.SecondaryText
            )
            Text(
                text = "Создал: ID ${establishment.createdUserId}",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.SecondaryText
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onReject(establishment) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.MainFailure,
                        contentColor = AppTheme.colors.MainText
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отклонить")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onApprove(establishment) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.MainSuccess,
                        contentColor = AppTheme.colors.MainText
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Одобрить")
                }
            }
        }
    }
}