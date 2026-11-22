package com.mochyy.mochymusic.core

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.mochyy.mochymusic.playback.audio.AudioOutputs

fun isBluetoothHeadphoneConnected(context: Context): Boolean = bluetoothOutput(context) != null

fun getConnectedBluetoothDeviceName(context: Context): String? =
    bluetoothOutput(context)?.productName?.toString()

private fun bluetoothOutput(context: Context): AudioDeviceInfo? {
    val devices = context.getSystemService(AudioManager::class.java)
        ?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
    return devices.firstOrNull { AudioOutputs.kindOf(it.type) == AudioOutputs.Kind.BLUETOOTH }
        ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
}

fun isBuds(name: String?): Boolean {
    if (name == null) return false
    val lowerName = name.lowercase()
    return lowerName.contains("buds") ||
           lowerName.contains("airpods") ||
           lowerName.contains("earpods") ||
           lowerName.contains("earphone") ||
           lowerName.contains("freebuds") ||
           lowerName.contains("pods")
}

fun isSpeaker(name: String?): Boolean {
    if (name == null) return false
    val lowerName = name.lowercase()
    return lowerName.contains("speaker") ||
           lowerName.contains("soundbar") ||
           lowerName.contains("homepod") ||
           lowerName.contains("echo") ||
           lowerName.contains("boombox") ||
           lowerName.contains("audio system") ||
           lowerName.contains("sound") ||
           lowerName.contains("audio") ||
           lowerName.contains("stereo") ||
           lowerName.contains("music") ||
           lowerName.contains("box") ||
           lowerName.contains("party") ||
           lowerName.contains("waves")
}
