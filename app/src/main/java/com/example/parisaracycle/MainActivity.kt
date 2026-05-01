package com.example.parisaracycle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.parisaracycle.data.AppContainer
import com.example.parisaracycle.ui.ParisaraCycleApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = AppContainer(applicationContext)
        setContent {
            ParisaraCycleApp(appContainer)
        }
    }
}
