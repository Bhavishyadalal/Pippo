package com.mochyy.mochymusic.playback.custom

/** Describes a failed playback lookup separately from decoder and transport failures. */
class NoProviderException(
    reason: String,
    val retryable: Boolean,
    val providerId: String? = null,
    cause: Throwable? = null,
) : java.io.IOException(reason, cause)
