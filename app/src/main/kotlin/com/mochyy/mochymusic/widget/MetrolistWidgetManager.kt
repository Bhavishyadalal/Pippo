// MetrolistWidgetManager.kt

package com.mochyy.mochymusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.mochyy.mochymusic.MainActivity
import com.mochyy.mochymusic.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MochyWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val imageLoader by lazy {
        ImageLoader.Builder(context)
            .crossfade(false)
            .build()
    }

    private val artworkWidgets = ArtworkWidgets(context)
    private var fullLegacyIds = IntArray(0)
    @Volatile var hasTickingWidgets = true
        private set

    private var cachedArtworkUri: String? = null
    private var cachedAlbumArt: Bitmap? = null
    private var cachedCircularAlbumArt: Bitmap? = null

    /** One update at a time: they share the cached artwork, and they no longer run on one thread. */
    private val updating = kotlinx.coroutines.sync.Mutex()

    /**
     * Redraws every placed widget. Runs off the main thread: the system calls and bitmap work here
     * used to run on it every 200ms while music played, taking frames from whatever was on screen.
     * Does nothing at all, not even load the artwork, when no widget is placed.
     */
    suspend fun updateWidgets(
        title: String,
        artist: String,
        artworkUri: String?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long = 0,
        currentPosition: Long = 0,
        // the once-a-second tick for the progress bar: the turntable widget has none, so it's left alone
        progressOnly: Boolean = false,
    ) = withContext(Dispatchers.Default) {
        updating.withLock {
            drawWidgets(title, artist, artworkUri, isPlaying, isLiked, duration, currentPosition, progressOnly)
        }
    }

    private suspend fun drawWidgets(
        title: String,
        artist: String,
        artworkUri: String?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long,
        currentPosition: Long,
        progressOnly: Boolean,
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, MusicWidgetReceiver::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        val turntableComponentName = ComponentName(context, TurntableWidgetReceiver::class.java)
        val turntableWidgetIds = if (progressOnly) IntArray(0) else appWidgetManager.getAppWidgetIds(turntableComponentName)
        val placedArtwork = artworkWidgets.placed()
        if (progressOnly) {
            placedArtwork.forEach { (_, ids) -> artworkWidgets.updateProgress(ids, duration, currentPosition) }
            val activeLegacyIds = fullLegacyIds.filter { it in widgetIds }.toIntArray()
            hasTickingWidgets = activeLegacyIds.isNotEmpty() || placedArtwork.isNotEmpty()
            if (activeLegacyIds.isNotEmpty()) {
                val progress = RemoteViews(context.packageName, R.layout.widget_music_player)
                progress.setLegacyProgress(duration, currentPosition)
                appWidgetManager.partiallyUpdateAppWidget(activeLegacyIds, progress)
            }
            return
        }
        val sizes = widgetIds.asIterable().associateWith { id ->
            widgetSize(context, appWidgetManager.getAppWidgetOptions(id), WidgetSize(230, 138))
        }
        fullLegacyIds = widgetIds.filter { sizes.getValue(it).legacyLayout() == R.layout.widget_music_player }.toIntArray()
        hasTickingWidgets = fullLegacyIds.isNotEmpty() || placedArtwork.isNotEmpty()
        if (widgetIds.isEmpty() && turntableWidgetIds.isEmpty() && placedArtwork.isEmpty()) return

        val albumArt: Bitmap?
        val circularAlbumArt: Bitmap?

        if (artworkUri != null && artworkUri == cachedArtworkUri && cachedAlbumArt != null) {
            albumArt = cachedAlbumArt
            circularAlbumArt = cachedCircularAlbumArt
        } else {
            albumArt = artworkUri?.let { loadAlbumArt(it, 300) }
            circularAlbumArt = albumArt?.let { getCircularBitmap(it) }

            cachedArtworkUri = artworkUri
            cachedAlbumArt = albumArt
            cachedCircularAlbumArt = circularAlbumArt
        }

        if (widgetIds.isNotEmpty()) {
            widgetIds.forEach { widgetId ->
                val views = createRemoteViewsForSize(
                    sizes.getValue(widgetId),
                    title,
                    artist,
                    albumArt,
                    isPlaying,
                    isLiked,
                    duration,
                    currentPosition
                )
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        placedArtwork.forEach { (style, ids) ->
            artworkWidgets.update(style, ids, title, artist, albumArt, isPlaying, isLiked, duration, currentPosition)
        }

        if (turntableWidgetIds.isNotEmpty()) {
            val turntableViews = createTurntableRemoteViews(
                circularAlbumArt,
                isPlaying,
            )
            turntableWidgetIds.forEach { widgetId ->
                appWidgetManager.updateAppWidget(widgetId, turntableViews)
            }
        }
    }

    internal fun updateEmpty(manager: AppWidgetManager, ids: IntArray, turntable: Boolean = false) {
        ids.forEach { id ->
            val views = if (turntable) createTurntableRemoteViews(null, false) else {
                createRemoteViewsForSize(
                    widgetSize(context, manager.getAppWidgetOptions(id), WidgetSize(230, 138)),
                    context.getString(R.string.no_song_playing),
                    context.getString(R.string.tap_to_open),
                    null, false, false, 0, 0,
                )
            }
            manager.updateAppWidget(id, views)
        }
    }

    private fun WidgetSize.legacyLayout(): Int {
        val fullText = widgetLineHeight(context, 16, bold = true) + widgetLineHeight(context, 13)
        val compactText = widgetLineHeight(context, 14, bold = true) + widgetLineHeight(context, 12)
        return when {
            width >= 205 && height >= 78 + fullText -> R.layout.widget_music_player
            width >= 164 && height >= maxOf(48, 10 + compactText) -> R.layout.widget_compact_wide
            else -> R.layout.widget_compact_square
        }
    }

    private fun createRemoteViewsForSize(
        size: WidgetSize,
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long,
        currentPosition: Long,
    ): RemoteViews = when (size.legacyLayout()) {
        R.layout.widget_music_player ->
            createRemoteViews(title, artist, albumArt, isPlaying, isLiked, duration, currentPosition)
        R.layout.widget_compact_wide ->
            createCompactWideRemoteViews(title, artist, albumArt, isPlaying, isLiked).apply {
                setViewVisibility(
                    R.id.widget_wide_like_container,
                    if (size.width >= 225) android.view.View.VISIBLE else android.view.View.GONE,
                )
            }
        else -> createCompactSquareRemoteViews(albumArt, isPlaying)
    }

    private fun RemoteViews.setLegacyProgress(duration: Long, position: Long) {
        val level = if (duration > 0) {
            (position.coerceIn(0L, duration).toDouble() / duration * 10_000).toInt()
        } else 0
        setInt(R.id.widget_progress_fill, "setImageLevel", level)
    }

    private fun createRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long = 0,
        currentPosition: Long = 0
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_music_player)

        views.setTextViewText(R.id.widget_song_title, title)
        views.setTextViewText(R.id.widget_artist_name, artist)

        if (albumArt != null) {
            val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 48f)
            views.setImageViewBitmap(R.id.widget_album_art, roundedAlbumArt)
        } else {
            views.setImageViewBitmap(R.id.widget_album_art, getRoundedDefaultIcon(48f))
        }

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

        val likeIcon = if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav
        views.setImageViewResource(R.id.widget_like_button, likeIcon)

        views.setLegacyProgress(duration, currentPosition)

        views.setOnClickPendingIntent(R.id.widget_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_play_pause_container, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_like_button, getLikeIntent())

        return views
    }

    private suspend fun loadAlbumArt(artworkUri: String, size: Int = 200): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .size(size, size)
                    .allowHardware(false)
                    .crossfade(300)
                    .build()
                val result = imageLoader.execute(request)
                result.image?.toBitmap()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float): Bitmap {

        val size = minOf(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - size) / 2
        val yOffset = (bitmap.height - size) / 2
        val squareBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            shader = BitmapShader(squareBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        if (squareBitmap != bitmap) {
            squareBitmap.recycle()
        }

        return output
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)

        val xOffset = (bitmap.width - size) / 2
        val yOffset = (bitmap.height - size) / 2
        val squareBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            shader = BitmapShader(squareBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        if (squareBitmap != bitmap) {
            squareBitmap.recycle()
        }
        return output
    }

    private fun createCompactSquareRemoteViews(
        albumArt: Bitmap?,
        isPlaying: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact_square)

        if (albumArt != null) {
            val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 48f)
            views.setImageViewBitmap(R.id.widget_compact_album_art, roundedAlbumArt)
        } else {
            views.setImageViewBitmap(R.id.widget_compact_album_art, getRoundedDefaultIcon(48f))
        }

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause_low else R.drawable.ic_widget_play_low
        views.setImageViewResource(R.id.widget_compact_play_pause, playPauseIcon)

        views.setOnClickPendingIntent(R.id.widget_compact_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_compact_play_container, getPlayPauseIntent())

        return views
    }

    private fun createCompactWideRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact_wide)

        views.setTextViewText(R.id.widget_wide_song_title, title)
        views.setTextViewText(R.id.widget_wide_artist_name, artist)

        if (albumArt != null) {
            val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 48f)
            views.setImageViewBitmap(R.id.widget_wide_album_art, roundedAlbumArt)
        } else {

            views.setImageViewBitmap(R.id.widget_wide_album_art, getRoundedDefaultIcon(48f))
        }

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause_low else R.drawable.ic_widget_play_low
        views.setImageViewResource(R.id.widget_wide_play_pause, playPauseIcon)

        val likeIcon = if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav
        views.setImageViewResource(R.id.widget_wide_like_button, likeIcon)

        views.setOnClickPendingIntent(R.id.widget_wide_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_wide_play_container, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_wide_like_button, getLikeIntent())

        return views
    }

    private fun createTurntableRemoteViews(
        circularAlbumArt: Bitmap?,
        isPlaying: Boolean,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_turntable)

        if (circularAlbumArt != null) {
            views.setImageViewBitmap(R.id.widget_turntable_album_art, circularAlbumArt)
        } else {

            views.setImageViewBitmap(R.id.widget_turntable_album_art, getCircularDefaultIcon())
        }

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause_secondary else R.drawable.ic_widget_play_secondary
        views.setImageViewResource(R.id.widget_turntable_play_pause, playPauseIcon)

        views.setOnClickPendingIntent(R.id.widget_turntable_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_turntable_play_container, getTurntablePlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_turntable_prev_button, getTurntablePreviousIntent())
        views.setOnClickPendingIntent(R.id.widget_turntable_next_button, getTurntableNextIntent())

        return views
    }

    private fun getCircularDefaultIcon(): Bitmap {

        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val size = 300
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return getCircularBitmap(bitmap)
    }

    private fun getRoundedDefaultIcon(cornerRadius: Float): Bitmap {

        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val size = 300
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return getRoundedCornerBitmap(bitmap, cornerRadius)
    }

    private fun getOpenAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getPlayPauseIntent(): PendingIntent {
        val intent = Intent(context, MusicWidgetReceiver::class.java).apply {
            action = MusicWidgetReceiver.ACTION_PLAY_PAUSE
        }
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getLikeIntent(): PendingIntent {
        val intent = Intent(context, MusicWidgetReceiver::class.java).apply {
            action = MusicWidgetReceiver.ACTION_LIKE
        }
        return PendingIntent.getBroadcast(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getTurntablePlayPauseIntent(): PendingIntent {
        val intent = Intent(context, TurntableWidgetReceiver::class.java).apply {
            action = TurntableWidgetReceiver.ACTION_TURNTABLE_PLAY_PAUSE
        }
        return PendingIntent.getBroadcast(
            context,
            3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getTurntableNextIntent(): PendingIntent {
        val intent = Intent(context, TurntableWidgetReceiver::class.java).apply {
            action = TurntableWidgetReceiver.ACTION_TURNTABLE_NEXT
        }
        return PendingIntent.getBroadcast(
            context,
            4,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getTurntablePreviousIntent(): PendingIntent {
        val intent = Intent(context, TurntableWidgetReceiver::class.java).apply {
            action = TurntableWidgetReceiver.ACTION_TURNTABLE_PREVIOUS
        }
        return PendingIntent.getBroadcast(
            context,
            5,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
