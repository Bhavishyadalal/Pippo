// MusicWidgetReceiver.kt

package com.mochyy.mochymusic.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.mochyy.mochymusic.playback.MusicService

class MusicWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        if (MusicService.isRunning) {
            requestWidgetRefresh(context)
        } else {
            MochyWidgetManager(context).updateEmpty(appWidgetManager, appWidgetIds)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_LIKE, ACTION_NEXT, ACTION_PREVIOUS -> {

                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = intent.action
                    putExtras(intent)
                }
                try {
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {

                }
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.mochyy.mochymusic.widget.PLAY_PAUSE"
        const val ACTION_LIKE = "com.mochyy.mochymusic.widget.LIKE"
        const val ACTION_NEXT = "com.mochyy.mochymusic.widget.NEXT"
        const val ACTION_PREVIOUS = "com.mochyy.mochymusic.widget.PREVIOUS"
        const val ACTION_UPDATE_WIDGET = "com.mochyy.mochymusic.widget.UPDATE_WIDGET"
    }
}


internal fun requestWidgetRefresh(context: Context): Boolean = runCatching {
    context.startService(Intent(context, MusicService::class.java).setAction(MusicWidgetReceiver.ACTION_UPDATE_WIDGET))
    true
}.getOrDefault(false)
