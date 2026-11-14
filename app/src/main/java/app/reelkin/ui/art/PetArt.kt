package app.reelkin.ui.art

import android.graphics.BitmapFactory
import app.reelkin.data.Form
import app.reelkin.data.Species
import app.reelkin.data.Stage
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

enum class Mood { IDLE, HAPPY, SCARED, SLEEP, WATCH, SAD }

data class Pose(
    val sp: Species,
    val stage: Stage = Stage.YOUNG,
    val shiny: Boolean = false,
    val form: Form? = null,
    val t: Double = 0.0,
    val mood: Mood = Mood.IDLE,
    val blink: Boolean = false,
    /** Art pixels off the floor. */
    val hop: Double = 0.0,
    /** 0 awake .. 1 drowsy from days without films: colour drains away. */
    val windup: Float = 0f,
    /** A can: 0 just opened .. 1 about to hatch. */
    val hatch: Float = 0f,
)

/**
 * The pets, drawn ahead of time (one [SIZE]-pixel picture per species and stage, [FINE] times finer: tools/cut.py
 * cuts them from the sheets Codex drew, with the eyes marked) and brought to life here: blinking, sleeping,
 * hopping, shaking at horror, a technicolor colouring and the colours of a grown form.
 */
object PetArt {
    const val SIZE = 64
    const val FINE = Buf.FINE
    private const val FS = SIZE * FINE
    /** The frame around the sprite, for hearts and tears. */
    const val W = 76
    const val H = 72
    private const val OX = 6
    private const val OY = 8
    val INK = hex(0x1A0E14)

    class Sprite(val buf: Buf, val eyes: List<IntArray>, val lids: BooleanArray?) {
        private fun any(x: Int, y: Int) = buf.getF(x, y) ushr 24 != 0
        val fineTop: Int = (0 until FS).firstOrNull { y -> (0 until FS).any { any(it, y) } } ?: 0
        val top: Int get() = fineTop / FINE
    }

    private val cache = HashMap<String, Sprite?>()

    private val eyeTable: Map<String, List<IntArray>> by lazy {
        (PetArt::class.java.getResourceAsStream("/pets/faces.txt")?.bufferedReader()?.use { it.readText() }.orEmpty())
            .lines().filter { it.isNotBlank() && !it.startsWith("#") }.associate { line ->
                val p = line.trim().split(Regex("\\s+"))
                p[0] to evenEyes(p.drop(1).map { it.toInt() }.chunked(4).filter { it.size == 4 }.map { it.toIntArray() })
            }
    }

    /** When the cutter found only part of one eye (far smaller than the other), give it the other's box, centred where the part is. */
    private fun evenEyes(e: List<IntArray>): List<IntArray> {
        if (e.size != 2) return e
        fun area(a: IntArray) = (a[2] - a[0]) * (a[3] - a[1])
        fun like(small: IntArray, big: IntArray): IntArray {
            val w = big[2] - big[0]; val cx = (small[0] + small[2]) / 2
            return intArrayOf(cx - w / 2, big[1], cx - w / 2 + w, big[3])
        }
        val (l, r) = e
        return when {
            area(l) * 5 < area(r) * 2 -> listOf(like(l, r), r)
            area(r) * 5 < area(l) * 2 -> listOf(l, like(r, l))
            else -> e
        }
    }

    private fun fine(bmp: android.graphics.Bitmap): IntArray {
        val n = bmp.width
        val src = IntArray(n * bmp.height).also { bmp.getPixels(it, 0, n, 0, 0, n, bmp.height) }
        if (n == FS && bmp.height == FS) return src
        return IntArray(FS * FS) { i -> val x = (i % FS) * n / FS; val y = (i / FS) * bmp.height / FS; src[y * n + x] }
    }

    fun key(sp: Species, st: Stage) = sp.file + "_" + st.name.lowercase()

