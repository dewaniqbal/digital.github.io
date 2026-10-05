package app.quranaudio.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.graphics.createBitmap
import app.quranaudio.ui.theme.ArtworkPalette
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates calm, gradient artwork for the lock screen / notification.
 *
 * The catalogue sources do not provide licensed reciter photos, so the app never shows scraped
 * images: each reciter gets a deterministic gradient with their initials instead. Files are
 * cached (small PNGs) and referenced by URI so no large bitmaps cross process boundaries.
 */
@Singleton
class ArtworkCache @Inject constructor(@ApplicationContext private val context: Context) {

    private val dir by lazy { File(context.cacheDir, "artwork").apply { mkdirs() } }

    suspend fun uriFor(reciterId: String, reciterName: String): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(dir, "v1_${reciterId.hashCode().toUInt()}.png")
            if (!file.exists()) {
                val tmp = File(dir, file.name + ".tmp")
                tmp.outputStream().use { render(reciterId, reciterName).compress(Bitmap.CompressFormat.PNG, 100, it) }
                tmp.renameTo(file)
            }
            Uri.fromFile(file)
        }.getOrNull()
    }

    private fun render(seed: String, name: String): Bitmap {
        val size = 512
        val bmp = createBitmap(size, size)
        val canvas = Canvas(bmp)
        val (start, end) = ArtworkPalette.colorsFor(seed)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), start, end, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = RadialGradient(size * 0.3f, size * 0.25f, size * 0.7f, 0x33FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null
        paint.color = 0xF2FFFFFF.toInt()
        paint.textSize = size * 0.30f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        paint.textAlign = Paint.Align.CENTER
        val initials = ArtworkPalette.initials(name)
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(initials, size / 2f, y, paint)
        return bmp
    }
}
