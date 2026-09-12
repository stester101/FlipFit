package com.simontester.flipfit

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.dp
import com.simontester.flipfit.ui.FlipFitV05App
import com.simontester.flipfit.ui.theme.FlipFitTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlipFitTheme {
                BoxWithConstraints {
                    val compact = maxWidth <= 480.dp && maxHeight <= 600.dp
                    val wide = maxWidth >= 700.dp
                    FlipFitV05App(vm = vm, compact = compact, wide = wide)
                }
            }
        }
    }

    fun applyKeepAwake(enabled: Boolean) {
        if (enabled) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