    fun sprite(sp: Species, st: Stage): Sprite? = synchronized(cache) {
        cache.getOrPut(key(sp, st)) {
            runCatching {
                val bmp = PetArt::class.java.getResourceAsStream("/pets/${key(sp, st)}.png")?.use { BitmapFactory.decodeStream(it) } ?: return@runCatching null
                val b = Buf(SIZE, SIZE, FINE)
                fine(bmp).copyInto(b.d)
                bmp.recycle()
                val lids = PetArt::class.java.getResourceAsStream("/pets/${key(sp, st)}_eyes.png")?.use { BitmapFactory.decodeStream(it) }?.let { m ->
                    val e = fine(m); m.recycle()
                    BooleanArray(FS * FS) { e[it] ushr 24 != 0 }
                }
                Sprite(b, eyeTable[key(sp, st)].orEmpty(), lids)
            }.getOrNull()
        }
    }

    /** The colour a species shows in lists: the commonest bright colour of its grown picture. */
    fun color(sp: Species): Int = synchronized(colors) {
        colors.getOrPut(sp) {
            val b = sprite(sp, Stage.ADULT)?.buf ?: return@getOrPut hex(0xB8243E)
            val n = HashMap<Int, Int>()
            for (c in b.d) {
                if (c ushr 24 == 0) continue
                val q = c and 0xE0E0E0
                val l = Buf.ch(q, 16) * .3f + Buf.ch(q, 8) * .59f + Buf.ch(q, 0) * .11f
                if (l < 50 || l > 220) continue
                n[q] = (n[q] ?: 0) + 1
            }
            (n.maxByOrNull { it.value }?.key ?: 0xB8243E) or Buf.OPAQUE
        }
    }
    private val colors = HashMap<Species, Int>()

    // ---------------------------------------------------------------- colour

    private fun hsv(c: Int): FloatArray { val f = FloatArray(3); android.graphics.Color.colorToHSV(c, f); return f }
    private fun fromHsv(a: Int, f: FloatArray): Int = (android.graphics.Color.HSVToColor(f) and 0xFFFFFF) or (a shl 24)

    /** Technicolor: every hue turned, the same way for each species. */
    private fun shine(b: Buf, sp: Species) {
        val turn = 120f + sp.ordinal * 37f % 160f
        for (i in b.d.indices) {
            val c = b.d[i]; val a = c ushr 24
            if (a == 0) continue
            val f = hsv(c)
            if (f[1] < 0.12f) continue
            f[0] = (f[0] + turn) % 360f
            b.d[i] = fromHsv(a, f)
        }
    }

    /** A grown form: the body leans to its colour. */
    private fun tint(b: Buf, form: Form) {
        val t = form.color or Buf.OPAQUE
        for (i in b.d.indices) {
            val c = b.d[i]; val a = c ushr 24
            if (a == 0) continue
            val l = (Buf.ch(c, 16) * 0.3f + Buf.ch(c, 8) * 0.59f + Buf.ch(c, 0) * 0.11f) / 255f
            if (l < 0.12f || l > 0.92f) continue
            b.d[i] = (mix(c, t, 0.18f) and 0xFFFFFF) or (a shl 24)
        }
    }

    /** Drowsy: colour drains away. */
    private fun drain(b: Buf, k: Float) {
        for (i in b.d.indices) {
            val c = b.d[i]; val a = c ushr 24
            if (a == 0) continue
            val g = (Buf.ch(c, 16) * 0.3f + Buf.ch(c, 8) * 0.59f + Buf.ch(c, 0) * 0.11f).roundToInt()
            val grey = rgb(g * 9 / 10 + 14, g * 9 / 10 + 12, g * 9 / 10 + 22)
            b.d[i] = (mix(c, grey, k * 0.7f) and 0xFFFFFF) or (a shl 24)
        }
    }

    // ---------------------------------------------------------------- frame

    private val OUT = rgb(0x1A0E22)
    private val PINK = rgb(0xF07AA0)

