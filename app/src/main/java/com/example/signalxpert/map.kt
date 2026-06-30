package com.example.signalxpert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.signalxpert.ui.theme.SignalXpertTheme

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignalXpertTheme {
                MapScreen()
            }
        }
    }
}

@Composable
fun MapScreen() {
    Text(text = "Map Screen")
}


