package com.elara.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.min

object AccentTokens {

    fun generateAccent(gradientColors: List<Color>): Color {
        val source = gradientColors.firstOrNull() ?: Color(0xFFED5564)
        val (h, s, v) = hsvComponents(source)
        return when {
            v > 0.8f -> copyHsv(source, h, s, v * 0.7f)
            v < 0.15f -> copyHsv(source, h, (s * 0.6f).coerceAtMost(1f), min(v + 0.4f, 0.55f))
            s > 0.85f -> copyHsv(source, h, s * 0.8f, v)
            else -> source
        }
    }

    private fun hsvComponents(color: Color): Triple<Float, Float, Float> {
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        return Triple(hsv[0], hsv[1], hsv[2])
    }

    private fun copyHsv(source: Color, h: Float, s: Float, v: Float): Color {
        val newArgb = android.graphics.Color.HSVToColor(
            (source.alpha * 255).toInt(),
            floatArrayOf(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)),
        )
        return Color(newArgb)
    }
}
