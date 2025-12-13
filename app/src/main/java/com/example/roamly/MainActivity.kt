package com.example.roamly

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.roamly.settings.ColorSettings
import com.example.roamly.ui.screens.MainScreen
import com.example.roamly.ui.theme.ColorViewModel
import com.example.roamly.ui.theme.LocalAppColors
import com.example.roamly.ui.theme.getColorsByConfig
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class
MainActivity : ComponentActivity() {

    lateinit var navController: NavHostController

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current

            // Создание ViewModel через Factory (для передачи UserSettings)
            // viewModel() теперь доступен благодаря импорту
            val viewModel: ColorViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ColorViewModel(ColorSettings(context)) as T
                    }
                }
            )

            val currentThemeConfig by viewModel.currentTheme.collectAsState()

            // Игнорируем системный Dark Mode (передаем false)
            val targetColors = getColorsByConfig(currentThemeConfig, false)

            CompositionLocalProvider(
                LocalAppColors provides targetColors
            ) {
                MaterialTheme {
                    MainScreen(
                        currentTheme = currentThemeConfig,
                        onThemeChange = { newTheme -> viewModel.switchTheme(newTheme) }
                    )
                }
            }
        }
    }
}