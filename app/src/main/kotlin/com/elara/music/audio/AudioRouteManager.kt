package com.elara.music.audio

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AudioRouteManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val castIsCasting: StateFlow<Boolean>? = null,
    private val castDeviceName: StateFlow<String?>? = null,
) : PlaybackRouteProvider {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

    private val _destination = MutableStateFlow(PlaybackDestination.deviceSpeaker())
    override val destination: StateFlow<PlaybackDestination> = _destination.asStateFlow()

    private var castJob: Job? = null

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            resolveDestination()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
            resolveDestination()
        }
    }

    override fun start() {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        castJob = scope.launch(Dispatchers.Main) {
            combine(
                castIsCasting ?: MutableStateFlow(false),
                castDeviceName ?: MutableStateFlow(null),
            ) { isCasting, deviceName ->
                Pair(isCasting, deviceName)
            }.collect {
                resolveDestination()
            }
        }
        resolveDestination()
    }

    override fun stop() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        castJob?.cancel()
        castJob = null
    }

    private fun resolveDestination() {
        _destination.value = detect()
    }

    private fun detect(): PlaybackDestination {
        val isCasting = castIsCasting?.value ?: false
        if (isCasting) {
            return PlaybackDestination(
                id = "chromecast",
                displayName = castDeviceName?.value ?: "Chromecast",
                routeCategory = RouteCategory.CAST,
                deviceType = DeviceType.TV,
            )
        }

        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_CAR) {
            val btCar = findBluetoothDeviceNames().firstOrNull()
            return PlaybackDestination(
                id = "android_auto",
                displayName = btCar ?: "Android Auto",
                routeCategory = RouteCategory.CAR,
                deviceType = DeviceType.CAR,
            )
        }

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val detected = devices.mapNotNull { classifyDevice(it) }
            .sortedBy { priority(it) }

        return detected.firstOrNull() ?: PlaybackDestination.deviceSpeaker()
    }

    private fun findBluetoothDeviceNames(): List<String> {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.filter {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }.mapNotNull { it.productName?.toString() }
    }

    private fun classifyDevice(device: AudioDeviceInfo): PlaybackDestination? {
        val name = device.productName?.toString() ?: return null
        val address = device.address ?: ""

        return when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                val deviceType = when {
                    name.contains("earbud", true) || name.contains("buds", true) || name.contains("airpods", true) -> DeviceType.EARBUDS
                    name.contains("headphone", true) || name.contains("headset", true) || name.contains("headphone", true) || name.contains("wh-", true) || name.contains("xm", true) -> DeviceType.HEADPHONES
                    name.contains("speaker", true) || name.contains("sound", true) || name.contains("jbl", true) || name.contains("sonos", true) -> DeviceType.SPEAKER
                    else -> DeviceType.HEADPHONES
                }
                PlaybackDestination(
                    id = "bt_$address",
                    displayName = name,
                    routeCategory = RouteCategory.BLUETOOTH,
                    deviceType = deviceType,
                )
            }

            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> PlaybackDestination(
                id = "wired_$address",
                displayName = name,
                routeCategory = RouteCategory.LOCAL,
                deviceType = DeviceType.HEADPHONES,
            )

            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> {
                val isDac = name.contains("dac", true) || name.contains("audio", true) || name.contains("amp", true)
                PlaybackDestination(
                    id = "usb_$address",
                    displayName = name,
                    routeCategory = RouteCategory.USB,
                    deviceType = if (isDac) DeviceType.DAC else DeviceType.HEADPHONES,
                )
            }

            AudioDeviceInfo.TYPE_AUX_LINE -> PlaybackDestination(
                id = "aux_$address",
                displayName = name,
                routeCategory = RouteCategory.LOCAL,
                deviceType = DeviceType.HEADPHONES,
            )

            AudioDeviceInfo.TYPE_DOCK,
            AudioDeviceInfo.TYPE_DOCK_ANALOG -> PlaybackDestination(
                id = "dock_$address",
                displayName = name,
                routeCategory = RouteCategory.LOCAL,
                deviceType = DeviceType.SPEAKER,
            )

            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC -> PlaybackDestination(
                id = "hdmi_$address",
                displayName = name,
                routeCategory = RouteCategory.LOCAL,
                deviceType = DeviceType.TV,
            )

            else -> null
        }
    }

    private fun priority(dest: PlaybackDestination): Int = when (dest.routeCategory) {
        RouteCategory.CAST -> 0
        RouteCategory.CAR -> 1
        RouteCategory.BLUETOOTH -> when (dest.deviceType) {
            DeviceType.SPEAKER -> 2
            DeviceType.HEADPHONES -> 3
            DeviceType.EARBUDS -> 4
            else -> 5
        }
        RouteCategory.USB -> when (dest.deviceType) {
            DeviceType.DAC -> 6
            else -> 7
        }
        RouteCategory.LOCAL -> when (dest.deviceType) {
            DeviceType.HEADPHONES -> 8
            DeviceType.TV -> 9
            DeviceType.SPEAKER -> 10
            else -> 11
        }
    }
}
