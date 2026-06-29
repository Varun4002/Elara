---
name: elara-ui
description: Complete glassmorphism design system for Elara, a premium YouTube Music Android app. Use when implementing player UI, glass components, motion/animation, theming, typography, or any screen layout. Covers the full visual language: frosted glass surfaces, dynamic blur, ambient color sampling from video artwork, spring animations, gesture interactions, and the floating control layer paradigm. Activates whenever the user mentions glassmorphism, player redesign, UI polish, animation, motion design, or theme customization.
---

# Elara Design System

> *"Luxury through simplicity."*

## Visual Philosophy

Elara's interface is made of **floating layers of frosted glass** suspended above the content. Nothing feels flat or cheap. Every component has realistic glassmorphism, background blur, soft reflections, thin translucent borders, layered depth, gentle shadows, and smooth gradients.

The video or album artwork always fills the entire display. The UI floats above it. No black bars. No hard edges. Everything respects safe areas.

---

# 1. Glass Material Tokens

## Glass Surface Parameters

| Token | Value | Usage |
|---|---|---|
| `glass-blur-large` | `40.dp` blur radius | Full-screen overlays, bottom sheets |
| `glass-blur-medium` | `24.dp` blur radius | Player controls dock, top bar |
| `glass-blur-small` | `12.dp` blur radius | Buttons, chips, small controls |
| `glass-alpha-light` | 15% white (`0x26FFFFFF`) | Dark mode glass fill |
| `glass-alpha-dark` | 30% black (`0x4D000000`) | Light mode glass fill |
| `glass-border` | 1px at 10% white (`0x1AFFFFFF`) | Glass surface borders |
| `glass-border-accent` | 1px at 30% accent | Selected/active glass states |
| `glass-highlight` | Top edge 5% white gradient | Inner light reflection on glass |
| `glass-shadow` | `y=4dp, blur=16dp, alpha=0.4` | Floating surface shadows |
| `glass-shadow-large` | `y=8dp, blur=32dp, alpha=0.5` | Bottom sheets, menus |

## Glass Modifier (Compose)

```kotlin
fun Modifier.glass(
    blurRadius: Dp = 24.dp,
    alpha: Float = 0.15f,
    borderAlpha: Float = 0.10f,
    shadowElevation: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(24.dp)
): Modifier
```

Implementation approach:
- Use `RenderEffect.createBlurEffect()` on API 31+ for runtime blur
- Fall back to `Color(0x26FFFFFF)` with `alpha` on older APIs
- Apply `1px` translucent border via `drawBorder`
- Add inner top highlight via gradient `drawContent`
- Layer ambient shadow beneath

---

# 2. Shape & Corner Radius System

| Token | Value | Usage |
|---|---|---|
| `shape-dock` | `24.dp` | Floating control dock, top bar capsule |
| `shape-sheet` | `20.dp` top corners | Bottom sheets, drawers |
| `shape-card` | `16.dp` | Cards, previews, Up Next |
| `shape-button` | `12.dp` | Regular buttons, action items |
| `shape-pill` | `999.dp` | Chips, tags, small badges |
| `shape-circular` | `50%` | Icon buttons, avatars, play button |

---

# 3. Spacing System

| Token | Value |
|---|---|
| `space-xxs` | `4.dp` |
| `space-xs` | `8.dp` |
| `space-sm` | `12.dp` |
| `space-md` | `16.dp` |
| `space-lg` | `24.dp` |
| `space-xl` | `32.dp` |
| `space-2xl` | `48.dp` |
| `content-margin` | `16.dp` (horizontal margins) |
| `safe-area-top` | `48.dp` (status bar + notch) |
| `safe-area-bottom` | `24.dp` (gesture bar) |

---

# 4. Color Palette

## Dark Mode (AMOLED)

```kotlin
// Backgrounds
val Background = Color(0xFF000000)        // Pure black AMOLED
val SurfaceGlass = Color(0x26FFFFFF)       // Frosted glass
val SurfaceElevated = Color(0x33FFFFFF)    // Higher glass
val SurfaceOverlay = Color(0x40FFFFFF)     // Modal glass

// Text
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xB3FFFFFF)      // 70% white
val TextTertiary = Color(0x80FFFFFF)       // 50% white

// Accent
val Accent = dynamicColorScheme.primary     // From Material You
val AccentGlow = Accent.copy(alpha = 0.3f) // Glow effect
val AccentContainer = Accent.copy(alpha = 0.15f)

// Borders
val BorderGlass = Color(0x1AFFFFFF)        // 10% white
val BorderAccent = Accent.copy(alpha = 0.3f)

// Shadows
val ShadowColor = Color(0x40000000)

// Glass highlights
val GlassHighlight = Color(0x0DFFFFFF)     // Top edge reflection
```

