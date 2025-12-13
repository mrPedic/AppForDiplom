package com.example.roamly.ui.theme

import androidx.compose.ui.graphics.Color

// Исходные цвета Earth
val PaletteBlue = Color(0xFF7CA7EB)
val PaletteChoc = Color(0xFF402924)
val PaletteCamel = Color(0xFFCBE290)
val PaletteNavy = Color(0xFF000B26)
val MySuccessColor = PaletteBlue
val MyErrorColor = Color(0xFFB94A3E)

// Дефолтная (СВЕТЛАЯ) палитра Earth (ThemePalette1)
val LightAppColors = MyAppColors(
    lightHart = Color.White,        // Самый светлый фон
    lightMedium = PaletteCamel,     // Светлый акцент/Карточка
    lightLow = PaletteBlue,         // Средний светлый/Акцент

    darkMain = PaletteBlue,         // Основной темный акцент
    darkContrast = PaletteNavy,     // Самый темный текст/фон
    darkChoc = PaletteChoc,         // Темный фон/Кнопки
    success = MySuccessColor,       // Цвет успеха
    failure = MyErrorColor          // Цвет ошибки
)

// Темная палитра Earth (пока не используется, но нужна для полноты)
val DarkAppColors = MyAppColors(
    lightHart = Color(0xFF121212),  // Темный фон
    lightMedium = Color(0xFF2C2C2E),
    lightLow = Color(0xFF3A3A3C),

    darkMain = Color(0xFF90C0F0),
    darkContrast = Color.White,
    darkChoc = Color(0xFF7D5954),
    success = Color(0xFF8CD87D),
    failure = Color(0xFFE57373)
)