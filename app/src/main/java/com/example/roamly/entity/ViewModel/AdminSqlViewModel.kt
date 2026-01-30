package com.example.roamly.entity.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.entity.DTO.AdminQueryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminSqlViewModel @Inject constructor(
    val apiService: ApiService
) : ViewModel() {

    // Список всех запросов
    private val _savedQueries = MutableStateFlow<List<AdminQueryDto>>(emptyList())
    val savedQueries = _savedQueries.asStateFlow()

    // Результат выполнения SQL
    private val _queryResult = MutableStateFlow<List<Map<String, Any?>>?>(null)
    val queryResult = _queryResult.asStateFlow()

    // Текущий открытый запрос
    private val _currentQuery = MutableStateFlow<AdminQueryDto?>(null)
    val currentQuery = _currentQuery.asStateFlow()

    // Индикатор загрузки (для выполнения SQL)
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Загрузить список запросов с сервера
    fun fetchQueries() {
        viewModelScope.launch {
            try {
                val list = apiService.getAdminQueries()
                _savedQueries.value = list
            } catch (e: Exception) {
                Log.e("AdminSqlViewModel", "Error fetching queries", e)
            }
        }
    }

    // Подготовка данных для DetailScreen
    fun loadQuery(id: Long) {
        _queryResult.value = null // Сброс результатов прошлого выполнения

        if (id == 0L) {
            // Режим создания нового запроса
            _currentQuery.value = AdminQueryDto(0, "Новый запрос", "", "SELECT * FROM users LIMIT 10")
        } else {
            // Режим редактирования существующего
            _currentQuery.value = null // Сбрасываем, чтобы показать лоадер

            viewModelScope.launch {
                // Если список пуст, инициируем загрузку
                if (_savedQueries.value.isEmpty()) {
                    fetchQueries()
                }

                // Ждем, пока в списке появятся данные, и ищем нужный ID.
                // .collect будет срабатывать каждый раз при обновлении savedQueries
                savedQueries.collect { queries ->
                    if (queries.isNotEmpty()) {
                        val found = queries.find { it.id == id }
                        if (found != null) {
                            _currentQuery.value = found
                        } else {
                            // Если данные пришли, но ID не найден (например, удален)
                            // Можно обработать ошибку или оставить null
                            Log.w("AdminSqlViewModel", "Query with id $id not found in list")
                        }
                    }
                }
            }
        }
    }

    // Выполнить SQL
    fun executeSql(sql: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = apiService.executeRawSql(sql)
                _queryResult.value = result
            } catch (e: Exception) {
                _queryResult.value = listOf(mapOf("ERROR" to (e.message ?: "Unknown error")))
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Сохранить изменения
    fun saveQuery(updatedQuery: AdminQueryDto) {
        viewModelScope.launch {
            try {
                val saved = apiService.saveAdminQuery(updatedQuery)
                // Обновляем текущий объект (особенно важно для получения ID после создания)
                _currentQuery.value = saved
                fetchQueries() // Обновляем общий список
            } catch (e: Exception) {
                Log.e("saveQuery", e.toString())
            }
        }
    }

    // Удалить
    fun deleteQuery(id: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteAdminQuery(id)
                if (response.isSuccessful) {
                    fetchQueries()
                }
            } catch (e: Exception) {
                Log.e("deleteQuery", e.toString())
            }
        }
    }
}