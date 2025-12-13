package com.example.roamly.ui.theme

import androidx.compose.ui.graphics.Color

// Новая палитра Enchanted
val EnchantedLight3_1 = Color(0xFFFDF7FF) // lightHart
val EnchantedLight2_1 = Color(0xFFF9CBDF) // lightMedium
val EnchantedLight1_1 = Color(0xFFB89AC9) // lightLow
val EnchantedMedium2_1 = Color(0xFF8F7AB8)
val EnchantedMedium1_1 = Color(0xFF794C9E) // darkMain
val EnchantedDark2_1 = Color(0xFF501F5B)   // darkChoc
val EnchantedDark1_1 = Color(0xFF29264C)   // darkContrast
val EnchantedError = Color(0xFF800020)

// Палитра Enchanted (ThemePalette2)
val LightEnchantedAppColors = MyAppColors(
    lightHart = EnchantedLight3_1,    // Самый светлый фон
    lightMedium = EnchantedLight2_1,  // Светлый акцент/Карточка
    lightLow = EnchantedLight1_1,     // Средний светлый/Нейтральный акцент

    darkMain = EnchantedMedium1_1,    // Основной темный акцент (Violet Spell)
    darkContrast = EnchantedDark1_1,  // Самый темный текст/фон (Midnight Whisper)
    darkChoc = EnchantedDark2_1,      // Темный фон/Кнопки (Twilight Gaze)
    success = EnchantedMedium2_1,
    failure = EnchantedError
)

// Функция-помощник для получения цветов по Enum
fun getColorsByConfig(config: AppThemeConfig, isDark: Boolean): MyAppColors {
    // ВАЖНО: Игнорируем isDark, чтобы тема не менялась при смене системной темы.
    return when (config) {
        AppThemeConfig.ThemePalette1 -> LightAppColors
        AppThemeConfig.ThemePalette2 -> LightEnchantedAppColors
    }
}