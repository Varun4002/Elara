package com.elara.music.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.elara.music.ui.component.GlassButton

@Composable
fun PlayerCenterControls(
    isPlaying: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassButton(
            icon = Icons.Rounded.SkipPrevious,
            onClick = onPrevious,
            size = 48.dp,
            iconSize = 28.dp,
        )

        GlassButton(
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            onClick = onPlayPause,
            size = 72.dp,
            iconSize = 40.dp,
            glowColor = Color(0xFFED5564).copy(alpha = 0.4f),
        )

        GlassButton(
            icon = Icons.Rounded.SkipNext,
            onClick = onNext,
            size = 48.dp,
            iconSize = 28.dp,
        )
    }
}
