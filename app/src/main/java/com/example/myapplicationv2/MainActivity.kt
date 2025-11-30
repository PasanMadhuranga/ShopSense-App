package com.example.myapplicationv2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.myapplicationv2.presentation.home.HomeScreen
import com.example.myapplicationv2.presentation.theme.MyApplicationV2Theme
import com.example.myapplicationv2.shopping.ShoppingModeService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read extras from the launching intent
        val highlightIds: List<Int> =
            intent?.getIntegerArrayListExtra(
                ShoppingModeService.EXTRA_HIGHLIGHT_ITEM_IDS
            ) ?: emptyList()

        val highlightFromNotification =
            intent?.getBooleanExtra(
                ShoppingModeService.EXTRA_HIGHLIGHT_FROM_NOTIFICATION,
                false
            ) ?: false

        setContent {
            MyApplicationV2Theme(dynamicColor = false) {
                HomeScreen(
                    initialHighlightIds = if (highlightFromNotification) {
                        highlightIds
                    } else {
                        emptyList()
                    }
                )
            }
        }
    }
}