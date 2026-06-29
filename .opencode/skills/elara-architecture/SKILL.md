---
name: elara-architecture
description: MVVM + Clean Architecture patterns, Hilt DI, Navigation Compose, and modular project structure for Elara. Use when designing new features, adding dependencies, wiring navigation, or structuring screens. Covers repository pattern, use cases, ViewModel factories, and module boundaries.
---

# Elara Architecture Guide

## Overview

Elara follows **MVVM + Repository** pattern with **Clean Architecture** layers enforced at module level. Each feature has its own package with clear separation between UI, domain, and data concerns.

## Architecture Layers

```
┌─────────────────────────────────────┐
│         UI Layer (Compose)          │
│  Screens / Components / ViewModels  │
├─────────────────────────────────────┤
│       Domain Layer (optional)       │
│  UseCases / Models / Repositories   │
├─────────────────────────────────────┤
│        Data Layer (app module)      │
│  Repository Impl / DataSources      │
├─────────────────────────────────────┤
│        Service / Library Modules    │
│  innertube / kugou / lastfm / etc.  │
└─────────────────────────────────────┘
```

## Module Structure

| Module | Purpose | Depends On |
|---|---|---|
| `:app` | Main app: UI, DI, navigation, data, playback | All modules |
| `:innertube` | YouTube Music InnerTube API client | None |
| `:kugou` | KuGou lyrics provider | None |
| `:lrclib` | LrcLib lyrics provider | None |
| `:lastfm` | LastFM scrobbling | None |
| `:betterlyrics` | Better Lyrics (TTML) | None |
| `:shazamkit` | Shazam music recognition | None |
| `:paxsenix` | Additional lyrics provider | None |
| `:metroproto` | Protobuf definitions (submodule) | None |

## Dependency Injection (Hilt)

### Module Structure

```kotlin
// di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
}
```

### Scoping

| Scope | Lifespan | Usage |
|---|---|---|
| `@Singleton` | App process | Database, HTTP client, repos |
| `@ViewModelScoped` | ViewModel lifespan | Per-screen use cases |
| `@ActivityScoped` | Activity lifecycle | Navigation, player state |
| `@FragmentScoped` | Fragment lifecycle | (not used — Compose only) |

### ViewModel Injection

```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: MusicPlayerController,
    private val musicRepository: MusicRepository
) : ViewModel()
```

## Navigation (Compose)

### Route Definition

```kotlin
// ui/navigation/Route.kt
sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Search : Route("search")
    data class Player(val mediaId: String) : Route("player/{mediaId}") {
        companion object {
            const val pattern = "player/{mediaId}"
            fun createRoute(mediaId: String) = "player/$mediaId"
        }
    }
    data object Library : Route("library")
    data class Playlist(val id: String) : Route("playlist/{id}") {
        companion object {
            const val pattern = "playlist/{id}"
            fun createRoute(id: String) = "playlist/$id"
        }
    }
}
```

### NavHost Setup

```kotlin
@Composable
fun ElaraNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.route,
        modifier = modifier
    ) {
        composable(Route.Home.route) { HomeScreen(onNavigateToPlayer = { /*...*/ }) }
        composable(
            route = Route.Player.pattern,
            arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
        ) { backStackEntry ->
            PlayerScreen(
                mediaId = backStackEntry.arguments?.getString("mediaId") ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

## Repository Pattern

```kotlin
// domain/repository/MusicRepository.kt (interface)
interface MusicRepository {
    fun getHomeContent(): Flow<HomeContent>
    suspend fun search(query: String): List<SearchResult>
    fun getPlaylist(id: String): Flow<Playlist>
}

// data/repository/MusicRepositoryImpl.kt (implementation)
class MusicRepositoryImpl @Inject constructor(
    private val innertube: InnerTube,
    private val database: MusicDatabase
) : MusicRepository {
    override fun getHomeContent(): Flow<HomeContent> = flow {
        // 1. Emit cached data
        val cached = database.getHomeContent()
        if (cached != null) emit(cached.toHomeContent())

        // 2. Fetch fresh data
        val fresh = innertube.getHomeContent()
        database.cacheHomeContent(fresh)
        emit(fresh.toHomeContent())
    }
}
```

## ViewModel Pattern

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getHomeContent()
                .catch { error -> _uiState.value = HomeUiState.Error(error) }
                .collect { content -> _uiState.value = HomeUiState.Success(content) }
        }
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val content: HomeContent) : HomeUiState
    data class Error(val exception: Throwable) : HomeUiState
}
```

## Composable Screen Pattern

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToPlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ElaraTheme {
        Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
            when (val state = uiState) {
                is HomeUiState.Loading -> GlassShimmer()
                is HomeUiState.Success -> HomeContent(
                    content = state.content,
                    onItemClick = onNavigateToPlayer
                )
                is HomeUiState.Error -> ErrorScreen(
                    message = state.exception.message,
                    onRetry = { viewModel.refresh() }
                )
            }
        }
    }
}
```

## Media3 Playback Architecture

```
┌──────────────────────┐
│   MusicService.kt    │  ← Foreground service (Media3 Session)
│  MediaSessionService │
└────────┬─────────────┘
         │ binds to
┌────────▼─────────────┐
│   PlayerViewModel    │  ← Manages playback state via MediaController
│  (connected to app)  │
└────────┬─────────────┘
         │ collects
┌────────▼─────────────┐
│  PlayerScreen.kt      │  ← UI layer (Compose)
│  Glass controls       │
└──────────────────────┘
```

## Feature Package Structure

Each feature follows this structure:

```
ui/
├── player/
│   ├── PlayerScreen.kt           # Composable entry point
│   ├── PlayerViewModel.kt        # Hilt ViewModel
│   ├── PlayerUiState.kt          # Sealed interface
│   ├── components/
│   │   ├── TopBar.kt
│   │   ├── CenterControls.kt
│   │   ├── ProgressSection.kt
│   │   └── BottomDock.kt
│   └── PlayerNavigation.kt       # Route constants
```

## State Management Guidelines

| Concern | Mechanism |
|---|---|
| UI state | `StateFlow` in ViewModel → `collectAsStateWithLifecycle()` |
| One-shot events | `SharedFlow` → `LaunchedEffect` to consume |
| Preferences | DataStore → Flow in ViewModel |
| Media playback | MediaController callback → StateFlow |
| Theme | DataStore → Compose theme recomposition |
| Network status | `NetworkConnectivityObserver` → StateFlow |
| DI scoped state | Hilt `@ViewModelScoped` or `@ActivityScoped` |

## Rules

1. **ViewModels never hold Context references** — Use `@ApplicationContext` if absolutely needed
2. **ViewModels never know about Compose** — No `@Composable` annotations in ViewModels
3. **Repository is the single source of truth** — No direct API/Database calls in ViewModels
4. **Use cases are optional** — For simple fetch-and-display, repository → ViewModel is sufficient
5. **One ViewModel per screen** — Do not share ViewModels across screens (use SavedStateHandle for navigation args)
6. **Presentation models** — Map domain models to UI models in ViewModel or a mapper layer
