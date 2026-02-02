package com.example.roamly.entity.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.entity.DTO.ReviewReportDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// AdminReportsViewModel.kt (только изменения)
@HiltViewModel
class AdminReportsViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _reports = MutableStateFlow<List<ReviewReportDto>>(emptyList())
    val reports = _reports.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun fetchReports() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Получаем "сырой" список жалоб (где есть ID, но нет текста)
                val rawReports = apiService.getReviewReports()

                // 2. Параллельно подгружаем детали для каждой жалобы
                val enrichedReports = coroutineScope {
                    rawReports.map { report ->
                        async {
                            enrichReportData(report)
                        }
                    }.awaitAll()
                }

                _reports.value = enrichedReports
            } catch (e: Exception) {
                Log.e("AdminReportsViewModel", "Error fetching reports", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Вспомогательная функция для загрузки деталей
    private suspend fun enrichReportData(report: ReviewReportDto): ReviewReportDto {
        return try {
            // Загружаем заведение, чтобы узнать его название
            val establishmentDeferred = coroutineScope {
                async {
                    try {
                        apiService.getEstablishmentById(report.establishmentId)
                    } catch (e: Exception) { null }
                }
            }

            // Загружаем отзывы заведения, чтобы найти нужный по ID
            // (так как нет метода getReviewById, ищем в списке отзывов заведения)
            val reviewsDeferred = coroutineScope {
                async {
                    try {
                        apiService.getReviewsByEstablishmentId(report.establishmentId)
                    } catch (e: Exception) { emptyList() }
                }
            }

            val establishment = establishmentDeferred.await()
            val reviews = reviewsDeferred.await()
            val targetReview = reviews.find { it.id == report.reviewId }

            // Возвращаем обновленный DTO с заполненными полями
            report.copy(
                establishmentName = establishment?.name,
                reviewText = targetReview?.reviewText,
                reviewRating = targetReview?.rating?.toDouble(),
                reviewAuthorName = "User #${targetReview?.createdUserId}", // Или загрузить юзера, если нужно имя
                reviewPhoto = targetReview?.photoBase64
            )
        } catch (e: Exception) {
            Log.e("AdminReportsViewModel", "Error enriching report ${report.id}", e)
            report // Возвращаем как есть в случае ошибки
        }
    }

    fun resolveReport(reportId: Long) {
        viewModelScope.launch {
            try {
                // Логика: "Удалить отзыв" (Resolve Report с удалением)
                apiService.resolveReviewReport(reportId)
                fetchReports() // Обновляем список после удаления
            } catch (e: Exception) {
                Log.e("AdminReportsViewModel", "Error resolving report", e)
            }
        }
    }

    fun deleteReport(reportId: Long) {
        viewModelScope.launch {
            try {
                // Логика: "Оставить отзыв" (Delete Report - удалить саму жалобу)
                apiService.deleteReviewReport(reportId)
                fetchReports() // Обновляем список после удаления
            } catch (e: Exception) {
                Log.e("AdminReportsViewModel", "Error deleting report", e)
            }
        }
    }
}