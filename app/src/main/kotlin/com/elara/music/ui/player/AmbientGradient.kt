package com.elara.music.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AmbientGradient(
    ambientColors: AmbientColors?,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = ambientColors,
        animationSpec = tween(800),
        label = "ambientCrossfade",
    ) { colors ->
        if (colors != null) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.darkMuted.copy(alpha = 0.4f),
                                Color.Transparent,
                                colors.vibrant.copy(alpha = 0.15f),
                                colors.muted.copy(alpha = 0.1f),
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
    }
}

fun emptyAmbientColors(): AmbientColors? = null
