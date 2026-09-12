package com.simontester.flipfit.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

val Accent = Color(0xFFB7FF39)
val Bg = Color(0xFF080909)
val Surface = Color(0xFF141616)
val SurfaceHigh = Color(0xFF1A1D1D)
val Glass = Color(0xCC151818)
val GlassSoft = Color(0x99191C1C)
val OutlineSoft = Color(0xFF303434)
val Muted = Color(0xFF969C9C)
val MutedLow = Color(0xFF676D6D)
val Danger = Color(0xFFFF7474)

val LimeGlassBrush = Brush.linearGradient(
    listOf(Color(0x331A2610), Color(0x111A1E17), Color(0x22151A12))
)

@Composable fun FlipFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Bg,
            surface = Surface,
            surfaceVariant = SurfaceHigh,
            outline = OutlineSoft,
            onPrimary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Muted
        ),
        shapes = Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        ),
        content = content
    )
}
