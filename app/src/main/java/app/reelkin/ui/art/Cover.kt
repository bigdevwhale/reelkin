package app.reelkin.ui.art

import kotlin.math.max

/**
 * A cover picture, kept small, and its pixel versions at every size asked for: shrunk, a little
 * more saturated, and brought down to a dozen colours so it sits with the drawn art.
 */
class CoverArt(val src: Buf, private val load: () -> android.graphics.Bitmap? = { null }) {
    /** The picture at a size fit for showing as it is, decoded the first time it's asked for. */
    val full: android.graphics.Bitmap? by lazy { load() }

    private val cache = HashMap<Long, Buf>()
    private val smooth = HashMap<Long, Buf>()

    fun pixels(w: Int, h: Int): Buf = cache.getOrPut(w.toLong() shl 32 or h.toLong()) { pixelArt(src, w, h, if (w * h < 200) 6 else 12) }

    /** The picture at w×h without the palette, for people who want covers as they are. */
    fun plain(w: Int, h: Int): Buf = smooth.getOrPut(w.toLong() shl 32 or h.toLong()) { fit(src, w, h) }

    /** The cover's most telling colour: frequent, saturated, not too dark or light. */
    val dominant: Int by lazy {
        val pal = quantize(fit(src, 24, 30).d, 12)
        var best = pal.firstOrNull()
        var bs = -1.0
        for (p in pal) {
            val r = Buf.ch(p.c, 16); val g = Buf.ch(p.c, 8); val b = Buf.ch(p.c, 0)
            val mx = maxOf(r, g, b); val mn = minOf(r, g, b)
            val sat = if (mx > 0) (mx - mn) / mx.toDouble() else 0.0
            val l = (mx + mn) / 510.0
            val sc = p.n * (sat + .15) * if (l > .12 && l < .9) 1.0 else .2
            if (sc > bs) { bs = sc; best = p }
        }
        best?.c ?: rgb(0x7A6A5A)
    }
}

class Swatch(val c: Int, val n: Int)

/** Median cut down to [k] colours. */
fun quantize(px: IntArray, k: Int): List<Swatch> {
    val boxes = mutableListOf(px.filter { it ushr 24 > 128 }.toMutableList())
    while (boxes.size < k) {
        var bi = -1; var br = -1; var bc = 0
        boxes.forEachIndexed { i, b ->
            if (b.size < 2) return@forEachIndexed
            for (sh in intArrayOf(16, 8, 0)) {
                var mn = 255; var mx = 0
                for (c in b) { val v = Buf.ch(c, sh); if (v < mn) mn = v; if (v > mx) mx = v }
                if (mx - mn > br) { br = mx - mn; bi = i; bc = sh }
            }
        }
        if (bi < 0 || br < 8) break
        val b = boxes[bi].sortedBy { Buf.ch(it, bc) }
        val m = b.size shr 1
        boxes[bi] = b.subList(0, m).toMutableList()
        boxes.add(bi + 1, b.subList(m, b.size).toMutableList())
    }
    return boxes.filter { it.isNotEmpty() }.map { b ->
        var r = 0L; var g = 0L; var bl = 0L
        for (c in b) { r += Buf.ch(c, 16); g += Buf.ch(c, 8); bl += Buf.ch(c, 0) }
        Swatch(rgb((r / b.size).toInt(), (g / b.size).toInt(), (bl / b.size).toInt()), b.size)
    }
}

/** Crops [src] to fill w×h and shrinks it by averaging, so small sizes keep their colours. */
fun fit(src: Buf, w: Int, h: Int): Buf {
    val out = Buf(w, h)
    val s = max(w / src.w.toDouble(), h / src.h.toDouble())
    val cw = w / s; val ch = h / s
    val ox = (src.w - cw) / 2; val oy = (src.h - ch) / 2
    for (y in 0 until h) for (x in 0 until w) {
        val x0 = (ox + x * cw / w).toInt().coerceIn(0, src.w - 1); val x1 = max(x0 + 1, (ox + (x + 1) * cw / w).toInt()).coerceAtMost(src.w)
        val y0 = (oy + y * ch / h).toInt().coerceIn(0, src.h - 1); val y1 = max(y0 + 1, (oy + (y + 1) * ch / h).toInt()).coerceAtMost(src.h)
        var r = 0; var g = 0; var b = 0; var n = 0
        for (j in y0 until y1) for (i in x0 until x1) { val c = src.d[j * src.w + i]; r += Buf.ch(c, 16); g += Buf.ch(c, 8); b += Buf.ch(c, 0); n++ }
        out.d[y * w + x] = rgb(r / n, g / n, b / n)
    }
    return out
}

/** Any picture → a pixel cover w×h: shrink, lift the saturation a little, bring down to [k] colours. */
fun pixelArt(src: Buf, w: Int, h: Int, k: Int = 10): Buf {
    val b = fit(src, w, h)
    for (i in b.d.indices) {
        val c = b.d[i]
        val r = Buf.ch(c, 16); val g = Buf.ch(c, 8); val bl = Buf.ch(c, 0)
        val l = (r + g + bl) / 3.0
        fun lift(v: Int) = (((l + (v - l) * 1.3) - 128) * 1.1 + 128).toInt().coerceIn(0, 255)
        b.d[i] = rgb(lift(r), lift(g), lift(bl))
    }
    val pal = quantize(b.d, k)
    for (i in b.d.indices) {
        val c = b.d[i]
        var best = pal[0].c; var bd = Double.MAX_VALUE
        for (p in pal) {
            val dr = Buf.ch(c, 16) - Buf.ch(p.c, 16); val dg = Buf.ch(c, 8) - Buf.ch(p.c, 8); val db = Buf.ch(c, 0) - Buf.ch(p.c, 0)
            val dd = dr * dr * .3 + dg * dg * .59 + db * db * .11
            if (dd < bd) { bd = dd; best = p.c }
        }
        b.d[i] = best
    }
    return b
}

