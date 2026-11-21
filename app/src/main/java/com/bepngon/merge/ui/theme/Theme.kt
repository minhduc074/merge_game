package com.bepngon.merge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CreamBg = Color(0xFFFFF3E0)
val CreamBgDeep = Color(0xFFFFE0B2)
val CellWhite = Color(0xFFFFFBF5)
val CellBorder = Color(0xFFE8D5B7)
val Cocoa = Color(0xFF6D4C41)
val Gold = Color(0xFFFFB300)
val MergeGreen = Color(0xFF66BB6A)
val WarnRed = Color(0xFFEF5350)

private val CuteColors = lightColorScheme(
    primary = Color(0xFFFF7043),
    onPrimary = Color.White,
    secondary = Color(0xFFFFB300),
    background = CreamBg,
    surface = Color(0xFFFFFBF5),
    onBackground = Cocoa,
    onSurface = Cocoa,
)

@Composable
fun BepNgonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CuteColors,
        content = content,
    )
}
