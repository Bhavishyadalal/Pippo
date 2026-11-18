package com.mochyy.mochymusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.widget.RemoteViews
import androidx.annotation.StringRes
import com.mochyy.mochymusic.MainActivity
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.playback.MusicService
import com.mochyy.mochymusic.utils.makeTimeString

internal enum class ArtworkWidgetStyle(val receiver: Class<out ArtworkWidgetReceiver>, @StringRes val label: Int) {
    VINYL(VinylCollageWidget::class.java, R.string.widget_vinyl_collage),
    CLOUD(CloudNotesWidget::class.java, R.string.widget_cloud_notes),
    NOIR(NoirSleeveWidget::class.java, R.string.widget_noir_sleeve),
    WARM(AmberFrameWidget::class.java, R.string.widget_amber_frame),
    NEON(CoralGlowWidget::class.java, R.string.widget_coral_glow),
}

abstract class ArtworkWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        if (MusicService.isRunning && requestPlaybackState(context)) return
        val style = ArtworkWidgetStyle.entries.firstOrNull { it.receiver == javaClass } ?: return
        val views = ArtworkWidgets(context).emptyView(style)
        manager.updateAppWidget(appWidgetIds, views)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        onUpdate(context, manager, intArrayOf(appWidgetId))
    }

    private fun requestPlaybackState(context: Context): Boolean = runCatching {
        context.startService(Intent(context, MusicService::class.java).setAction(MusicWidgetReceiver.ACTION_UPDATE_WIDGET))
        true
    }.getOrDefault(false)
}

class VinylCollageWidget : ArtworkWidgetReceiver()
class CloudNotesWidget : ArtworkWidgetReceiver()
class NoirSleeveWidget : ArtworkWidgetReceiver()
class AmberFrameWidget : ArtworkWidgetReceiver()
class CoralGlowWidget : ArtworkWidgetReceiver()

internal class ArtworkWidgets(private val context: Context) {
    private val manager get() = AppWidgetManager.getInstance(context)
    private val surfaces = mutableMapOf<ArtworkWidgetStyle, Bitmap>()
    private var renderedArt: Bitmap? = null

    fun placed(): List<Pair<ArtworkWidgetStyle, IntArray>> = ArtworkWidgetStyle.entries.mapNotNull { style ->
        val ids = manager.getAppWidgetIds(ComponentName(context, style.receiver))
        if (ids.isEmpty()) null else style to ids
    }

    fun updateProgress(ids: IntArray, duration: Long, position: Long) {
        val progress = RemoteViews(context.packageName, R.layout.widget_pippo_artwork)
        progress.setTimeline(duration, position)
        manager.partiallyUpdateAppWidget(ids, progress)
    }

    fun update(
        style: ArtworkWidgetStyle,
        ids: IntArray,
        title: String,
        artist: String,
        art: Bitmap?,
        playing: Boolean,
        liked: Boolean,
        duration: Long,
        position: Long,
    ) {
        manager.updateAppWidget(ids, render(style, title, artist, art, playing, liked, duration, position))
    }

    fun emptyView(style: ArtworkWidgetStyle): RemoteViews =
        render(style, context.getString(R.string.no_song_playing), context.getString(R.string.tap_to_open), null, false, false, 0, 0)

