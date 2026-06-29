package com.elara.music.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.elara.music.R
import com.elara.music.ui.animation.ElaraSpring
import com.elara.music.ui.component.GlassSurface
import com.elara.music.ui.theme.BlurRadius

enum class GlassMediaType {
    Song,
    Album,
    Artist,
    Playlist,
    Podcast,
}

data class GlassMediaData(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String?,
    val type: GlassMediaType = GlassMediaType.Song,
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val explicit: Boolean = false,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassMediaCard(
    data: GlassMediaData,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    mosaicLayout: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = ElaraSpring.press,
        label = "cardPress",
    )

    val pressModifier = Modifier
        .scale(scale)
        .combinedClickable(
            interactionSource = interactionSource,
            onClick = onClick,
            onLongClick = onLongClick,
        )

    if (mosaicLayout) {
        MosaicCard(data = data, clickModifier = pressModifier, modifier = modifier)
    } else {
        GridCard(data = data, clickModifier = pressModifier, modifier = modifier)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MosaicCard(
    data: GlassMediaData,
    clickModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        blurRadius = BlurRadius.MEDIUM.dp,
        alpha = 0.08f,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth().then(clickModifier),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                AsyncImage(
                    model = data.thumbnailUrl,
                    contentDescription = data.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (data.isActive && data.isPlaying) {
                    NowPlayingBars(modifier = Modifier.align(Alignment.Center).size(24.dp))
                } else if (!data.isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_widget_play),
                            contentDescription = "Play ${data.title}",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (data.subtitle.isNotBlank()) {
                    Text(
                        text = data.subtitle,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                if (data.isActive && data.isPlaying) {
                    Text(
                        text = "NOW PLAYING",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                    if (data.explicit) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
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
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridCard(
    data: GlassMediaData,
    clickModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val isArtist = data.type == GlassMediaType.Artist
    val cornerShape = if (isArtist) CircleShape else RoundedCornerShape(16.dp)

    GlassSurface(
        blurRadius = BlurRadius.MEDIUM.dp,
        alpha = 0.08f,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().then(clickModifier),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(cornerShape),
            ) {
                AsyncImage(
                    model = data.thumbnailUrl,
                    contentDescription = data.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (data.isActive && data.isPlaying) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                    NowPlayingBars(modifier = Modifier.align(Alignment.Center).size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = data.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
            )
            if (data.subtitle.isNotBlank()) {
                Text(
                    text = data.subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
                )
            }
        }
    }
}

@Composable
private fun NowPlayingBars(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "npBars")
    val heights = listOf(
        transition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "b1"),
        transition.animateFloat(0.6f, 0.8f, infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "b2"),
        transition.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "b3"),
        transition.animateFloat(0.7f, 0.5f, infiniteRepeatable(tween(380), RepeatMode.Reverse), label = "b4"),
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEach { bar ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((12.dp * bar.value).coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
fun GlassMediaCardSkeleton(
    modifier: Modifier = Modifier,
    mosaicLayout: Boolean = false,
) {
    val shimmerColor = Color.White.copy(alpha = 0.05f)

    GlassSurface(
        blurRadius = BlurRadius.MEDIUM.dp,
        alpha = 0.05f,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (mosaicLayout) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmerColor),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.fillMaxWidth(0.8f).height(14.dp)
                            .clip(RoundedCornerShape(4.dp)).background(shimmerColor),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(0.5f).height(12.dp)
                            .clip(RoundedCornerShape(4.dp)).background(shimmerColor),
                    )
                }
            }
        } else {
            Column(modifier = Modifier.padding(8.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)).background(shimmerColor),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(0.8f).height(14.dp)
                        .clip(RoundedCornerShape(4.dp)).background(shimmerColor),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(0.5f).height(12.dp)
                        .clip(RoundedCornerShape(4.dp)).background(shimmerColor),
                )
            }
        }
    }
}
