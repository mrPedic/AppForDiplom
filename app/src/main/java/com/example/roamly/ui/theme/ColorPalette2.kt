// ColorPalette2.kt
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

// Дополнительные цвета для новой схемы
val EnchantedSecondarySuccess = EnchantedLight1_1 // Светлый акцент для вторичного успеха
val EnchantedSecondaryFailure = Color(0xFFB94A3E) // Светлый красный для вторичной ошибки (заимствовано для consistency)
val EnchantedSelected = EnchantedMedium1_1 // Цвет выбранного элемента
val EnchantedUnSelected = Color(0xFF808080) // Серый для невыбранного элемента
val EnchantedMainBorder = EnchantedMedium2_1 // Основной бордер
val EnchantedSecondaryBorder = EnchantedDark2_1 // Вторичный бордер

// Светлая палитра Enchanted (ThemePalette2)
val EnchantedAppColors = MyAppColors(
    MainText = EnchantedDark1_1,          // Основной текст (темный контрастный)
    SecondaryText = EnchantedDark2_1,     // Вторичный текст (темный фон/кнопки)
    MainContainer = EnchantedLight3_1,    // Основной контейнер (самый светлый фон)
    SecondaryContainer = EnchantedLight2_1, // Вторичный контейнер (светлый акцент/карточка)
    MainBorder = EnchantedMainBorder,     // Основной бордер
    SecondaryBorder = EnchantedSecondaryBorder, // Вторичный бордер
    MainSuccess = EnchantedMedium2_1,     // Основной успех
    SecondarySuccess = EnchantedSecondarySuccess, // Вторичный успех
    MainFailure = EnchantedError,         // Основная ошибка
    SecondaryFailure = EnchantedSecondaryFailure, // Вторичная ошибка
    SelectedItem = EnchantedSelected, // Выбранный элемент
    UnSelectedItem = EnchantedUnSelected // Невыбранный элемент
)