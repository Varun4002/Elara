package com.elara.music.ui.component

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elara.music.ui.theme.BlurRadius
import com.elara.music.ui.theme.GlassColorTokens

fun Modifier.glassBlurModifier(
    blurPx: Float,
    alpha: Float,
    borderAlpha: Float,
    cornerRadiusPx: Float,
    surfaceColor: Color = GlassColorTokens.dark.surface,
    borderColor: Color = GlassColorTokens.dark.border,
): Modifier = this
    .graphicsLayer {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            renderEffect = RenderEffect.Companion.createBlurEffect(
                blurPx, blurPx, RenderEffect.Companion.EdgeTreatment.REPEAT
            )
        }
        this@graphicsLayer.alpha = alpha
        this@graphicsLayer.clip = true
    }
    .drawBehind {
        drawRoundRect(
            color = surfaceColor.copy(alpha = alpha),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
        drawRoundRect(
            color = borderColor.copy(alpha = borderAlpha),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = 1.dp.toPx())
        )
    }

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    blurRadius: Dp = BlurRadius.MEDIUM.dp,
    alpha: Float = 0.08f,
    borderAlpha: Float = 0.10f,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "glassScale"
    )

    val density = LocalDensity.current
    val blurPx = with(density) { blurRadius.toPx() }
    val cornerRadiusPx = with(density) { shape.topStart.toPx() }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .scale(scale)
            .glassBlurModifier(
                blurPx = blurPx,
                alpha = alpha,
                borderAlpha = borderAlpha,
                cornerRadiusPx = cornerRadiusPx,
            )
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onClick() }
                        )
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    isPressed = event.changes.any { it.pressed }
                                }
                            }
                        }
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