    private fun render(
        style: ArtworkWidgetStyle,
        title: String,
        artist: String,
        art: Bitmap?,
        playing: Boolean,
        liked: Boolean,
        duration: Long,
        position: Long,
    ): RemoteViews {
        if (renderedArt !== art) {
            renderedArt = art
            surfaces.clear()
        }
        val chrome = surfaces.getOrPut(style) { renderBackground(style, art) }
        return RemoteViews(context.packageName, R.layout.widget_pippo_artwork).apply {
            setImageViewBitmap(R.id.art_widget_background, chrome)
            val inset = (14 * context.resources.displayMetrics.density).toInt()
            val vinylEndInset = (44 * context.resources.displayMetrics.density).toInt()
            val vertical = (10 * context.resources.displayMetrics.density).toInt()
            val rtl = context.resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
            val end = if (style == ArtworkWidgetStyle.VINYL) vinylEndInset else inset
            setViewPadding(R.id.art_widget_controls, if (rtl) end else inset, vertical, if (rtl) inset else end, vertical)
            setTextViewText(R.id.art_widget_title, title)
            setTextViewText(R.id.art_widget_artist, artist)
            setTimeline(duration, position)
            setImageViewResource(R.id.art_widget_toggle, if (playing) R.drawable.ic_widget_pause_secondary else R.drawable.ic_widget_play_secondary)
            setContentDescription(R.id.art_widget_toggle, context.getString(if (playing) R.string.pause else R.string.play))
            setImageViewResource(R.id.art_widget_like, if (liked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav)
            setContentDescription(R.id.art_widget_like, context.getString(if (liked) R.string.widget_art_remove_like else R.string.widget_art_like))
            setOnClickPendingIntent(R.id.art_widget_root, PendingIntent.getActivity(
                context, 80, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ))
            setOnClickPendingIntent(R.id.art_widget_previous, action(81, MusicWidgetReceiver.ACTION_PREVIOUS))
            setOnClickPendingIntent(R.id.art_widget_toggle, action(82, MusicWidgetReceiver.ACTION_PLAY_PAUSE))
            setOnClickPendingIntent(R.id.art_widget_next, action(83, MusicWidgetReceiver.ACTION_NEXT))
            setOnClickPendingIntent(R.id.art_widget_like, action(84, MusicWidgetReceiver.ACTION_LIKE))
        }
    }

    private fun action(id: Int, action: String): PendingIntent = PendingIntent.getBroadcast(
        context, id, Intent(context, MusicWidgetReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun RemoteViews.setTimeline(duration: Long, position: Long) {
        val played = if (duration > 0L) position.coerceIn(0L, duration) else position.coerceAtLeast(0L)
        val fraction = if (duration > 0L) (played.toDouble() / duration * 1000).toInt() else 0
        setProgressBar(R.id.art_widget_progress, 1000, fraction, false)
        setTextViewText(R.id.art_widget_elapsed, makeTimeString(played))
        setTextViewText(R.id.art_widget_remaining, if (duration > 0L) "−" + makeTimeString(duration - played) else "")
    }
}

private fun renderBackground(style: ArtworkWidgetStyle, artwork: Bitmap?): Bitmap {
    // Full updates happen on track/state changes. Keep the alpha channel for wallpaper cutouts.
    val bitmap = Bitmap.createBitmap(360, 360, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    fun tint(color: Int) {
        paint.shader = null
        paint.color = color
    }

    fun color(value: Long) = value.toInt()
    fun rounded(bounds: RectF, radius: Float, color: Int) {
        tint(color)
        canvas.drawRoundRect(bounds, radius, radius, paint)
    }
    fun image(bounds: RectF) {
        if (artwork == null) return
        val scale = maxOf(bounds.width() / artwork.width, bounds.height() / artwork.height)
        val width = artwork.width * scale
        val height = artwork.height * scale
        val target = RectF(
            bounds.centerX() - width / 2f, bounds.centerY() - height / 2f,
            bounds.centerX() + width / 2f, bounds.centerY() + height / 2f,
        )
        canvas.save()
        canvas.clipRect(bounds)
        tint(Color.WHITE)
        canvas.drawBitmap(artwork, null, target, paint)
        canvas.restore()
    }

    when (style) {
        ArtworkWidgetStyle.VINYL -> {
            tint(color(0xFF1A171C))
            canvas.drawCircle(224f, 146f, 123f, paint)
            for (radius in 30..120 step 5) {
                tint(color(if (radius % 2 == 0) 0xFF3D3740 else 0xFF29232C))
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawCircle(224f, 146f, radius.toFloat(), paint)
                paint.style = Paint.Style.FILL
            }
            tint(color(0xFFBBA29E))
            canvas.drawCircle(224f, 146f, 36f, paint)
            val sleeve = RectF(14f, 14f, 284f, 330f)
            tint(color(0xFF2C252E))
            canvas.drawPath(Path().apply {
                moveTo(38f, 296f)
                cubicTo(66f, 303f, 59f, 343f, 76f, 351f)
                cubicTo(93f, 358f, 94f, 324f, 105f, 327f)
                cubicTo(120f, 330f, 117f, 359f, 130f, 359f)
                cubicTo(143f, 359f, 140f, 326f, 153f, 323f)
                cubicTo(174f, 317f, 176f, 351f, 188f, 349f)
                cubicTo(200f, 349f, 201f, 331f, 207f, 321f)
                lineTo(224f, 296f)
                close()
            }, paint)
            rounded(sleeve, 28f, color(0xFF2C252E))
            canvas.save()
            canvas.clipPath(Path().apply { addRoundRect(sleeve, 28f, 28f, Path.Direction.CW) })
            image(RectF(18f, 75f, 273f, 241f))
            paint.shader = LinearGradient(0f, 60f, 315f, 280f, intArrayOf(color(0x6630272F), color(0xF12C252E)), null, Shader.TileMode.CLAMP)
            canvas.drawRect(sleeve, paint)
            canvas.restore()
            tint(color(0xFFBA9CA6))
            canvas.drawPath(Path().apply {
                moveTo(12f, 43f); lineTo(75f, 11f); lineTo(84f, 28f); lineTo(19f, 59f); close()
            }, paint)
            canvas.drawCircle(277f, 215f, 6f, paint)
            canvas.drawCircle(295f, 228f, 4f, paint)
        }
        ArtworkWidgetStyle.CLOUD -> {
            rounded(RectF(10f, 10f, 350f, 350f), 66f, color(0xEF203E53))
            val cloud = Path().apply {
                addOval(RectF(36f, 86f, 187f, 223f), Path.Direction.CW)
                addOval(RectF(103f, 66f, 254f, 225f), Path.Direction.CW)
                addOval(RectF(177f, 89f, 327f, 221f), Path.Direction.CW)
            }
            tint(color(0xFF729CB2))
            canvas.drawPath(cloud, paint)
            canvas.save()
            canvas.clipPath(cloud)
            image(RectF(36f, 66f, 327f, 225f))
            canvas.restore()
            tint(color(0xFFFFDBAC))
            canvas.drawCircle(15f, 214f, 8f, paint)
            canvas.drawCircle(22f, 233f, 5f, paint)
            canvas.drawCircle(339f, 103f, 6f, paint)
            canvas.drawCircle(348f, 125f, 8f, paint)
        }
        ArtworkWidgetStyle.NOIR, ArtworkWidgetStyle.WARM, ArtworkWidgetStyle.NEON -> {
            val edge = when (style) {
                ArtworkWidgetStyle.NOIR -> color(0xAA8C90A1)
                ArtworkWidgetStyle.WARM -> color(0xC9E9AA7C)
                else -> color(0xC9FF5A6E)
            }
            val panel = RectF(8f, 8f, 352f, 352f)
            rounded(panel, 45f, edge)
            val inner = RectF(12f, 12f, 348f, 348f)
            val radius = 42f
            canvas.save()
            canvas.clipPath(Path().apply { addRoundRect(inner, radius, radius, Path.Direction.CW) })
            rounded(inner, radius, when (style) {
                ArtworkWidgetStyle.NOIR -> color(0xEF15151B)
                ArtworkWidgetStyle.WARM -> color(0xEC432A26)
                else -> color(0xF1371425)
            })
            image(inner)
            tint(Color.WHITE)
            paint.shader = LinearGradient(
                0f, 12f, 0f, 348f,
                intArrayOf(color(0xDD0C080D), color(0x190C080D), color(0xEF0C080D)),
                floatArrayOf(0f, .49f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawRect(inner, paint)
            if (style != ArtworkWidgetStyle.NOIR) {
                paint.shader = RadialGradient(340f, 336f, 145f, edge, Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawRect(inner, paint)
            }
            canvas.restore()
        }
    }
    return bitmap
}
