package com.elara.music.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.elara.music.LocalPlayerConnection
import com.elara.music.R
import com.elara.music.utils.joinToArtistString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun ElaraPlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val playbackState by playerConnection.playbackState.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val isCasting by playerConnection.service.castConnectionHandler?.isCasting?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
                playerConnection.player.duration.takeIf { it > 0 }?.let { duration = it }
            }
        }
    }

    LaunchedEffect(playbackState, mediaMetadata?.id) {
        position = playerConnection.player.currentPosition
        duration = (mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L))
            ?: playerConnection.player.duration
    }

    val progress by remember {
        derivedStateOf {
            if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
        }
    }

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }

    suspend fun extractPalette(bitmap: android.graphics.Bitmap) {
        val palette = withContext(Dispatchers.Default) {
            androidx.palette.graphics.Palette
                .from(bitmap)
                .maximumColorCount(8)
                .resizeBitmapArea(100 * 100)
                .generate()
        }
        val vibrant = palette?.vibrantSwatch?.rgb?.let { Color(it) }
        val muted = palette?.mutedSwatch?.rgb?.let { Color(it) }
        val darkMuted = palette?.darkMutedSwatch?.rgb?.let { Color(it) }
        if (vibrant != null && muted != null) {
            gradientColors = listOf(vibrant, muted, darkMuted ?: Color(0xFF0D0D0D))
        }
    }

    LaunchedEffect(mediaMetadata?.id) {
        val thumbUrl = mediaMetadata?.thumbnailUrl
        if (thumbUrl != null) {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(thumbUrl)
                    .size(100, 100)
                    .allowHardware(false)
                    .build()
                val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                val bitmap = result?.image?.toBitmap()
                if (bitmap != null) {
                    extractPalette(bitmap)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (mediaMetadata == null && gradientColors.isEmpty()) {
            withContext(Dispatchers.IO) {
                val bitmap = try {
                    android.graphics.BitmapFactory.decodeResource(
                        context.resources, R.drawable.app_icon
                    )
                } catch (e: Exception) { null }
                if (bitmap != null) {
                    extractPalette(bitmap)
                }
            }
        }
    }

    val ambientColors = if (gradientColors.isNotEmpty()) {
        AmbientColors(
            vibrant = gradientColors.getOrElse(0) { Color(0xFFED5564) },
            muted = gradientColors.getOrElse(1) { Color(0xFF595959) },
            darkMuted = gradientColors.getOrElse(2) { Color(0xFF0D0D0D) },
            dominant = gradientColors.getOrElse(0) { Color(0xFFED5564) },
        )
    } else null

    var isLocked by rememberSaveable { mutableStateOf(false) }
    var isFullScreen by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AmbientGradient(
            ambientColors = ambientColors,
        )

        if (!isFullScreen && !isLocked) {
            PlayerTopBar(
                title = mediaMetadata?.title ?: "",
                subtitle = mediaMetadata?.artists?.joinToArtistString(" & ") { it.name },
                isCasting = isCasting,
                onBack = onBack,
                onCast = {},
                onPiP = {},
                onMenu = {},
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (mediaMetadata == null) {
                Image(
                    painter = painterResource(R.drawable.app_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(24.dp)),
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            Spacer(modifier = Modifier.weight(if (mediaMetadata == null) 0f else 1f))

            PlayerCenterControls(
                isPlaying = isPlaying,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                onPrevious = { playerConnection.seekToPrevious() },
                onPlayPause = { playerConnection.togglePlayPause() },
                onNext = { playerConnection.seekToNext() },
            )

            Spacer(modifier = Modifier.weight(0.5f))

            PlayerProgressSection(
                progress = progress,
                bufferedProgress = 1f,
                position = position,
                duration = duration,
                onSeek = { fraction ->
                    playerConnection.player.seekTo((fraction * duration).toLong())
                },
            )

            Spacer(modifier = Modifier.height(24.dp))

            PlayerBottomDock(
                qualityLabel = "Auto",
                playbackSpeed = 1.0f,
                isLocked = isLocked,
                onQuality = {},
                onSpeed = {},
                onSubtitles = {},
                onAudio = {},
                onLock = { isLocked = !isLocked },
                onFullscreen = { isFullScreen = !isFullScreen },
            )
        }
    }
}
