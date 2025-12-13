package com.example.roamly.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.roamly.ui.theme.AppThemeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Создаем расширение для контекста, чтобы получить доступ к DataStore
private val Context.dataStore by preferencesDataStore("app_preferences")

class ColorSettings(private val context: Context) {

    // Ключ, по которому будем сохранять имя темы
    private val THEME_KEY = stringPreferencesKey("theme_config")

    // Чтение: возвращаем Flow (поток данных), который сам обновится при изменении
    val themeFlow: Flow<AppThemeConfig> = context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: AppThemeConfig.ThemePalette1.name
        try {
            AppThemeConfig.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppThemeConfig.ThemePalette1 // Если что-то пошло не так, вернем дефолт
        }
    }

    // Запись: сохраняем новую тему
    suspend fun saveTheme(theme: AppThemeConfig) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
}