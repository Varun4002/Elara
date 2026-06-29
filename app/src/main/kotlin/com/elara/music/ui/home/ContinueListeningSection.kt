package com.elara.music.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elara.music.ui.component.GlassSurface
import com.elara.music.ui.theme.BlurRadius

@Composable
fun ContinueListeningSection(
    items: List<GlassMediaData>,
    onClick: (Int) -> Unit,
    onLongClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    GlassSurface(
        blurRadius = BlurRadius.LIGHT.dp,
        alpha = 0.06f,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        GlassNavigationTitle(
            title = "Continue Listening",
            modifier = Modifier.fillMaxWidth(),
        )

        LazyRow(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().height(180.dp),
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> "continue_${item.id}" },
            ) { index, _ ->
                GlassMediaCard(
                    data = items[index],
                    onClick = { onClick(index) },
                    onLongClick = onLongClick?.let { { it(index) } },
                    modifier = Modifier.width(140.dp),
                    mosaicLayout = false,
                )
            }
        }
    }
}

@Composable
fun ContinueListeningSectionSkeleton(
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        blurRadius = BlurRadius.LIGHT.dp,
        alpha = 0.06f,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        GlassNavigationTitle(
            title = "Continue Listening",
            modifier = Modifier.fillMaxWidth(),
        )

        LazyRow(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().height(180.dp),
        ) {
            itemsIndexed(listOf(1, 2, 3, 4)) { index, _ ->
                GlassMediaCardSkeleton(
                    modifier = Modifier.width(140.dp),
                    mosaicLayout = false,
                )
            }
        }
    }
}
