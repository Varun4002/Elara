/**
 * Elara Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.elara.music.models

import com.elara.innertube.models.YTItem
import com.elara.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
