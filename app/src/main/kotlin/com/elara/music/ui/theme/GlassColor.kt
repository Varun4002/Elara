package com.elara.music.ui.theme

import androidx.compose.ui.graphics.Color

data class GlassColors(
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val highlight: Color,
    val shadow: Color,
    val overlay: Color,
)

object GlassColorTokens {
    val light = GlassColors(
        surface = Color(0x19FFFFFF),
        surfaceElevated = Color(0x26FFFFFF),
        border = Color(0x33FFFFFF),
        highlight = Color(0x4DFFFFFF),
        shadow = Color(0x1A000000),
        overlay = Color(0x0A000000),
    )

    val dark = GlassColors(
        surface = Color(0x0DFFFFFF),
        surfaceElevated = Color(0x14FFFFFF),
        border = Color(0x1AFFFFFF),
        highlight = Color(0x33FFFFFF),
        shadow = Color(0x4D000000),
        overlay = Color(0x1A000000),
    )

    val amoled = GlassColors(
        surface = Color(0x0AFFFFFF),
        surfaceElevated = Color(0x12FFFFFF),
        border = Color(0x18FFFFFF),
        highlight = Color(0x28FFFFFF),
        shadow = Color(0x66000000),
        overlay = Color(0x26000000),
    )
}

enum class BlurRadius(val dp: Float) {
    LIGHT(12f),
    MEDIUM(24f),
    HEAVY(40f),
    EXTRA_HEAVY(60f),
}

enum class GlassAlpha(val value: Float) {
    SURFACE(0.05f),
    SURFACE_ELEVATED(0.08f),
    BORDER(0.10f),
    HIGHLIGHT(0.20f),
    SHADOW(0.30f),
}
