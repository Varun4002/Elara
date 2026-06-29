package com.elara.music.ui.home

import com.elara.innertube.models.YTItem
import com.elara.innertube.pages.HomePage
import com.elara.music.db.entities.LocalItem
import com.elara.music.db.entities.Song
import com.elara.music.db.entities.Song as LocalSong
import com.elara.music.models.MediaMetadata

object HomeHeroEngine {

    fun compute(
        quickPicks: List<Song>?,
        keepListening: List<LocalItem>?,
        speedDialItems: List<YTItem>,
        mediaMetadata: MediaMetadata?,
        homePage: HomePage?,
    ): List<HeroContent> {
        val contents = mutableListOf<HeroContent>()
        val usedIds = mutableSetOf<String>()

        fun addIfNew(content: HeroContent) {
            if (content.id !in usedIds) {
                usedIds.add(content.id)
                contents.add(content)
            }
        }

        // 1. Currently Playing
        if (mediaMetadata != null) {
            addIfNew(
                HeroContent(
                    id = mediaMetadata.id,
                    title = mediaMetadata.title,
                    subtitle = mediaMetadata.artists.joinToString(", ") { it.name },
                    thumbnailUrl = mediaMetadata.thumbnailUrl,
                    type = HeroContentType.Primary,
                )
            )
        }

        // 2. Continue Listening (most recent unfinished)
        if (contents.size < 3 && !keepListening.isNullOrEmpty()) {
            val item = keepListening.firstOrNull { it is LocalSong } as? LocalSong
            if (item != null) {
                addIfNew(
                    HeroContent(
                        id = item.id,
                        title = item.title,
                        subtitle = item.orderedArtists.joinToString(", ") { it.name },
                        thumbnailUrl = item.thumbnailUrl,
                        type = HeroContentType.ContinueListening,
                    )
                )
            }
        }

        // 3. Speed Dial (first pinned item)
        if (contents.size < 3 && speedDialItems.isNotEmpty()) {
            val item = speedDialItems.first()
            addIfNew(
                HeroContent(
                    id = item.id,
                    title = item.title,
                    subtitle = "",
                    thumbnailUrl = item.thumbnail,
                    type = HeroContentType.Recommendation,
                )
            )
        }

        // 4. Quick Picks (first item)
        if (contents.size < 3 && !quickPicks.isNullOrEmpty()) {
            val item = quickPicks.first()
            addIfNew(
                HeroContent(
                    id = item.id,
                    title = item.title,
                    subtitle = item.orderedArtists.joinToString(", ") { it.name },
                    thumbnailUrl = item.thumbnailUrl,
                    type = HeroContentType.Recommendation,
                )
            )
        }

        // 5. Home recommendation (first section, first item)
        if (contents.size < 3) {
            val firstItem = homePage?.sections?.firstOrNull()?.items?.firstOrNull()
            if (firstItem != null) {
                addIfNew(
                    HeroContent(
                        id = firstItem.id,
                        title = firstItem.title,
                        subtitle = "",
                        thumbnailUrl = firstItem.thumbnail,
                        type = HeroContentType.Recommendation,
                    )
                )
            }
        }

        return contents
    }
}
