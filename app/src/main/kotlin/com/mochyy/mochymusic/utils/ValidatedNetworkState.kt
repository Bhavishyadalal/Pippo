package com.mochyy.mochymusic.utils

/** Default-network callbacks can outlive a handover. A lost old network must not hide its replacement. */
internal class ValidatedNetworkState(initialNetwork: Long? = null, initiallyConnected: Boolean = false) {
    private var current = initialNetwork
    var connected: Boolean = initialNetwork != null && initiallyConnected
        private set

    @Synchronized fun available(network: Long): Boolean {
        if (current != network) {
            current = network
            connected = false // availability alone does not mean internet access
        }
        return connected
    }

    @Synchronized fun capabilities(network: Long, internet: Boolean, validated: Boolean): Boolean {
        if (network == current) connected = internet && validated
        return connected
    }

    @Synchronized fun lost(network: Long): Boolean {
        if (network == current) {
            current = null
            connected = false
        }
        return connected
    }
}
