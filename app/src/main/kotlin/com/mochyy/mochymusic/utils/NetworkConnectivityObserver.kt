package com.mochyy.mochymusic.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkConnectivityObserver(context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val initialNetwork = runCatching { connectivityManager.activeNetwork }.getOrNull()
    private val state = ValidatedNetworkState(initialNetwork?.networkHandle, isCurrentlyConnected())
    private val _networkStatus = MutableStateFlow(state.connected)
    // Every collector receives the current status; a Channel split updates between consumers.
    val networkStatus = _networkStatus.asStateFlow()

    data class NetworkSnapshot(val handle: Long?, val metered: Boolean, val validated: Boolean)
    private fun snapshot(): NetworkSnapshot = NetworkSnapshot(
        runCatching { connectivityManager.activeNetwork?.networkHandle }.getOrNull(),
        runCatching { connectivityManager.isActiveNetworkMetered }.getOrDefault(true),
        isCurrentlyConnected(),
    )
    private val _networkSnapshot = MutableStateFlow(snapshot())
    val networkSnapshot = _networkSnapshot.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _networkStatus.value = state.available(network.networkHandle)
            _networkSnapshot.value = snapshot()
        }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _networkStatus.value = state.capabilities(network.networkHandle,
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            _networkSnapshot.value = snapshot()
        }
        override fun onLost(network: Network) {
            _networkStatus.value = state.lost(network.networkHandle)
            _networkSnapshot.value = snapshot()
        }
    }

    init {
        // Default-network observation is available on API 24, this app's minimum SDK.
        runCatching { connectivityManager.registerDefaultNetworkCallback(networkCallback) }
    }

    fun unregister() {
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }

    fun isCurrentlyConnected(): Boolean = runCatching {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(false)
}
