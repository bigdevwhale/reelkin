package app.reelkin.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import app.reelkin.ui.art.Buf
import app.reelkin.ui.art.CoverArt
import java.io.File
import java.util.UUID

/** Posters in the app's own folder, and their small pixel-ready copies in memory. */
object Covers {
    private val cache = HashMap<String, CoverArt?>()

    fun dir(context: Context): File = File(context.filesDir, "covers").apply { mkdirs() }

    fun file(context: Context, name: String?): File? = name?.let { File(dir(context), it) }?.takeIf { it.exists() }

    /** Removes a poster unless something else still shows it. */
    fun delete(context: Context, name: String?) { file(context, name)?.delete(); synchronized(cache) { cache.remove(name) } }

    /** Saves picture bytes as a poster, shrunk to a sane size and turned upright; the file's name. */
    fun save(context: Context, bytes: ByteArray): String? {
        val bmp = decode(bytes, 480) ?: return null
        val name = UUID.randomUUID().toString() + ".jpg"
        File(dir(context), name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return name
    }

    fun saveUri(context: Context, uri: Uri): String? = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()?.let { save(context, it) }

    private fun decode(bytes: ByteArray, max: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        if (o.outWidth <= 0) return null
        var sample = 1
        while (o.outWidth / (sample * 2) >= max || o.outHeight / (sample * 2) >= max) sample *= 2
        val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val rot = runCatching { ExifInterface(bytes.inputStream()).rotationDegrees }.getOrDefault(0)
        return if (rot == 0) b else Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
    }

    /** The picture as a small buffer the art can redraw at any size. */
    fun art(context: Context, name: String?): CoverArt? {
        name ?: return null
        return synchronized(cache) { cache.getOrPut(name) { file(context, name)?.readBytes()?.let { fromBytes(it) } } }
    }

    fun fromBytes(bytes: ByteArray): CoverArt? {
        val keep = bytes
        val b = decode(bytes, 160) ?: return null
        val s = 120.0 / maxOf(b.width, b.height)
        val w = maxOf(8, (b.width * s).toInt()); val h = maxOf(8, (b.height * s).toInt())
        val small = Bitmap.createScaledBitmap(b, w, h, true)
        val buf = Buf(w, h)
        small.getPixels(buf.d, 0, w, 0, 0, w, h)
        for (i in buf.d.indices) buf.d[i] = buf.d[i] or Buf.OPAQUE
        return CoverArt(buf) { decode(keep, 360) }
    }
}
