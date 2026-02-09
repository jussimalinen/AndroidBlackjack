package com.jmalinen.blackjack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.jmalinen.blackjack.ui.navigation.BlackjackNavGraph
import com.jmalinen.blackjack.ui.theme.BlackjackTheme
import com.jmalinen.blackjack.ui.theme.FeltGreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlackjackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FeltGreen
                ) {
                    BlackjackNavGraph()
                }
            }
        }
    }
}
