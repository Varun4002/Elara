---
name: elara-security
description: Security best practices for Elara — secure storage, network security, ProGuard/R8 obfuscation, certificate handling, and dependency safety. Use when handling user tokens, API keys, ProGuard rules, or reviewing security-sensitive code.
---

# Elara Security Guide

## Security Requirements

| Concern | Implementation |
|---|---|
| User auth tokens | EncryptedSharedPreferences |
| API keys | BuildConfig (from local.properties / CI secrets) |
| Discord tokens | EncryptedSharedPreferences |
| Network | HTTPS only + certificate pinning (optional) |
| Downloaded media | App-private directory |
| ProGuard | R8 full mode for release builds |

## Credential Storage

### API Keys

```kotlin
// BuildConfig fields (set in app/build.gradle.kts)
val lastFmKey = localProperties.getProperty("LASTFM_API_KEY")
    ?: System.getenv("LASTFM_API_KEY") ?: ""
buildConfigField("String", "LASTFM_API_KEY", "\"$lastFmKey\"")
```

```kotlin
// Usage (safe — stripped by ProGuard in release)
val apiKey = BuildConfig.LASTFM_API_KEY
```

### User Tokens (Encrypted)

```kotlin
// Use EncryptedSharedPreferences for tokens
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "elara_secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

fun saveDiscordToken(token: String) {
    encryptedPrefs.edit().putString("discord_token", token).apply()
}

fun getDiscordToken(): String? {
    return encryptedPrefs.getString("discord_token", null)
}
```

## Network Security

### Network Security Config

```xml
<!-- res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">music.youtube.com</domain>
        <domain includeSubdomains="true">youtube.com</domain>
        <domain includeSubdomains="true">yt3.ggpht.com</domain>
        <!-- Pin config for production:
        <pin-set expiration="2027-01-01">
            <pin digest="SHA-256">...</pin>
        </pin-set>
        -->
    </domain-config>
    <!-- Allow cleartext for local development -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

### Certificate Pinning (Optional)

```kotlin
// Implement in OkHttp or Ktor engine
val client = HttpClient(CIO) {
    engine {
        https {
            trustManager = CustomTrustManager()
        }
    }
}
```

## ProGuard / R8

### Keep Rules

```kotlin
# Kotlin Serialization — CRITICAL
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.elara.**$$serializer { *; }
-keepclassmembers class com.elara.** { *** Companion; }
-keepclasseswithmembers class com.elara.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Compose
-keep class androidx.compose.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Media3
-keep class androidx.media3.** { *; }

# Ktor (if serialization issues)
-keep class io.ktor.** { *; }

# Remove logging in release
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
```

## Data Safety

### Local Data

| Data Type | Storage | Encryption |
|---|---|---|
| Playlist metadata | Room (SQLite) | No (app sandbox) |
| OAuth tokens | EncryptedSharedPreferences | AES-256 GCM |
| Downloaded media | App cache dir | No (sandbox-only access) |
| Search history | Room | No (opt-out available) |
| Preferences | DataStore | No |

### User Data Handling

```kotlin
// Clear user data on account switch
fun clearUserData() {
    encryptedPrefs.edit().clear().apply()
    database.clearAllTables()
    cacheDir.deleteRecursively()
}
```

## Dependency Safety

- **Verify dependency checksums** in `gradle/verification-metadata.xml`
- **Renovate bot** configured for automated dependency updates
- **Dependabot** enabled on GitHub for vulnerability alerts

## Android Manifest

```xml
<!-- Security-related manifest flags -->
<application
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    android:networkSecurityConfig="@xml/network_security_config">
</application>
```

## Development Notes

1. **API keys never committed** — All keys via `local.properties` or CI env vars
2. **Discord App ID** is public (part of OAuth flow) — safe in BuildConfig
3. **LastFM keys** are per-developer — set in local properties
4. **YouTube InnerTube API** requires no keys — uses client version + cookies
5. **ProGuard mapping files** should be saved for crash deobfuscation
6. **No hardcoded URLs** with secrets — use BuildConfig or resources
