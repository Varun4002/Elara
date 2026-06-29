package com.elara.music.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
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
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = verticalArrangement
    ) {
        items.chunked(columns).forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = horizontalArrangement
            ) {
                rowItems.forEachIndexed { colIndex, item ->
                    val index = rowIndex * columns + colIndex
                    Box(modifier = Modifier.weight(1f)) {
                        GlassMediaCard(
                            data = item,
                            onClick = { onClick(index) },
                            onLongClick = onLongClick?.let { { it(index) } },
                            modifier = Modifier.fillMaxWidth(),
                            mosaicLayout = mosaicLayout,
                        )
                    }
                }
                
                // Add empty spacers to fill the remaining slots if the last row is incomplete
                val remaining = columns - rowItems.size
                if (remaining > 0) {
                    repeat(remaining) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val rows = (itemCount + columns - 1) / columns
        repeat(rows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val rowItemCount = if (rowIndex == rows - 1 && itemCount % columns != 0) itemCount % columns else columns
                repeat(rowItemCount) {
                    Box(modifier = Modifier.weight(1f)) {
                        GlassMediaCardSkeleton(
                            modifier = Modifier.fillMaxWidth(),
                            mosaicLayout = mosaicLayout,
                        )
                    }
                }
                val remaining = columns - rowItemCount
                if (remaining > 0) {
                    repeat(remaining) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
