package com.elara.music.ui.player

import androidx.compose.ui.graphics.Color

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isControlsVisible: Boolean = true,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val artworkUrl: String? = null,
    val title: String = "",
    val artist: String = "",
    val ambientColors: AmbientColors? = null,
    val playbackSpeed: Float = 1.0f,
    val isFullScreen: Boolean = false,
    val isLocked: Boolean = false,
    val repeatMode: Int = 0,
    val isShuffled: Boolean = false,
)

data class AmbientColors(
    val vibrant: Color = Color(0xFFED5564),
    val muted: Color = Color(0xFF595959),
    val darkMuted: Color = Color(0xFF0D0D0D),
    val dominant: Color = Color(0xFFED5564),
)
