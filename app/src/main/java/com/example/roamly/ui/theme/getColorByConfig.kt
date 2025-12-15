package com.example.roamly.ui.theme

// Функция-помощник для получения цветов по Enum
fun getColorsByConfig(config: AppThemeConfig, isDark: Boolean): MyAppColors {
    // ВАЖНО: Игнорируем isDark, чтобы тема не менялась при смене системной темы.
    return when (config) {
        AppThemeConfig.ThemePalette1 -> EarthAppColors
        AppThemeConfig.ThemePalette2 -> EnchantedAppColors
    }
}