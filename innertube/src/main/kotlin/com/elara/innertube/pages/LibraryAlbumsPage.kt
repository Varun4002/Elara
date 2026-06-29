package com.elara.innertube.pages

import com.elara.innertube.models.Album
import com.elara.innertube.models.AlbumItem
import com.elara.innertube.models.Artist
import com.elara.innertube.models.ArtistItem
import com.elara.innertube.models.MusicResponsiveListItemRenderer
import com.elara.innertube.models.MusicTwoRowItemRenderer
import com.elara.innertube.models.PlaylistItem
import com.elara.innertube.models.SongItem
import com.elara.innertube.models.YTItem
import com.elara.innertube.models.oddElements
import com.elara.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
