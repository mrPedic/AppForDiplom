package com.example.roamly.manager

import android.content.Context
import com.example.roamly.entity.DTO.establishment.EstablishmentSearchResultDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.lang.reflect.Type

private const val PREFS_NAME = "search_history_prefs"
private const val HISTORY_KEY = "recent_establishments"

@Singleton
class SearchHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // Тип для десериализации списка
    private val type: Type = object : TypeToken<List<EstablishmentSearchResultDto>>() {}.type

    /**
     * Загружает историю поиска из SharedPreferences.
     */
    fun loadHistory(): List<EstablishmentSearchResultDto> {
        val json = prefs.getString(HISTORY_KEY, null)
        return if (json == null) {
            emptyList()
        } else {
            try {
                // Если JSON не пуст, десериализуем
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                // Обработка ошибки десериализации (например, если формат данных изменился)
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Сохраняет текущий список истории поиска в SharedPreferences.
     */
    fun saveHistory(history: List<EstablishmentSearchResultDto>) {
        val json = gson.toJson(history)
        prefs.edit().putString(HISTORY_KEY, json).apply()
    }
}