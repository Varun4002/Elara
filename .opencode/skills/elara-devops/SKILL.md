---
name: elara-devops
description: Build optimization, Gradle configuration, CI/CD workflows, code quality tools (ktlint, Detekt), versioning, and release management for Elara. Use when modifying Gradle config, running lint, fixing build issues, optimizing build times, or setting up CI pipelines.
---

# Elara DevOps Guide

## Build System

| Tool | Version / Config |
|---|---|
| **Gradle** | 8.x (see `gradle/wrapper/gradle-wrapper.properties`) |
| **AGP** | 8.x (see `gradle/libs.versions.toml`) |
| **Kotlin** | 2.x (see `libs.versions.toml`) |
| **JVM target** | 21 |
| **Compile SDK** | 37 |
| **Min SDK** | 26 |
| **Target SDK** | 36 |

## Module Graph

```
:app ───────────────────────────────────────────────┐
 ├── :innertube ─── libs (newpipe extractor, ktor)  │
 ├── :kugou                                         │
 ├── :lrclib                                        │
 ├── :lastfm                                        │
 ├── :betterlyrics                                  │
 ├── :shazamkit                                     │
 └── :paxsenix                                      │
:metroproto (git submodule — protobuf definitions) ─┘
```

## Build Commands

```bash
# Debug build (GMS flavor)
./gradlew :app:assembleGmsDebug

# Release build (signed, minified)
./gradlew :app:assembleGmsRelease

# Run unit tests
./gradlew :app:testGmsDebugUnitTest

# Run lint
./gradlew :app:lintGmsDebug

# Clean
./gradlew clean

# Incremental build (faster)
./gradlew :app:assembleGmsDebug --build-cache --parallel

# Build with profiling
./gradlew :app:assembleGmsDebug --profile --scan
```

## Build Optimization

### Gradle Properties

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.unsafe.isolated-projects=true
android.enableR8.fullMode=true
kotlin.code.style=official
```

### Parallel Builds

```kotlin
// Priority for parallel module building
// Ensure modules have no circular dependencies (they don't)
```

## Code Quality

### ktlint

```bash
# Run ktlint
./gradlew ktlintCheck

# Auto-format
./gradlew ktlintFormat
```

### Detekt

```bash
# Run Detekt
./gradlew detekt

# Generate HTML report
# Output: app/build/reports/detekt/detekt.html
```

### Lint

```kotlin
// lint.xml — Elara lint configuration
// Warnings as errors: false (preferred for CI)
// Abort on error: false
```

## Versioning

```kotlin
// In app/build.gradle.kts
versionCode = 149  // DO NOT CHANGE MANUALLY
versionName = "13.6.0"  // DO NOT CHANGE MANUALLY
```

**Version bumps are done by the core team only.**

## CI/CD

### GitHub Actions

```yaml
# .github/workflows/build.yml
name: Build
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          submodules: recursive
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: 21
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew :app:assembleGmsDebug --build-cache --parallel
```

### Available Workflows

| Workflow | Trigger | Artifacts |
|---|---|---|
| `build.yml` | Push to main | Universal debug APK |
| `build_pr.yml` | PR to main | Debug APK per PR |
| `build_quick.yml` | Push to any branch | Quick compile check (no APK) |
| `release.yml` | Tag publish | Signed release APK, GitHub Release |
| `sync-player-configs.yml` | Schedule | Sync YouTube player configs |

## Release Process

1. **Version bump** — Core team only (manual)
2. **Create tag** — `v13.6.0` format
3. **Release workflow** — Builds signed APK, creates GitHub release
4. **ProGuard mapping** — Saved for crash deobfuscation

## Dependencies

```toml
# gradle/libs.versions.toml
# Centralized version catalog. All versions here.
[versions]
kotlin = "2.1.0"
compose-bom = "2025.01.00"
media3 = "1.5.0"
hilt = "2.52"
ktor = "3.0.0"
room = "2.6.1"

[libraries]
compose-ui = { module = "androidx.compose.ui:ui" }
media3 = { module = "androidx.media3:media3-exoplayer", version.ref = "media3" }
hilt = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }

[plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

## Environment Variables

| Variable | Purpose | Required For |
|---|---|---|
| `LASTFM_API_KEY` | LastFM scrobbling | Debug + Release |
| `LASTFM_SECRET` | LastFM authentication | Debug + Release |
| `STORE_PASSWORD` | Keystore password | Release build |
| `KEY_ALIAS` | Key alias | Release build |
| `KEY_PASSWORD` | Key password | Release build |
| `ELARA_APPLICATION_ID` | Override app ID | CI (optional) |
| `ELARA_APP_NAME` | Override app name | CI (optional) |

## Git Conventions

```bash
# Commit format
type(scope): short description

# Types
feat     # New feature
fix      # Bug fix
refactor # Code restructuring
style    # Formatting, lint fixes
perf     # Performance improvement
test     # Test additions/changes
chore    # Build, CI, dependencies
docs     # Documentation changes

# Examples
feat(player): add glassmorphism playback controls
fix(search): resolve crash on empty query
refactor(theme): extract glass colors to composable
perf(images): add memory cache for thumbnails
```

## Flavor Strategy

Elara has one flavor: **GMS** (Google Mobile Services).

- Google Cast support
- Auto-updater
- No F-Droid restrictions

The original `foss` and `izzy` flavors were removed during the fork.

## Troubleshooting Build Issues

| Symptom | Likely Cause | Fix |
|---|---|---|
| `Unresolved reference: R` | Build cache stale | `./gradlew clean` |
| `Failed to apply plugin` | AGP/Kotlin version mismatch | Check `libs.versions.toml` |
| `Cannot find symbol Hilt_*` | KSP/Hilt annotation processing | `./gradlew clean` + rebuild |
| `Duplicate class` | Dependency conflict | Use `./gradlew :app:dependencies` |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Signature mismatch | Uninstall app first |
| `java.lang.NoSuchFieldError: myArrayList` | org.json exclusion | Ensure `configurations.exclude` in build.gradle.kts |
