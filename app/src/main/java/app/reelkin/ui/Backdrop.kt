package app.reelkin.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelkin.data.Genre
import app.reelkin.domain.Cinema
import app.reelkin.domain.Words
import app.reelkin.ui.components.Flow
import app.reelkin.ui.components.t
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A picture behind every screen, softened so the text stays easy to read: a photo for each genre
 * and a red curtain (public-domain photos in assets/backdrops, see CREDITS.txt there), the genre of
 * what's on now, or a picture of one's own from the gallery.
 */
object Backdrops {
    const val NONE = ""
    const val AUTO = "auto"
    const val OWN = "own"
    const val CURTAIN = "cinema"
    private const val OWN_FILE = "backdrop.jpg"
    /** How much of the page colour lies over the picture. */
    const val VEIL = 0.8f

    fun of(g: Genre) = "g:" + g.name
    private fun genreOf(key: String): Genre? = key.removePrefix("g:").takeIf { key.startsWith("g:") }?.let { n -> Genre.entries.firstOrNull { it.name == n } }

    fun ownFile(context: Context) = File(context.filesDir, OWN_FILE)

    /** The genre of what's on now: the film on the projector, else the first series running. */
    fun nowOn(c: Cinema): Genre? = c.s.seance?.genre ?: c.running.firstOrNull()?.genre

    /** The picture [key] shows, an asset path or [OWN]; [now] is the genre on now, for [AUTO]. */
    fun resolve(key: String, now: Genre?): String? = when (key) {
        NONE -> null
        AUTO -> now?.let(::asset) ?: asset(CURTAIN)
        OWN -> OWN
        CURTAIN -> asset(CURTAIN)
        else -> genreOf(key)?.let(::asset)
    }

    fun asset(g: Genre) = asset(g.name.lowercase())
    private fun asset(name: String) = "backdrops/$name.jpg"

    /** Reads [what] (an asset path, or [OWN]) no larger than [maxSide] on its longer side. */
    fun load(context: Context, what: String, maxSide: Int): Bitmap? = runCatching {
        fun open() = if (what == OWN) ownFile(context).inputStream() else context.assets.open(what)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        open().use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
    }.getOrNull()

    /** Keeps a picture from the gallery as one's own backdrop; false if it couldn't be read. */
    suspend fun importPicked(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 2000) sample *= 2
            val bmp = context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }!!
            val tmp = File(context.filesDir, "$OWN_FILE.tmp")
            tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            tmp.renameTo(ownFile(context))
        }.getOrDefault(false)
    }
}

/** The backdrop picture as loaded at the root, so full-screen pages can lie over the same one. */
val LocalBackdrop = staticCompositionLocalOf<ImageBitmap?> { null }

@Composable
fun rememberBackdrop(key: String, now: Genre?, stamp: Long): ImageBitmap? {
    val ctx = LocalContext.current
    val what = Backdrops.resolve(key, now)
    val image by produceState<ImageBitmap?>(null, what, stamp) {
        value = what?.let { w -> withContext(Dispatchers.IO) { Backdrops.load(ctx, w, 1800)?.asImageBitmap() } }
    }
    return image
}

/** The page colour, or the backdrop with the page colour laid thin over it. */
@Composable
fun BackdropLayer(modifier: Modifier = Modifier) {
    val image = LocalBackdrop.current
    Box(modifier.fillMaxSize().background(C.bg)) {
        if (image != null) {
            Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(C.bg.copy(alpha = Backdrops.VEIL)))
        }
    }
}

/** Every backdrop in sight, four to a row: none, what's on now, one's own, the curtain and a photo for each genre. */
@Composable
fun BackdropPicker(c: Cinema, onPick: (String) -> Unit, onOwn: () -> Unit) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val key = c.s.backdrop
    val stamp = c.s.backdropStamp
    val hasOwn = remember(stamp) { Backdrops.ownFile(ctx).exists() }
    Flow(gap = 10.dp) {
        Tile(null, t("без фона", "none"), key == Backdrops.NONE, stamp) { onPick(Backdrops.NONE) }
        Tile(Backdrops.resolve(Backdrops.AUTO, Backdrops.nowOn(c)), t("по сеансу", "what's on"), key == Backdrops.AUTO, stamp) { onPick(Backdrops.AUTO) }
        Tile(if (hasOwn) Backdrops.OWN else null, t("своя", "my own"), key == Backdrops.OWN, stamp, plus = !hasOwn) {
            if (hasOwn && key != Backdrops.OWN) onPick(Backdrops.OWN) else onOwn()
        }
        Tile(Backdrops.resolve(Backdrops.CURTAIN, null), t("занавес", "curtain"), key == Backdrops.CURTAIN, stamp) { onPick(Backdrops.CURTAIN) }
        for (g in Genre.entries) Tile(Backdrops.asset(g), Words.genre(g, ru), key == Backdrops.of(g), stamp) { onPick(Backdrops.of(g)) }
    }
}

@Composable
private fun Tile(what: String?, label: String, selected: Boolean, stamp: Long, plus: Boolean = false, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val thumb by produceState<ImageBitmap?>(null, what, stamp) {
        value = what?.let { w -> withContext(Dispatchers.IO) { Backdrops.load(ctx, w, 320)?.asImageBitmap() } }
    }
    Column(Modifier.width(74.dp).clip(R8.Sm).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.width(74.dp).height(124.dp).clip(R8.Sm).background(C.surface2).border(if (selected) 3.dp else 1.dp, if (selected) C.gold else C.line, R8.Sm),
            contentAlignment = Alignment.Center,
        ) {
            thumb?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            if (plus) Text("+", style = T.Title.copy(color = C.gold))
        }
        Text(label, style = T.Small.copy(color = if (selected) C.gold else C.muted), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 2.dp))
    }
}
