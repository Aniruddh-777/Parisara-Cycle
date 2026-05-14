package com.example.parisaracycle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.parisaracycle.data.AppContainer
import com.example.parisaracycle.ui.ParisaraCycleApp
import com.example.parisaracycle.ui.theme.ParisaraCycleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appContainer by produceState<AppContainer?>(initialValue = null, applicationContext) {
                value = withContext(Dispatchers.IO) {
                    AppContainer(applicationContext)
                }
            }

            appContainer?.let { container ->
                ParisaraCycleApp(container)
            } ?: AppLoadingContent()
        }
    }
}

@Composable
private fun AppLoadingContent() {
    ParisaraCycleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
