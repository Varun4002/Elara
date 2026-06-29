---
name: elara-data
description: Room database, DataStore preferences, offline-first caching strategy, and local storage patterns for Elara. Use when designing database entities, writing DAOs, managing migrations, implementing caching, storing user preferences, or handling offline data. DO NOT USE when the question is about modifying the existing database schema — that is prohibited.
---

# Elara Data Layer

## Storage Stack

| Layer | Technology | Purpose |
|---|---|---|
| Relational DB | Room (SQLite) | Playlists, favorites, history, cache |
| Key-value | DataStore (Proto) | User preferences, theme settings |
| File cache | ExoPlayer cache | Downloaded media, album art cache |
| Coil cache | OkHttp disk cache | Image loading cache |

## Room Database

### Database Definition

```kotlin
@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongMap::class,
        AlbumEntity::class,
        ArtistEntity::class,
        HistoryEntity::class,
        AlbumArtistMap::class,
        ArtistPageCache::class,
        FormatEntity::class,
        SpeedDialEntity::class
    ],
    version = 38,  // DO NOT CHANGE
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun dao(): DatabaseDao
}
```

### DO NOT

- **Change the database schema** — This is strictly prohibited. Room entities and migrations are off-limits.
- **Add new entities** without explicit authorization.
- **Modify existing `@Entity` annotations** or column definitions.

### If reading data:

```kotlin
// Safe: reading via DAO
@Transaction
@Query("SELECT * FROM songs WHERE id = :songId")
abstract fun getSong(songId: String): SongEntity?
```

### If you need new data:

```kotlin
// Instead of modifying Room, use DataStore for new preferences
// Or add an in-memory cache via a Hilt @Singleton
@Singleton
class InMemoryCache @Inject constructor() {
    private val cache = mutableMapOf<String, Any>()
    fun <T> get(key: String): T? = cache[key] as? T
    fun <T> set(key: String, value: T) { cache[key] = value }
}
```

## DataStore (Preferences)

### Proto DataStore

```kotlin
// Preferences are defined in a proto file or via PreferencesDataStore
val Context.dataStore by preferencesDataStore(name = "elara_preferences")

// Read
val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
    val mode = prefs[PreferenceKeys.THEME_MODE] ?: "dynamic"
    ThemeMode.valueOf(mode)
}

// Write
suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { prefs ->
        prefs[PreferenceKeys.THEME_MODE] = mode.name
    }
}
```

### Preference Keys

```kotlin
// constants/PreferenceKeys.kt
object PreferenceKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
    val BLUR_RADIUS = floatPreferencesKey("blur_radius")
    val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
    val AUDIO_NORMALIZATION = booleanPreferencesKey("audio_normalization")
    val LAST_LIBRARY_FILTER = stringPreferencesKey("last_library_filter")
    val DISCORD_TOKEN = stringPreferencesKey("discord_token") // Store encrypted
}
```

## Offline-First Strategy

```kotlin
// Repository-level caching
class MusicRepositoryImpl @Inject constructor(
    private val api: InnerTube,
    private val db: MusicDatabase,
    private val cache: InMemoryCache
) : MusicRepository {

    override fun getHomeContent(): Flow<HomeContent> = flow {
        // 1. Emit cached immediately
        val cached = db.dao().getHomeContent()
        if (cached != null) {
            emit(cached.toHomeContent())
        }

        // 2. Fetch fresh
        try {
            val fresh = api.getHomeContent()
            db.dao().insertHomeContent(fresh.toEntity())
            emit(fresh.toHomeContent())
        } catch (e: Exception) {
            if (cached == null) throw e
            // Otherwise keep showing cached data silently
        }
    }

    override fun getSearchSuggestions(query: String): Flow<List<String>> = flow {
        // Memory cache first (fastest)
        cache.get<List<String>>("search_$query")?.let { emit(it) }

        // Disk cache next
        val diskCached = db.dao().getSearchCache(query)
        if (diskCached != null) emit(diskCached.toList())

        // Network last
        val fresh = api.getSearchSuggestions(query)
        db.dao().cacheSearchSuggestions(query, fresh.toEntities())
        cache.set("search_$query", fresh)
        emit(fresh)
    }
}
```

## Caching Configuration

### ExoPlayer Cache

```kotlin
// In MusicService
val cache = SimpleCache(
    cacheDir = File(context.cacheDir, "media_cache"),
    LeastRecentlyUsedCacheEvictor(maxBytes = 500 * 1024 * 1024), // 500MB
    DatabaseProvider(context)
)
```

### Coil Image Cache

```kotlin
// In App.kt
val imageLoader = ImageLoader.Builder(context)
    .memoryCachePolicy(CachePolicy.ENABLED)
    .diskCachePolicy(CachePolicy.ENABLED)
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("image_cache"))
            .maxSizeBytes(100 * 1024 * 1024) // 100MB
            .build()
    }
    .build()
```

## Album Art Caching

```kotlin
// Coil handles this automatically via disk cache
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(artworkUrl)
        .crossfade(300)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .build(),
    contentDescription = null
)
```

## Offline Scenarios

| Scenario | Handling |
|---|---|
| No network → view home | Show cached home content with "offline" indicator |
| No network → search | Show cached search suggestions + local results only |
| No network → play | Play downloaded/cached content only |
| Network restored | Auto-refresh stale content, background sync |
| Download for offline | `ExoDownloadService` handles this |

## Data Flow

```
UI (Compose) → ViewModel → Repository → DAO/API
                                      ↕
                                   Room / InnerTube
```

## Security

- **Playback positions** — No sensitive data
- **Account tokens** — Stored via AccountManager or EncryptedSharedPreferences
- **Discord tokens** — Store in EncryptedSharedPreferences
- **Downloaded media** — In app-private directory (not accessible to other apps)
