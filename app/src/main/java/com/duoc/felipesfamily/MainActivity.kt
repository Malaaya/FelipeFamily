package com.duoc.felipesfamily

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.duoc.felipesfamily.ui.AppNavHost
import com.duoc.felipesfamily.ui.theme.FelipesFamilyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FelipesFamilyTheme {
                AppNavHost()
            }
        }
    }
}
