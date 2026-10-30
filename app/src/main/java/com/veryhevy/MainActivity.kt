package com.veryhevy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.veryhevy.ui.AppRoot
import com.veryhevy.ui.theme.VeryHevyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeryHevyTheme {
                AppRoot()
            }
        }
    }
}
