package com.elara.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elara.music.R
import com.elara.music.audio.DeviceType
import com.elara.music.audio.PlaybackDestination
import com.elara.music.audio.RouteCategory
import com.elara.music.ui.component.GlassSurface
import com.elara.music.ui.theme.BlurRadius
import com.elara.music.ui.theme.GlassAlpha

private enum class EmphasisLevel { SUBTLE, LOW, MEDIUM, HIGH }

private fun emphasisFor(destination: PlaybackDestination): EmphasisLevel = when {
    destination.isCasting -> EmphasisLevel.HIGH
    destination.isBluetooth || destination.routeCategory == RouteCategory.USB -> EmphasisLevel.MEDIUM
    destination.routeCategory == RouteCategory.CAR -> EmphasisLevel.MEDIUM
    destination.isExternal -> EmphasisLevel.LOW
    else -> EmphasisLevel.SUBTLE
}

private fun iconRes(category: RouteCategory, deviceType: DeviceType): Int = when (category) {
    RouteCategory.CAST -> R.drawable.cast_connected
    RouteCategory.BLUETOOTH -> R.drawable.bluetooth
    RouteCategory.USB -> R.drawable.ic_usb
    RouteCategory.CAR -> R.drawable.ic_car
    RouteCategory.LOCAL -> when (deviceType) {
        DeviceType.HEADPHONES, DeviceType.EARBUDS -> R.drawable.ic_headphones
        DeviceType.TV -> R.drawable.ic_tv
        else -> R.drawable.ic_speaker
    }
}

@Composable
fun PlaybackDestinationCapsule(
    destination: PlaybackDestination,
    tintColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val emphasis = emphasisFor(destination)
    val effectiveTint = if (destination.routeCategory == RouteCategory.CAR) {
        Color(0xFF4CAF50)
    } else {
        tintColor
    }

    val glassAlpha = when (emphasis) {
        EmphasisLevel.HIGH -> GlassAlpha.SURFACE_ELEVATED.value * 1.5f
        EmphasisLevel.MEDIUM -> GlassAlpha.SURFACE_ELEVATED.value
        EmphasisLevel.LOW -> GlassAlpha.SURFACE.value
        EmphasisLevel.SUBTLE -> GlassAlpha.SURFACE.value * 0.6f
    }
    val borderAlpha = when (emphasis) {
        EmphasisLevel.HIGH -> GlassAlpha.HIGHLIGHT.value
        EmphasisLevel.MEDIUM -> GlassAlpha.BORDER.value * 1.2f
        EmphasisLevel.LOW -> GlassAlpha.BORDER.value
        EmphasisLevel.SUBTLE -> GlassAlpha.BORDER.value * 0.6f
    }
    val contentAlpha = when (emphasis) {
        EmphasisLevel.HIGH -> 1f
        EmphasisLevel.MEDIUM -> 0.95f
        EmphasisLevel.LOW -> 0.85f
        EmphasisLevel.SUBTLE -> 0.6f
    }
    val iconDp: Dp = when (emphasis) {
        EmphasisLevel.HIGH -> 24.dp
        EmphasisLevel.MEDIUM -> 22.dp
        EmphasisLevel.LOW -> 20.dp
        EmphasisLevel.SUBTLE -> 18.dp
    }

    val accessibleDescription = if (destination.isCasting) {
        "Casting to ${destination.displayName}."
    } else {
        "Playing through ${destination.displayName}."
    }

    GlassSurface(
        blurRadius = BlurRadius.LIGHT.dp,
        alpha = glassAlpha,
        borderAlpha = borderAlpha,
        shape = RoundedCornerShape(50),
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = accessibleDescription
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .alpha(contentAlpha),
        ) {
            val iconKey = destination.routeCategory to destination.deviceType

            AnimatedContent(
                targetState = iconKey,
                transitionSpec = {
                    fadeIn(animationSpec = tween(120)) togetherWith fadeOut(animationSpec = tween(80))
                },
                label = "capsuleIcon",
                modifier = Modifier.size(iconDp),
            ) { (category, deviceType) ->
                Image(
                    painter = painterResource(iconRes(category, deviceType)),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .let { if (emphasis == EmphasisLevel.HIGH) it.scale(1.1f) else it },
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = destination.displayName,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(170, delayMillis = 80)) +
                            slideInVertically(
                                animationSpec = tween(170, delayMillis = 80),
                                initialOffsetY = { it / 4 },
                            )
                            togetherWith
                            fadeOut(animationSpec = tween(100)) +
                            slideOutVertically(
                                animationSpec = tween(100),
                                targetOffsetY = { -it / 4 },
                            )
                    },
                    label = "capsuleName",
                ) { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelLarge,
                        color = effectiveTint,
                        maxLines = 1,
                    )
                }

                if (destination.subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    AnimatedContent(
                        targetState = destination.subtitle,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(120, delayMillis = 200))
                                togetherWith fadeOut(animationSpec = tween(80))
                        },
                        label = "capsuleSubtitle",
                    ) { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = effectiveTint.copy(alpha = 0.7f),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
