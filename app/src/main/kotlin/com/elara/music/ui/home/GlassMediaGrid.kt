package com.elara.music.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GlassMediaGrid(
    items: List<GlassMediaData>,
    onClick: (Int) -> Unit,
    onLongClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    mosaicLayout: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    verticalArrangement: Arrangement.VerticalOrHorizontal = Arrangement.spacedBy(12.dp),
    horizontalArrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(12.dp),
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        modifier = modifier.fillMaxWidth(),
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> "glass_grid_${item.id}" },
        ) { index, item ->
            GlassMediaCard(
                data = item,
                onClick = { onClick(index) },
                onLongClick = onLongClick?.let { { it(index) } },
                modifier = Modifier.fillMaxWidth(),
                mosaicLayout = mosaicLayout,
            )
        }
    }
}

@Composable
fun GlassMediaGridSkeleton(
    modifier: Modifier = Modifier,
    columns: Int = 2,
    itemCount: Int = 4,
    mosaicLayout: Boolean = false,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(itemCount) { index ->
            GlassMediaCardSkeleton(
                modifier = Modifier.fillMaxWidth(),
                mosaicLayout = mosaicLayout,
            )
        }
    }
}
