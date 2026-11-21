package com.didi.chatarchive.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Pink {
    val Primary = Color(0xFFE91E63)
    val DeepRose = Color(0xFFC2185B)
    val Soft = Color(0xFFFCE4EC)
    val LightBg = Color(0xFFFFF5F8)
    val ChatBg = Color(0xFFFBE7EE)
    val Outgoing = Color(0xFFF8C5D6)
    val Incoming = Color(0xFFFFFFFF)
    val Ink = Color(0xFF2B1620)
    val Muted = Color(0xFF8E6C7A)
    val Chip = Color(0xFFFFF0F5)
}

@Composable
fun ChatArchiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Pink.Primary, onPrimary = Color.White,
            primaryContainer = Pink.Soft, onPrimaryContainer = Pink.DeepRose,
            secondary = Pink.DeepRose, background = Pink.LightBg, surface = Color.White,
            onSurface = Pink.Ink, onBackground = Pink.Ink
        ),
        content = content
    )
}
