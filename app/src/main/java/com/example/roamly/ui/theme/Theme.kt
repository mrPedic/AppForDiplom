package com.example.roamly.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =========================================================
// ⭐ 1. ОПРЕДЕЛЕНИЕ ЦВЕТОВ ИЗ ПАЛИТРЫ (#9A8FA6, #E4D5F2, #D7C4F2, #EDE9F2, #0D0D0D)
// =========================================================

// Основные оттенки из палитры
private val PalettePrimary = Color(0xFFE4D5F2) // Светлый лиловый, используем как Primary
private val PalettePrimaryContainer = Color(0xFFD7C4F2) // Более насыщенный лиловый для контейнеров
private val PaletteSecondary = Color(0xFF9A8FA6) // Серый лиловый
private val PaletteBackground = Color(0xFFEDE9F2) // Почти белый для фона
private val PaletteBlack = Color(0xFF0D0D0D) // Почти черный для текста


// =========================================================
// ⭐ 2. СХЕМЫ ЦВЕТОВ (Light & Dark)
// =========================================================

private val DarkColorScheme = darkColorScheme(
    primary = PalettePrimaryContainer, // Светлее, чем основной, для контраста на темном
    onPrimary = PaletteBlack,

    secondary = PaletteSecondary,
    onSecondary = PaletteBlack,

    background = PaletteBlack, // Темный фон
    onBackground = PaletteBackground, // Светлый текст

    surface = PaletteBlack, // Темная поверхность
    onSurface = PaletteBackground, // Светлый текст

    // Используем более насыщенные цвета для контейнеров
    primaryContainer = PaletteSecondary,
    onPrimaryContainer = PaletteBlack,
    secondaryContainer = PaletteSecondary,
    onSecondaryContainer = PaletteBlack,

    // Остальные цвета по умолчанию
)

private val LightColorScheme = lightColorScheme(
    primary = PalettePrimary, // Светлый лиловый - основной акцент
    onPrimary = PaletteBlack, // Черный текст на акценте

    secondary = PaletteSecondary, // Серый лиловый
    onSecondary = PaletteBlack, // Черный текст на вторичном

    background = PaletteBackground, // Почти белый фон
    onBackground = PaletteBlack, // Черный текст на фоне

    surface = PaletteBackground, // Поверхность (карточки)
    onSurface = PaletteBlack, // Черный текст на поверхности

    // Контейнеры
    primaryContainer = PalettePrimaryContainer,
    onPrimaryContainer = PaletteBlack,
    secondaryContainer = PaletteSecondary,
    onSecondaryContainer = PaletteBlack,

    // Дополнительные цвета для Material 3
    surfaceContainerHigh = Color(0xFFF7F4F9), // Чуть темнее поверхности для виджетов
    outline = PaletteSecondary.copy(alpha = 0.5f) // Серый лиловый для границ
)


@Composable
fun RoamlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Мы отключаем Dynamic Color, так как нам нужна строго заданная палитра
        // dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        //    val context = LocalContext.current
        //    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        // }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // ⭐ Установка цвета статус-бара для Light Theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.hashCode()
            // Установка светлого содержимого статус-бара, если фон темный, и наоборот
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Предполагается, что Typography определен
        content = content
    )
}