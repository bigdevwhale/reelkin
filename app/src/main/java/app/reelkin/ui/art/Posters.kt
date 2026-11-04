package app.reelkin.ui.art

import app.reelkin.data.Genre
import app.reelkin.data.Who
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Posters for films without a picture: the genre's colour and one sign of it, drawn in pixels. */
object Posters {
    private val cache = HashMap<String, Buf>()

    fun of(title: String, genre: Genre, w: Int, h: Int, art: CoverArt? = null): Buf =
        art?.pixels(w, h) ?: synchronized(cache) { cache.getOrPut("$title|$genre|$w|$h") { draw(title, genre, w, h) } }

    fun draw(title: String, g: Genre, w: Int, h: Int): Buf {
        val b = Buf(w, h)
        val r = ramp(hex(g.color.toLong()))
        val s = hash(title)
        val wide = w > h * 1.5
        val dark = g == Genre.NOIR || g == Genre.HORROR || g == Genre.SCIFI
        val top = if (dark) r.dd else r.b
        val bot = r.d
        for (y in 0 until h) for (x in 0 until w) b.p(x, y, if (dith(x, y, y / h.toDouble())) bot else top)
        val cx = w / 2.0 + ((s % 7) - 3) * w / 60.0
        val cy = h * if (wide) .5 else .4
        val R = min(w, h) * .3
        val light = hex(0xFFF4D8); val ink = hex(0x14080E)
        when (g) {
            Genre.NOIR -> {
                b.el(cx + R * .7, cy - R * .6, R * .32, R * .32, hex(0xF2E2A0))
                var y = 2; while (y < h * .7) { b.box(0.0, y.toDouble(), w * .35, 1.0, mix(top, hex(0x000000), .4f)); y += 3 }
                b.poly(ink, cx - R * .55, h.toDouble(), cx - R * .4, cy + R * .15, cx + R * .4, cy + R * .15, cx + R * .55, h.toDouble())
                b.el(cx, cy - R * .05, R * .22, R * .24, ink); b.box(cx - R * .45, cy - R * .25, R * .9, maxOf(1.0, R * .08), ink); b.box(cx - R * .24, cy - R * .5, R * .48, R * .26, ink)
            }
            Genre.SCIFI -> {
                for (i in 0 until w * h / 40) b.p(((s * (i + 3) * 7 + i * 31) % w).toInt(), ((s * (i + 1) * 13 + i * 17) % h).toInt(), if (i % 3 != 0) hex(0xCFE8F0) else hex(0xFFFFFF))
                b.lit(ramp(hex(0xE0A050))) { el(cx, cy, R * .7, R * .7, M) }
                var a = -.15; while (a < PI + .15) { b.dot(cx + cos(a) * R * 1.25, cy + sin(a) * R * .3, hex(0xF8E8C0)); a += .02 }
            }
            Genre.ROMANCE -> b.lit(ramp(hex(0xF8E0E8))) {
                el(cx - R * .35, cy - R * .15, R * .4, R * .4, M); el(cx + R * .35, cy - R * .15, R * .4, R * .4, M)
                poly(M, cx - R * .74, cy - R * .05, cx + R * .74, cy - R * .05, cx, cy + R * .8)
            }
            Genre.COMEDY -> {
                b.el(cx, cy, R * .75, R * .75, hex(0xFFF0B0)); b.el(cx, cy + R * .05, R * .18, R * .18, hex(0xE02A2A)); b.dot(cx - R * .06, cy - R * .05, hex(0xFFFFFF))
                var a = .4; while (a < PI - .4) { b.dot(cx + cos(a) * R * .45, cy + R * .15 + sin(a) * R * .35, ink); a += .03 }
                b.el(cx - R * .3, cy - R * .3, 1.0, 1.0, ink); b.el(cx + R * .3, cy - R * .3, 1.0, 1.0, ink)
            }
            Genre.DOC -> {
                b.el(cx, cy, R * .8, R * .8, hex(0x2A2A30)); b.el(cx, cy, R * .62, R * .62, hex(0x6A7A8A)); b.el(cx, cy, R * .42, R * .42, hex(0x1A2A4A)); b.el(cx, cy, R * .2, R * .2, hex(0x0A0A14))
                b.dot(cx - R * .25, cy - R * .25, hex(0xFFFFFF)); b.dot(cx - R * .25 + 1, cy - R * .25, hex(0xCFE8FF))
            }
            Genre.HORROR -> {
                b.lit(ramp(hex(0xE8E4F0))) {
                    el(cx, cy - R * .2, R * .5, R * .5, M); box(cx - R * .5, cy - R * .2, R + 1, R * .8, M)
                    for (i in 0 until 4) el(cx - R * .38 + i * R * .25, cy + R * .6, R * .13, R * .13, M)
                }
                b.el(cx - R * .18, cy - R * .2, 1.0, 2.0, ink); b.el(cx + R * .18, cy - R * .2, 1.0, 2.0, ink)
            }
            Genre.WESTERN -> {
                b.el(cx, h * .62, R * .7, R * .7, hex(0xFFD070)); b.box(0.0, h * .62, w.toDouble(), h * .38 + 1, mix(r.d, hex(0x000000), .3f))
                for ((dx, hh) in listOf(-.8 to .9, .9 to .6)) {
                    val x = cx + dx * R; val base = h * .62; val cactus = hex(0x2A5A2A)
                    b.box(x, base - R * hh, maxOf(2.0, R * .18), R * hh, cactus); b.box(x - R * .22, base - R * hh * .7, R * .22, maxOf(1.0, R * .12), cactus); b.box(x - R * .22, base - R * hh * .95, maxOf(1.0, R * .1), R * .3, cactus)
                }
            }
            Genre.ANIMATION -> {
                val p = DoubleArray(20)
                for (i in 0 until 10) { val a = -PI / 2 + i * PI / 5; val rr = if (i % 2 == 1) R * .38 else R * .85; p[2 * i] = cx + cos(a) * rr; p[2 * i + 1] = cy + sin(a) * rr }
                b.lit(ramp(hex(0xFFD84A))) { poly(M, *p) }
                b.box(cx - R * .2, cy - R * .05, 1.0, 2.0, ink); b.box(cx + R * .15, cy - R * .05, 1.0, 2.0, ink)
            }
            Genre.MUSICAL -> {
                for (y in 0 until h) { val hw = y.toDouble() / h * R * 1.2; for (x in (cx - hw).toInt()..(cx + hw).toInt()) if (dith(x, y, .45)) b.p(x, y, hex(0xFFE8B0)) }
                for ((dx, dy) in listOf(-.9 to -.5, .8 to -.2, -.6 to .5)) { val x = cx + dx * R; val y = cy + dy * R; b.el(x, y, 1.5, 1.0, light); b.box(x + 1, y - 5, 1.0, 5.0, light); b.dot(x + 2, y - 5, light) }
            }
            Genre.DRAMA -> {
                for (i in 0 until w * h / 30) { val x = ((s * (i + 2) + i * 23) % w).toInt(); val y = ((s * (i + 5) + i * 37) % h).toInt(); b.p(x, y, hex(0x9AB0D0)); b.p(x, y + 1, hex(0x6A80A0)) }
                b.lit(ramp(hex(0xC83A4A))) {
                    val ry = (R * .55).toInt()
                    for (y in -ry..0) { val ww = R * .7 * kotlin.math.sqrt(1 - (y * y) / ((ry + .5) * (ry + .5))); box(cx - ww, cy + y, 2 * ww + 1, 1.0, M) }
                }
                b.ln(cx, cy, cx, cy + R * .9, ink); b.dot(cx - 1, cy + R * .9, ink)
            }
            Genre.REALITY -> {
                // A studio camera filming: the body, the lens and the red light that means it's on.
                b.lit(ramp(hex(0x5A5E6A))) { box(cx - R * .7, cy - R * .35, R * 1.1, R * .7, M) }
                b.el(cx + R * .55, cy, R * .3, R * .3, hex(0x1A1A24)); b.el(cx + R * .55, cy, R * .16, R * .16, hex(0x4A6A9A)); b.dot(cx + R * .5, cy - R * .06, hex(0xFFFFFF))
                b.box(cx - R * .6, cy - R * .6, R * .5, R * .22, hex(0x3A3E48))
                b.el(cx - R * .5, cy - R * .2, maxOf(1.0, R * .1), maxOf(1.0, R * .1), hex(0xF03A3A))
                b.ln(cx - R * .2, cy + R * .35, cx - R * .6, cy + R * .95, ink); b.ln(cx - R * .1, cy + R * .35, cx + R * .3, cy + R * .95, ink)
            }
            Genre.ADVENTURE -> {
                b.poly(hex(0x3A4A5A), cx - R * 1.3, h * .75, cx - R * .3, cy - R * .5, cx + R * .6, h * .75)
                b.poly(hex(0xF0F0F0), cx - R * .3, cy - R * .5, cx - R * .55, cy - R * .1, cx - R * .08, cy - R * .15)
                b.poly(hex(0x2A3A4A), cx, h * .75, cx + R * .8, cy, cx + R * 1.6, h * .75)
                b.ln(cx + R * .8, cy, cx + R * .8, cy - R * .5, ink); b.box(cx + R * .8 + 1, cy - R * .5, R * .3, R * .18, hex(0xE83A3A))
                b.box(0.0, h * .75, w.toDouble(), h * .25 + 1, mix(r.d, hex(0x000000), .25f))
            }
        }
        if (!wide) {
            val by = (h * .83).toInt()
            b.rect(0, by, w, h - by, mix(r.dd, hex(0x000000), .3f))
            b.box(2.0, by + 2.0, w * .7, 1.0, hex(0xF4E8CC)); b.box(2.0, by + 4.0, w * .45, 1.0, hex(0xC8B896))
        }
        return b
    }
}

