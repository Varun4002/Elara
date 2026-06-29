---
name: elara-testing
description: Testing strategies for Elara — unit tests with JUnit/MockK, Compose UI tests, ViewModel tests, and integration tests. Use when writing tests, mocking dependencies, testing Compose UI, or verifying business logic.
---

# Elara Testing Guide

## Test Stack

| Tool | Purpose |
|---|---|
| **JUnit 5** | Test framework |
| **MockK** | Kotlin mocking library |
| **Turbine** | Flow testing |
| **Compose UI Test** | Compose component testing |
| **Robolectric** | Android framework testing (on JVM) |
| **Ktor Mock** | HTTP client mocking |

## Test Location

```
app/src/test/kotlin/com/elara/music/
├── discord/
│   ├── DiscordAuthTest.kt
│   ├── DiscordPresenceTest.kt
│   ├── DiscordReconnectStrategyTest.kt
│   └── DiscordTokenStoreTest.kt
├── ui/
│   └── utils/
│       └── YouTubeUtilsTest.kt
└── utils/
    └── cipher/
        ├── PlayerConfigStoreCooldownTest.kt
        ├── PlayerConfigStoreEpochTest.kt
        └── ...
```

## ViewModel Testing

```kotlin
class HomeViewModelTest {
    private val repository = mockk<MusicRepository>()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        viewModel = HomeViewModel(repository)
    }

    @Test
    fun `loads home content successfully`() = runTest {
        // Arrange
        val expected = HomeContent(sections = listOf(mockk()))
        coEvery { repository.getHomeContent() } returns flowOf(expected)

        // Act
        viewModel.refresh()

        // Assert
        val state = viewModel.uiState.first()
        assertTrue(state is HomeUiState.Success)
        assertEquals(expected, (state as HomeUiState.Success).content)
    }

    @Test
    fun `shows error on network failure`() = runTest {
        // Arrange
        coEvery { repository.getHomeContent() } throws IOException("No network")

        // Act
        viewModel.refresh()

        // Assert
        val state = viewModel.uiState.first()
        assertTrue(state is HomeUiState.Error)
        assertEquals("No network", (state as HomeUiState.Error).exception.message)
    }
}
```

## Repository Testing

```kotlin
class MusicRepositoryTest {
    private val api = mockk<InnerTube>()
    private val db = mockk<MusicDatabase>()
    private val dao = mockk<DatabaseDao>()
    private lateinit var repository: MusicRepository

    @Before
    fun setup() {
        every { db.dao() } returns dao
        repository = MusicRepositoryImpl(api, db)
    }

    @Test
    fun `returns cached content when offline`() = runTest {
        // Arrange
        val cached = HomeContentEntity(...)
        every { dao.getHomeContent() } returns cached
        coEvery { api.getHomeContent() } throws IOException()

        // Act
        val result = repository.getHomeContent().first()

        // Assert
        assertEquals(cached.title, result.title)
    }

    @Test
    fun `fetches and caches fresh content`() = runTest {
        // Arrange
        val fresh = HomeContent(...)
        every { dao.getHomeContent() } returns null
        coEvery { api.getHomeContent() } returns fresh
        coEvery { dao.insertHomeContent(any()) } returns Unit

        // Act
        val result = repository.getHomeContent().first()

        // Assert
        assertEquals(fresh, result)
        coVerify { dao.insertHomeContent(any()) }
    }
}
```

## Flow Testing with Turbine

```kotlin
@Test
fun `player state updates on play pause`() = runTest {
    val viewModel = PlayerViewModel(mockk())

    viewModel.playbackState.test {
        // Initial state
        assertEquals(PlaybackState.IDLE, awaitItem())

        // After play
        viewModel.togglePlayPause()
        assertEquals(PlaybackState.PLAYING, awaitItem())

        // After pause
        viewModel.togglePlayPause()
        assertEquals(PlaybackState.PAUSED, awaitItem())

        cancel()
    }
}
```

## Compose UI Testing

```kotlin
@RunWith(AndroidJUnit4::class)
class GlassButtonTest {
    @Test
    fun `glass button triggers onClick`() {
        var clicked = false
        composeTestRule.setContent {
            GlassButton(onClick = { clicked = true }, label = "Play")
        }

        composeTestRule
            .onNodeWithText("Play")
            .performClick()

        assertTrue(clicked)
    }

    @Test
    fun `glass button shows selected state`() {
        composeTestRule.setContent {
            GlassButton(onClick = {}, selected = true, icon = Icons.Default.Play)
        }

        composeTestRule
            .onNodeWithContentDescription("Play")
            .assertExists()
    }
}

@RunWith(AndroidJUnit4::class)
class PlayerScreenTest {
    @Test
    fun `shows play button when paused`() {
        val vm = PlayerViewModel(mockk())
        composeTestRule.setContent {
            PlayerScreen(viewModel = vm, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithContentDescription("Play")
            .assertIsDisplayed()
    }
}
```

## MockK Patterns

```kotlin
// Basic mock
val repository = mockk<MusicRepository>()
every { repository.getPlaylist(any()) } returns flowOf(mockk())

// Co-routine mock
coEvery { api.getHomeContent() } returns HomeContent()

// Argument capture
val slot = slot<String>()
coEvery { api.search(capture(slot)) } returns SearchResult()
assertEquals("query", slot.captured)

// Relaxed mock (returns defaults)
val relaxed = mockk<InnerTube>(relaxed = true)

// Spy
val spy = spyk(realRepository)
every { spy.getHomeContent() } returns flowOf(customContent)
```

## Running Tests

```bash
# All unit tests
./gradlew :app:testGmsDebugUnitTest

# Single test class
./gradlew :app:testGmsDebugUnitTest --tests "com.elara.music.discord.DiscordAuthTest"

# Compose UI tests (on emulator)
./gradlew :app:connectedGmsDebugAndroidTest
```

## Testing Guidelines

1. **Test behavior, not implementation** — Test what the code does, not how
2. **Use `runTest` for coroutines** — Never use `runBlocking` in tests
3. **Prefer `coEvery`/`coVerify`** for suspend functions
4. **Test error states** — Every flow should have error handling covered
5. **Keep tests fast** — Mock external dependencies, avoid Android framework when possible (use Robolectric)
6. **One assertion per test** — Or at least one logical assertion group per test
7. **Name tests descriptively** — `fun shows_error_when_network_fails()`
