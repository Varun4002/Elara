---
name: elara-compose
description: Jetpack Compose UI implementation patterns for Elara. Use when writing, modifying, or reviewing Compose UI code. Covers glassmorphism component implementation, Compose architecture patterns, state management, animation integration, theme wiring, and pixel-perfect implementation of the elara-ui design system.
---

# Elara Compose UI Development

> Implementation patterns for Elara's Jetpack Compose UI layer.

This skill bridges the **elara-ui design system** (frosted glass, motion, typography tokens) into actual **Compose code**. Always reference `elara-ui/SKILL.md` for design tokens and component specs.

---

# 1. Theme Wiring

Wire the design tokens through standard Material 3 `MaterialTheme`. Create a custom `ElaraTheme` composable that layers glassmorphism on top of Material You.

## Theme.kt

```kotlin
// ui/theme/Theme.kt
@Composable
fun ElaraTheme(
    darkTheme: Boolean = true, // Elara defaults to AMOLED dark
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ElaraTypography,
        shapes = ElaraShapes,
        content = content
    )
}
```

## Extending Colors

Add glass-specific colors as `Color` top-level vals rather than extending the color scheme (to keep Material 3 internals clean):

```kotlin
// ui/theme/GlassColor.kt
object GlassColors {
    val surface get() = if (isSystemInDarkTheme()) Color(0x26FFFFFF) else Color(0x4D000000)
    val surfaceElevated get() = if (isSystemInDarkTheme()) Color(0x33FFFFFF) else Color(0x59E8E8E8)
    val border get() = if (isSystemInDarkTheme()) Color(0x1AFFFFFF) else Color(0x1A000000)
    val highlight get() = Color(0x0DFFFFFF)
    val shadow get() = Color(0x40000000)
}
```

Access via `LocalContext` / composition local or pass as parameters. For simplicity, a `GlassColor` composition local can be created:

```kotlin
val LocalGlassColors = staticCompositionLocalOf { GlassColors }
```

---

# 2. The Glass Modifier

The most critical composable utility. This applies the frosted glass effect to any surface.

## Implementation Strategy (API 31+)

Use `RenderEffect.createBlurEffect()` via `Modifier.graphicsLayer`:

```kotlin
fun Modifier.glassBlur(
    radius: Float = 24f,
    alpha: Float = 0.15f,
    borderAlpha: Float = 0.10f,
    shadowElevation: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(24.dp)
): Modifier = this
    .shadow(
        elevation = shadowElevation,
        shape = shape,
        ambientColor = Color.Black,
        spotColor = Color.Black
    )
    .drawWithCache {
        val blurred = RenderEffect.createBlurEffect(
            radius, radius, Shader.TileMode.CLAMP
        )
        onDrawWithRenderEffect(blurred) {
            drawRoundRect(
                color = Color.White.copy(alpha = alpha),
                cornerRadius = CornerRadius(
                    shape.toPx(size, Offset.Zero)
                )
            )
        }
    }
    .then(
        Modifier.drawWithContent {
            // Border
            drawRoundRect(
                color = Color.White.copy(alpha = borderAlpha),
                cornerRadius = CornerRadius(/*...*/),
                style = Stroke(width = 1.dp.toPx())
            )
            // Inner highlight
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                ),
                cornerRadius = CornerRadius(/*...*/)
            )
        }
    )
```

For API < 31, fall back to a simple translucent background with a shadow — no blur.

## GlassSurface Composable

Wrap the modifier in a reusable composable:

```kotlin
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 24.dp,
    shape: Shape = RoundedCornerShape(24.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(stiffness = 800f, dampingRatio = 0.5f)
    )

    Box(
        modifier = modifier
            .scale(scale)
            .glassBlur(radius = blurRadius, shape = shape)
            .then(
                if (onClick != null) Modifier
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null // custom glass ripple
                    ) { onClick() }
                else Modifier
            ),
        content = content
    )
}
```

---

# 3. Component Implementation Patterns

## GlassButton

```kotlin
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    label: String? = null,
    selected: Boolean = false,
    selectedTint: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(12.dp),
    size: Dp = 36.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val glowAlpha by animateFloatAsState(
        targetValue = if (selected) 0.3f else 0f,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.7f)
    )

    Box(
        modifier = modifier
            .size(if (label != null) WindowInsets.run { size } else size, size)
            .scale(if (isPressed) 0.96f else 1f)
            .glassBlur(radius = 12f, shape = shape)
            .drawBehind {
                if (selected) {
                    drawRoundRect(
                        color = selectedTint.copy(alpha = glowAlpha),
                        cornerRadius = CornerRadius(/*shape radius*/)
                    )
                }
            }
            .clickable(interactionSource, null) { onClick() }
            .padding(horizontal = if (label != null) 12.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) selectedTint else Color.White.copy(alpha = 0.8f)
            )
        }
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) selectedTint else Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
```

