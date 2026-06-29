package com.elara.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class GestureAction {
    SEEK_BACKWARD,
    SEEK_FORWARD,
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    VOLUME_UP,
    VOLUME_DOWN,
}

@Composable
fun GestureHUD(
    action: GestureAction?,
    value: Float = 0f,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = action != null,
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier,
    ) {
        if (action != null) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val icon = when (action) {
                        GestureAction.SEEK_BACKWARD -> Icons.Rounded.Replay10
                        GestureAction.SEEK_FORWARD -> Icons.Rounded.FastForward
                        else -> Icons.Rounded.Replay10
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                    val label = when (action) {
                        GestureAction.SEEK_BACKWARD -> "-10s"
                        GestureAction.SEEK_FORWARD -> "+10s"
                        GestureAction.BRIGHTNESS_UP -> "Brightness"
                        GestureAction.BRIGHTNESS_DOWN -> "Brightness"
                        GestureAction.VOLUME_UP -> "Volume"
                        GestureAction.VOLUME_DOWN -> "Volume"
                    }
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (action in listOf(GestureAction.BRIGHTNESS_UP, GestureAction.BRIGHTNESS_DOWN)) {
                        Text(
                            text = "${(value * 100).toInt()}%",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}
