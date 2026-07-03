package com.smokingtracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smokingtracker.data.DataStoreManager
import com.smokingtracker.data.ThemePreference
import com.smokingtracker.ui.MainApp
import com.smokingtracker.ui.theme.AppTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val dataStoreManager = DataStoreManager(applicationContext)
        val themePreference = runBlocking { dataStoreManager.appTheme.first() }
        
        val isSystemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val isDark = when (themePreference) {
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
            ThemePreference.SYSTEM -> isSystemDark
        }
        
        if (isDark) {
            setTheme(R.style.Theme_App_Starting_Dark)
        } else {
            setTheme(R.style.Theme_App_Starting_Light)
        }
        
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return MainViewModel(dataStoreManager, applicationContext) as T
                    }
                }
            )

            val themePreference by viewModel.themePreference.collectAsState()
            val fontPreset by viewModel.fontPreset.collectAsState()
            val amoledTheme by viewModel.amoledTheme.collectAsState()
            val useDarkTheme = when (themePreference) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }

            AppTheme(useDarkTheme = useDarkTheme, fontPreset = fontPreset, amoledThemeEnabled = amoledTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(viewModel = viewModel)
                }
            }
        }
    }
}
