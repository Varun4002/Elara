package com.elara.music.ui.player

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade

enum class AmbientMode { Gradient, Blur, Solid }

enum class BlurStrength(val dp: Float) {
    Light(12f),
    Medium(24f),
    Heavy(40f),
}

@Composable
fun ElaraAmbientBackground(
    colors: AmbientColors? = null,
    backgroundImage: Any? = null,
    alpha: Float = 1f,
    mode: AmbientMode = AmbientMode.Gradient,
    blurStrength: BlurStrength = BlurStrength.Medium,
    glassOverlay: Boolean = true,
    crossfadeDurationMs: Int = 2000,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val context = LocalContext.current

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when (mode) {
            AmbientMode.Gradient -> {
                Crossfade(
                    targetState = colors,
                    animationSpec = tween(crossfadeDurationMs),
                    label = "ambientGradient",
                ) { c ->
                    if (c != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(alpha)
                                .drawBehind { drawGradientLayers(c) },
                        )
                    }
                }
            }

            AmbientMode.Blur -> {
                Box(modifier = Modifier.fillMaxSize().alpha(alpha)) {
                    if (backgroundImage != null) {
                        val isLowEnd = remember {
                            try {
                                val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                                val mi = android.app.ActivityManager.MemoryInfo()
                                am?.getMemoryInfo(mi)
                                mi?.totalMem?.let { it < 2L * 1024 * 1024 * 1024 } == true
                            } catch (_: Exception) { false }
                        }
                        val actualBlurDp = if (isLowEnd) BlurStrength.Light.dp else blurStrength.dp
                        val actualBlurPx = with(density) { actualBlurDp.dp.toPx() }

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(backgroundImage)
                                .size(200, 200)
                                .allowHardware(false)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        renderEffect = BlurEffect(
                                            actualBlurPx,
                                            actualBlurPx,
                                            TileMode.Decal,
                                        )
                                    }
                                    clip = true
                                },
                        )
                    }

                    val overlayColors = colors ?: AmbientColors()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            overlayColors.darkMuted.copy(alpha = 0.15f),
                                            Color.Transparent,
                                            overlayColors.darkMuted.copy(alpha = 0.35f),
                                        ),
                                    ),
                                )
                            },
                    )
                }
            }

            AmbientMode.Solid -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors?.dominant ?: Color.Black),
                )
            }
        }

        if (mode != AmbientMode.Solid) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind { drawVignette() },
            )
        }

        if (glassOverlay && mode != AmbientMode.Solid) {
            val glassBlurPx = with(density) { 24.dp.toPx() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            renderEffect = BlurEffect(
                                glassBlurPx,
                                glassBlurPx,
                                TileMode.Decal,
                            )
                        }
                        this.alpha = 0.04f * alpha
                        clip = true
                    },
            )
        }
    }
}

private fun DrawScope.drawGradientLayers(colors: AmbientColors) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                colors.vibrant.copy(alpha = 0.20f),
                colors.vibrant.copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.5f, size.height * 0.3f),
            radius = size.maxDimension * 0.8f,
        ),
    )

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                colors.muted.copy(alpha = 0.06f),
                colors.darkMuted.copy(alpha = 0.25f),
            ),
        ),
    )
}

private fun DrawScope.drawVignette() {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.18f),
            ),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * 0.7f,
        ),
    )
}
