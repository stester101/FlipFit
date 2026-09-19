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
val Glass = Color(0xB8181C1C)
val GlassSoft = Color(0x801C2221)
val GlassDeep = Color(0xF0191D1D)
val GlassEdge = Color(0x52FFFFFF)
val GlassLimeEdge = Color(0x66B7FF39)
val OutlineSoft = Color(0xFF343A39)
val Muted = Color(0xFF969C9C)
val MutedLow = Color(0xFF676D6D)
val Danger = Color(0xFFFF7474)

val LimeGlassBrush = Brush.linearGradient(
    listOf(Color(0x331A2610), Color(0x111A1E17), Color(0x22151A12))
)
val LiquidGlassBrush = Brush.linearGradient(
    listOf(Color(0xD9282E2D), Color(0xA8141818), Color(0xC91C2221))
)
val LiquidLimeBrush = Brush.linearGradient(
    listOf(Color(0xCC1C2814), Color(0xA8141A17), Color(0xC71B2119))
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
