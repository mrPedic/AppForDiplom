package com.example.roamly.ui.theme

import androidx.compose.runtime.Composable

// 5. Синтаксический сахар: чтобы писать AppTheme.colors.key
// Должен быть в ThemePalette1.kt (или переименуй его в Theme.kt)
object AppTheme {
    val colors: MyAppColors
        @Composable
        get() = LocalAppColors.current
}