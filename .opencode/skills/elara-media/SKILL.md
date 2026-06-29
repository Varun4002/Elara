---
name: elara-media
description: Media3 (ExoPlayer) playback architecture, audio/video management, MediaSession, and Picture-in-Picture for Elara. Use when working with MusicService, MediaController, ExoPlayer, playback controls, audio focus, notifications, PiP mode, or media session callbacks.
---

# Elara Media & Playback

## Overview

Elara uses **AndroidX Media3** (ExoPlayer) for audio/video playback. The player runs in a **foreground service** with a MediaSession for external control (notification, wear OS, Android Auto).

## Key Components

| Component | Class | Purpose |
|---|---|---|
| Player service | `MusicService` | Foreground service hosting ExoPlayer |
| Session | `MediaSessionService` | Handles media session lifecycle |
| Controller | `MediaController` | IPC bridge from UI to service |
| Player | `ExoPlayer` | Core playback engine |
| Download | `ExoDownloadService` | Offline download/cache |
| Notifications | `MediaNotification.Provider` | System notification for playback |

## Service Architecture

```
┌─────────────────────────────────────┐
│         App Process                 │
│                                     │
│  ┌─────────────────────────────┐    │
│  │  Compose UI (Screens)       │    │
│  │  PlayerScreen.kt            │    │
│  │  MiniPlayer.kt              │    │
│  └───────────┬─────────────────┘    │
│              │ MediaController       │
│              ▼ (IPC via AIDL)        │
│  ┌─────────────────────────────┐    │
│  │  MusicService (Foreground)  │    │
│  │  - ExoPlayer               │    │
│  │  - MediaSession            │    │
│  │  - AudioManager            │    │
│  │  - Notification            │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

## MusicService.kt

```kotlin
@AndroidEntryPoint
class MusicService : MediaSessionService() {
    @Inject lateinit var playerProvider: Provider<ExoPlayer>

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = playerProvider.get()
        mediaSession = MediaSession.Builder(this, player)
            .setSessionCallback(ElaraSessionCallback(player))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaController.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            controller.send(SessionCommand(COMMAND_CODE_CUSTOM_COMMAND, Bundle.EMPTY))
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
```

## MediaSession Callbacks

```kotlin
class ElaraSessionCallback(
    private val player: ExoPlayer
) : MediaSession.Callback {

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaController.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): MutableList<MediaItem> {
        // Resolve URIs, add metadata
        mediaItems.forEach { item ->
            // Convert YouTube video IDs to streaming URIs
            item.mediaMetadata = item.mediaMetadata.buildUpon()
                .setArtworkUri(item.mediaMetadata.artworkUri)
                .build()
        }
        return mediaItems
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaController.ControllerInfo
    ): MediaSession.ConnectionResult {
        val availableSessionCommands = SessionCommands.Builder()
            .addAllLibraryCommands(COMMAND_LIBRARY_ALL)
            .build()
        return AcceptedResultBuilder(session)
            .setSessionCommands(availableSessionCommands)
            .build()
    }
}
```

## PlayerViewModel Connection

```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            // Map ExoPlayer state to UI state
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        mediaController?.run {
            MediaController.releaseFuture(
                MediaController.Builder(context, sessionToken).buildAsync()
            )
        }
    }
}
```

## Mini Player Integration

```kotlin
@Composable
fun MiniPlayer(viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.playbackState.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = state != PlaybackState.IDLE,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        GlassSurface(
            blurRadius = 24.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art
                AsyncImage(
                    model = state.media?.artworkUri,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                    contentDescription = null
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.media?.title ?: "", style = MaterialTheme.typography.titleMedium)
                    Text(state.media?.artist ?: "", style = MaterialTheme.typography.bodySmall)
                }
                GlassButton(onClick = { viewModel.togglePlayPause() }, icon = Icons.Default.Play)
                GlassButton(onClick = { /* open player */ }, icon = Icons.Default.ExpandLess)
            }
        }
    }
}
```

## Picture-in-Picture (PiP)

### Manifest Setup

```xml
<activity android:name=".MainActivity"
    android:supportsPictureInPicture="true"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation" />
```

### Enter PiP

```kotlin
// In MainActivity
fun enterPip() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        enterPictureInPictureMode(params)
    }
}
```

## Audio Focus & Playback

```kotlin
// Handled by Media3 automatically via AudioAttributes
val audioAttributes = AudioAttributes.Builder()
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .build()

val player = ExoPlayer.Builder(context)
    .setAudioAttributes(audioAttributes, true) // true = handle focus
    .build()
```

## Notification

Media3 handles notification creation automatically via `MediaNotification.Provider`. Customize in:

```kotlin
override fun onUpdateNotification(
    mediaSession: MediaSession,
    startInForegroundRequired: Boolean
) {
    // Custom notification with playback controls, artwork, progress
    val notification = MediaNotification(
        NOTIFICATION_ID,
        notificationBuilder.build()
    )
    startForeground(NOTIFICATION_ID, notification)
}
```

## Offline Downloads

```kotlin
// ExoDownloadService handles downloads
@AndroidEntryPoint
class ExoDownloadService : DownloadService(...) {
    // Configured in service manifest declaration
    override fun getDownloadManager(): DownloadManager = downloadManager
}
```

## Playback Pipeline

```
YouTube URL → InnerTube API → Stream URL → ExoPlayer → Audio/Video
                    │                              │
                    ▼                              ▼
            Cached streams                 MediaSession
                                            │
                                            ▼
                                    Notification + UI
```

## Key Media3 Dependencies

```kotlin
implementation(libs.media3)          // Core
implementation(libs.media3.session)  // MediaSession
implementation(libs.media3.okhttp)   // OkHttp data source
implementation(libs.media3.cast)     // Google Cast (gms flavor only)
```

## Rules

1. **Always use Media3** — Never use deprecated ExoPlayer APIs directly
2. **Service runs in its own process?** — No, MusicService runs in the main process (`:crash` handler is separate)
3. **Downloaded content** — Isolated to app-specific directory, encrypted at rest
4. **Foreground service type** — `mediaPlayback` for music, `dataSync` for downloads
5. **Audio focus** — Managed by Media3 automatically; do not manually request
6. **Notification** — Never build from scratch; use Media3's `MediaNotification`
