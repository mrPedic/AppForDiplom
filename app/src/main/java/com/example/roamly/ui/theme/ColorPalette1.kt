// ColorPalette1.kt
package com.example.roamly.ui.theme

import androidx.compose.ui.graphics.Color

// Исходные цвета Earth
val PaletteBlue = Color(0xFF7CA7EB)
val PaletteChoc = Color(0xFF402924)
val PaletteCamel = Color(0xFFCBE290)
val PaletteNavy = Color(0xFF000B26)
val MySuccessColor = PaletteBlue
val MyErrorColor = Color(0xFFB94A3E)

// Дополнительные цвета для новой схемы
val MySecondarySuccessColor = PaletteCamel // Светлый акцент для вторичного успеха
val MySecondaryErrorColor = Color(0xFFE57373) // Светлый красный для вторичной ошибки
val SelectedColor = PaletteBlue // Цвет выбранного элемента
val UnSelectedColor = Color(0xFF808080) // Серый для невыбранного элемента
val MainBorderColor = PaletteBlue // Основной бордер
val SecondaryBorderColor = PaletteChoc // Вторичный бордер

// Цвета для темной палитры Earth
val EarthDarkMainText = Color.White
val EarthDarkSecondaryText = Color(0xFF8E6864)
val EarthDarkMainContainer = Color(0xFF121212)
val EarthDarkSecondaryContainer = Color(0xFF2C2C2E)
val EarthDarkMainBorder = Color(0xFF90C0F0)
val EarthDarkSecondaryBorder = Color(0xFF3A3A3C)
val EarthDarkMainSuccess = Color(0xFF8CD87D)
val EarthDarkSecondarySuccess = Color(0xFF90C0F0)
val EarthDarkMainFailure = Color(0xFFE57373)
val EarthDarkSecondaryFailure = Color(0xFFB94A3E)
val EarthDarkSelectedItem = Color(0xFF90C0F0)
val EarthDarkUnSelectedItem = Color(0xFF808080)

// Темная палитра Earth (ThemePalette1)
val EarthAppColors = MyAppColors(
    MainText = EarthDarkMainText,              // Основной текст (светлый в темной теме)
    SecondaryText = EarthDarkSecondaryText,    // Вторичный текст
    MainContainer = EarthDarkMainContainer,    // Основной контейнер (темный фон)
    SecondaryContainer = EarthDarkSecondaryContainer, // Вторичный контейнер
    MainBorder = EarthDarkMainBorder,          // Основной бордер
    SecondaryBorder = EarthDarkSecondaryBorder,// Вторичный бордер
    MainSuccess = EarthDarkMainSuccess,        // Основной успех
    SecondarySuccess = EarthDarkSecondarySuccess,// Вторичный успех
    MainFailure = EarthDarkMainFailure,        // Основная ошибка
    SecondaryFailure = EarthDarkSecondaryFailure,// Вторичная ошибка
    SelectedItem = EarthDarkSelectedItem,      // Выбранный элемент
    UnSelectedItem = EarthDarkUnSelectedItem   // Невыбранный элемент
)