package com.simontester.flipfit

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.simontester.flipfit.ui.FlipFitApp
import com.simontester.flipfit.ui.theme.FlipFitTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        setContent { FlipFitTheme { BoxWithConstraints { FlipFitApp(vm, maxWidth <= 480.dp && maxHeight <= 600.dp) } } }
    }
    fun applyKeepAwake(enabled: Boolean) { if(enabled) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
}