    /** One frame of the pet in a [W]×[H] buffer, its feet on the bottom. */
    fun draw(into: Buf, o: Pose) {
        into.clear()
        if (o.stage == Stage.EGG) { egg(into, o); return }
        val sp = sprite(o.sp, o.stage) ?: run { egg(into, o.copy(stage = Stage.EGG)); return }
        val b = Buf(SIZE, SIZE, FINE).also { it.copyFrom(sp.buf) }
        if (o.form != null && o.stage >= Stage.ADULT) tint(b, o.form)
        if (o.shiny) shine(b, o.sp)
        val shut = o.mood == Mood.SLEEP || o.blink || o.mood == Mood.HAPPY
        if (shut) { val lid = lidColour(b, sp.eyes); for (e in sp.eyes) closeEye(b, e, o.mood == Mood.HAPPY, lid) }
        if (o.windup > 0f) drain(b, o.windup)
        val t = o.t
        var dy = -o.hop
        var dx = 0
        var breath = 0
        when {
            o.mood == Mood.SCARED -> dx = if ((t * 30).roundToInt() % 2 == 1) 1 else -1
            o.sp.floats -> dy += sin(t * 2.2) * 1.5 - 2
            // Breathing: the whole picture a fine pixel up and back, slowly; nothing stretches.
            o.windup < 1f -> breath = if (sin(t * (if (o.mood == Mood.SLEEP) 1.4 else 2.0)) > .3) 1 else 0
        }
        val ty0 = OY * FINE + (dy * FINE).roundToInt() - breath
        for (y in sp.fineTop until FS) for (x in 0 until FS) {
            val c = b.d[y * FS + x]
            if (c ushr 24 != 0) into.pf((OX + dx) * FINE + x, ty0 + y, c)
        }
        val eyeY = sp.eyes.firstOrNull()?.let { OY + it[1] / FINE } ?: (OY + sp.top + 10)
        val headX = sp.eyes.takeIf { it.size >= 2 }?.let { OX + (it[0][0] + it[1][2]) / 2 / FINE } ?: (OX + SIZE / 2)
        if (o.mood == Mood.HAPPY) heart(into, headX + 10, eyeY - 12 - ((t * 6).toInt() % 6))
        if (o.mood == Mood.SAD && sp.eyes.isNotEmpty()) {
            val e = sp.eyes[0]; val x = OX + e[0] / FINE; val y = OY + e[3] / FINE + ((t * 4).toInt() % 3)
            into.p(x, y, hex(0x8AD0FF)); into.p(x, y + 1, hex(0x5AA8F0))
        }
        if (o.shiny) sparkle(into, (t * 11).toInt())
    }

    /**
     * Shuts an eye (a box in fine pixels): an oval lid a little wider than the eye, in the colour of the fur just above it,
     * and one arc across its middle: down at the ends (‿) asleep or blinking, up (‿ turned over) when happy.
     */
    /**
     * One lid colour for both eyes: the commonest shade of the face around them — beside and just below,
     * not above (a hat brim, a fringe) — leaving out the outline and pink blush. Shades are grouped
     * roughly so a dithered fur counts as one.
     */
    private fun lidColour(b: Buf, eyes: List<IntArray>): Int {
        val groups = HashMap<Int, HashMap<Int, Int>>()
        for (e in eyes) {
            val y0 = e[1]; val y1 = e[3] + 2 * FINE
            for (y in y0 until y1) for (x in e[0] - 2 * FINE until e[2] + 2 * FINE) {
                if (y < e[3] + FINE / 2 && x in e[0] - FINE until e[2] + FINE) continue
                if (x !in 0 until FS || y !in 0 until FS) continue
                if (eyes.any { o -> x in o[0] - FINE until o[2] + FINE && y in o[1] until o[3] + FINE / 2 }) continue
                val c = b.getF(x, y)
                if (c ushr 24 == 0) continue
                val r = Buf.ch(c, 16); val g = Buf.ch(c, 8); val bl = Buf.ch(c, 0)
                if (r * 0.3f + g * 0.59f + bl * 0.11f < 40) continue
                if (r - g > 45 && bl >= g - 10) continue  // pink blush, not orange fur
                if (r > 200 && r - g > 80 && bl > 90) continue  // salmon blush (the tanuki's), still not orange fur
                val key = (r / 40) * 100 + (g / 40) * 10 + bl / 40
                val m = groups.getOrPut(key) { HashMap() }
                m[c] = (m[c] ?: 0) + 1
            }
        }
        val best = groups.values.maxByOrNull { it.values.sum() } ?: return eyes.firstOrNull()?.let { b.getF(it[0], max(0, it[1] - FINE)) } ?: 0
        return best.maxBy { it.value }.key
    }

