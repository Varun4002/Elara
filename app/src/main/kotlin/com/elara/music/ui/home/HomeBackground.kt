package com.elara.music.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.elara.music.ui.animation.ElaraSpring

@Composable
fun HomeBackground(
    gradientColors: List<Color>,
    ambientIntensity: Float,
    modifier: Modifier = Modifier,
) {
    val intensity by animateFloatAsState(
        targetValue = ambientIntensity.coerceIn(0f, 1f),
        animationSpec = ElaraSpring.smooth,
        label = "ambientIntensity",
    )

    val displayColors = if (gradientColors.size >= 2) {
        gradientColors.take(2).map { it.copy(alpha = it.alpha * intensity * 0.3f) }
    } else {
        listOf(
            Color(0xFF1A1A2E).copy(alpha = 0.15f * intensity),
            Color(0xFF0D0D0D).copy(alpha = 0.1f * intensity),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = displayColors.ifEmpty {
                        listOf(
                            Color(0xFF1A1A2E).copy(alpha = 0.15f * intensity),
                            Color(0xFF0D0D0D).copy(alpha = 0.1f * intensity),
                        )
                    },
                ),
            ),
        contentAlignment = Alignment.Center,
    )
}
