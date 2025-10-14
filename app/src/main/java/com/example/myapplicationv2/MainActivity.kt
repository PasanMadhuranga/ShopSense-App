package com.example.myapplicationv2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplicationv2.presentation.home.HomeScreen
import com.example.myapplicationv2.presentation.theme.MyApplicationV2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationV2Theme(dynamicColor = false) {
                HomeScreen()
            }
        }
    }
}