## GlassCard

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    blurRadius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    GlassSurface(
        modifier = modifier,
        blurRadius = blurRadius,
        shape = shape,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            content = content
        )
    }
}
```

## GlassSlider

```kotlin
@Composable
fun GlassSlider(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    buffered: Float = 1f,
    trackHeight: Dp = 2.dp,
    thumbRadius: Dp = 6.dp
) {
    val isDragging = remember { mutableStateOf(false) }
    val thumbSize by animateDpAsState(
        targetValue = if (isDragging.value) thumbRadius * 1.5f else thumbRadius,
        animationSpec = spring(stiffness = 500f)
    )

    Box(modifier = modifier.height(40.dp)) {
        // Track background
        LinearProgressIndicator(
            progress = { buffered },
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.Center)
                .glassBlur(radius = 4f),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            trackColor = Color.Transparent
        )

        // Progress fill
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.Center),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )

        // Thumb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        isDragging.value = true
                        onProgressChange(change.position.x / size.width)
                    }
                }
                .drawBehind {
                    // Glow behind thumb
                    val thumbX = size.width * progress
                    drawCircle(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        radius = thumbSize.toPx() * 1.5f,
                        center = Offset(thumbX, size.height / 2f)
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = (size.width * progress) - thumbSize / 2)
                    .size(thumbSize * 2)
                    .glassBlur(
                        radius = 4f,
                        shape = CircleShape
                    )
            )
        }
    }
}
```

---

# 4. State Management Patterns

## ViewModel → UI Flow

```kotlin
// PlayerViewModel.kt
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    data class PlayerUiState(
        val isPlaying: Boolean = false,
        val currentPosition: Long = 0L,
        val duration: Long = 0L,
        val artworkColors: AmbientColors? = null,
        val isControlsVisible: Boolean = true
    )
}
```

```kotlin
// In the Composable
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.playerState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Background artwork
        AsyncImage(
            model = state.artworkUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Ambient gradient overlay
        AmbientGradient(state.artworkColors)

        // Glass controls layer
        PlayerControlsLayer(
            state = state,
            onPlayPause = { viewModel.togglePlayPause() },
            onSeek = { viewModel.seek(it) }
        )
    }
}
```

## Auto-hiding Controls

Use a coroutine-based timer that resets on user interaction:

```kotlin
@Composable
fun rememberAutoHideControls(
    interactionTrigger: Flow<*>,
    delayMs: Long = 3000L
): State<Boolean> {
    val isVisible = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        interactionTrigger
            .debounce(delayMs)
            .collect { isVisible.value = false }
    }

    LaunchedEffect(interactionTrigger) {
        interactionTrigger.collect { isVisible.value = true }
    }

    return isVisible
}
```

---

# 5. Animation Integration

## Shared Element Transitions

Use `Modifier.sharedElement()` with a `SharedTransitionScope`:

```kotlin
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementExample() {
    val sharedTransitionScope = rememberSharedContentState()

    SharedTransitionLayout {
        // In list
        AsyncImage(
            modifier = Modifier.sharedElement(
                state = sharedTransitionScope,
                key = "artwork_${song.id}"
            ),
            // ...
        )

        // In player
        AsyncImage(
            modifier = Modifier.sharedElement(
                state = sharedTransitionScope,
                key = "artwork_${song.id}"
            ),
            // ...
        )
    }
}
```

## Staggered Enter Animations

Use `AnimatedVisibility` with staggered children for the player controls:

```kotlin
@Composable
fun StaggeredPlayerControls(visible: Boolean) {
    val items = listOf(
        { TopBar() },
        { CenterControls() },
        { ProgressSection() },
        { BottomDock() }
    )

    items.forEachIndexed { index, content ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() +
                scaleIn(initialScale = 0.9f) +
                slideInVertically { it / 4 } +
                blurIn(initialBlur = 12.dp),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier then if (index > 0)
                Modifier.padding(top = 8.dp) else Modifier
        ) {
            content()
        }
    }
}
```

## Spring Animation Utility

```kotlin
object ElaraAnim {
    fun <T> spring(
        dampingRatio: Float = 0.7f,
        stiffness: Float = 300f
    ) = SpringSpec<T>(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )

