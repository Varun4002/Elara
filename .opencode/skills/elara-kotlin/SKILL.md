---
name: elara-kotlin
description: Kotlin language conventions, idioms, Coroutines, and Flow patterns for Elara. Use when writing Kotlin code, handling concurrency, managing coroutine scopes, or implementing reactive streams with StateFlow/SharedFlow. Covers language best practices, null safety, sealed classes, and extension functions.
---

# Elara Kotlin Conventions

## Language Version & Toolchain

- **Kotlin version:** 2.x (as defined in `gradle/libs.versions.toml`)
- **JVM target:** 21
- **Compiler args:** `-Xannotation-default-target=param-property`, `-opt-in=kotlin.RequiresOptIn`

## Style & Idioms

### Naming Conventions

| Category | Convention | Example |
|---|---|---|
| Packages | `com.elara.music.<layer>.<feature>` | `com.elara.music.ui.player` |
| Classes | PascalCase | `PlayerViewModel` |
| Functions | camelCase | `togglePlayPause()` |
| Composables | PascalCase | `GlassSurface` |
| State flows | `_` prefixed for private | `_uiState: MutableStateFlow` |
| Public flows | No prefix | `uiState: StateFlow` |
| Constants | UPPER_SNAKE_CASE | `DEFAULT_BLUR_RADIUS` |
| Compose Modifier | `modifier` parameter | `modifier: Modifier = Modifier` |

### Null Safety

```kotlin
// Prefer immutable, non-null properties
lateinit var serviceConnection: MusicServiceConnection

// Use nullable only when necessary with explicit scoping
fun updatePlaybackState(state: PlaybackState?) {
    state?.let { /* handle non-null */ } ?: run { /* handle null */ }
}

// Avoid !! — use scoping functions or safe casts
val duration = player?.duration ?: 0L
```

### Sealed Classes for State

```kotlin
sealed interface PlayerState {
    data object Idle : PlayerState
    data class Playing(val media: MediaMetadata) : PlayerState
    data class Paused(val media: MediaMetadata, val position: Long) : PlayerState
    data class Error(val message: String) : PlayerState
}
```

### Extension Functions

```kotlin
// Prefer extension functions over utility classes
fun Long.toFormattedDuration(): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return "%d:%02d".format(minutes, seconds)
}

fun Modifier.glassBlur(radius: Dp = 24.dp): Modifier = this.then(
    // glass effect implementation
)
```

## Coroutines

### Dispatchers

- **Main** — UI work, Compose state reads
- **IO** — Network, database, file I/O
- **Default** — CPU-intensive work (parsing, Palette extraction)
- **Unconfined** — Avoid unless in tests

### ViewModel Scope

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.homeContent
                .catch { e -> _uiState.value = HomeUiState.Error(e.message ?: "Unknown") }
                .collect { content -> _uiState.value = HomeUiState.Success(content) }
        }
    }
}
```

### Lifecycle-Aware Collection

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Use collectAsStateWithLifecycle(), NOT collectAsState()
}
```

### Structured Concurrency Patterns

```kotlin
// Parallel decomposition
viewModelScope.launch {
    val (home, library) = awaitAll(
        async { repository.getHomeContent() },
        async { repository.getLibrary() }
    )
}

// Timeout pattern
viewModelScope.launch {
    withTimeout(5000L) {
        repository.fetchPlaylist(id)
    }
}
```

## Flow Patterns

### StateFlow vs SharedFlow

| Flow Type | When to Use |
|---|---|
| `StateFlow` | UI state — has current value, collectors see latest |
| `SharedFlow` | One-shot events (snackbar, navigation) |
| `MutableStateFlow` | Private state backing field |

### Event Channel (One-Shot Events)

```kotlin
// Event wrapper to prevent re-delivery
class EventBus {
    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun send(event: UiEvent) {
        _events.tryEmit(event)
    }
}

// In ViewModel
private val _navigation = MutableSharedFlow<String>()
val navigation: SharedFlow<String> = _navigation.asSharedFlow()

fun openPlaylist(id: String) {
        viewModelScope.launch { _navigation.emit("playlist/$id") }
}
```

### Combining Flows

```kotlin
val combinedState = combine(
    playerState,
    playbackProgress,
    themePreferences
) { state, progress, theme ->
    PlayerUiState(
        isPlaying = state.isPlaying,
        currentPosition = progress.position,
        duration = progress.duration,
        themeMode = theme.mode
    )
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = PlayerUiState()
)
```

## Resource Management

```kotlin
// Use use() for Closeable resources
fun loadConfig(): String = assetManager.open("player_configs.json").use { stream ->
    stream.bufferedReader().readText()
}

// Auto-dispose with Lifecycle
@Composable
fun rememberMusicService(): MusicServiceConnection {
    val context = LocalContext.current
    return remember {
        MusicServiceConnection(context)
    }.also { connection ->
        DisposableEffect(Unit) {
            connection.connect()
            onDispose { connection.disconnect() }
        }
    }
}
```

## Compose-Specific Kotlin

### Remember Patterns

```kotlin
// Mutable state
var isVisible by remember { mutableStateOf(false) }

// Derived state
val isPlaying by remember { derivedStateOf { playerState.value == PlayerState.Playing } }

// Calculation on first composition
val formattedTime = remember(currentPosition) { currentPosition.toFormattedDuration() }
```

### Lambda Stability

```kotlin
// Mark stable parameters to prevent recomposition
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,  // Stable: will not cause recomposition
    modifier: Modifier = Modifier
) {
    // ...
}
```

## Gradle Dependencies

All dependency versions are in `gradle/libs.versions.toml`. Do not hardcode versions in module `build.gradle.kts` files. Reference via:

```kotlin
implementation(libs.hilt)
implementation(libs.compose.ui)
implementation(project(":innertube"))
```

## Key Libraries

| Library | Usage |
|---|---|
| Ktor | HTTP client for InnerTube API |
| Coil | Image loading in Compose |
| Room | Local database |
| Hilt | Dependency injection |
| Media3 | Audio/video playback |
| DataStore | Key-value preferences |
| materialKolor | Material You dynamic color |
| Kotlin Serialization | JSON parsing |

## Prohibited Patterns

- **Global scope** — No `GlobalScope.launch` or `GlobalScope.async`
- **Thread sleep** — No `Thread.sleep()` in coroutines (use `delay()`)
- **Blocking on main** — No `runBlocking` in UI code
- **!! operator** — Avoid unless in tests with explicit justification
- **Mutable static state** — No `object` with mutable state (use Hilt singletons)
- **Raw AsyncTask** — Deprecated; use coroutines
