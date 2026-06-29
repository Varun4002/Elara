package com.elara.music.audio

import kotlinx.coroutines.flow.StateFlow

enum class RouteCategory {
    LOCAL,
    BLUETOOTH,
    CAST,
    USB,
    CAR,
}

enum class DeviceType {
    SPEAKER,
    EARBUDS,
    HEADPHONES,
    DAC,
    TV,
    CAR,
    UNKNOWN,
}

data class PlaybackDestination(
    val id: String,
    val displayName: String,
    val routeCategory: RouteCategory,
    val deviceType: DeviceType,
    val subtitle: String? = null,
    val batteryLevel: Int? = null,
    val codec: String? = null,
    val supportsBattery: Boolean = false,
    val supportsCodec: Boolean = false,
) {
    val isBluetooth: Boolean
        get() = routeCategory == RouteCategory.BLUETOOTH
    val isCasting: Boolean
        get() = routeCategory == RouteCategory.CAST
    val isWireless: Boolean
        get() = routeCategory in setOf(RouteCategory.BLUETOOTH, RouteCategory.CAST, RouteCategory.CAR)
    val isExternal: Boolean
        get() = routeCategory != RouteCategory.LOCAL

    companion object {
        fun deviceSpeaker() = PlaybackDestination(
            id = "device_speaker",
            displayName = "This Device",
            routeCategory = RouteCategory.LOCAL,
            deviceType = DeviceType.SPEAKER,
        )
    }
}

interface PlaybackRouteProvider {
    val destination: StateFlow<PlaybackDestination>
    fun start()
    fun stop()
}