/** Little pixel things: popcorn for ratings, the statuette, the hall's spectators. */
object Icons {
    private fun popcorn(on: Boolean): Buf {
        val b = Buf(7, 8)
        if (on) for ((x, y) in listOf(1 to 1, 3 to 0, 5 to 1, 2 to 2, 4 to 2)) { b.rect(x - 1, y, 2, 2, hex(0xFFF4D0)); b.p(x, y + 1, hex(0xF0C850)) }
        b.rect(1, 3, 5, 5, if (on) hex(0xFFFFFF) else hex(0x5A4A52))
        for (x in intArrayOf(1, 3, 5)) b.rect(x, 3, 1, 5, if (on) hex(0xD82A3A) else hex(0x6E5A62))
        b.rect(1, 3, 5, 1, if (on) hex(0xA01828) else hex(0x7E6A72))
        return b
    }
    val popcornOn = popcorn(true)
    val popcornOff = popcorn(false)

    val statue: Buf = Buf(12, 24).also { b ->
        b.rect(1, 19, 10, 5, hex(0x2A1A14)); b.rect(1, 19, 10, 1, hex(0x5A3A24))
        b.lit(ramp(hex(0xE8B840))) { el(6.0, 3.0, 2.0, 2.0, M); rect(4, 6, 5, 7, M); rect(5, 13, 3, 6, M); rect(3, 7, 1, 4, M); rect(9, 7, 1, 4, M); rect(3, 18, 7, 1, M) }
        b.outline(PetArt.INK)
    }

