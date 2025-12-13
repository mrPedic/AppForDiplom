package com.example.roamly.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.settings.ColorSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ColorViewModel(private val colorSettings: ColorSettings) : ViewModel() {

    // Превращаем Flow из DataStore в State для Compose
    // initialValue = Earth, пока грузится реальное значение
    val currentTheme = colorSettings.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppThemeConfig.ThemePalette1
        )

    fun switchTheme(newTheme: AppThemeConfig) {
        viewModelScope.launch {
            colorSettings.saveTheme(newTheme)
        }
    }
}