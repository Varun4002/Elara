---
name: elara-networking
description: Ktor HTTP client, InnerTube YouTube Music API integration, Kotlin Serialization, and network layer patterns for Elara. Use when implementing API calls, serializing/deserializing JSON, handling auth, managing network errors, or extending InnerTube API support.
---

# Elara Networking

## HTTP Stack

| Library | Purpose |
|---|---|
| **Ktor Client** | Core HTTP client (CIO engine) |
| **Ktor Content Negotiation** | JSON serialization |
| **Ktor Encoding** | Gzip/deflate compression |
| **Kotlin Serialization** | JSON parsing (kotlinx.serialization) |
| **OkHttp** | Coil image loading (not used for API) |

## InnerTube API

Elara uses the **InnerTube** protocol to communicate with YouTube Music's internal API. The `:innertube` module handles this.

### Client Configuration

```kotlin
// innertube/src/main/kotlin/com/elara/innertube/InnerTube.kt
class InnerTube(
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        install(ContentEncoding) {
            gzip()
        }
        defaultRequest {
            url("https://music.youtube.com/youtubei/v1/")
            header("Content-Type", "application/json")
            header("User-Agent", USER_AGENT)
        }
    }
)
```

### Request Pattern

```kotlin
suspend fun getHomeContent(): HomeContent {
    val response = httpClient.post("browse") {
        setBody(
            BrowseBody(
                context = defaultContext(),
                browseId = "FEmusic_home"
            )
        )
    }.body<BrowseResponse>()

    return HomePage.parse(response)
}
```

### Models (kotlinx.serialization)

```kotlin
@Serializable
data class BrowseBody(
    val context: Context = defaultContext(),
    @SerialName("browseId") val browseId: String
)

@Serializable
data class BrowseResponse(
    val contents: SectionListRenderer? = null,
    val continuationContents: ContinuationContents? = null,
    val header: MusicResponsiveHeaderRenderer? = null
)

@Serializable
data class Context(
    val client: YouTubeClient = YouTubeClient(),
    val user: UserContext = UserContext(),
    val request: RequestContext = RequestContext()
)
```

### Page Parsers

```kotlin
// innertube/src/main/kotlin/com/elara/innertube/pages/HomePage.kt
object HomePage {
    fun parse(response: BrowseResponse): HomeContent {
        val sections = response.contents
            ?.singleColumnMusicWatchNextResultsRenderer
            ?.let { /*...*/ } ?: emptyList()

        return HomeContent(sections = sections.map { it.toSection() })
    }

    private fun MusicCarouselShelfRenderer.toSection(): ContentSection {
        return ContentSection(
            title = header?.title?.text ?: "",
            items = contents.mapNotNull { it.toMusicItem() }
        )
    }
}
```

## Network Module (Ktor)

```kotlin
// di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }

            expectSuccess = false // Handle errors manually
        }
    }

    @Provides
    @Singleton
    fun provideInnerTube(client: HttpClient): InnerTube {
        return InnerTube(client)
    }
}
```

## Error Handling

```kotlin
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(val code: Int, val message: String) : NetworkResult<Nothing>
    data class NetworkError(val exception: Throwable) : NetworkResult<Nothing>
}

suspend fun <T> safeApiCall(call: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(call())
    } catch (e: ClientRequestException) {
        NetworkResult.Error(e.response.status.value, e.message)
    } catch (e: HttpRequestTimeoutException) {
        NetworkResult.Error(408, "Request timed out")
    } catch (e: Exception) {
        NetworkResult.NetworkError(e)
    }
}
```

## Additional API Integrations

### LastFM (Scrobbling)

```kotlin
// lastfm/src/main/kotlin/com/elara/lastfm/LastFM.kt
class LastFM(private val apiKey: String, private val secret: String) {
    suspend fun scrobble(song: Song, timestamp: Long) {
        // MD5-signed request to LastFM API
    }
}
```

### KuGou (Lyrics)

```kotlin
// kugou/src/main/kotlin/com/elara/kugou/KuGou.kt
class KuGou(private val client: HttpClient) {
    suspend fun searchLyrics(query: String): List<LyricsResult> {
        // Search KuGou's lyrics database
    }
}
```

### Shazam (Music Recognition)

```kotlin
// shazamkit/src/main/kotlin/com/elara/shazamkit/Shazam.kt
class Shazam(private val client: HttpClient) {
    suspend fun recognize(audioData: ByteArray): RecognitionResult? {
        // Send audio fingerprint to Shazam API
    }
}
```

## Authentication

### YouTube Music Account

```kotlin
// OAuth via browser or stored cookies
class AccountManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getAuthToken(): String? {
        // Retrieve stored OAuth token or cookies
    }

    suspend fun login() {
        // Open browser for OAuth, intercept callback
    }
}
```

### Discord OAuth (Listen Together)

```kotlin
// discord/DiscordOAuthActivity.kt
class DiscordOAuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Opens Discord auth URL, receives callback with code
        // Exchanges code for access token
        // Stores token in EncryptedSharedPreferences
    }
}
```

## Network Security

```kotlin
// Network Security Config (res/xml/network_security_config.xml)
// Allows cleartext for local dev, pins certificates for production
```

## ProGuard Rules

```kotlin
# Kotlin Serialization
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
```

## Data Flow

```
Composable → ViewModel → Repository → InnerTube → Ktor → YouTube API
                              ↕
                           Room (cache)
```

## Rules

1. **Always use Ktor** for API calls — Retrofit is not used in this project
2. **Use Kotlin Serialization** — Not Gson or Moshi
3. **Handle errors in the repository layer** — ViewModels should not catch network exceptions
4. **Cache API responses** in Room where appropriate
5. **Never hardcode API keys** — Use BuildConfig fields from local.properties or CI secrets
6. **Network calls are suspend functions** — Never use blocking calls
