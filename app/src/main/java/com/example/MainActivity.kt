package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.data.repository.AppContainer
import com.example.ui.navigation.TrueCheckNavHost
import com.example.ui.theme.TrueCheckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = remember { AppContainer(applicationContext) }
            LaunchedEffect(Unit) {
                container.initializeSeedData()
            }
            TrueCheckTheme {
                TrueCheckNavHost(container = container)
            }
        }
    }
}
