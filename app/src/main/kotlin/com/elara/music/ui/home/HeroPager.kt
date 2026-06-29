package com.elara.music.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.elara.music.ui.component.GlassSurface
import com.elara.music.ui.theme.BlurRadius

private const val HERO_HEIGHT_DP = 460f

@Composable
fun HeroPager(
    contents: List<HeroContent>,
    scrollState: HomeScrollState,
    modifier: Modifier = Modifier,
) {
    if (contents.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { contents.size })

    val collapseProgress = scrollState.collapseProgress.coerceIn(0f, 1f)
    val visualHeightDp = HERO_HEIGHT_DP * (1f - collapseProgress)
    val visualAlpha = 1f - collapseProgress

    if (visualHeightDp <= 0f || visualAlpha <= 0.01f) return

    val currentContent = remember(pagerState.currentPage, contents) {
        contents.getOrNull(pagerState.currentPage)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(visualHeightDp.dp),
    ) {
        Crossfade(
            targetState = currentContent?.id ?: "",
            animationSpec = tween(durationMillis = 600),
            label = "heroCrossfade",
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = currentContent?.thumbnailUrl,
                    contentDescription = currentContent?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0D0D)),
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.1f),
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.85f),
                                ),
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 100.dp),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    GlassSurface(
                        blurRadius = BlurRadius.MEDIUM.dp,
                        alpha = 0.12f,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            if (currentContent?.type == HeroContentType.ContinueListening) {
                                Text(
                                    text = "Continue Listening",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            currentContent?.let { content ->
                                Text(
                                    text = content.title,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                if (content.subtitle.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = content.subtitle,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (contents.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(contents.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White
                                else Color.White.copy(alpha = 0.4f),
                            ),
                    )
                }
            }
        }
    }
}