## Color Extraction (Ambient)

The player should continuously sample the current video/artwork's dominant colors to generate:

1. A **soft blurred gradient** behind the controls
2. **Dynamic accent tinting** on glass surfaces
3. A **subtle edge glow** matching the artwork

```kotlin
// Use Palette from AndroidX to extract colors from Bitmap
fun extractAmbientColors(bitmap: Bitmap): AmbientColors {
    val palette = Palette.from(bitmap).generate()
    return AmbientColors(
        dominant = palette.getDominantColor(Accent.value),
        vibrant = palette.getVibrantColor(Accent.value),
        muted = palette.getMutedColor(Accent.value),
        darkMuted = palette.getDarkMutedColor(Accent.value)
    )
}
```

---

# 5. Typography

Use the system default sans-serif (or Inter if bundled). No custom display fonts unless explicitly added.

```kotlin
val ElaraTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

---

# 6. Motion & Animation

## Spring Constants

```kotlin
object ElaraSpring {
    // For large objects (sheets, full-screen transitions)
    val large = spring(
        dampingRatio = 0.8f,    // Slightly underdamped
        stiffness = 200f        // Slower, weightier
    )

    // For medium objects (cards, buttons)
    val medium = spring(
        dampingRatio = 0.7f,    // More bounce
        stiffness = 300f        // Responsive
    )

    // For small objects (icons, micro-interactions)
    val small = spring(
        dampingRatio = 0.6f,    // Noticeable bounce
        stiffness = 500f        // Snappy
    )

    // For press/click feedback
    val press = spring(
        dampingRatio = 0.5f,
        stiffness = 800f        // Very snappy
    )
}
```

## Animation Guidelines

1. **Large objects move slower** than small ones (use `stiffness = 200f` for sheets, `500f` for icons)
2. **Nothing pops** — no sudden appearances. Elements should fade + scale + blur in.
3. **Shared element transitions** for navigation (album art, thumbnails)
4. **Blur interpolation** — glass blur intensity animates (e.g., 0 → 24dp on appear)
5. **Elastic overscroll** on all scrollable content
6. **Haptic feedback** on button presses, gesture completions, and state changes

## Micro-interactions

Every interactive element should have:
- **Press animation** — Scale to 0.96x with spring, shadow intensifies
- **Release animation** — Spring back to 1.0x
- **Ripple** — Custom glass ripple (lighter, wider, with blur)
- **Hover/state change** — Soft glow or border accent transition (300ms spring)

---

# 7. Player Screen Spec

The player is the heart of Elara. This spec overrides any generic component guidelines.

## Layout Hierarchy

```
┌──────────────────────────────────────┐
│  Status Bar (transparent, edge2edge) │
├──────────────────────────────────────┤
│  Top Bar (glass capsule, 56dp)       │
│  ← Back | Title | Channel | Cast|PiP │
├──────────────────────────────────────┤
│                                      │
│                                      │
│          (Video / Artwork)           │
│          Fills remaining space       │
│                                      │
│                                      │
├──────────────────────────────────────┤
│  Center Controls (floating)          │
│     ⏮    ⏯    ⏭                     │
├──────────────────────────────────────┤
│  Progress Section (floating capsule) │
│  ─────●──────────────────── 1:23/3:45│
├──────────────────────────────────────┤
│  Bottom Dock (glass, 48dp)          │
│  Quality | Speed | Subtitles | More  │
├──────────────────────────────────────┤
│  Metadata Bar (subtle text row)      │
│  1:23 / 3:45  •  1080p  •  1.0x      │
├──────────────────────────────────────┤
│  Gesture Bar (system)                │
└──────────────────────────────────────┘
```

## Top Nav Bar

```
┌────────── Floating Glass Capsule ──────────┐
│  ○ ←  [Video Title...]  [Channel]  [Cast]  │
└────────────────────────────────────────────┘
```

- Large rounded capsule: `24.dp` corner radius, `56.dp` height
- Horizontal padding: `8.dp` on each side
- Background: `glass-blur-large` with `alpha 0.15`
- Title: Single line, ellipsized with gradient fade at end if too long
- Buttons: Circular glass buttons, `32.dp` diameter, `8.dp` spacing
- Cast icon: Only visible when Chromecast is available

## Center Controls

```
          ⏮        ⏯        ⏭
     (32dp)    (64dp)    (32dp)
