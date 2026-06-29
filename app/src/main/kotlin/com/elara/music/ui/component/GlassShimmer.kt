package com.elara.music.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassShimmer(
    modifier: Modifier = Modifier,
    width: Dp = 80.dp,
    height: Dp = 12.dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.06f),
        ),
        start = Offset(translateX - 200f, 0f),
        end = Offset(translateX, 0f),
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(shimmerBrush)
    )
}

@Composable
fun GlassShimmerCard(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    width: Dp = 160.dp,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            GlassShimmer(width = width - 24.dp, height = 60.dp, shape = RoundedCornerShape(8.dp))
            Spacer(modifier = Modifier.height(8.dp))
            GlassShimmer(width = (width - 24.dp) * 0.7f, height = 10.dp)
            Spacer(modifier = Modifier.height(4.dp))
            GlassShimmer(width = (width - 24.dp) * 0.5f, height = 8.dp)
        }
    }
}
