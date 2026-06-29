package com.elara.music.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.elara.music.ui.component.GlassButton
import com.elara.music.ui.component.GlassSurface
import com.elara.music.ui.theme.BlurRadius

@Composable
fun PlayerBottomDock(
    qualityLabel: String,
    playbackSpeed: Float,
    isLocked: Boolean,
    onQuality: () -> Unit,
    onSpeed: () -> Unit,
    onSubtitles: () -> Unit,
    onAudio: () -> Unit,
    onLock: () -> Unit,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        blurRadius = BlurRadius.LIGHT.dp,
        alpha = 0.10f,
        borderAlpha = 0.06f,
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockButton(
                label = qualityLabel,
                icon = Icons.Rounded.Tune,
                onClick = onQuality,
            )
            DockButton(
                label = "${playbackSpeed}x",
                icon = Icons.Rounded.Speed,
                onClick = onSpeed,
            )
            DockButton(
                icon = Icons.Rounded.ClosedCaption,
                onClick = onSubtitles,
            )
            DockButton(
                icon = Icons.Rounded.MusicNote,
                onClick = onAudio,
            )
            DockButton(
                icon = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                onClick = onLock,
                tint = if (isLocked) Color(0xFFED5564) else Color.White,
            )
            DockButton(
                icon = Icons.Rounded.Fullscreen,
                onClick = onFullscreen,
            )
        }
    }
}

@Composable
private fun DockButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String? = null,
    tint: Color = Color.White,
) {
    GlassButton(
        icon = icon,
        onClick = onClick,
        size = 40.dp,
        iconSize = 20.dp,
        iconTint = tint,
        modifier = modifier.size(40.dp),
    )
}
