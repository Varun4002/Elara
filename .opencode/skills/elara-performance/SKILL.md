---
name: elara-performance
description: Android performance optimization patterns for Elara — Compose recomposition, memory management, startup optimization, battery efficiency, and profiling. Use when debugging performance issues, optimizing scroll performance, reducing APK size, or improving battery life.
---

# Elara Performance Guide

## Performance Targets

| Metric | Target |
|---|---|
| UI frame rate | 60fps minimum, 120fps target |
| Cold start | < 2s to interactive |
| Memory (baseline) | < 150MB RSS |
| APK size | < 25MB (universal) |
| Scroll jank | < 5 frames dropped per 1000 |
| Image decode | < 50ms per image on mid-range devices |

## Compose Performance

### Avoid Recompositions

```kotlin
// BAD: Lambda creates new instance every recomposition
@Composable
fun BadExample() {
    val state by viewModel.uiState.collectAsState()
    GlassButton(onClick = { viewModel.doSomething(state.id) })  // New lambda = recomposition
}

// GOOD: Stable lambda reference
@Composable
fun GoodExample() {
    val state by viewModel.uiState.collectAsState()
    val onPlay = remember { { viewModel.togglePlayPause() } }
    GlassButton(onClick = onPlay)  // Stable = no recomposition
}

// BEST: Use composable lambdas properly
@Composable
fun BestExample(onPlay: () -> Unit) {  // Lambda passed from parent (stable)
    GlassButton(onClick = onPlay)
}
```

### Stability Annotations

```kotlin
// Mark data classes as @Stable (immutable = stable)
@Stable
data class PlayerUiState(
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val title: String = ""
)

// NOT stable: interfaces with open functions
// NOT stable: classes with mutable properties
```

### Key Composables Properly

```kotlin
LazyColumn {
    items(songs, key = { it.id }) { song ->  // Key prevents full re-layout
        SongCard(song)
    }
}
```

### Avoid Measurably Expensive Operations

```kotlin
// BAD: Expensive operation in composable body
@Composable
fun SongCard(song: Song) {
    val formatted = song.publishedDate.toFormattedString()  // Runs every recomposition
}

// GOOD: Use remember + key
@Composable
fun SongCard(song: Song) {
    val formatted = remember(song.publishedDate) { song.publishedDate.toFormattedString() }
}

// BETTER: Move to ViewModel
// In ViewModel: val formattedDate = date.toFormattedString() // computed once
```

## Image Loading

```kotlin
// Efficient Coil configuration
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(url)
        .size(480)            // Limit decode size
        .crossfade(true)      // Smooth appearance
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build(),
    contentScale = ContentScale.Crop,
    contentDescription = null
)

// Preload thumbnails
LaunchedEffect(Unit) {
    val loader = ImageLoader(LocalContext.current)
    songs.forEach { song ->
        loader.enqueue(ImageRequest.Builder(context).data(song.thumbnail).size(200).build())
    }
}
```

## Startup Optimization

```kotlin
// App.kt — Lazy initialization for non-critical services
class App : Application() {
    lateinit var imageLoader: ImageLoader
        private set

    override fun onCreate() {
        super.onCreate()
        // 1. Initialize critical services synchronously
        installShutdownHandler()

        // 2. Defer non-critical initialization to a coroutine
        CoroutineScope(Dispatchers.Default).launch {
            initializeMusicService()
            warmUpCache()
        }
    }

    private suspend fun warmUpCache() {
        // Pre-load home screen data
        withContext(Dispatchers.IO) {
            database.dao().getHomeContentCached()
        }
    }
}
```

### Baseline Profiles

```kotlin
// Generate baseline profiles for AOT compilation
// in src/main/baseline-prof.txt
HSPLcom/elara/music/MainActivity;->onCreate(Landroid/os/Bundle;)V
HSPLcom/elara/music/ui/theme/ElaraThemeKt;->ElaraTheme(ZLkotlin/jvm/functions/Function2;Landroidx/compose/runtime/Composer;II)V
HSPLcom/elara/music/ui/player/PlayerScreenKt;->PlayerScreen(Ljava/lang/String;Lkotlin/jvm/functions/Function0;Landroidx/compose/runtime/Composer;II)V
```

## Memory Optimization

```kotlin
// Avoid holding references to large objects
@Composable
fun VideoScreen() {
    // BAD: State holds entire bitmap
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // GOOD: Only hold URL, Coil manages the bitmap
    val imageUrl = remember { "https://..." }
}

// Clear large caches on low memory
class App : Application(), ComponentCallbacks2 {
    override fun onTrimMemory(level: Int) {
        if (level >= TRIM_MEMORY_MODERATE) {
            Coil.imageLoader(this).memoryCache?.clear()
        }
    }
}
```

## Battery Optimization

```kotlin
// Respect Doze and App Standby
class MusicService : MediaSessionService() {
    // Use foreground service with proper FGS type
    // FGS type: mediaPlayback (for music), dataSync (for downloads)

    // Minimize wake locks
    // Media3 handles wake locks automatically via AudioAttributes
}

// Batch network requests
// Room + Ktor: cache responses, batch when possible

// Defer non-urgent work
val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()
WorkManager.getInstance(context).enqueue(workRequest)
```

## APK Size Optimization

```kotlin
// Enable R8 full mode (in build.gradle.kts)
release {
    isMinifyEnabled = true
    isShrinkResources = true
}

// Use WebP instead of PNG
// Already configured: app launcher icons use WebP

// Remove unused resources
// ShrinkResources + resource shrinking config

// Use ABI splits (already configured: arm64-v8a, armeabi-v7a)
android.defaultConfig.ndk.abiFilters += listOf("arm64-v8a", "armeabi-v7a")
```

## Profiling Tools

| Tool | When to Use |
|---|---|
| **Android Studio Profiler** | CPU, memory, network, energy |
| **Compose Layout Inspector** | Recomposition counts, layout passes |
| **Macrobenchmark** | Startup, scroll, user journey perf |
| **Baseline Profile Generator** | Generate profiles for AOT |
| **R8 Playground** | ProGuard/R8 rules testing |
| **Perfetto** | Deep system tracing |

## ProGuard & R8

```kotlin
# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Media3
-keep class androidx.media3.** { *; }
```

## Key Metrics to Monitor

| Metric | Tool | Action if Degraded |
|---|---|---|
| Frame timing | Perfetto / Macrobenchmark | Review recompositions, reduce hierarchy depth |
| PSS memory | Android Profiler | Check bitmap sizes, cache limits |
| Cold start time | Macrobenchmark | Add baseline profiles, defer init |
| Network latency | Network profiler | Add caching, batch requests |
| Battery drain | Energy profiler | Check wake locks, network frequency |
