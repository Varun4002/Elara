package com.elara.music.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil3.compose.AsyncImage
import com.elara.music.ui.component.GlassCard
import com.elara.music.ui.component.GlassSurface
import com.elara.music.ui.theme.BlurRadius

enum class CarouselStyle {
    Standard,
    HeroCard,
}

@Composable
fun RecommendationCarousel(
    title: String,
    items: List<GlassMediaData>,
    onClick: (Int) -> Unit,
    onLongClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    style: CarouselStyle = CarouselStyle.Standard,
    onPlayAllClick: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        GlassNavigationTitle(
            title = title,
            onPlayAllClick = onPlayAllClick,
            modifier = Modifier.fillMaxWidth(),
        )

        if (style == CarouselStyle.Standard) {
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> "rec_${item.id}" },
                ) { index, _ ->
                    GlassMediaCard(
                        data = items[index],
                        onClick = { onClick(index) },
                        onLongClick = onLongClick?.let { { it(index) } },
                        modifier = Modifier.width(160.dp),
                        mosaicLayout = false,
                    )
                }
            }
        } else {
            val carouselState = rememberCarouselState { items.size }

            HorizontalMultiBrowseCarousel(
                state = carouselState,
                preferredItemWidth = 300.dp,
                itemSpacing = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(horizontal = 16.dp),
            ) { index ->
                GlassHeroRecommendationCard(
                    data = items[index],
                    onClick = { onClick(index) },
                    modifier = Modifier.fillMaxWidth().height(380.dp),
                )
            }
        }
    }
}

@Composable
private fun GlassHeroRecommendationCard(
    data: GlassMediaData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp)),
            ) {
                AsyncImage(
                    model = data.thumbnailUrl,
                    contentDescription = data.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    GlassSurface(
                        blurRadius = BlurRadius.MEDIUM.dp,
                        alpha = 0.12f,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = data.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (data.subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = data.subtitle,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
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
}

@Composable
fun RecommendationCarouselSkeleton(
    modifier: Modifier = Modifier,
    style: CarouselStyle = CarouselStyle.Standard,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GlassNavigationTitle(
            title = "",
            modifier = Modifier.fillMaxWidth(),
        )

        LazyRow(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(listOf(1, 2, 3)) { _, _ ->
                GlassMediaCardSkeleton(
                    modifier = Modifier.width(if (style == CarouselStyle.HeroCard) 300.dp else 160.dp),
                    mosaicLayout = false,
                )
            }
        }
    }
}
