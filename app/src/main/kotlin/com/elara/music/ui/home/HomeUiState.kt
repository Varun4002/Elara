package com.elara.music.ui.home

import androidx.compose.ui.graphics.Color
import com.elara.innertube.models.YTItem
import com.elara.innertube.pages.HomePage
import com.elara.music.db.entities.LocalItem
import com.elara.music.db.entities.Song

data class HomeUiState(
    val heroContents: List<HeroContent> = emptyList(),
    val quickPicks: List<Song>? = null,
    val continueListening: List<LocalItem>? = null,
    val recommendations: List<YTItem>? = null,
    val homePageSections: List<HomePage.Section>? = null,
    val scrollState: HomeScrollState = HomeScrollState(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
)

data class HomeScrollState(
    val collapseProgress: Float = 0f,
    val heroVisibleFraction: Float = 1f,
    val toolbarOpacity: Float = 0f,
    val ambientIntensity: Float = 1f,
)

data class HeroContent(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String?,
    val type: HeroContentType,
    val gradientColors: List<Color> = emptyList(),
)

enum class HeroContentType {
    Primary,
    ContinueListening,
    Recommendation,
}
