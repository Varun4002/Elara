package com.elara.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elara.music.LocalNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage

import com.elara.innertube.models.AlbumItem
import com.elara.innertube.models.ArtistItem
import com.elara.innertube.models.EpisodeItem
import com.elara.innertube.models.PlaylistItem
import com.elara.innertube.models.PodcastItem
import com.elara.innertube.models.SongItem
import com.elara.innertube.models.WatchEndpoint
import com.elara.innertube.models.YTItem
import com.elara.innertube.pages.HomePage
import com.elara.innertube.utils.completed
import com.elara.innertube.utils.parseCookieString
import com.elara.music.LocalDatabase
import com.elara.music.LocalListenTogetherManager
import com.elara.music.LocalPlayerAwareWindowInsets
import com.elara.music.LocalPlayerConnection
import com.elara.music.R
import com.elara.music.constants.AutoRadioQueueKey
import com.elara.music.constants.GridItemSize
import com.elara.music.constants.GridItemsSizeKey
import com.elara.music.constants.GridThumbnailHeight
import com.elara.music.constants.InnerTubeCookieKey
import com.elara.music.constants.ListItemHeight
import com.elara.music.constants.ListThumbnailSize
import com.elara.music.constants.RandomizeHomeOrderKey
import com.elara.music.constants.SmallGridThumbnailHeight
import com.elara.music.constants.ThumbnailCornerRadius
import com.elara.music.db.entities.Album
import com.elara.music.db.entities.Artist
import com.elara.music.db.entities.LocalItem
import com.elara.music.db.entities.Playlist
import com.elara.music.db.entities.Song
import com.elara.music.extensions.toMediaItem
import com.elara.music.models.toMediaMetadata
import com.elara.music.playback.queues.ListQueue
import com.elara.music.playback.queues.LocalAlbumRadio
import com.elara.music.playback.queues.YouTubeAlbumRadio
import com.elara.music.playback.queues.YouTubeQueue
import com.elara.music.ui.component.ChipsRow
import com.elara.music.ui.component.HideOnScrollFAB
import com.elara.music.ui.component.LocalBottomSheetPageState
import com.elara.music.ui.component.LocalMenuState
import com.elara.music.ui.component.NavigationTitle
import com.elara.music.ui.component.RandomizeGridItem
import com.elara.music.ui.component.SpeedDialGridItem
import com.elara.music.ui.component.YouTubeGridItem
import com.elara.music.ui.component.shimmer.GridItemPlaceHolder
import com.elara.music.ui.component.shimmer.ShimmerHost
import com.elara.music.ui.component.shimmer.TextPlaceholder
import com.elara.music.ui.home.CarouselStyle
import com.elara.music.ui.home.ContinueListeningSection
import com.elara.music.ui.home.GlassMediaData
import com.elara.music.ui.home.GlassMediaCard
import com.elara.music.ui.home.GlassMediaGrid
import com.elara.music.ui.home.GlassNavigationTitle
import com.elara.music.ui.home.GlassQuickActionChips
import com.elara.music.ui.home.HeroPager
import com.elara.music.ui.home.HomeBackground
import com.elara.music.ui.home.HomeHeroEngine
import com.elara.music.ui.home.HomeScrollState
import com.elara.music.ui.home.RecommendationCarousel
import com.elara.music.ui.home.toGlassMediaData
import com.elara.music.ui.home.toHeroContent
import com.elara.music.ui.menu.SongMenu
import com.elara.music.ui.menu.YouTubeAlbumMenu
import com.elara.music.ui.menu.YouTubeArtistMenu
import com.elara.music.ui.menu.YouTubePlaylistMenu
import com.elara.music.ui.menu.YouTubeSongMenu
import com.elara.music.ui.utils.SnapLayoutInfoProvider
import com.elara.music.utils.joinByBullet
import com.elara.music.utils.joinToArtistString
import com.elara.music.utils.makeTimeString
import com.elara.music.utils.rememberEnumPreference
import com.elara.music.utils.rememberPreference
import com.elara.music.viewmodels.CommunityPlaylistItem
import com.elara.music.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

