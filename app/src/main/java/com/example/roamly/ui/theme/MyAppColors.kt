package com.example.roamly.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color


@Immutable
data class MyAppColors(
    // Твои переменные (Светлые тона)
    val lightHart: Color,    // Самый светлый фон (Background)
    val lightMedium: Color,  // Светлый акцент/Карточка (Camel/Dawn)
    val lightLow: Color,     // Средний светлый/Нейтральный акцент

    // Добавленные переменные (для текста, темного акцента и семантики)
    val darkMain: Color,     // Основной темный акцент (Blue/Violet)
    val darkContrast: Color, // Самый темный/Контрастный (Navy/Midnight)
    val darkChoc: Color,     // Темный фон/Кнопки (Choc/Twilight)
    val success: Color,      // Цвет успеха (зеленый/синий)
    val failure: Color       // Цвет ошибки (красный)
)

val LocalAppColors = staticCompositionLocalOf {
    // Дефолтные значения (не будут использоваться, но нужны для инициализации)
    MyAppColors(
        lightHart = Color.Unspecified,
        lightMedium = Color.Unspecified,
        lightLow = Color.Unspecified,
        darkMain = Color.Unspecified,
        darkContrast = Color.Unspecified,
        darkChoc = Color.Unspecified,
        success = Color.Unspecified,
        failure = Color.Unspecified
    )
}