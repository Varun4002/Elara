package com.elara.music.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset as GeomOffset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player as Media3Player
import coil3.compose.AsyncImage
import com.elara.music.LocalAudioRouteManager
import com.elara.music.LocalListenTogetherManager
import com.elara.music.LocalPlayerConnection
import com.elara.music.R
import com.elara.music.audio.PlaybackDestination
import com.elara.music.models.MediaMetadata
import com.elara.music.playback.CastConnectionHandler
import com.elara.music.playback.PlayerConnection
import com.elara.music.ui.animation.ElaraSpring
import com.elara.music.ui.component.GlassButton
import com.elara.music.ui.component.GlassSurface
import com.elara.music.ui.theme.AccentTokens
import com.elara.music.ui.theme.BlurRadius
import com.elara.music.ui.theme.GlassAlpha
import com.elara.music.ui.utils.resize
import com.elara.music.utils.joinToArtistString
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val MINI_CORNER = 32.dp
private val ARTWORK_SIZE = 48.dp
private val MINI_HEIGHT = 80.dp
private val PROGRESS_HEIGHT = 2.dp

@Composable
fun ElaraMiniPlayer(
    progressState: ProgressState,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val error by playerConnection.error.collectAsState()

    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (_: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    val audioRouteProvider = LocalAudioRouteManager.current
    val playbackDestination by audioRouteProvider?.destination?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(PlaybackDestination.deviceSpeaker()) }

    val swipeSensitivity = 0.73f
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest =
        listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val swipeThumbnail = true && !isListenTogetherGuest

    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    val windowInfo = LocalWindowInfo.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isTabletLandscape =
        remember(windowInfo.containerSize.width, configuration.orientation) {
            (windowInfo.containerSize.width / density.density) >= 600f &&
                configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val swipeAnimationSpec =
        remember {
            spring<Float>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow,
            )
        }

    val accentColor = remember(gradientColors) { AccentTokens.generateAccent(gradientColors) }
    val accentColorAnim by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(400),
        label = "miniAccent",
    )

    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(MINI_HEIGHT)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 12.dp)
                .shadow(12.dp, RoundedCornerShape(MINI_CORNER), clip = false)
                .shadow(4.dp, RoundedCornerShape(MINI_CORNER), clip = false)
                .let { base ->
                    if (swipeThumbnail) {
                        base.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    dragStartTime = System.currentTimeMillis()
                                    totalDragDistance = 0f
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        offsetXAnimatable.animateTo(0f, swipeAnimationSpec)
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    val adjusted =
                                        if (layoutDirection == LayoutDirection.Rtl) -dragAmount
                                        else dragAmount
                                    val canPrev = playerConnection.player.previousMediaItemIndex != -1
                                    val canNext = playerConnection.player.nextMediaItemIndex != -1
                                    val dragRight = adjusted > 0
                                    val dragLeft = adjusted < 0
                                    val allowLeft = dragLeft && canNext
                                    val allowRight = dragRight && canPrev
                                    val canReturn =
                                        (dragRight && !canPrev && offsetXAnimatable.value < 0) ||
                                            (dragLeft && !canNext && offsetXAnimatable.value > 0)
                                    if (allowLeft || allowRight || canReturn) {
                                        totalDragDistance += absoluteValue(adjusted)
                                        coroutineScope.launch {
                                            offsetXAnimatable.snapTo(
                                                offsetXAnimatable.value + adjusted,
                                            )
                                        }
                                    }
                                },
                                onDragEnd = {
                                    val dragDuration =
                                        System.currentTimeMillis() - dragStartTime
                                    val velocity =
                                        if (dragDuration > 0) totalDragDistance / dragDuration
                                        else 0f
                                    val currentOffset = offsetXAnimatable.value
                                    val autoThreshold =
                                        (600 / (1f + kotlin.math.exp(
                                            -(-11.44748f * swipeSensitivity + 9.04945f),
                                        ))).roundToInt()
                                    val shouldChange =
                                        (absoluteValue(currentOffset) > 50f &&
                                            velocity > (swipeSensitivity * -8.25f + 8.5f)) ||
                                            absoluteValue(currentOffset) > autoThreshold
                                    if (shouldChange) {
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.LightImpact,
                                        )
                                        if (currentOffset > 0 && canSkipPrevious) {
                                            playerConnection.player.seekToPreviousMediaItem()
                                        } else if (currentOffset <= 0 && canSkipNext) {
                                            playerConnection.player.seekToNext()
                                        }
                                    }
                                    coroutineScope.launch {
                                        offsetXAnimatable.animateTo(0f, swipeAnimationSpec)
                                    }
                                },
                            )
                        }
                    } else base
                },
    ) {
        Box(
            modifier =
                Modifier
                    .then(
                        if (isTabletLandscape) Modifier.width(500.dp).align(Alignment.Center)
                        else Modifier.fillMaxWidth(),
                    )
                    .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) },
        ) {
            GlassSurface(
                blurRadius = BlurRadius.LIGHT.dp,
                alpha = GlassAlpha.SURFACE.value,
                borderAlpha = GlassAlpha.BORDER.value,
                shape = RoundedCornerShape(MINI_CORNER),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    coroutineScope.launch {
                        hapticFeedback.performHapticFeedback(
                            HapticFeedbackType.LightImpact,
                        )
                        onClick()
                    }
                },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(MINI_HEIGHT - PROGRESS_HEIGHT)
                                .padding(start = 12.dp, end = 8.dp, top = 0.dp, bottom = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiniPlayerArtwork(
                            mediaMetadata = mediaMetadata,
                            isPlaying = effectiveIsPlaying,
                            playbackState = playbackState,
                            modifier = Modifier.size(ARTWORK_SIZE),
                        )

                        Spacer(Modifier.width(12.dp))

                        MiniPlayerSongInfoSection(
                            mediaMetadata = mediaMetadata,
                            error = error,
                            playbackDestination = playbackDestination,
                            accentColor = accentColorAnim,
                            onRouteClick = {
                                when {
                                    playbackDestination.isCasting ->
                                        context.startActivity(
                                            android.content.Intent(
                                                android.provider.Settings.ACTION_CAST_SETTINGS,
                                            ),
                                        )
                                    playbackDestination.isBluetooth ->
                                        context.startActivity(
                                            android.content.Intent(
                                                android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,
                                            ),
                                        )
                                    else ->
                                        context.startActivity(
                                            android.content.Intent(
                                                android.provider.Settings.ACTION_SOUND_SETTINGS,
                                            ),
                                        )
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )

                        Spacer(Modifier.width(8.dp))

                        MiniPlayerControlsRow(
                            isPlaying = effectiveIsPlaying,
                            isListenTogetherGuest = isListenTogetherGuest,
                            canSkipNext = canSkipNext,
                            canSkipPrevious = canSkipPrevious,
                            playerConnection = playerConnection,
                            castHandler = castHandler,
                            isCasting = isCasting,
                            playbackState = playbackState,
                        )
                    }

                    MiniPlayerProgressBar(
                        progressState = progressState,
                        accentColor = accentColorAnim,
                        modifier = Modifier.fillMaxWidth().height(PROGRESS_HEIGHT),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerArtwork(
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    playbackState: Int,
    modifier: Modifier = Modifier,
) {
    val thumbnailUrl = remember(mediaMetadata?.thumbnailUrl) {
        mediaMetadata?.thumbnailUrl?.resize(120, 120)
    }

    val scaleAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = ElaraSpring.small,
        label = "artworkScale",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(ARTWORK_SIZE)
                    .clip(RoundedCornerShape(12.dp))
                    .semantics {
                        contentDescription =
                            mediaMetadata?.let { "Album art for ${it.title}" }
                                ?: "Album art"
                    },
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scaleAnim
                            scaleY = scaleAnim
                        },
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    Color.Transparent,
                                ),
                                start = GeomOffset(0f, 0f),
                                end = GeomOffset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                            ),
                            RoundedCornerShape(12.dp),
                        ),
            )
        }

        if (!isPlaying || playbackState == Media3Player.STATE_ENDED) {
            Box(
                modifier =
                    Modifier
                        .size(ARTWORK_SIZE)
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (playbackState == Media3Player.STATE_ENDED) {
                        Icons.Rounded.Replay
                    } else {
                        Icons.Rounded.PlayArrow
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerSongInfoSection(
    mediaMetadata: MediaMetadata?,
    error: PlaybackException?,
    playbackDestination: PlaybackDestination,
    accentColor: Color,
    onRouteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        mediaMetadata?.let { metadata ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = metadata.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee(
                            iterations = 1,
                            initialDelayMillis = 2000,
                            velocity = 20.dp,
                        ),
                )

                AnimatedVisibility(
                    visible = metadata.explicit,
                    enter = scaleIn(animationSpec = tween(200)) + fadeIn(tween(200)),
                    exit = scaleOut(animationSpec = tween(150)) + fadeOut(tween(150)),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .padding(start = 4.dp)
                                .size(16.dp)
                                .background(
                                    Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(3.dp),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "E",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (metadata.artists.any { it.name.isNotBlank() }) {
                Text(
                    text = metadata.artists.joinToArtistString(
                        " ${stringResource(R.string.and)}",
                    ) { it.name },
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.basicMarquee(
                        iterations = 1,
                        initialDelayMillis = 2000,
                        velocity = 20.dp,
                    ),
                )
            }
        }

        MiniPlayerRouteIndicator(
            error = error,
            playbackDestination = playbackDestination,
            accentColor = accentColor,
            onClick = onRouteClick,
        )
    }
}

@Composable
private fun MiniPlayerRouteIndicator(
    error: PlaybackException?,
    playbackDestination: PlaybackDestination,
    accentColor: Color,
    onClick: () -> Unit,
) {
    when {
        error != null -> {
            GlassSurface(
                blurRadius = BlurRadius.LIGHT.dp,
                alpha = GlassAlpha.SURFACE.value,
                borderAlpha = GlassAlpha.BORDER.value,
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.warning),
                        contentDescription = null,
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = error.localizedMessage
                            ?: stringResource(R.string.error_playing),
                        color = Color(0xFFFF6B6B),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        playbackDestination.isExternal -> {
            PlaybackDestinationCapsule(
                destination = playbackDestination,
                tintColor = accentColor,
                onClick = onClick,
            )
        }

        else -> {
            Text(
                text = "Playing on this device",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MiniPlayerControlsRow(
    isPlaying: Boolean,
    isListenTogetherGuest: Boolean,
    canSkipNext: Boolean,
    canSkipPrevious: Boolean,
    playerConnection: PlayerConnection,
    castHandler: CastConnectionHandler?,
    isCasting: Boolean,
    playbackState: Int,
) {
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState()
        ?: remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassButton(
            icon = Icons.Rounded.SkipPrevious,
            iconTint = Color.White.copy(alpha = if (canSkipPrevious) 1f else 0.35f),
            contentDescription = "Previous track",
            size = 40.dp,
            iconSize = 20.dp,
            onClick = {
                if (canSkipPrevious) {
                    playerConnection.seekToPrevious()
                }
            },
        )

        GlassButton(
            icon = if (!isPlaying || playbackState == Media3Player.STATE_ENDED) {
                if (playbackState == Media3Player.STATE_ENDED) {
                    Icons.Rounded.Replay
                } else {
                    Icons.Rounded.PlayArrow
                }
            } else {
                Icons.Rounded.Pause
            },
            iconTint = Color.White,
            contentDescription = if (isPlaying) "Pause" else "Play",
            size = 48.dp,
            iconSize = 24.dp,
            glowColor = Color.White.copy(alpha = 0.15f),
            onClick = {
                if (isListenTogetherGuest) {
                    playerConnection.toggleMute()
                } else if (isCasting) {
                    if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                } else if (playbackState == Media3Player.STATE_ENDED) {
                    playerConnection.player.seekTo(0, 0)
                    playerConnection.player.playWhenReady = true
                } else {
                    playerConnection.togglePlayPause()
                }
            },
        )

        GlassButton(
            icon = Icons.Rounded.SkipNext,
            iconTint = Color.White.copy(alpha = if (canSkipNext) 1f else 0.35f),
            contentDescription = "Next track",
            size = 40.dp,
            iconSize = 20.dp,
            onClick = {
                if (canSkipNext) {
                    playerConnection.seekToNext()
                }
            },
        )
    }
}

@Composable
private fun MiniPlayerProgressBar(
    progressState: ProgressState,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier.drawBehind {
            val barHeight = size.height
            val barWidth = size.width
            val cornerPx = barHeight / 2
            val progress = progressState.progress

            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            )

            val progressWidth = barWidth * progress
            if (progressWidth > 0f) {
                drawRoundRect(
                    color = accentColor,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    size = androidx.compose.ui.geometry.Size(progressWidth, barHeight),
                )

                val glowWidthDp = 4.dp
                val glowWidthPx = with(density) { glowWidthDp.toPx() }
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.15f),
                    cornerRadius = CornerRadius(glowWidthPx / 2, glowWidthPx / 2),
                    size = androidx.compose.ui.geometry.Size(
                        (progressWidth + glowWidthPx).coerceAtMost(barWidth),
                        (barHeight + glowWidthPx).coerceAtMost(barHeight + glowWidthPx),
                    ),
                    topLeft = GeomOffset(-glowWidthPx / 2, -glowWidthPx / 2),
                )

                val hlHeight = barHeight * 0.6f
                val minHlWidth = with(density) { 2.dp.toPx() }
                val hlWidth = (progressWidth * 0.03f).coerceAtLeast(minHlWidth)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.35f),
                    cornerRadius = CornerRadius(hlHeight / 2, hlHeight / 2),
                    size = androidx.compose.ui.geometry.Size(hlWidth, hlHeight),
                    topLeft = GeomOffset(progressWidth - hlWidth, (barHeight - hlHeight) / 2),
                )
            }
        },
    )
}