    val large = spring<Float>(dampingRatio = 0.8f, stiffness = 200f)
    val medium = spring<Float>(dampingRatio = 0.7f, stiffness = 300f)
    val small = spring<Float>(dampingRatio = 0.6f, stiffness = 500f)
    val press = spring<Float>(dampingRatio = 0.5f, stiffness = 800f)
}
```

---

# 6. Gesture Implementation

## Double-Tap

```kotlin
@Composable
fun Modifier.doubleTapSeek(
    onSeek: (Direction) -> Unit
): Modifier = this.pointerInput(Unit) {
    detectTapGestures(
        onDoubleTap = { offset ->
            val midPoint = size.width / 2f
            if (offset.x < midPoint) onSeek(Direction.LEFT)
            else onSeek(Direction.RIGHT)
        }
    )
}
```

## Vertical Swipe (Brightness/Volume)

```kotlin
@Composable
fun Modifier.verticalSwipeControl(
    onSwipe: (Float) -> Unit,
    onEnd: () -> Unit
): Modifier = this.pointerInput(Unit) {
    detectVerticalDragGestures(
        onDragStart = { /* show HUD */ },
        onDragEnd = { onEnd() },
        onVerticalDrag = { change, dragAmount ->
            change.consume()
            onSwipe(dragAmount)
        }
    )
}
```

## Gesture HUD Overlay

```kotlin
@Composable
fun GestureHUD(
    icon: ImageVector,
    progress: Float,
    label: String,
    visible: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(stiffness = 200f)) +
            scaleIn(initialScale = 0.5f),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        GlassSurface(
            blurRadius = 40.dp,
            shape = CircleShape,
            modifier = Modifier.size(100.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }
    }
}
```

---

# 7. Ambient Color Extraction

```kotlin
// ui/player/AmbientColors.kt
data class AmbientColors(
    val dominant: Color,
    val vibrant: Color,
    val muted: Color,
    val darkMuted: Color
)

@Composable
fun rememberAmbientColors(imageUrl: String): AmbientColors? {
    var colors by remember { mutableStateOf<AmbientColors?>(null) }
    val imageLoader = Coil.imageLoader(LocalContext.current)

    LaunchedEffect(imageUrl) {
        val request = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .size(100) // small size for palette extraction
            .build()

        val bitmap = (imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
        if (bitmap != null) {
            val palette = Palette.from(bitmap).generate()
            colors = AmbientColors(
                dominant = Color(palette.getDominantColor(0)),
                vibrant = Color(palette.getVibrantColor(0)),
                muted = Color(palette.getMutedColor(0)),
                darkMuted = Color(palette.getDarkMutedColor(0))
            )
        }
    }

    return colors
}
```

---

# 8. Screen Architecture Pattern

Every screen should follow this structure:

```kotlin
// ui/screens/feature/FeatureScreen.kt
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = hiltViewModel(),
    onNavigate: (FeatureRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ElaraTheme {
        Box(modifier = modifier.fillMaxSize()) {
            // Background layer (full-screen artwork/video)
            BackgroundLayer(state = uiState)

            // Glass controls layer (floating above)
            ControlsLayer(
                state = uiState,
                onAction = viewModel::onAction
            )

            // Gesture overlay (on top of everything)
            GestureLayer(
                onGesture = viewModel::onGesture
            )
        }
    }
}
```

---

# 9. File Organization

```
ui/
├── theme/
│   ├── ElaraTheme.kt        # ElaraTheme composable
│   ├── Type.kt              # ElaraTypography
│   ├── Shape.kt             # ElaraShapes
│   ├── GlassColor.kt        # GlassColors + LocalGlassColors
│   └── Dimensions.kt        # Spacing tokens
├── component/
│   ├── GlassSurface.kt      # Glass modifier + GlassSurface
│   ├── GlassButton.kt
│   ├── GlassCard.kt
│   ├── GlassSlider.kt
│   ├── GlassSheet.kt
│   └── GlassShimmer.kt
├── animation/
│   ├── ElaraSpring.kt       # Spring constants
│   ├── StaggeredEnter.kt
│   └── SharedTransitions.kt
├── player/
│   ├── ElaraPlayerScreen.kt # Main player composable
│   ├── TopBar.kt
│   ├── CenterControls.kt
│   ├── ProgressSection.kt
│   ├── BottomDock.kt
│   ├── MetadataBar.kt
│   ├── GestureHUD.kt
│   ├── AmbientGradient.kt
│   └── PlayerViewModel.kt
├── screens/
│   ├── home/
│   ├── search/
│   ├── library/
│   ├── playlist/
│   └── settings/
└── navigation/
    ├── ElaraNavHost.kt
    └── Routes.kt
```

---

# 10. Do's and Don'ts

| DO | DON'T |
|---|---|
| Use `collectAsStateWithLifecycle()` for ViewModel flows | Use `collectAsState()` directly (lifecycle-unsafe) |
| Use `Modifier.glassBlur()` for floating surfaces | Use hard `Color.Black.copy(alpha = 0.5f)` backgrounds |
| Use `GlassSurface` wrapper composable | Draw glass effects inline in every screen |
| Use `ElaraAnim.spring()` for interactive animations | Use `tween()` for press/gesture feedback |
| Extract ambient colors from artwork | Hardcode accent colors for player |
| Use `Modifier.scale()` for press feedback | Use `Modifier.alpha()` for press feedback |
| Wrap gestures in `pointerInput` | Use `Modifier.clickable` for complex gestures |
| Use `WindowInsets` for safe areas | Hardcode padding for status/nav bars |
