package com.elara.music.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassSlider(
    progress: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
    trackHeight: Dp = 4.dp,
    thumbRadius: Dp = 6.dp,
    thumbExpandedRadius: Dp = 12.dp,
    accentColor: Color = Color(0xFFED5564),
    bufferedProgress: Float = 1f,
) {
    var isDragging by remember { mutableStateOf(false) }
    val animatedThumbRadius by animateFloatAsState(
        targetValue = if (isDragging) thumbExpandedRadius.value else thumbRadius.value,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "thumbRadius"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight + 16.dp)
                .align(androidx.compose.ui.Alignment.Center)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false; onSeekFinished() },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, _ ->
                            change.consume()
                            val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                            onSeek(newProgress)
                        }
                    )
                }
        ) {
            val canvasHeight = size.height
            val trackY = canvasHeight / 2
            val trackHeightPx = trackHeight.toPx()
            val currentThumbRadius = animatedThumbRadius * density

            val glowColor = accentColor.copy(alpha = 0.3f)

            drawRoundRect(
                color = Color.White.copy(alpha = 0.1f),
                topLeft = Offset(0f, trackY - trackHeightPx / 2),
                size = Size(size.width * bufferedProgress, trackHeightPx),
                cornerRadius = CornerRadius(trackHeightPx / 2)
            )

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(accentColor, accentColor.copy(alpha = 0.7f))
                ),
                topLeft = Offset(0f, trackY - trackHeightPx / 2),
                size = Size(size.width * progress, trackHeightPx),
                cornerRadius = CornerRadius(trackHeightPx / 2)
            )

            val thumbX = size.width * progress
            val thumbY = trackY

            drawCircle(
                color = glowColor,
                radius = currentThumbRadius * 2.5f,
                center = Offset(thumbX, thumbY)
            )

            drawCircle(
                color = Color.White,
                radius = currentThumbRadius,
                center = Offset(thumbX, thumbY)
            )

            drawCircle(
                color = accentColor,
                radius = currentThumbRadius * 0.6f,
                center = Offset(thumbX, thumbY)
            )
        }
    }
}