```

- Play/Pause button: Large frosted glass circle, `64.dp` diameter
  - Soft glowing outline using `AccentGlow`
  - Subtle inner light reflection
  - Pressed: compresses to 0.92x, glow intensifies briefly, haptic tick
- Previous/Next: Smaller, `32.dp` diameter, same styling
- All buttons fade in with stagger: previous(0ms) → play(50ms) → next(100ms)
- Enter animation: fade + scale (0.8 → 1.0) + blur (12dp → 0dp)

## Progress Section

```
┌────────── Floating Capsule ──────────┐
│  ─────────●───────────────── 1:23     │
└──────────────────────────────────────┘
```

- Floating capsule: `glass-blur-medium`, `40.dp` height
- Progress line: 2dp height, `Accent` color
- Buffered section: `Accent.copy(alpha = 0.3f)`, below the progress line
- Thumb: `12.dp` circle, `Accent` color, emits `AccentGlow` (`8.dp` blur)
- Dragging: Thumb scales to `16.dp`, glow expands to `16.dp` blur
- Preview thumbnail: Floats above thumb in a `glass-blur-small` card (`120.dp` x `68.dp`)
- Timestamp: `labelSmall` typography, positioned above thumb

## Bottom Dock

```
┌────────── Floating Glass Dock ──────────┐
│  [Quality] [Speed] [Subtitles] [Lock]   │
└──────────────────────────────────────────┘
```

- Dock: `glass-blur-large`, `48.dp` height, `24.dp` corner radius
- Each button: `36.dp` rounded square, `12.dp` corner radius
- Glass surface with `AccentGlow` when selected
- Spring animation (medium) on selection change
- `Lock` button toggles control visibility (prevents accidental touches)

## Gesture Interactions

| Gesture | Action | Visual Feedback |
|---|---|---|
| Double-tap left side | Seek -10s | Large animated "-10" — circle ripple expands outward, particles fade |
| Double-tap right side | Seek +10s | Large animated "+10" — same ripple animation |
| Vertical swipe left edge | Brightness | Large floating HUD: sun icon, circular progress ring, percentage text |
| Vertical swipe right edge | Volume | Speaker icon, circular progress indicator, blur background |
| Horizontal swipe on progress | Scrubbing | Floating timeline with preview thumbnail + timestamp |

### Gesture HUD Spec
- Container: `100.dp` diameter glass circle
- Icon: `24.dp`, centered
- Progress: Circular ring, 3dp stroke, `Accent` color
- Text: `bodyLarge`, centered below icon
- Animation: Fade in (100ms), hold, fade out (200ms) after gesture ends
- Background: `glass-blur-large` with ambient tint

### Double-tap Ripple Spec
- Starting point: Center of tap
- Expanding circle: `Accent` color, `alpha 0.3`, grows from 0 to full screen width
- Text: "−10" or "+10" — `displayLarge`, bold, `Accent` color, fades from alpha 1.0 to 0
- Duration: 400ms spring
- Tiny particles: 8-12 small dots radially bursting from center, fade over 600ms

## Ambient Background

The player constantly samples the video's dominant colors and renders a soft blurred gradient behind controls:

```kotlin
@Composable
fun AmbientGradient(colors: List<Color>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = colors.map { it.copy(alpha = 0.3f) },
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    )
}
```

- Gradient: vertical, from artwork's `darkMuted` (top) to transparent (center) to `vibrant` muted (bottom)
- Transitions between songs: Crossfade over 800ms using `lerp` on color values
- Intensity: Very subtle — 30% alpha maximum

---

# 8. Other Screen Specs

## Mini Player

```
┌─────────── Floating Glass Card ───────────┐
│  [Art]  Title - Artist              [▶][⏏] │
└────────────────────────────────────────────┘
```

- Floating card: `glass-blur-large`, `16.dp` corner radius, `64.dp` height
- Soft shadow: `glass-shadow-large`
- Artwork: `48.dp` rounded square, `8.dp` corner radius
- Swipe down → collapses to mini bar
- Swipe left/right → dismisses with spring animation
- Spring animation: `ElaraSpring.large` for collapse, `ElaraSpring.medium` for dismiss

## Comments Drawer

```
┌──────────────────────────────────────┐
│  ┌─── Drag Handle ───┐              │
│  Comments (247)          [Close] ×   │
│                                      │
│  ┌── Glass Comment Card ────────┐   │
│  │ [Avatar]  Username · 2h      │   │
│  │  Great song!                 │   │
│  │  ♥ 12    [Reply]             │   │
│  └──────────────────────────────┘   │
│  ┌── Glass Comment Card ────────┐   │
│  │ ...                          │   │
│  └──────────────────────────────┘   │
└──────────────────────────────────────┘
```

- Sheet: `glass-blur-large`, `20.dp` top corner radius only
- Background blur: 40dp
- Drag handle: `4.dp` x `32.dp` pill at top
- Comment cards: `glass-blur-small`, `12.dp` corner radius
- Replies animate open with sequential spring stagger (30ms delay per item)

## Up Next

- Floating cards overlapping vertically by 30% each
- `16.dp` corner radius, `glass-blur-medium`
- Thumbnail: slightly larger than normal list items
- Carousel: smooth horizontal scrolling with snap behavior

## Home Screen

- Large featured hero card at top: full-width, `16.dp` radius, glass surface
- Category chips: `glass-blur-small` pills, horizontally scrollable
- Content sections: "Continue Watching", "Quick Picks", "Trending", "New Releases"
  - Each section: section title in `headlineMedium` + horizontal carousel
  - Carousel items: `12.dp` radius glass cards, `140.dp` wide
- Pull to refresh: glass shimmer effect

## Search Screen

- Search bar: `glass-blur-medium` capsule, `48.dp` height, `24.dp` radius
  - Animated expanding on focus (width scales with spring)
- Trending searches: pill chips in a wrap layout
- Recent searches: glass card list items with clear button
- Results: glass cards with thumbnail, title, metadata

## Library Screen

- Section headers: floating glass pills
- Content organized in glass cards with icon + label + count
- Playlist covers: elevated glass with rounded corners
- Swipe actions: glass background with icon

## Settings Screen

- Grouped into glass sections with subtle separator
- Each setting row: glass surface, switch/radio with accent color
- Icons: rounded, consistent 20dp stroke width

---

# 9. Component Library

## GlassButton

```kotlin
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: ImageVector? = null,
    label: String? = null,
    shape: Shape = RoundedCornerShape(12.dp)
)
```

- States: default, pressed (scale 0.96x), selected (border accent glow)
- Haptic feedback on press
- Spring animation on state change

## GlassCard

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    blurRadius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
)
```

