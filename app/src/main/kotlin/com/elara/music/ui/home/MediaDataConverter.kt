package com.elara.music.ui.home

import com.elara.innertube.models.AlbumItem
import com.elara.innertube.models.ArtistItem
import com.elara.innertube.models.EpisodeItem
import com.elara.innertube.models.PlaylistItem
import com.elara.innertube.models.PodcastItem
import com.elara.innertube.models.SongItem
import com.elara.innertube.models.YTItem
import com.elara.music.db.entities.Album
import com.elara.music.db.entities.Artist
import com.elara.music.db.entities.LocalItem
import com.elara.music.db.entities.Playlist
import com.elara.music.db.entities.Song
import com.elara.music.models.MediaMetadata
import com.elara.music.utils.joinByBullet
import com.elara.music.utils.joinToArtistString
import com.elara.music.utils.makeTimeString

fun YTItem.toGlassMediaData(
    isActive: Boolean = false,
    isPlaying: Boolean = false,
): GlassMediaData {
    val subtitle = when (this) {
        is SongItem -> joinByBullet(
            artists.joinToArtistString(" & ") { it.name },
            duration?.let { makeTimeString(it * 1000L) },
        )
        is AlbumItem -> joinByBullet(
            artists?.joinToArtistString(" & ") { it.name },
            year?.toString(),
        )
        is ArtistItem -> null
        is PlaylistItem -> joinByBullet(author?.name, songCountText)
        is PodcastItem -> joinByBullet(author?.name, episodeCountText)
        is EpisodeItem -> joinByBullet(author?.name, duration?.let { makeTimeString(it * 1000L) })
        else -> null
    }

    return GlassMediaData(
        id = id,
        title = title,
        subtitle = subtitle ?: "",
        thumbnailUrl = thumbnail,
        type = when (this) {
            is SongItem -> GlassMediaType.Song
            is AlbumItem -> GlassMediaType.Album
            is ArtistItem -> GlassMediaType.Artist
            is PlaylistItem -> GlassMediaType.Playlist
            is PodcastItem -> GlassMediaType.Podcast
            is EpisodeItem -> GlassMediaType.Song
            else -> GlassMediaType.Song
        },
        isActive = isActive,
        isPlaying = isPlaying,
        explicit = if (this is SongItem) explicit else if (this is AlbumItem) explicit else if (this is EpisodeItem) explicit else false,
    )
}

fun Song.toGlassMediaData(
    isActive: Boolean = false,
    isPlaying: Boolean = false,
): GlassMediaData = GlassMediaData(
    id = id,
    title = song.title,
    subtitle = joinByBullet(
        orderedArtists.joinToArtistString(" & ") { it.name },
        makeTimeString(song.duration * 1000L),
    ),
    thumbnailUrl = song.thumbnailUrl,
    type = GlassMediaType.Song,
    isActive = isActive,
    isPlaying = isPlaying,
    explicit = song.explicit,
)

fun Album.toGlassMediaData(
    isActive: Boolean = false,
    isPlaying: Boolean = false,
): GlassMediaData = GlassMediaData(
    id = id,
    title = album.title,
    subtitle = artists.joinToArtistString(" & ") { it.name },
    thumbnailUrl = album.thumbnailUrl,
    type = GlassMediaType.Album,
    isActive = isActive,
    isPlaying = isPlaying,
    explicit = album.explicit,
)

fun Artist.toGlassMediaData(
    isActive: Boolean = false,
    isPlaying: Boolean = false,
): GlassMediaData = GlassMediaData(
    id = id,
    title = artist.name,
    subtitle = "",
    thumbnailUrl = artist.thumbnailUrl,
    type = GlassMediaType.Artist,
    isActive = isActive,
    isPlaying = isPlaying,
)

fun Playlist.toGlassMediaData(
    isActive: Boolean = false,
    isPlaying: Boolean = false,
): GlassMediaData {
    val thumbnailUrl = thumbnails.firstOrNull()
    return GlassMediaData(
        id = id,
        title = playlist.name,
        subtitle = "",
        thumbnailUrl = thumbnailUrl,
        type = GlassMediaType.Playlist,
        isActive = isActive,
        isPlaying = isPlaying,
    )
}

fun LocalItem.toGlassMediaData(
    isActive: Boolean = false,
    isPlaying: Boolean = false,
): GlassMediaData = when (this) {
    is Song -> toGlassMediaData(isActive, isPlaying)
    is Album -> toGlassMediaData(isActive, isPlaying)
    is Artist -> toGlassMediaData(isActive, isPlaying)
    is Playlist -> toGlassMediaData(isActive, isPlaying)
}

fun MediaMetadata.toHeroContent(): HeroContent = HeroContent(
    id = id,
    title = title,
    subtitle = artists.joinToString(", ") { it.name },
    thumbnailUrl = thumbnailUrl,
    type = HeroContentType.Primary,
)
