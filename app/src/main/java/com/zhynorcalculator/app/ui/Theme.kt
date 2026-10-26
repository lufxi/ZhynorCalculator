package com.zhynorcalculator.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object CalcColors {
    val Background = Color(0xFF000000)
    val Divider = Color(0xFF2A2A2A)

    val NumberKey = Color(0xFF141414)
    val NumberText = Color(0xFFFFFFFF)

    val FunctionKey = Color(0xFF1C1C1C)
    val FunctionText = Color(0xFFB8B8B8)

    val ControlKey = Color(0xFF101A36)
    val ControlText = Color(0xFF6C8CFF)

    val PrimaryKey = Color(0xFF5B84FF)
    val PrimaryText = Color(0xFFFFFFFF)

    val ToggleKey = Color(0xFF2A3A70)
    val ToggleText = Color(0xFFA9BEFF)

    val Accent = Color(0xFF6C8CFF)
    val Muted = Color(0xFF8E8E93)
    val Error = Color(0xFFFF6B6B)
}

@Composable
fun CalcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CalcColors.Accent,
            background = CalcColors.Background,
            surface = CalcColors.Background,
        ),
        content = content,
    )
}
