# Working with Elara as an AI agent

Elara is a premium Android YouTube Music client forked from Elara. It is written in Kotlin with Jetpack Compose and follows Material 3 design guidelines with a custom **glassmorphism** design system layered on top.

**Tagline:** *Listen Beautifully.*

## Core Principles

1. **Luxury through simplicity** â€” Every UI element should feel expensive, intentional, and crafted. No clutter, no sharp edges.
2. **Content first** â€” The video/audio always remains the center of attention. Controls float above as frosted glass layers.
3. **Motion with purpose** â€” Every animation should enhance usability, never distract. Use spring physics, not linear tweening.
4. **Material You integration** â€” Dynamic color from wallpaper powers the accent palette. The player additionally samples video artwork for ambient colors.
5. **Glassmorphism** â€” All floating surfaces use strong background blur, slight transparency, soft inner highlight, 1px translucent borders, and soft ambient shadows.

## Project Structure

| Path | Purpose |
|---|---|
| `app/` | Main Android application module |
| `innertube/` | YouTube Music InnerTube API client (Kotlin/Ktor) |
| `kugou/` | KuGou lyrics API client |
| `lrclib/` | LrcLib lyrics API client |
| `lastfm/` | LastFM scrobbling integration |
| `betterlyrics/` | Better Lyrics integration (TTML parser) |
| `shazamkit/` | Music recognition via Shazam API |
| `paxsenix/` | Additional lyrics provider |
| `metroproto/` | Git submodule: Listen Together protobuf definitions |

## Architecture

- **Pattern:** MVVM + Repository
- **UI:** Jetpack Compose with custom glassmorphism components
- **DI:** Hilt
- **Navigation:** Compose Navigation (~45+ routes)
- **Database:** Room (DO NOT EDIT SCHEMA)
- **Playback:** Media3 ExoPlayer in foreground service
- **Networking:** Ktor (YouTube InnerTube API)
- **Image loading:** Coil
- **State:** ViewModels + DataStore preferences

## Design System

The full design system is documented in `.opencode/skills/elara-ui/SKILL.md`. Key tokens:

- **Glass blur:** 24-40dp blur radius for large surfaces, 12dp for small controls
- **Corner radius:** 24dp for floating docks/capsules, 16dp for cards, 12dp for buttons, 8dp for small elements
- **Elevation:** No standard Android elevation. Use ambient shadow + blur instead.
- **Typography:** Clean sans-serif (Inter / system default), generous letter-spacing for display text
- **Colors:** AMOLED dark background (`#000000`), frosted glass surfaces, pure white text, dynamic accent from wallpaper
- **Icons:** Rounded, consistent stroke width, never filled unless actively selected

## Rules for working on the project

1. **Pull latest before starting** â€” Always `git pull --rebase` before beginning work to minimize merge conflicts.
2. **Commit format** â€” `type(scope): short description` e.g. `feat(player): add glassmorphism playback controls`. Scope is optional.
3. **String edits** â€” All string additions go in `app/src/main/res/values/elara_strings.xml` (NOT `strings.xml`). Do NOT edit translated `elara_strings.xml` files â€” only the default English one.
4. **DO NOT modify the database schema** â€” Room entities and migrations are off-limits unless explicitly authorized.
5. **DO NOT bump version codes** â€” This is handled by the core team.
6. **DO NOT edit markdown/readme files** including this one or SKILL.md files (those are source-of-truth design documents).
7. **Follow Kotlin/Android best practices** â€” Proper coroutine usage, lifecycle awareness, efficient Compose recomposition.
8. **Performance** â€” Prioritize battery efficiency, memory usage, and smooth scrolling at 60/120fps. Avoid unnecessary recompositions.
9. **Comments** â€” Only for complex or non-obvious logic. Don't restate what the code clearly says.
10. **When in doubt, ask** â€” Never make assumptions about design intent or requirements without clarification.

## UI Conventions

- **Player screen** â€” The most important screen. All controls float as frosted glass over full-screen artwork/video. See the elara-ui skill for exact specs.
- **Navigation** â€” Bottom navigation uses outlined icons, filled when selected with accent glow.
- **Cards** â€” Use `GlassCard` composable (rounded corners, blur background, soft shadow) instead of standard Material Cards.
- **Dialogs/Sheets** â€” Use glass sheets (large rounded corners, blur background) pulled up from bottom.
- **Skeleton loading** â€” Use shimmer effects with glass-colored placeholders.

## Building and testing

### Build the app:

```bash
./gradlew :app:assembleGmsDebug
```

### Install APK:

The built APK is at: `app/build/outputs/apk/universalGms/debug/app-universal-gms-debug.apk`

### Run tests:

```bash
./gradlew :app:testGmsDebugUnitTest
```

### Build types:

- `GmsDebug` â€” Development build with Google Cast + auto-updater
- `GmsRelease` â€” Release build with ProGuard + signing

## Flavor note

Elara only has the **GMS** flavor (Google Mobile Services). The original Elara had `foss` and `izzy` flavors which were removed during the fork. Google Cast and the auto-updater are available in all builds.

## Listen Together Discord

Elara uses Discord OAuth for Listen Together presence. The Discord App ID is configured in `app/build.gradle.kts` under `defaultConfig` as `DISCORD_APP_ID`.

## Deep links

- Listen Together invites: `https://Elara.cc/listen/...`
- Discord OAuth callback: `elaradiscord://oauth2/callback`
- YouTube intent filters: `youtube.com`, `youtu.be`, `music.youtube.com`
- Recognition shortcut: `com.elara.music.action.RECOGNITION`

## Key references

- **Original Elara repo:** `ElaraGroup/Elara` (available via @Elara-og reference)
- **Design system:** See `.opencode/skills/elara-ui/SKILL.md` for the complete glassmorphism design spec
- **Version:** 13.6.0 (forked)
