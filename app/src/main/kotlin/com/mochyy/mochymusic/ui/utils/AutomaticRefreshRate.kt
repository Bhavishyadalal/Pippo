package com.mochyy.mochymusic.ui.utils

import android.app.Activity
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/** Keep the current resolution: a faster mode at another resolution is not a compatible choice. */
internal fun highestCompatibleRefreshMode(display: Display): Display.Mode? {
    val current = display.mode
    return display.supportedModes.filter {
        it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight &&
            it.refreshRate.isFinite() && it.refreshRate > 0f
    }.maxWithOrNull(compareBy<Display.Mode> { it.refreshRate }.thenBy { if (it.modeId == current.modeId) 1 else 0 })
}

/** Event-driven window preference. Android remains responsible for battery/thermal/system constraints. */
internal class AutomaticRefreshRate(
    private val activity: Activity,
    private val view: View,
    private val lifecycle: Lifecycle,
) : DefaultLifecycleObserver, View.OnAttachStateChangeListener, AutoCloseable {
    private val displays = activity.getSystemService(DisplayManager::class.java)
    private var registered = false
    private var resumed = false
    private var closed = false
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = request()
        override fun onDisplayRemoved(displayId: Int) = request()
        override fun onDisplayChanged(displayId: Int) {
            if (view.display?.displayId == displayId) request()
        }
    }

    init {
        runCatching {
            displays?.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
            registered = displays != null
        }
        view.addOnAttachStateChangeListener(this)
        lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        resumed = true
        request()
    }

    override fun onPause(owner: LifecycleOwner) {
        resumed = false
        clear()
    }

    override fun onDestroy(owner: LifecycleOwner) = close()
    override fun onViewAttachedToWindow(v: View) = request()
    override fun onViewDetachedFromWindow(v: View) = clear()

    @Suppress("DEPRECATION")
    private fun request() {
        if (closed || !resumed || !view.isAttachedToWindow) return
        val mode = runCatching {
            highestCompatibleRefreshMode(view.display ?: activity.windowManager.defaultDisplay)
        }.getOrNull()
        val modeId = mode?.modeId ?: 0
        val rate = mode?.refreshRate ?: 0f
        val window = activity.window
        val attributes = window.attributes
        // A mode switch can emit another display event; avoid a feedback loop of identical writes.
        if (attributes.preferredDisplayModeId != modeId || attributes.preferredRefreshRate != rate) {
            attributes.preferredDisplayModeId = modeId
            attributes.preferredRefreshRate = rate
            window.attributes = attributes
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            if (view.requestedFrameRate != rate) view.requestedFrameRate = rate
            refreshHighRateVote(view)
        }
    }

    private fun clear() {
        val attributes = activity.window.attributes
        if (attributes.preferredDisplayModeId != 0 || attributes.preferredRefreshRate != 0f) {
            attributes.preferredDisplayModeId = 0
            attributes.preferredRefreshRate = 0f
            activity.window.attributes = attributes
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && view.requestedFrameRate != 0f) {
            view.requestedFrameRate = 0f
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        resumed = false
        if (registered) runCatching { displays?.unregisterDisplayListener(displayListener) }
        lifecycle.removeObserver(this)
        view.removeOnAttachStateChangeListener(this)
        clear()
    }
}
