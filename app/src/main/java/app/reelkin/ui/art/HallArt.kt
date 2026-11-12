package app.reelkin.ui.art

import app.reelkin.data.Who
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** One occupied seat: who sits there and a seed for their hair. */
class Seat(val who: Who, val seed: Long)

/**
 * The hall seen from the back row: a curtained screen and a hundred velvet seats. Each stub seats a
 * spectator dressed by the film's genre, filling from the front row.
 */
object HallArt {
    const val W = 174
    const val H = 176
    const val SEATS = 100

    fun seatXY(i: Int): Pair<Int, Int> { val r = i / 10; val j = i % 10; return (18 + j * 14 + if (j >= 5) 6 else 0) to (64 + r * 11) }

    /** The seat under an art pixel, or -1. */
    fun seatAt(x: Double, y: Double, taken: Int): Int {
        for (i in 0 until min(taken, SEATS)) { val (sx, sy) = seatXY(i); if (x >= sx - 1 && x <= sx + 12 && y >= sy - 10 && y <= sy + 7) return i }
        return -1
    }

    /** [screen] is what's on it (already sized 120×36); [running] makes it flicker and the beam brighter. */
    fun draw(b: Buf, seats: List<Seat>, screen: Buf?, sel: Int, running: Boolean, t: Double) {
        b.rect(0, 0, W, H, hex(0x1C0F18))
        var x = 0; while (x < W) { b.rect(x, 0, 3, H, hex(0x22121E)); x += 6 }
        val stage = Scenery.stage
        val hole = Scenery.at("screen")
        val bright = if (running) (.75 + .25 * sin(t * 23) * sin(t * 7)).toFloat() else .55f
        if (stage != null && hole != null) {
            // The curtains and the screen as drawn (tools/scene.py), the film shown in the screen's frame.
            b.blit(stage, 0, 0)
            val (sx, sy, sw, sh) = hole
            if (screen != null) {
                b.blitScaled(screen, sx, sy, sw, sh)
                for (j in sy until sy + sh) for (i in sx until sx + sw) b.p(i, j, hex(0x0A0810), 1 - bright)
            }
        } else {
            b.rect(24, 7, 126, 42, hex(0x6A4A10)); b.rect(25, 8, 124, 40, hex(0xA87418)); b.rect(27, 10, 120, 36, hex(0x0A0810))
            if (screen != null) b.blit(screen, 27, 10, bright)
            for ((a, e) in listOf(6 to 30, 144 to 168)) for (xx in a until e) { val k = (xx - a) % 6; b.rect(xx, 4, 1, 52, if (k < 2) hex(0xD8405A) else if (k < 4) hex(0xA82040) else hex(0x6E1026)) }
            b.rect(4, 2, 166, 6, hex(0x8A1830)); x = 6; while (x < 168) { b.ell(x + 3, 8, 3, 2, hex(0xA82040)); x += 8 }; b.rect(4, 2, 166, 1, hex(0xE8B840))
        }
        b.rect(0, 56, W, 3, hex(0x3A2214)); b.rect(0, 56, W, 1, hex(0x6A4228))
        if (running) for (y in 46 until H) {
            val t2 = (y - 46.0) / (H - 46); val hw = 60 * (1 - t2) + 3
            for (xx in (87 - hw).toInt()..(87 + hw).toInt()) if (((xx * 73856093) xor (y * 19349663)).toLong().and(0xFFFFFFFFL) % 997 < 90 * (1 - t2 * .6)) b.p(xx, y, hex(0xFFF0C0))
        }
        for (i in 0 until SEATS) {
            val (sx, sy) = seatXY(i); val f = seats.getOrNull(i)
            if (f != null) Icons.spectator(b, f.who, sx + 5.0, sy.toDouble(), f.seed)
            b.rect(sx, sy, 11, 7, if (f != null) hex(0xA82038) else hex(0x7A1A2E)); b.rect(sx + 1, sy - 1, 9, 1, if (f != null) hex(0xC83048) else hex(0x8A2236))
            b.rect(sx, sy, 11, 1, if (f != null) hex(0xE05068) else hex(0xA03048)); b.rect(sx, sy + 6, 11, 1, hex(0x4A0A1A)); b.rect(sx - 1, sy + 2, 1, 5, hex(0x2A0A12))
            if (f == null) b.rect(sx + 2, sy + 3, 7, 1, hex(0x5A1022))
            if (i == sel) { for ((a, c) in listOf(-2 to -10, 12 to -10, -2 to 7, 12 to 7)) b.p(sx + a, sy + c, hex(0xFFE060)); b.rect(sx - 2, sy + 8, 15, 1, hex(0xFFE060)) }
        }
        val shade = vignette(b.s)
        for (i in b.d.indices) {
            val l = shade[i]; val c = b.d[i]
            b.d[i] = rgb((Buf.ch(c, 16) * l).toInt(), (Buf.ch(c, 8) * l).toInt(), (Buf.ch(c, 0) * l).toInt())
        }
    }

    private val vignettes = HashMap<Int, FloatArray>()

    /** The hall darkening to the edges, dithered on the fine grid. */
    private fun vignette(s: Int): FloatArray = synchronized(vignettes) {
        vignettes.getOrPut(s) {
            val fw = W * s; val fh = H * s
            FloatArray(fw * fh) { i ->
                val fx = i % fw; val fy = i / fw
                val v = (1 - min(.6, hypot((fx.toDouble() / s - 87) / 110.0, (fy.toDouble() / s - 30) / 190.0) * .75)) * 8; val f = kotlin.math.floor(v)
                ((f + if (dith(fx, fy, v - f)) 1 else 0) / 8).toFloat()
            }
        }
    }
}
