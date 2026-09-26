package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.service.NexaNotificationHelper
import com.example.ui.screens.NexaPurgeApp
import com.example.ui.theme.NexaPurgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup Android Oreo notification channels immediately
        NexaNotificationHelper.createNotificationChannels(this)
        
        enableEdgeToEdge()
        
        setContent {
            val sharedPrefs = remember { getSharedPreferences("nexapurge_prefs", Context.MODE_PRIVATE) }
            val darkThemeState = remember { mutableStateOf(sharedPrefs.getBoolean("dark_mode", true)) }

            // Persist dark theme state when manual override switch toggles
            LaunchedEffect(darkThemeState.value) {
                sharedPrefs.edit().putBoolean("dark_mode", darkThemeState.value).apply()
            }

            NexaPurgeTheme(darkTheme = darkThemeState.value) {
                NexaPurgeApp(darkThemeState = darkThemeState)
            }
        }
    }
}
