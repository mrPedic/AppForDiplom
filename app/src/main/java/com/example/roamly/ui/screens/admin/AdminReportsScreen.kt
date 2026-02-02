package com.example.roamly.ui.screens.admin

import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.roamly.entity.DTO.ReviewReportDto
import com.example.roamly.entity.ViewModel.AdminReportsViewModel
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AdminReportsScreen(
    navController: NavController
) {
    val viewModel: AdminReportsViewModel = hiltViewModel()
    val reports by viewModel.reports.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val colors = AppTheme.colors

    LaunchedEffect(Unit) {
        viewModel.fetchReports()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Жалобы на отзывы", color = colors.MainText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Назад",
                            tint = colors.MainText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.MainContainer,
                    titleContentColor = colors.MainText
                )
            )
        },
        containerColor = colors.MainContainer
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.MainSuccess)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (reports.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Жалоб нет. Все чисто!", color = colors.SecondaryText)
                        }
                    }
                }

                items(reports) { report ->
                    ReportCard(report, viewModel, navController)
                }

                // Отступ снизу
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// AdminReportsScreen.kt (только изменения в ReportCard)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReportCard(
    report: ReviewReportDto,
    viewModel: AdminReportsViewModel,
    navController: NavController
) {
    val colors = AppTheme.colors
    val context = LocalContext.current // Для AsyncImage

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, colors.MainFailure.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // --- Шапка: Причина жалобы ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.MainFailure.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = colors.MainFailure,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = report.reason,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.MainText
                    )
                    Text(
                        text = "ID жалобы: ${report.id} • ID отзыва: ${report.reviewId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.SecondaryText
                    )
                }
            }

            // Комментарий репортера
            if (!report.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Комментарий: ${report.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.MainText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.MainContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Карточка самого отзыва ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.MainContainer),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.SecondaryBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Отзыв",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.SecondaryText
                        )
                        // Рейтинг
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${report.reviewRating ?: "?"}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.MainText
                            )
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = colors.MainSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = report.reviewText ?: "Загрузка текста отзыва...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (report.reviewText != null) colors.MainText else colors.SecondaryText,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Добавляем отображение фото отзыва (base64)
                    if (!report.reviewPhoto.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val imageBytes = remember { Base64.decode(report.reviewPhoto, Base64.DEFAULT) }
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageBytes)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Фото отзыва",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = colors.SecondaryText, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = report.reviewAuthorName ?: "User #${report.userId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.SecondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = colors.SecondaryBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // --- Кнопки действий ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Перейти к заведению
                OutlinedButton(
                    onClick = {
                        navController.navigate(EstablishmentScreens.EstablishmentDetail.createRoute(report.establishmentId))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.MainText),
                    border = BorderStroke(1.dp, colors.SecondaryBorder)
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Открыть заведение" + if(report.establishmentName != null) " (${report.establishmentName})" else "")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Оставить отзыв (Удалить жалобу)
                    Button(
                        onClick = { viewModel.deleteReport(report.id ?: 0L) },
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.SecondarySuccess.copy(alpha = 0.2f),
                            contentColor = colors.MainSuccess
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Оставить") // Оставить отзыв (игнорировать жалобу)
                    }

                    // Удалить отзыв (Решить проблему)
                    Button(
                        onClick = { viewModel.resolveReport(report.id ?: 0L) },
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.MainFailure,
                            contentColor = colors.MainContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Удалить") // Удалить отзыв
                    }
                }
            }
        }
    }
}