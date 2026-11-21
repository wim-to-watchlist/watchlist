package to.wim.watchlist.ui.demo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import to.wim.watchlist.data.demo.DemoCatalog
import to.wim.watchlist.data.demo.DemoPalette
import to.wim.watchlist.data.demo.DemoTitle

/**
 * Draws the demo's pictures on the phone instead of downloading them: a sky, a sun and hills in
 * the title's colours, with the title on posters. Images for `demo:poster/{id}`,
 * `demo:fanart/{id}` and `demo:episode/{id}/{season}/{episode}`.
 */
class DemoArtFetcher(
    private val data: String,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val parts = data.removePrefix(DemoCatalog.SCHEME).split('/')
        val title = parts.getOrNull(1)?.toLongOrNull()?.let { DemoCatalog.title(it) } ?: return null
        val bitmap =
            when (parts[0]) {
                "poster" -> draw(title, 380, 570, sunX = 0.5f, withTitle = true)
                "fanart" -> draw(title, 960, 540, sunX = 0.72f, withTitle = false)
                "episode" -> {
                    val n = parts.getOrNull(3)?.toIntOrNull() ?: 1
                    draw(title, 600, 338, sunX = 0.15f + (n * 37 % 70) / 100f, withTitle = false, sunHeight = 0.2f + (n % 4) * 0.08f)
                }
                else -> return null
            }
        return ImageFetchResult(image = bitmap.asImage(), isSampled = false, dataSource = DataSource.MEMORY)
    }

    private fun draw(
        title: DemoTitle,
        width: Int,
        height: Int,
        sunX: Float,
        withTitle: Boolean,
        sunHeight: Float = 0.25f,
    ): Bitmap {
        val palette: DemoPalette = DemoCatalog.palettes[title.palette % DemoCatalog.palettes.size]
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = width.toFloat()
        val h = height.toFloat()
        val horizon = h * if (withTitle) 0.6f else 0.66f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(0f, 0f, 0f, horizon, palette.skyTop.toInt(), palette.skyBottom.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.color = palette.sun.toInt()
        canvas.drawCircle(w * sunX, h * sunHeight, minOf(w, h) * 0.16f, paint)

        // Three layers of hills, lighter at the back.
        listOf(0.55f to 0f, 0.8f to 0.08f, 1f to 0.17f).forEachIndexed { i, (alpha, drop) ->
            paint.color = palette.hills.toInt()
            paint.alpha = (alpha * 255).toInt()
            val top = horizon + h * drop
            val path =
                Path().apply {
                    moveTo(0f, top + h * 0.02f * i)
                    quadTo(w * (0.25f + 0.1f * i), top - h * 0.05f, w * 0.55f, top + h * 0.01f)
                    quadTo(w * (0.8f - 0.05f * i), top + h * 0.05f, w, top - h * 0.02f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
            canvas.drawPath(path, paint)
        }

        if (withTitle) {
            val text =
                TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textSize = w * 0.095f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    setShadowLayer(w * 0.02f, 0f, w * 0.006f, 0x99000000.toInt())
                }
            val maxWidth = (w * 0.84f).toInt()
            val layout = StaticLayout.Builder.obtain(title.title, 0, title.title.length, text, maxWidth).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
            canvas.save()
            canvas.translate((w - maxWidth) / 2f, horizon - layout.height - h * 0.03f)
            layout.draw(canvas)
            canvas.restore()
        }
        return bitmap
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? = data.toString().takeIf { it.startsWith(DemoCatalog.SCHEME) }?.let { DemoArtFetcher(it) }
    }
}
