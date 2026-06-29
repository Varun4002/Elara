<div align="center">

<img src="https://raw.githubusercontent.com/Varun4002/Elara/main/app_icon2.jpg" alt="Elara app icon" width="200" />

# Elara

### *Listen Beautifully.*

A premium Android YouTube Music client with a custom glassmorphism design system.

<br/>

[![Latest release](https://img.shields.io/github/v/release/Varun4002/Elara?style=for-the-badge&labelColor=0d1117)](https://github.com/Varun4002/Elara/releases)
[![License](https://img.shields.io/github/license/Varun4002/Elara?style=for-the-badge&labelColor=0d1117)](https://github.com/Varun4002/Elara/blob/main/LICENSE)
[![Downloads](https://img.shields.io/github/downloads/Varun4002/Elara/total?style=for-the-badge&labelColor=0d1117)](https://github.com/Varun4002/Elara/releases)

<br/>

[**Features**](#features) · [**Screenshots**](#screenshots) · [**Build**](#build) · [**License**](#license)

</div>

---

> [!WARNING]
> **Regional Restriction** — If YouTube Music is unavailable in your region, this app will not work without a **VPN or proxy** connecting to a supported region.

---

<div align="center">

## <a id="features"></a>Features

<table>
  <tr>
    <td width="50%" valign="top">

#### Playback
- Stream any song or video from YouTube Music
- Background playback with Media3 ExoPlayer
- Download & cache for offline use
- Skip silence, sleep timer
- Google Cast support

</td>
    <td width="50%" valign="top">

#### Audio
- Audio normalization
- Tempo & pitch control
- 10-band equalizer
- High-quality audio streaming

</td>
  </tr>
  <tr>
    <td width="50%" valign="top">

#### Lyrics & Discovery
- Live synced lyrics (KuGou, LrcLib, BetterLyrics, Paxsenix)
- AI-powered lyrics translation
- Personalized quick picks
- Search songs, albums, artists, videos, playlists

</td>
    <td width="50%" valign="top">

#### Library
- Full library management
- Local playlists with reorder
- Import playlists
- YouTube Music account sync

</td>
  </tr>
  <tr>
    <td width="50%" valign="top">

#### Social
- Listen Together — real-time sync with friends
- LastFM scrobbling
- Discord Rich Presence

</td>
    <td width="50%" valign="top">

#### Interface
- Custom glassmorphism design system
- Material You dynamic color + 19 presets
- Full-screen artwork with floating glass controls
- AMOLED dark mode

</td>
  </tr>
  <tr>
    <td width="50%" valign="top">

#### Recognition
- Music recognition via Shazam API
- Quick Settings tile
- Home screen widgets

</td>
    <td width="50%" valign="top">

#### More
- Auto-updater
- Proxy support (HTTP/SOCKS)
- Android TV support
- Tablet UI scaling

</td>
  </tr>
</table>

</div>

---

<div align="center">

## <a id="screenshots"></a>Screenshots

<img src="https://raw.githubusercontent.com/Varun4002/Elara/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_1.png" alt="Home screen" width="30%" />
<img src="https://raw.githubusercontent.com/Varun4002/Elara/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_2.png" alt="Artist screen" width="30%" />
<img src="https://raw.githubusercontent.com/Varun4002/Elara/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_3.png" alt="Recognize music screen" width="30%" />
<img src="https://raw.githubusercontent.com/Varun4002/Elara/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_4.png" alt="Listen together screen" width="30%" />
<img src="https://raw.githubusercontent.com/Varun4002/Elara/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_5.png" alt="Player screen" width="30%" />
<img src="https://raw.githubusercontent.com/Varun4002/Elara/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_6.png" alt="Player lyrics screen" width="30%" />

</div>

---

## <a id="build"></a>Build

### Prerequisites

- JDK 21
- Android SDK (platform 37+)
- Android platform tools

### Setup

```bash
# Clone with submodules
git clone https://github.com/Varun4002/Elara.git
cd Elara
git submodule update --init --recursive

# Generate protobuf stubs
cd app && bash generate_proto.sh && cd ..

# Create debug keystore (first time)
keytool -genkeypair -v -keystore app/persistent-debug.keystore \
  -storepass android -keypass android -alias androiddebugkey \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"
```

### Build & Test

```bash
# Build debug APK
./gradlew :app:assembleGmsDebug

# APK location:
# app/build/outputs/apk/universalGms/debug/app-universal-gms-debug.apk

# Run unit tests
./gradlew :app:testGmsDebugUnitTest
```

> **Note:** Only the `Gms` flavor is available (Google Mobile Services). The original `foss` and `izzy` flavors were removed during the fork.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository |
| DI | Dagger Hilt |
| Playback | Media3 ExoPlayer |
| Networking | Ktor (YouTube InnerTube API) |
| Database | Room |
| Image Loading | Coil 3 |
| Navigation | Compose Navigation (~45+ routes) |
| Theme | Material You + 19 presets |

---

## Deep Links

| Intent | Action |
|---|---|
| `https://elara.cc/listen/...` | Listen Together invites |
| `elaradiscord://oauth2/callback` | Discord OAuth callback |
| `com.elara.music.action.RECOGNITION` | Music recognition shortcut |
| `youtube.com`, `youtu.be`, `music.youtube.com` | YouTube intent filters |

---

## <a id="license"></a>License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.

```
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
```

---

<div align="center">

### Disclaimer

This project is **not affiliated with, funded, authorized, endorsed by, or in any way associated** with YouTube, Google LLC, or any of their affiliates and subsidiaries.

All trademarks, service marks, and intellectual property rights referenced in this project belong to their respective owners.

</div>

---

<div align="center">

### Acknowledgments

Elara is a fork of **InnerTune** (by Zion Huang & Malopieds) and **OuterTune** (by Davide Garberi & Michael Zh), with a complete glassmorphism UI redesign and significant enhancements.

Special thanks to:
- [Better Lyrics](https://better-lyrics.boidu.dev) — time-synced lyrics
- [metroserver](https://github.com/ElaraGroup/metroserver) — Listen Together backend
- [MusicRecognizer](https://github.com/aleksey-saenko/MusicRecognizer) — Shazam integration
- [Mo Agamy](https://github.com/mostafaalagamy) — original Elara development

</div>