    private fun closeEye(b: Buf, e: IntArray, happy: Boolean, lid: Int) {
        val pad = max(FINE, ((e[2] - e[0]) * .2f).roundToInt()); val vpad = max(FINE / 2, ((e[3] - e[1]) * .2f).roundToInt())
        val x0 = e[0] - pad; val y0 = e[1] - vpad; val x1 = e[2] + pad; val y1 = e[3] + vpad
        val cx = (x0 + x1 - 1) / 2f; val cy = (y0 + y1 - 1) / 2f
        val rx = (x1 - x0) / 2f; val ry = (y1 - y0) / 2f
        for (y in y0 until y1) for (x in x0 until x1) {
            if (x !in 0 until FS || y !in 0 until FS) continue
            val nx = (x - cx) / rx; val ny = (y - cy) / ry
            if (nx * nx + ny * ny <= 1f && b.getF(x, y) ushr 24 != 0) b.d[y * FS + x] = lid
        }
        // The line: an arc as wide as the eye, a fine pixel thick plus one.
        val w = e[2] - e[0]
        val lift = (w * .18f).coerceAtLeast(FINE.toFloat())
        val mid = cy + ry * .1f
        for (x in e[0] - 1 until e[2] + 1) {
            val u = ((x - (e[0] + e[2]) / 2f) / (w / 2f + 1)).coerceIn(-1f, 1f)
            val bend = (1 - u * u) * lift
            val yy = (if (happy) mid - bend + lift / 2 else mid + bend - lift / 2).roundToInt()
            for (k in 0 until 2) b.pf(x, yy + k, OUT)
        }
    }

    private fun heart(b: Buf, x: Int, y: Int) {
        val rows = intArrayOf(0b0110110, 0b1111111, 0b1111111, 0b0111110, 0b0011100, 0b0001000)
        for ((j, r) in rows.withIndex()) for (i in 0 until 7) if (r shr (6 - i) and 1 == 1) b.p(x + i, y + j, PINK)
        b.p(x + 1, y + 1, rgb(0xFFF0F4))
    }

    private fun sparkle(b: Buf, tick: Int) {
        val c = rgb(0xEAF4FF)
        for (i in 0 until 3) {
            if ((tick / 8 + i * 3) % 7 >= 2) continue
            val x = 10 + (hashI("s$i${tick / 56}") % 56)
            val y = 10 + (hashI("t$i${tick / 56}") % 48)
            b.p(x, y, c); b.p(x - 1, y, c, .6f); b.p(x + 1, y, c, .6f); b.p(x, y - 1, c, .6f); b.p(x, y + 1, c, .6f)
        }
    }

    // ---------------------------------------------------------------- the can