    /** A film can for the empty booth and the hatching. */
    fun can(b: Buf, cx: Double, cy: Double, label: Int) {
        b.box(cx - 12, cy - 2, 25.0, 5.0, hex(0x6A6E78)); b.box(cx - 12, cy, 25.0, 2.0, label)
        b.box(cx - 12, cy - 2, 1.0, 5.0, hex(0x9AA0AA)); b.box(cx + 12, cy - 2, 1.0, 5.0, hex(0x3A3E46))
        b.el(cx, cy - 2, 12.0, 2.0, hex(0xB0B6C0)); b.el(cx, cy - 2, 5.0, 1.0, hex(0x868C96)); b.dot(cx - 7, cy - 3, hex(0xE8EEF4))
    }

    private val HAIR = longArrayOf(0x2A1A14, 0x5A3A1A, 0xC8A050, 0x8A2A1A, 0x141414, 0x7A6A60, 0xD0D0D0)

    /** A spectator from behind, head above the seat at [y]. */
    fun spectator(b: Buf, who: Who, cx: Double, y: Double, seed: Long) {
        val hair = hex(HAIR[(seed % HAIR.size).toInt()]); val dk = hex(0x0C0810)
        when (who) {
            Who.GHOST -> { b.el(cx, y - 4, 4.0, 4.0, hex(0xE8E4F0)); b.box(cx - 4, y - 4, 9.0, 5.0, hex(0xE8E4F0)); b.dot(cx - 2, y - 5, hex(0xFFFFFF)); return }
            Who.ASTRO -> { b.el(cx, y - 3, 3.0, 3.0, hair); var a = 0.0; while (a < 6.3) { b.dot(cx + cos(a) * 4.5, y - 3 + sin(a) * 4.5, hex(0xA8D8F0)); a += .3 }; b.dot(cx - 2, y - 6, hex(0xFFFFFF)); return }
            Who.PROF -> { b.el(cx, y - 3, 3.0, 3.0, hex(0xE0B090)); b.box(cx - 3, y - 2, 1.0, 2.0, hex(0xD0D0D0)); b.box(cx + 3, y - 2, 1.0, 2.0, hex(0xD0D0D0)); b.dot(cx - 1, y - 5, hex(0xF8D8B8)); return }
            else -> b.el(cx, y - 3, 3.0, 3.0, hair)
        }
        when (who) {
            Who.DETECTIVE -> { b.box(cx - 5, y - 5, 11.0, 1.0, hex(0x4A4A56)); b.box(cx - 3, y - 8, 7.0, 3.0, hex(0x5A5A68)); b.box(cx - 3, y - 6, 7.0, 1.0, dk) }
            Who.COWBOY -> { b.box(cx - 6, y - 5, 13.0, 1.0, hex(0xA0682A)); b.box(cx - 3, y - 8, 7.0, 3.0, hex(0xB87A3A)); b.dot(cx, y - 8, hex(0x7A4A1A)); b.dot(cx - 6, y - 6, hex(0xA0682A)); b.dot(cx + 6, y - 6, hex(0xA0682A)) }
            Who.LADY -> { b.el(cx, y - 6, 2.0, 1.0, hair); b.poly(hex(0xE83A5A), cx, y - 7, cx - 4, y - 9, cx - 4, y - 5); b.poly(hex(0xE83A5A), cx, y - 7, cx + 4, y - 9, cx + 4, y - 5) }
            Who.CLOWN -> { b.el(cx - 4, y - 3, 2.0, 2.0, hex(0xF06A2A)); b.el(cx + 4, y - 3, 2.0, 2.0, hex(0xF06A2A)); b.box(cx - 1, y - 8, 3.0, 2.0, hex(0x5AD06A)); b.dot(cx, y - 9, hex(0xFFE040)) }
            Who.MOUSE -> { b.el(cx - 4, y - 6, 2.0, 2.0, dk); b.el(cx + 4, y - 6, 2.0, 2.0, dk) }
            Who.TOPHAT -> { b.box(cx - 4, y - 5, 9.0, 1.0, dk); b.box(cx - 2, y - 11, 5.0, 6.0, hex(0x1C1820)); b.box(cx - 2, y - 7, 5.0, 1.0, hex(0x8A2030)) }
            Who.BERET -> { b.el(cx + 1, y - 5, 4.0, 1.0, hex(0x8A2030)); b.dot(cx + 1, y - 7, hex(0x8A2030)) }
            Who.PITH -> { b.el(cx, y - 5, 4.0, 2.0, hex(0xD8C08A)); b.box(cx - 5, y - 4, 11.0, 1.0, hex(0xB8A070)) }
            Who.SELFIE -> { b.box(cx + 3, y - 6, 1.0, 3.0, hex(0xE0B090)); b.box(cx + 2, y - 11, 4.0, 6.0, hex(0x1A1A22)); b.box(cx + 3, y - 10, 2.0, 4.0, hex(0x6A8ACA)); b.dot(cx + 5, y - 11, hex(0xF03A3A)) }
            else -> Unit
        }
    }

    /** One spectator on a seat, for the reward sheet. */
    fun seated(who: Who, seed: Long): Buf = Buf(17, 17).also { b ->
        spectator(b, who, 8.0, 12.0, seed)
        b.rect(3, 12, 11, 5, hex(0xA82038)); b.rect(3, 12, 11, 1, hex(0xE05068))
    }
}
