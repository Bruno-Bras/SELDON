package com.seldon.fleet

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.seldon.fleet.presentation.navigation.SeldonNavHost
import com.seldon.fleet.ui.theme.SeldonTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            SeldonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.seldon.fleet.ui.theme.SeldonColors.Void,
                ) {
                    SeldonNavHost()
                }
            }
        }
    }
}