    private fun egg(b: Buf, o: Pose) {
        val sp = sprite(o.sp, Stage.EGG)
        val shake = o.hatch.coerceIn(0f, 1f)
        val phase = ((o.t * 11).toInt() % 40)
        val rock = if (phase < 8 && shake > 0.3f) ((if (phase % 4 < 2) 1 else -1) * (1 + (shake * 2).toInt())) else 0
        if (sp == null) { Icons.can(b, W / 2.0 + rock, H - 6.0, hex(0xB8243E)); return }
        val img = sp.buf
        for (y in 0 until FS) for (x in 0 until FS) {
            val c = img.d[y * FS + x]
            if (c ushr 24 != 0) b.pf((OX + rock) * FINE + x, OY * FINE + y, c)
        }
        // Light through the seam, the nearer to opening the more.
        val cracks = (shake * 4).toInt()
        val cx = OX + SIZE / 2; val top = OY + sp.top + 8
        for (k in 0 until cracks) {
            var x = cx - 8 + k * 5; var y = top + k * 6
            repeat(5) { i -> b.p(x + rock, y, hex(0xFFE8A0)); x += if (i % 2 == 0) 1 else -1; y += 1 }
        }
    }

    /** A still picture for lists and sheets. */
    fun still(o: Pose): Buf = Buf(W, H, FINE).also { draw(it, o) }

    /** Where the pet is in its frame at a stage, in art pixels (x, y, w, h), with room above for a hop and a heart. */
    fun bounds(sp: Species, st: Stage): IntArray {
        val b = still(sp, st)
        var x0 = b.fw; var y0 = b.fh; var x1 = -1; var y1 = -1
        for (y in 0 until b.fh) for (x in 0 until b.fw) if (b.d[y * b.fw + x] ushr 24 != 0) { if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y }
        if (x1 < 0) return intArrayOf(0, 0, W, H)
        val ax0 = (x0 / FINE - 4).coerceAtLeast(0); val ay0 = (y0 / FINE - 10).coerceAtLeast(0)
        val ax1 = (x1 / FINE + 5).coerceAtMost(W); val ay1 = (y1 / FINE + 2).coerceAtMost(H)
        return intArrayOf(ax0, ay0, ax1 - ax0, ay1 - ay0)
    }

    /** One frame of the pet, cut to [box] (from [bounds]) and drawn into [into], which is box-sized. */
    fun drawIn(into: Buf, o: Pose, box: IntArray) {
        val full = Buf(W, H, FINE)
        draw(full, o)
        into.clear()
        for (y in 0 until into.fh) System.arraycopy(full.d, (box[1] * FINE + y) * full.fw + box[0] * FINE, into.d, y * into.fw, into.fw)
    }

    /** The pet alone, cut to itself with a pixel to spare, so it can stand in the middle of any frame. */
    fun portrait(sp: Species, st: Stage, shiny: Boolean = false, form: Form? = null, dim: Boolean = false): Buf {
        val b = still(sp, st, shiny, form, dim)
        var x0 = b.fw; var y0 = b.fh; var x1 = -1; var y1 = -1
        for (y in 0 until b.fh) for (x in 0 until b.fw) if (b.d[y * b.fw + x] ushr 24 != 0) { if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y }
        if (x1 < 0) return b
        val ax0 = (x0 / FINE - 1).coerceAtLeast(0); val ay0 = (y0 / FINE - 1).coerceAtLeast(0)
        val ax1 = (x1 / FINE + 2).coerceAtMost(b.w); val ay1 = (y1 / FINE + 2).coerceAtMost(b.h)
        val out = Buf(ax1 - ax0, ay1 - ay0, FINE)
        for (y in 0 until out.fh) System.arraycopy(b.d, (ay0 * FINE + y) * b.fw + ax0 * FINE, out.d, y * out.fw, out.fw)
        return out
    }

    fun still(sp: Species, st: Stage, shiny: Boolean = false, form: Form? = null, dim: Boolean = false): Buf {
        val b = still(Pose(sp, st, shiny, form))
        if (dim) for (i in b.d.indices) if (b.d[i] ushr 24 != 0) b.d[i] = rgb(0x2A2236)
        return b
    }
}
