package com.example.roamly.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class MyAppColors(
    val MainText: Color,
    val SecondaryText: Color,
    val MainContainer: Color,
    val SecondaryContainer: Color,
    val MainBorder: Color,
    val SecondaryBorder: Color,
    val MainSuccess: Color,
    val SecondarySuccess: Color,
    val MainFailure: Color,
    val SecondaryFailure: Color,
    val SelectedItem: Color,
    val UnSelectedItem: Color
)

val LocalAppColors = staticCompositionLocalOf {
    // Дефолтные значения (не будут использоваться, но нужны для инициализации)
    MyAppColors(
        MainText = Color.Unspecified,
        SecondaryText = Color.Unspecified,
        MainContainer = Color.Unspecified,
        SecondaryContainer = Color.Unspecified,
        MainBorder = Color.Unspecified,
        SecondaryBorder = Color.Unspecified,
        MainSuccess = Color.Unspecified,
        SecondarySuccess = Color.Unspecified,
        MainFailure = Color.Unspecified,
        SecondaryFailure = Color.Unspecified,
        SelectedItem = Color.Unspecified,
        UnSelectedItem = Color.Unspecified
    )
}