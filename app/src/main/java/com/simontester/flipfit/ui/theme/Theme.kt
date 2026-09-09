package com.simontester.flipfit.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFB6FF3B)
val Bg = Color(0xFF090A0C)
val Surface = Color(0xFF15171B)
val Muted = Color(0xFF9AA0A8)

@Composable fun FlipFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary=Accent, background=Bg, surface=Surface, onPrimary=Color.Black, onBackground=Color.White, onSurface=Color.White), content=content)
}
