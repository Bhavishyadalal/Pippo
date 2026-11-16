// SleepTimer.kt

package com.mochyy.mochymusic.playback

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SleepTimer(
    private val scope: CoroutineScope,
    var player: Player,
) : Player.Listener {
    private var sleepTimerJob: Job? = null
    // Deadline in elapsed realtime, unaffected by changes to the phone clock.
    var triggerTime by mutableLongStateOf(-1L)
        private set
    var totalDurationMillis by mutableLongStateOf(0L)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var remainingMillisWhenPaused by mutableLongStateOf(0L)
        private set
    var pauseWhenSongEnd by mutableStateOf(false)
        private set

    // a paused timer still counts as set, so the menu row and the cancel button keep showing it
    val isActive: Boolean
        get() = triggerTime != -1L || isPaused || pauseWhenSongEnd

    val isRunning: Boolean
        get() = triggerTime != -1L && !isPaused

    fun start(minute: Int) {
        if (minute == -1) {
            cancelJob()
            pauseWhenSongEnd = true
            triggerTime = -1L
            isPaused = false
            totalDurationMillis = 0L
            remainingMillisWhenPaused = 0L
        } else {
            startSeconds(minute * 60L)
        }
    }

    fun startSeconds(seconds: Long) {
        cancelJob()
        if (seconds <= 0L) {
            clear()
            return
        }
        pauseWhenSongEnd = false
        isPaused = false
        val millis = seconds * 1000L
        totalDurationMillis = millis
        triggerTime = SystemClock.elapsedRealtime() + millis
        scheduleJob(millis)
    }

    fun pause() {
        if (triggerTime > 0L && !isPaused) {
            val left = (triggerTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            cancelJob()
            if (left == 0L) {
                clear()
                player.pause()
                return
            }
            remainingMillisWhenPaused = left
            isPaused = true
            triggerTime = -1L
        }
    }

    fun resume() {
        if (isPaused && remainingMillisWhenPaused > 0L) {
            isPaused = false
            val millis = remainingMillisWhenPaused
            triggerTime = SystemClock.elapsedRealtime() + millis
            scheduleJob(millis)
        }
    }

    // the -1 / +1 min buttons; going below zero ends the timer right away, like it ran out
    fun adjust(deltaSeconds: Long) {
        val deltaMillis = deltaSeconds * 1000L
        if (isPaused) {
            val newRemaining = (remainingMillisWhenPaused + deltaMillis).coerceAtLeast(0L)
            remainingMillisWhenPaused = newRemaining
            totalDurationMillis = maxOf(totalDurationMillis, newRemaining)
            if (newRemaining == 0L) {
                clear()
            }
        } else if (triggerTime > 0L) {
            val left = (triggerTime - SystemClock.elapsedRealtime()) + deltaMillis
            if (left <= 0L) {
                clear()
                player.pause()
            } else {
                cancelJob()
                totalDurationMillis = maxOf(totalDurationMillis, left)
                triggerTime = SystemClock.elapsedRealtime() + left
                scheduleJob(left)
            }
        }
    }

    fun clear() {
        cancelJob()
        pauseWhenSongEnd = false
        isPaused = false
        triggerTime = -1L
        totalDurationMillis = 0L
        remainingMillisWhenPaused = 0L
    }

    private fun scheduleJob(millis: Long) {
        sleepTimerJob = scope.launch {
            var left = millis
            while (left > 0L) {
                delay(left.coerceAtMost(1_000L))
                left = triggerTime - SystemClock.elapsedRealtime()
            }
            player.pause()
            clear()
        }
    }

    private fun cancelJob() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }

    fun notifySongTransition() {
        if (pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            player.pause()
        }
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            player.pause()
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        if (playbackState == Player.STATE_ENDED && pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            player.pause()
        }
    }
}