sealed class HomeSection(
    val id: String,
    val baseWeight: Int,
) {
    data object SpeedDial : HomeSection("speed_dial", 100)
    data object QuickPicks : HomeSection("quick_picks", 90)
    data object DailyDiscover : HomeSection("daily_discover", 80)
    data object KeepListening : HomeSection("keep_listening", 50)
    data object AccountPlaylists : HomeSection("account_playlists", 40)
    data object ForgottenFavorites : HomeSection("forgotten_favorites", 30)
    data object FromTheCommunity : HomeSection("from_the_community", 20)
    data class SimilarRecommendation(val index: Int) : HomeSection("similar_recommendation_$index", 10)
    data class HomePageSection(val index: Int) : HomeSection("home_page_section_$index", 10)
    data object MoodAndGenres : HomeSection("mood_and_genres", 5)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val quickPicks by viewModel.quickPicks.collectAsStateWithLifecycle()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsStateWithLifecycle()
    val keepListening by viewModel.keepListening.collectAsStateWithLifecycle()
    val similarRecommendations by viewModel.similarRecommendations.collectAsStateWithLifecycle()
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val explorePage by viewModel.explorePage.collectAsStateWithLifecycle()
    val dailyDiscover by viewModel.dailyDiscover.collectAsStateWithLifecycle()
    val communityPlaylists by viewModel.communityPlaylists.collectAsStateWithLifecycle()

    val allLocalItems by viewModel.allLocalItems.collectAsStateWithLifecycle()
    val allYtItems by viewModel.allYtItems.collectAsStateWithLifecycle()
    val speedDialItems by viewModel.speedDialItems.collectAsStateWithLifecycle()
    val pinnedSpeedDialItems by viewModel.pinnedSpeedDialItems.collectAsStateWithLifecycle()
    val selectedChip by viewModel.selectedChip.collectAsStateWithLifecycle()

    val savedPodcastShows by viewModel.savedPodcastShows.collectAsStateWithLifecycle()
    val episodesForLater by viewModel.episodesForLater.collectAsStateWithLifecycle()

    val isLoading: Boolean by viewModel.isLoading.collectAsStateWithLifecycle()
    val isMoodAndGenresLoading = isLoading && explorePage?.moodAndGenres == null
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isRandomizing by viewModel.isRandomizing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val (randomizeHomeOrder) = rememberPreference(RandomizeHomeOrderKey, true)
    val autoRadioQueue by rememberPreference(AutoRadioQueueKey, defaultValue = true)

    LaunchedEffect(Unit) { viewModel.loadHomeData() }

    val shouldShowWrappedCard by viewModel.showWrappedCard.collectAsStateWithLifecycle()
    val wrappedState by viewModel.wrappedManager.state.collectAsStateWithLifecycle()
    val isWrappedDataReady = wrappedState.isDataReady

    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val url = if (isLoggedIn) accountImageUrl else null

    var cachedPodcasts by remember { mutableStateOf<List<PodcastItem>>(emptyList()) }

    val featuredPodcasts = remember(homePage, selectedChip) {
        if (selectedChip == null) {
            cachedPodcasts = emptyList()
            emptyList()
        } else {
            val newPodcasts = homePage?.sections?.flatMap { it.items }
                ?.filterIsInstance<EpisodeItem>()
                ?.mapNotNull { episode -> episode.podcast?.let { podcast -> PodcastItem(id = podcast.id, title = podcast.name, author = episode.author, episodeCountText = null, thumbnail = episode.thumbnail, playEndpoint = null, shuffleEndpoint = null) } }
                ?.distinctBy { it.id }?.shuffled()?.take(10) ?: emptyList()
            if (newPodcasts.isNotEmpty()) cachedPodcasts = newPodcasts
            cachedPodcasts
        }
    }

    val scope = rememberCoroutineScope()
    var randomizeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val lazylistState = rememberLazyListState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val currentGridHeight = if (gridItemSize == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsStateWithLifecycle()

    val wrappedDismissed by backStackEntry?.savedStateHandle?.getStateFlow("wrapped_seen", false)?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    var randomSeed by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isRefreshing) { if (isRefreshing) randomSeed = System.currentTimeMillis() }

    val foundInSettings = stringResource(R.string.found_in_settings_content)
    LaunchedEffect(wrappedDismissed) {
        if (wrappedDismissed) {
            viewModel.markWrappedAsSeen()
            scope.launch { snackbarHostState.showSnackbar(foundInSettings) }
            backStackEntry?.savedStateHandle?.set("wrapped_seen", false)
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) { lazylistState.animateScrollToItem(0); backStackEntry?.savedStateHandle?.set("scrollToTop", false) }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect { lastVisibleIndex ->
            val len = lazylistState.layoutInfo.totalItemsCount
            if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) viewModel.loadMoreYouTubeItems(homePage?.continuation)
        }
    }

    if (selectedChip != null) BackHandler { viewModel.toggleChip(selectedChip) }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(item = item, isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id), isPlaying = isPlaying, coroutineScope = scope, thumbnailRatio = 1f, modifier = Modifier.combinedClickable(onClick = {
            when (item) {
                is SongItem -> { if (!isListenTogetherGuest) playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()) else ListQueue(title = item.title, items = listOf(item.toMediaItem()))) }
                is AlbumItem -> navController.navigate("album/${item.id}")
                is ArtistItem -> navController.navigate("artist/${item.id}")
                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                is EpisodeItem -> { if (!isListenTogetherGuest) playerConnection.playQueue(ListQueue(title = item.title, items = listOf(item.toMediaMetadata().toMediaItem()))) }
            }
        }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show {
            when (item) { is SongItem -> YouTubeSongMenu(song = item, onDismiss = menuState::dismiss); is AlbumItem -> YouTubeAlbumMenu(albumItem = item, onDismiss = menuState::dismiss); is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss); is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss); is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = scope, onDismiss = menuState::dismiss); is EpisodeItem -> YouTubeSongMenu(song = item.asSongItem(), onDismiss = menuState::dismiss) }
        } }))
    }

    // Hero content
    val heroContents = remember(quickPicks, keepListening, speedDialItems, mediaMetadata, homePage) {
        HomeHeroEngine.compute(quickPicks, keepListening, speedDialItems, mediaMetadata, homePage)
    }

    // Scroll state
    val collapseThresholdPx = with(LocalDensity.current) { 460.dp.toPx() }
    val homeScrollState by remember {
        derivedStateOf {
            val info = lazylistState.layoutInfo
            val scrollOffset = if (info.visibleItemsInfo.isNotEmpty() && info.visibleItemsInfo.first().index == 0) {
                info.visibleItemsInfo.first().offset
            } else if (info.visibleItemsInfo.isNotEmpty() && info.visibleItemsInfo.first().index > 0) {
                0
            } else {
                0
            }
            val collapse = (-scrollOffset.toFloat() / collapseThresholdPx).coerceIn(0f, 1f)

            // Hero is visible when first item (hero index 0) is on screen or scroll is at top
            val firstVisible = info.visibleItemsInfo.firstOrNull()
            val heroVisible = firstVisible == null || firstVisible.index <= 1

            HomeScrollState(
                collapseProgress = collapse,
                heroVisibleFraction = if (heroVisible) 1f - collapse else 0f,
                toolbarOpacity = collapse,
                ambientIntensity = if (heroVisible) 1f - collapse * 0.5f else 0.3f,
            )
        }
    }

    // Glass data conversions
    val quickPickMediaData = remember(quickPicks, mediaMetadata, isPlaying) {
        quickPicks?.map { it.toGlassMediaData(isActive = it.id == mediaMetadata?.id, isPlaying = isPlaying) } ?: emptyList()
    }

    val keepListeningMediaData = remember(keepListening, mediaMetadata, isPlaying) {
        keepListening?.map { localItem ->
            val isActive = when (localItem) { is Song -> localItem.id == mediaMetadata?.id; else -> false }
            localItem.toGlassMediaData(isActive = isActive, isPlaying = isPlaying && isActive)
        } ?: emptyList()
    }

    val dailyDiscoverMediaData = remember(dailyDiscover) {
        dailyDiscover?.map { item -> item.recommendation.toGlassMediaData() } ?: emptyList()
    }

    val communityMediaData = remember(communityPlaylists) {
        communityPlaylists?.flatMap { playlist -> playlist.songs.map { it.toGlassMediaData() } } ?: emptyList()
    }

    val forgottenFavoritesMediaData = remember(forgottenFavorites, mediaMetadata, isPlaying) {
        forgottenFavorites?.map { it.toGlassMediaData(isActive = it.id == mediaMetadata?.id, isPlaying = isPlaying) } ?: emptyList()
    }

    val accountPlaylistsMediaData = remember(accountPlaylists, mediaMetadata, isPlaying) {
        accountPlaylists?.map { it.toGlassMediaData(isActive = it.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id), isPlaying = isPlaying) } ?: emptyList()
    }

    val homeSections = remember(
        randomizeHomeOrder, randomSeed, selectedChip, speedDialItems, quickPicks, dailyDiscover, keepListening, accountPlaylists, forgottenFavorites, communityPlaylists, similarRecommendations, homePage?.sections, explorePage?.moodAndGenres,
    ) {
        val list = mutableListOf<HomeSection>()
        val chipActive = selectedChip != null
        if (!chipActive && speedDialItems.isNotEmpty()) list.add(HomeSection.SpeedDial)
        if (!chipActive && quickPicks?.isNotEmpty() == true) list.add(HomeSection.QuickPicks)
        if (!chipActive && communityPlaylists?.isNotEmpty() == true) list.add(HomeSection.FromTheCommunity)
        if (!chipActive && dailyDiscover?.isNotEmpty() == true) list.add(HomeSection.DailyDiscover)
        if (!chipActive && keepListening?.isNotEmpty() == true) list.add(HomeSection.KeepListening)
        if (!chipActive && accountPlaylists?.isNotEmpty() == true) list.add(HomeSection.AccountPlaylists)
        if (!chipActive && forgottenFavorites?.isNotEmpty() == true) list.add(HomeSection.ForgottenFavorites)
        if (!chipActive) similarRecommendations?.indices?.forEach { i -> list.add(HomeSection.SimilarRecommendation(i)) }
        homePage?.sections?.indices?.forEach { i -> list.add(HomeSection.HomePageSection(i)) }
        if (explorePage?.moodAndGenres != null) list.add(HomeSection.MoodAndGenres)
        if (randomizeHomeOrder) {
            list.sortedByDescending { section ->
                val sectionRandom = Random(randomSeed + section.id.hashCode())
                val base = when (section) { HomeSection.SpeedDial, HomeSection.QuickPicks, HomeSection.DailyDiscover -> 500; HomeSection.KeepListening, HomeSection.AccountPlaylists, HomeSection.ForgottenFavorites, HomeSection.FromTheCommunity -> 300; else -> 100 }
                val modifier = when (section) { HomeSection.SpeedDial, HomeSection.QuickPicks, HomeSection.DailyDiscover -> sectionRandom.nextInt(-200, 400); HomeSection.KeepListening, HomeSection.AccountPlaylists, HomeSection.ForgottenFavorites, HomeSection.FromTheCommunity -> sectionRandom.nextInt(-100, 400); else -> sectionRandom.nextInt(-50, 50) }
                base + modifier
            }
        } else {
            val defaultOrder = mapOf(HomeSection.SpeedDial to 100, HomeSection.QuickPicks to 90, HomeSection.FromTheCommunity to 80, HomeSection.DailyDiscover to 70, HomeSection.KeepListening to 60, HomeSection.AccountPlaylists to 50, HomeSection.ForgottenFavorites to 40, HomeSection.MoodAndGenres to 10)
            list.sortedByDescending { section -> when (section) { is HomeSection.SimilarRecommendation -> 30 - section.index; is HomeSection.HomePageSection -> 20 - section.index; else -> defaultOrder[section] ?: 0 } }
        }
    }

    LaunchedEffect(quickPicks) { quickPicksLazyGridState.scrollToItem(0) }
    LaunchedEffect(forgottenFavorites) { forgottenFavoritesLazyGridState.scrollToItem(0) }

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        indicator = { Indicator(isRefreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())) },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) { SnapLayoutInfoProvider(lazyGridState = quickPicksLazyGridState, positionInLayout = { layoutSize, itemSize -> (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f) }) }
            val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) { SnapLayoutInfoProvider(lazyGridState = forgottenFavoritesLazyGridState, positionInLayout = { layoutSize, itemSize -> (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f) }) }

            // Hero background
            val heroGradColors = remember(heroContents) {
                heroContents.firstOrNull()?.gradientColors ?: emptyList()
            }
            HomeBackground(
                gradientColors = heroGradColors,
                ambientIntensity = homeScrollState.ambientIntensity,
                modifier = Modifier.fillMaxSize(),
            )

            LazyColumn(
                state = lazylistState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            ) {
                // Hero Pager
                item(key = "hero_pager") {
                    HeroPager(
                        contents = heroContents,
                        scrollState = homeScrollState,
                    )
                }

                // Glass Quick Action Chips
                item(key = "glass_chips") {
                    if (selectedChip == null) {
                        val chips = homePage?.chips?.map { chip -> com.elara.music.ui.home.ChipData(label = chip.title, isSelected = false) } ?: emptyList()
                        GlassQuickActionChips(
                            chips = chips,
                            onChipClick = { index -> homePage?.chips?.getOrNull(index)?.let { viewModel.toggleChip(it) } },
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    } else {
                        ChipsRow(
                            chips = homePage?.chips?.map { it to it.title } ?: emptyList(),
                            currentValue = selectedChip,
                            onValueUpdate = { viewModel.toggleChip(it) },
                        )
                    }
                }

                // Loading shimmer for chips
                if (isLoading && homePage?.chips.isNullOrEmpty()) {
                    item(key = "chips_shimmer") {
                        ShimmerHost(showGradient = false) {
                            LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                items(5) { TextPlaceholder(height = 30.dp, shape = RoundedCornerShape(16.dp), modifier = Modifier.width(72.dp)) }
                            }
                        }
                    }
                }

                // Podcast chip sections (keep as-is)
                if (selectedChip?.title?.contains("Podcast", ignoreCase = true) == true) {
                    if (savedPodcastShows.isNotEmpty()) {
                        item(key = "00_your_shows_title") { NavigationTitle(title = stringResource(R.string.your_shows), onClick = { navController.navigate("youtube_browse/FEmusic_library_non_music_audio_list") }) }
                        item(key = "00_your_shows_list") { LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()) { items(savedPodcastShows.distinctBy { it.id }, key = { "home_saved_podcast_${it.id}" }) { ytGridItem(it) } } }
                    }
                    if (episodesForLater.isNotEmpty()) {
                        item(key = "00_episodes_for_later_title") { NavigationTitle(title = stringResource(R.string.episodes_for_later), onClick = { navController.navigate("online_playlist/SE") }) }
                        item(key = "00_episodes_for_later_list") { LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()) { items(episodesForLater.distinctBy { it.id }, key = { "home_episode_later_${it.id}" }) { ytGridItem(it) } } }
                    }
                    if (featuredPodcasts.isNotEmpty() && savedPodcastShows.isEmpty()) {
                        item(key = "0_podcast_channels_title") { NavigationTitle(title = stringResource(R.string.podcast_channels)) }
                        item(key = "0_podcast_channels_list") { LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()) { items(featuredPodcasts.distinctBy { it.id }, key = { "home_featured_podcast_${it.id}" }) { ytGridItem(it) } } }
                    }
                    if (homeSections.filterIsInstance<HomeSection.HomePageSection>().isNotEmpty()) {
                        item(key = "0_latest_episodes_title") { NavigationTitle(title = stringResource(R.string.latest_episodes)) }
                    }
                    homeSections.filterIsInstance<HomeSection.HomePageSection>().forEach { section ->
                        val sectionData = homePage?.sections?.getOrNull(section.index)
                        val skipTitles = listOf("your shows", "episodes for later", "podcast channels", "new episodes")
                        if (sectionData?.title?.lowercase()?.let { title -> skipTitles.any { title.contains(it) } } == true) return@forEach
                        sectionData?.let {
                            item(key = "1_chip_section_title_${section.index}") {
                                NavigationTitle(title = sectionData.title, label = sectionData.label, thumbnail = sectionData.thumbnail?.let { thumbnailUrl -> { AsyncImage(model = thumbnailUrl, contentDescription = null, modifier = Modifier.size(ListThumbnailSize).clip(if (sectionData.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(ThumbnailCornerRadius))) } }, onClick = sectionData.endpoint?.let { endpoint -> { when { endpoint.browseId == "FEmusic_moods_and_genres" -> navController.navigate("mood_and_genres"); endpoint.params != null -> navController.navigate("youtube_browse/${endpoint.browseId}?params=${endpoint.params}"); else -> navController.navigate("browse/${endpoint.browseId}") } } })
                            }
                            item(key = "1_chip_section_list_${section.index}") { LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()) { items(sectionData.items.distinctBy { it.id }, key = { "home_chip_section_${it.id}" }) { ytGridItem(it) } } }
                        }
                    }
                }

                // Wrapped card
                if (selectedChip == null) {
                    item(key = "wrapped_card") {
                        AnimatedVisibility(visible = shouldShowWrappedCard) {
                            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    if (isWrappedDataReady) {
                                        val bbhFont = try { FontFamily(Font(R.font.bbh_bartle_regular)) } catch (_: Exception) { FontFamily.Default }
                                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Text(text = stringResource(R.string.wrapped_ready_title), style = MaterialTheme.typography.headlineLarge.copy(fontFamily = bbhFont, textAlign = TextAlign.Center))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = stringResource(R.string.wrapped_ready_subtitle), style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center))
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(onClick = { navController.navigate("wrapped") }) { Text(stringResource(R.string.open)) }
                                        }
                                    } else { ContainedLoadingIndicator() }
                                }
                            }
                        }
                    }
                }

                // Home sections
                homeSections.forEach { section ->
                    when (section) {
                        HomeSection.SpeedDial -> {
                            speedDialItems.takeIf { it.isNotEmpty() }?.let { items ->
                                item(key = "speed_dial_title") { NavigationTitle(title = stringResource(R.string.speed_dial)) }
                                item(key = "speed_dial_list") {
                                    val targetItemSize = 160.dp; val availableWidth = maxWidth - 32.dp; val columns = (availableWidth / targetItemSize).toInt().coerceAtLeast(3)
                                    val rows = if (columns >= 6) 1 else if (columns >= 4) 2 else 3; val itemsPerPage = columns * rows; val itemWidth = availableWidth / columns
                                    val pagerState = rememberPagerState(pageCount = { (items.size + itemsPerPage - 1) / itemsPerPage })
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        HorizontalPager(state = pagerState, contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 16.dp, modifier = Modifier.fillMaxWidth().height(itemWidth * rows)) { page ->
                                            val pageStartIndex = page * itemsPerPage
                                            val pageItems = items.drop(pageStartIndex).take(itemsPerPage)
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                for (row in 0 until rows) { Row(modifier = Modifier.fillMaxWidth()) {
                                                    for (col in 0 until columns) { val itemIndex = row * columns + col; val isRandomizeSlot = (page == 0 && itemIndex == itemsPerPage - 1)
                                                        if (isRandomizeSlot) {
                                                            Box(modifier = Modifier.width(itemWidth).height(itemWidth).padding(4.dp)) {
                                                                RandomizeGridItem(isLoading = isRandomizing, onClick = {
                                                                    if (isRandomizing) randomizeJob?.cancel()
                                                                    else if (!isListenTogetherGuest) randomizeJob = scope.launch {
                                                                        val randomItem = viewModel.getRandomItem()
                                                                        if (randomItem != null) when (randomItem) { is SongItem -> playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue(randomItem.endpoint ?: WatchEndpoint(videoId = randomItem.id), randomItem.toMediaMetadata()) else ListQueue(title = randomItem.title, items = listOf(randomItem.toMediaItem()))); is AlbumItem -> navController.navigate("album/${randomItem.id}"); is ArtistItem -> navController.navigate("artist/${randomItem.id}"); is PlaylistItem -> navController.navigate("online_playlist/${randomItem.id}"); is PodcastItem -> navController.navigate("online_podcast/${randomItem.id}"); is EpisodeItem -> playerConnection.playQueue(ListQueue(title = randomItem.title, items = listOf(randomItem.toMediaMetadata().toMediaItem()))) }
                                                                    }
                                                                })
                                                            }
                                                        } else if (itemIndex < pageItems.size) {
                                                            val item = pageItems[itemIndex]; val isPinned by database.speedDialDao.isPinned(item.id).collectAsStateWithLifecycle(initialValue = false)
                                                            Box(modifier = Modifier.width(itemWidth).height(itemWidth).padding(4.dp)) {
                                                                SpeedDialGridItem(item = item, isPinned = isPinned, isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id), isPlaying = isPlaying, modifier = Modifier.fillMaxSize().combinedClickable(onClick = { when (item) { is SongItem -> playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()) else ListQueue(title = item.title, items = listOf(item.toMediaItem()))); is AlbumItem -> navController.navigate("album/${item.id}"); is ArtistItem -> navController.navigate("artist/${item.id}"); is PlaylistItem -> { val rawType = pinnedSpeedDialItems.find { it.id == item.id }?.type; if (rawType == "LOCAL_PLAYLIST") navController.navigate("local_playlist/${item.id}") else navController.navigate("online_playlist/${item.id}") }; is PodcastItem -> navController.navigate("online_podcast/${item.id}"); is EpisodeItem -> playerConnection.playQueue(ListQueue(title = item.title, items = listOf(item.toMediaMetadata().toMediaItem()))) } }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { when (item) { is SongItem -> YouTubeSongMenu(song = item, onDismiss = menuState::dismiss); is AlbumItem -> YouTubeAlbumMenu(albumItem = item, onDismiss = menuState::dismiss); is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss); is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss); is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = scope, onDismiss = menuState::dismiss); is EpisodeItem -> YouTubeSongMenu(song = item.asSongItem(), onDismiss = menuState::dismiss) } } }))
                                                            }
                                                        } else { Spacer(modifier = Modifier.width(itemWidth)) }
                                                    } }
                                                }
                                            }
                                        }
                                        if (pagerState.pageCount > 1) { Row(modifier = Modifier.height(24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { repeat(pagerState.pageCount) { iteration -> Box(modifier = Modifier.padding(4.dp).clip(CircleShape).background(if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)).size(8.dp)) } } }
                                    }
                                }
                            }
                        }

                        HomeSection.QuickPicks -> {
                            if (quickPickMediaData.isNotEmpty()) {
                                item(key = "quick_picks_title") { GlassNavigationTitle(title = stringResource(R.string.quick_picks), onPlayAllClick = if (!isListenTogetherGuest) {{ playerConnection.playQueue(ListQueue(title = stringResource(R.string.quick_picks), items = quickPicks!!.distinctBy { it.id }.map { it.toMediaItem() })) }} else null) }
                                item(key = "quick_picks_grid") {
                                    GlassMediaGrid(
                                        items = quickPickMediaData,
                                        onClick = { index -> val song = quickPicks?.getOrNull(index) ?: return@GlassMediaGrid; if (!isListenTogetherGuest) { if (song.id == mediaMetadata?.id) playerConnection.togglePlayPause() else playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue.radio(song.toMediaMetadata()) else ListQueue(title = song.title, items = listOf(song.toMediaItem()))) } },
                                        onLongClick = { index -> val song = quickPicks?.getOrNull(index) ?: return@GlassMediaGrid; haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { SongMenu(originalSong = song, onDismiss = menuState::dismiss) } },
                                        columns = 2,
                                        mosaicLayout = true,
                                    )
                                }
                            }
                        }

                        HomeSection.FromTheCommunity -> {
                            communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                                item(key = "community_playlists_title") { GlassNavigationTitle(title = stringResource(R.string.from_the_community)) }
                                item(key = "community_playlists_content") {
                                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        items(playlists) { item ->
                                            CommunityPlaylistCard(
                                                item = item,
                                                onClick = { navController.navigate("online_playlist/${item.playlist.id.removePrefix("VL")}") },
                                                onSongClick = { song -> if (!isListenTogetherGuest) playerConnection.playQueue(YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), song.toMediaMetadata())) },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HomeSection.DailyDiscover -> {
                            if (dailyDiscoverMediaData.isNotEmpty()) {
                                item(key = "daily_discover_carousel") {
                                    RecommendationCarousel(
                                        title = stringResource(R.string.your_daily_discover),
                                        items = dailyDiscoverMediaData,
                                        onClick = { index -> val item = dailyDiscover?.getOrNull(index)?.recommendation as? SongItem ?: return@RecommendationCarousel; if (!isListenTogetherGuest) playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()) else ListQueue(title = item.title, items = listOf(item.toMediaItem()))) },
                                        style = CarouselStyle.HeroCard,
                                        onPlayAllClick = if (!isListenTogetherGuest) {{ val queueItems = dailyDiscover?.mapNotNull { (it.recommendation as? SongItem)?.toMediaMetadata() }; if (queueItems != null && queueItems.isNotEmpty()) playerConnection.playQueue(ListQueue(title = stringResource(R.string.your_daily_discover), items = queueItems.map { it.toMediaItem() })) }} else null,
                                    )
                                }
                            }
                        }

                        HomeSection.KeepListening -> {
                            if (keepListeningMediaData.isNotEmpty()) {
                                item(key = "keep_listening_section") {
                                    ContinueListeningSection(
                                        items = keepListeningMediaData,
                                        onClick = { index -> val item = keepListening?.getOrNull(index) ?: return@ContinueListeningSection; if (!isListenTogetherGuest) { if (item is Song && item.id == mediaMetadata?.id) playerConnection.togglePlayPause() else { val md = when (item) { is Song -> item.toMediaMetadata(); else -> return@ContinueListeningSection }; playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue.radio(md) else ListQueue(title = item.title, items = listOf(md.toMediaItem()))) } } },
                                        onLongClick = { index -> val item = keepListening?.getOrNull(index) ?: return@ContinueListeningSection; haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (item is Song) menuState.show { SongMenu(originalSong = item, onDismiss = menuState::dismiss) } },
                                    )
                                }
                            }
                        }

                        HomeSection.AccountPlaylists -> {
                            if (accountPlaylistsMediaData.isNotEmpty()) {
                                item(key = "account_playlists_title") { GlassNavigationTitle(title = accountName, label = stringResource(R.string.mixes)) }
                                item(key = "account_playlists_grid") {
                                    GlassMediaGrid(
                                        items = accountPlaylistsMediaData,
                                        onClick = { index -> val item = accountPlaylists?.getOrNull(index) ?: return@GlassMediaGrid; when (item) { is AlbumItem -> navController.navigate("album/${item.id}"); is ArtistItem -> navController.navigate("artist/${item.id}"); is PlaylistItem -> navController.navigate("online_playlist/${item.id}"); is PodcastItem -> navController.navigate("online_podcast/${item.id}"); else -> {} } },
                                        columns = 2,
                                    )
                                }
                            }
                        }

                        HomeSection.ForgottenFavorites -> {
                            if (forgottenFavoritesMediaData.isNotEmpty()) {
                                item(key = "forgotten_favorites_title") { GlassNavigationTitle(title = stringResource(R.string.forgotten_favorites), onPlayAllClick = if (!isListenTogetherGuest) {{ playerConnection.playQueue(ListQueue(title = stringResource(R.string.forgotten_favorites), items = forgottenFavorites!!.distinctBy { it.id }.map { it.toMediaItem() })) }} else null) }
                                item(key = "forgotten_favorites_grid") {
                                    GlassMediaGrid(
                                        items = forgottenFavoritesMediaData,
                                        onClick = { index -> val song = forgottenFavorites?.getOrNull(index) ?: return@GlassMediaGrid; if (!isListenTogetherGuest) { if (song.id == mediaMetadata?.id) playerConnection.togglePlayPause() else playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue.radio(song.toMediaMetadata()) else ListQueue(title = song.title, items = listOf(song.toMediaItem()))) } },
                                        onLongClick = { index -> val song = forgottenFavorites?.getOrNull(index) ?: return@GlassMediaGrid; haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { SongMenu(originalSong = song, onDismiss = menuState::dismiss) } },
                                        columns = 2,
                                    )
                                }
                            }
                        }

                        is HomeSection.SimilarRecommendation -> {
                            val recommendation = similarRecommendations?.getOrNull(section.index)
                            recommendation?.let {
                                item(key = "similar_to_title_${section.index}") {
                                    NavigationTitle(label = stringResource(R.string.similar_to), title = recommendation.title.title, thumbnail = recommendation.title.thumbnailUrl?.let { thumbnailUrl -> { AsyncImage(model = thumbnailUrl, contentDescription = null, modifier = Modifier.size(ListThumbnailSize).clip(if (recommendation.title is Artist) CircleShape else RoundedCornerShape(ThumbnailCornerRadius))) } }, onClick = { when (recommendation.title) { is Song -> recommendation.title.album?.let { navController.navigate("album/${it.id}") }; is Album -> navController.navigate("album/${recommendation.title.id}"); is Artist -> navController.navigate("artist/${recommendation.title.id}"); is Playlist -> {} } })
                                }
                                item(key = "similar_to_list_${section.index}") { LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()) { items(recommendation.items.distinctBy { it.id }, key = { "home_similar_${it.id}" }) { ytGridItem(it) } } }
                            }
                        }

                        is HomeSection.HomePageSection -> {
                            if (selectedChip?.title?.contains("Podcast", ignoreCase = true) == true) return@forEach
                            val sectionData = homePage?.sections?.getOrNull(section.index)
                            sectionData?.let {
                                val sectionSongs = sectionData.items.filterIsInstance<SongItem>()
                                val hasPlayableSongs = sectionSongs.isNotEmpty()
                                val isSongsOnlySection = sectionData.items.isNotEmpty() && sectionData.items.all { it is SongItem }

                                val mediaItems = remember(sectionData.items) {
                                    sectionData.items.map { ytItem ->
                                        ytItem.toGlassMediaData(
                                            isActive = ytItem.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                                            isPlaying = isPlaying,
                                        )
                                    }
                                }

                                item(key = "home_section_title_${section.index}") {
                                    GlassNavigationTitle(title = sectionData.title, label = sectionData.label, onPlayAllClick = if (hasPlayableSongs && !isListenTogetherGuest) {{ playerConnection.playQueue(ListQueue(title = sectionData.title, items = sectionSongs.map { it.toMediaMetadata().toMediaItem() })) }} else null)
                                }

                                if (isSongsOnlySection) {
                                    item(key = "home_section_grid_${section.index}") {
                                        GlassMediaGrid(
                                            items = mediaItems,
                                            onClick = { index -> val song = sectionSongs.getOrNull(index) ?: return@GlassMediaGrid; if (!isListenTogetherGuest) playerConnection.playQueue(if (autoRadioQueue) YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), song.toMediaMetadata()) else ListQueue(title = song.title, items = listOf(song.toMediaItem()))) },
                                            onLongClick = { index -> val song = sectionSongs.getOrNull(index) ?: return@GlassMediaGrid; haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { YouTubeSongMenu(song = song, onDismiss = menuState::dismiss) } },
                                            columns = 2,
                                        )
                                    }
                                } else {
                                    item(key = "home_section_list_${section.index}") {
                                        LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()) {
                                            items(mediaItems, key = { "home_section_item_${it.id}" }) { glassItem ->
                                                GlassMediaCard(
                                                    data = glassItem,
                                                    onClick = { val item = sectionData.items.find { yt -> yt.id == glassItem.id }; if (item != null) { when (item) { is AlbumItem -> navController.navigate("album/${item.id}"); is ArtistItem -> navController.navigate("artist/${item.id}"); is PlaylistItem -> navController.navigate("online_playlist/${item.id}"); is PodcastItem -> navController.navigate("online_podcast/${item.id}"); else -> {} } } },
                                                    modifier = Modifier.width(160.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HomeSection.MoodAndGenres -> {
                            if (selectedChip?.title?.contains("Podcast", ignoreCase = true) == true) return@forEach
                            explorePage?.moodAndGenres?.let { moodAndGenres ->
                                item(key = "mood_and_genres_title") { NavigationTitle(title = stringResource(R.string.mood_and_genres), onClick = { navController.navigate("mood_and_genres") }) }
                                item(key = "mood_and_genres_list") { LazyHorizontalGrid(rows = GridCells.Fixed(4), contentPadding = PaddingValues(6.dp), modifier = Modifier.height((52.dp + 12.dp) * 4 + 12.dp)) { items(moodAndGenres.distinctBy { "${it.title}_${it.endpoint.browseId}_${it.endpoint.params}" }, key = { "${it.title}_${it.endpoint.browseId}_${it.endpoint.params}" }) { MoodAndGenresButton(title = it.title, onClick = { navController.navigate("youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}") }, modifier = Modifier.padding(6.dp).width(180.dp)) } } }
                            }
                        }
                    }
                }

                // Loading shimmer
                if (isLoading && homePage?.sections.isNullOrEmpty()) {
                    item(key = "loading_shimmer") { ShimmerHost {
                        repeat(2) { TextPlaceholder(height = 36.dp, modifier = Modifier.padding(12.dp).width(250.dp)); LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()) { items(4) { GridItemPlaceHolder() } } }
                        TextPlaceholder(height = 36.dp, modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp).width(250.dp))
                        repeat(4) { Row { repeat(2) { TextPlaceholder(height = 52.dp, shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(horizontal = 12.dp).width(200.dp)) } } }
                    } }
                }
            }

            HideOnScrollFAB(
                visible = allLocalItems.isNotEmpty() || allYtItems.isNotEmpty(),
                lazyListState = lazylistState,
                icon = R.drawable.shuffle,
                onClick = {
                    if (!isListenTogetherGuest) {
                        val local = when { allLocalItems.isNotEmpty() && allYtItems.isNotEmpty() -> Random.nextFloat() < 0.5; allLocalItems.isNotEmpty() -> true; else -> false }
                        scope.launch(Dispatchers.Main) {
                            if (local) { when (val luckyItem = allLocalItems.random()) { is Song -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata())); is Album -> { val albumWithSongs = withContext(Dispatchers.IO) { database.albumWithSongs(luckyItem.id).first() }; albumWithSongs?.let { playerConnection.playQueue(LocalAlbumRadio(it)) } }; is Artist -> {}; is Playlist -> {} } }
                            else { when (val luckyItem = allYtItems.random()) { is SongItem -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata())); is AlbumItem -> playerConnection.playQueue(YouTubeAlbumRadio(luckyItem.playlistId)); is ArtistItem -> luckyItem.radioEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }; is PlaylistItem -> luckyItem.playEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }; is PodcastItem -> luckyItem.playEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }; is EpisodeItem -> playerConnection.playQueue(ListQueue(title = luckyItem.title, items = listOf(luckyItem.toMediaMetadata().toMediaItem()))) } }
                        }
                    }
                },
                onRecognitionClick = { navController.navigate("recognition") },
            )
        }
    }
}

@Composable
private fun CommunityPlaylistCard(
    item: CommunityPlaylistItem,
    onClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
) {
    Row(
        modifier = Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
    ) {
        Box(modifier = Modifier.size(120.dp)) {
            val songs = item.songs.take(4)
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0 until 2) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 2) {
                            val index = row * 2 + col
                            val song = songs.getOrNull(index)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .then(
                                        if (song != null) Modifier
                                            .combinedClickable(
                                                onClick = { onSongClick(song) },
                                            )
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (song != null) {
                                    AsyncImage(
                                        model = song.thumbnail,
                                        contentDescription = song.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (item.songs.size > 4) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topStart = 8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "+${item.songs.size - 4}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
                .align(Alignment.CenterVertically),
        ) {
            Text(
                text = item.playlist.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.playlist.author?.name ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.playlist.songCountText ?: "${item.songs.size} songs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
