package com.neo.glassplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

// Stage 1 stub. Stage 2 replaces this with the glass UI and navigation.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("Glass Player: stage 1") }
    }
}