- Subtle scale animation on press (0.98x)
- Shadow animates with press
- Corner radius adapts to content type

## GlassSlider

Custom slider for progress/seek:
- Track: `2.dp` height, `GlassBorder` color
- Progress: `Accent` color
- Thumb: `12.dp` circle with `AccentGlow`
- Enlarges on drag (`16.dp`)
- Haptic feedback on major position changes (every 5 seconds)

## GlassSheet

```kotlin
@Composable
fun GlassSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    content: @Composable ColumnScope.() -> Unit
)
```

- Drag to dismiss with spring physics
- Background scrim: black at 40% alpha
- Content fades in with scale
- Blur animates from 0 → 40dp on appear

---

# 10. Skeleton Loading

```kotlin
@Composable
fun GlassShimmer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp)
)
```

- Base color: `SurfaceGlass`
- Shimmer highlight: white at 8% alpha, sweeping from left to right
- Animation: 1200ms cycle, infinite
- Used for: cards before content loads, player before video loads

---

# 11. Implementation Priority

When building Elara's UI, implement in this order:

1. **Foundation:** `GlassSurface` modifier, theme tokens, typography
2. **Glass components:** `GlassButton`, `GlassCard`, `GlassSlider`, `GlassSheet`
3. **Animation:** Spring constants, gesture feedback components
4. **Player screen:** Top bar → Center controls → Progress → Bottom dock → Gestures
5. **Mini player:** Floating glass card with dismiss gesture
6. **Home screen:** Hero card, category chips, content carousels
7. **Search:** Animated bar, result cards
8. **Library/Playlists:** Glass organized layouts
9. **Comments drawer:** Pull-up sheet with glass cards
10. **Micro-interactions:** Polish pass (haptics, glow, shadows)

---

# 12. Do NOT

- Use standard Material 3 `Card` — replace with `GlassCard`
- Use standard `Slider` — replace with `GlassSlider`
- Use standard `ModalBottomSheet` — replace with `GlassSheet`
- Use filled Material icons when outlined is the default state
- Apply hard shadows (use blur + alpha instead)
- Use saturated neon colors
- Add clipped/overflowing content without rounded corners
