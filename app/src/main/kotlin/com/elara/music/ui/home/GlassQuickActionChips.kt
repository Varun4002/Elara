package com.elara.music.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elara.music.ui.component.GlassTextButton

data class ChipData(
    val label: String,
    val isSelected: Boolean,
)

@Composable
fun GlassQuickActionChips(
    chips: List<ChipData>,
    onChipClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chips.isEmpty()) return

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(
            items = chips,
            key = { "chip_${it.label}" },
        ) { chip ->
            GlassTextButton(
                text = chip.label,
                onClick = { onChipClick(chips.indexOf(chip)) },
                selected = chip.isSelected,
                shape = RoundedCornerShape(20.dp),
            )
        }
    }
}
