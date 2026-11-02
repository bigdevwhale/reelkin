package app.reelkin.ui.art

import app.reelkin.data.Genre
import app.reelkin.data.Species
import app.reelkin.data.Trait
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/** What the booth needs to know to draw itself. */
data class BoothLook(
    val species: Species?,
    val shiny: Boolean,
    val stage: Int,
    val traits: Set<Trait>,
    /** The favourite film's poster on the wall, already in pixels. */
    val poster: Buf?,
    /** Colours of the last four films, for the stack of cans. */
    val cans: List<Int>,
    val garland: Boolean,
    val chair: Boolean,
    val neon: Boolean,
    val popcorn: Boolean,
    val night: Boolean,
    /** The film on the projector, if any. */
    val seance: Genre?,
    /** The neon says ТИШЕ in Russian, HUSH in English. */
    val ru: Boolean = true,
)

enum class Spot { PET, PROJECTOR, POSTER, CANS, POPCORN, PORT, NONE }

/**
 * The projectionist's booth, as in the prototype: a projector with two reels, a stack of film cans,
 * a bulb on a cord, the port into the hall and the pet. Lit by the bulb in the evening, by the
 * projector's beam during a film and by almost nothing at night.
 */
class BoothScene {
    private val bg = Buf(W, H)
    private var bgKey: Any? = null
    private val pet = Buf(PetArt.W, PetArt.H)
    private val lights = HashMap<String, FloatArray>()

    // the pet's walk and moods
    var x = 114.0; private var tx = 114.0; private var dir = 1; private var walk = false
    private var hop = 0.0; private var vy = 0.0; private var next = 0.0
    private var blinkUntil = 0.0; private var nextBlink = 5.0
    private var mood = Mood.IDLE; private var moodUntil = 0.0
    private var ang = 0.0
    private var hatchAt = -1.0
    var hatching = false; private set
    private var onHatched: (() -> Unit)? = null

    private class Z(var x: Double, var y: Double, var a: Double)
    private class Spark(var x: Double, var y: Double, val vx: Double, var vy: Double, var a: Double, val l: Double, val c: Int)
    private class Mote(var t: Double, val o: Double, val v: Double)
    private val zz = ArrayList<Z>()
    private val sparks = ArrayList<Spark>()
    private val motes = List(16) { Mote(Random.nextDouble(), Random.nextDouble() - .5, .02 + Random.nextDouble() * .04) }
    private var now = 0.0

    fun hop(v: Double = 26.0) { if (hop == 0.0) { vy = v; hop = .01 } }
    fun mood(m: Mood, sec: Double = 2.5) { mood = m; moodUntil = now + sec }
    fun burst(n: Int = 24) {
        val cols = intArrayOf(hex(0xFFF4C0), hex(0xE8B840), hex(0xFF7AA8), hex(0xFFFFFF))
        repeat(n) { i -> val a = Random.nextDouble() * 6.28; val v = 20 + Random.nextDouble() * 40; sparks += Spark(x, 110.0, cos(a) * v, sin(a) * v - 20, 0.0, .6 + Random.nextDouble() * .6, cols[i % 4]) }
    }
    fun hatch(done: () -> Unit) { if (!hatching) { hatching = true; hatchAt = now; onHatched = done } }

    fun hit(px: Double, py: Double, look: BoothLook): Spot = when {
        abs(px - x) < 18 && py > 88 && py < 142 -> Spot.PET
        px < 66 && py > 20 && py < 140 -> Spot.PROJECTOR
        px in 90.0..118.0 && py in 14.0..52.0 -> Spot.POSTER
        px > 66 && px < 94 && py > 112 -> Spot.CANS
        look.popcorn && px > 134 && px < 157 && py > 92 -> Spot.POPCORN
        px > 156 -> Spot.PORT
        else -> Spot.NONE
    }

    /** Draws the moment [t] seconds in; [dt] moves the pet and the reels. */
    fun draw(b: Buf, look: BoothLook, t: Double, dt: Double) {
        now = t
        val key = listOf(look.poster, look.cans, look.neon, look.popcorn)
        if (key != bgKey) { drawBg(look); bgKey = key }
        b.copyFrom(bg)
        if (look.seance != null) ang += dt * 4.2
        reel(b, 26.0, 36.0, 14.0, ang); reel(b, 52.0, 46.0, 11.0, ang * 1.27 + .4)
        val chair = look.chair && !hatching
        if (chair) chairBack(b)
        update(look, t, dt)
        val gy = if (chair && !walk) 133 else 140
        if (look.species == null || hatching) drawCan(b, look, t, gy) else {
            val m = when { moodUntil > t -> mood; look.seance != null -> Mood.WATCH; look.night -> Mood.SLEEP; else -> Mood.IDLE }
            PetArt.draw(pet, Pose(look.species, look.shiny, look.stage, look.traits, t, m, t < blinkUntil, hop + if (walk && hop == 0.0) abs(sin(t * 12)) * 1.5 else 0.0, if (look.seance != null && m == Mood.WATCH) 1 else 0))
            val px = (x - 32).roundToInt()
            if (dir < 0 && walk) blitFlipped(b, pet, px, gy - 64) else b.blit(pet, px, gy - 64)
            if (m == Mood.SLEEP && Random.nextDouble() < dt * .8) zz += Z(x + 10, gy - 44.0, 0.0)
        }
        if (chair) chairFront(b)
        val mode = when { look.seance != null -> "seance"; look.night -> "night"; else -> "eve" }
        light(b, lightMap(mode, look.neon))
        glow(b, look, t, mode)
        zz.removeAll { z -> z.a += dt; z.a >= 3 }
        for (z in zz) {
            val zx = (z.x + z.a * 4 + sin(z.a * 3) * 2).roundToInt(); val zy = (z.y - z.a * 8).roundToInt(); val c = if (z.a < 2.2) hex(0xE8DCF8) else hex(0x8A7AA0)
            b.rect(zx, zy, 3, 1, c); b.p(zx + 1, zy + 1, c); b.rect(zx, zy + 2, 3, 1, c)
        }
        sparks.removeAll { s -> s.a += dt; s.a >= s.l }
        for (s in sparks) { s.x += s.vx * dt; s.y += s.vy * dt; s.vy += 40 * dt; b.dot(s.x, s.y, s.c) }
    }

    private fun update(look: BoothLook, t: Double, dt: Double) {
        val chair = look.chair
        when {
            look.seance != null -> tx = if (chair) 114.0 else 120.0
            chair -> tx = 114.0
            look.night -> tx = x
            t > next -> { tx = 104 + Random.nextDouble() * 24; next = t + 4 + Random.nextDouble() * 6 }
        }
        val dx = tx - x
        walk = abs(dx) > .5 && look.species != null && !hatching
        if (walk) { x += sign(dx) * min(abs(dx), dt * 14); dir = sign(dx).toInt() }
        if (hop > 0) { vy -= dt * 130; hop = max(0.0, hop + vy * dt); if (hop == 0.0) vy = 0.0 }
        if (t > nextBlink) { blinkUntil = t + .13; nextBlink = t + 2 + Random.nextDouble() * 3.5 }
    }

    private fun drawCan(b: Buf, look: BoothLook, t: Double, gy: Int) {
        val cx = 114.0
        if (!hatching) { Icons.can(b, cx, 135.0, hex(0xB8243E)); return }
        val ht = t - hatchAt
        if (ht < 2.5) {
            val sh = if (ht % .8 < .25) (if ((ht * 40).roundToInt() % 2 == 1) 1 else -1) else 0
            Icons.can(b, cx + sh, 135.0, hex(0xB8243E))
        } else {
            val lt = ht - 2.5
            if (lt < 1.2) { val ly = 130 - lt * 80 + lt * lt * 60; val lx = cx + lt * 30; b.el(lx, ly, 12.0, 2.0, hex(0xB0B6C0)); b.el(lx, ly, 5.0, 1.0, hex(0x868C96)) }
            PetArt.draw(pet, Pose(look.species ?: Species.CAT, look.shiny, 1, emptySet(), t, Mood.HAPPY, hop = max(0.0, sin(lt * 6) * 4)))
            b.blit(pet, (cx - 32).roundToInt(), gy - 64)
            b.box(cx - 12, 136.0, 25.0, 4.0, hex(0x6A6E78)); b.box(cx - 12, 136.0, 25.0, 2.0, hex(0xB8243E))
            if (lt < .05) burst(36)
        }
        if (ht > 4.2) { hatching = false; x = 114.0; tx = 114.0; onHatched?.invoke(); onHatched = null }
    }

    private fun blitFlipped(b: Buf, o: Buf, x: Int, y: Int) {
        for (j in 0 until o.h) for (i in 0 until o.w) { val c = o.d[j * o.w + (o.w - 1 - i)]; if (c ushr 24 != 0) b.p(x + i, y + j, c) }
    }

    private fun reel(b: Buf, cx: Double, cy: Double, r: Double, a: Double) {
        b.el(cx, cy, r, r, hex(0x1E1E24)); b.el(cx, cy, r - 1, r - 1, hex(0x8A909A))
        b.el(cx, cy, (r * .78).roundToInt().toDouble(), (r * .78).roundToInt().toDouble(), hex(0x3A2A22))
        val hole = if (r > 12) 2.5 else 2.0
        for (i in 0 until 3) { val an = a + i * 2.094; b.el(cx + cos(an) * r * .5, cy + sin(an) * r * .5, hole, hole, hex(0x120A10)) }
        for (i in 0 until 3) { val an = a + i * 2.094 + 1.047; b.ln(cx, cy, cx + cos(an) * (r - 2), cy + sin(an) * (r - 2), hex(0xB8C0CA)) }
        b.el(cx, cy, 2.0, 2.0, hex(0xDCE0E8)); b.dot(cx, cy, hex(0x3A3A40))
        var an = -2.7; while (an < -.5) { b.dot(cx + cos(an) * (r - 1), cy + sin(an) * (r - 1), hex(0xD8E0E8)); an += .07 }
    }

    private fun chairBack(b: Buf) {
        val c = 114.0
        b.lit(ramp(hex(0xA82038))) { el(c, 106.0, 17.0, 5.0, M); box(c - 17, 106.0, 35.0, 24.0, M) }
        var x = c - 12; while (x < c + 14) { b.box(x, 108.0, 1.0, 18.0, hex(0x7A1628)); x += 6 }
    }

    private fun chairFront(b: Buf) {
        val c = 114.0
        b.lit(ramp(hex(0xA82038))) { box(c - 21, 120.0, 8.0, 16.0, M); box(c + 14, 120.0, 8.0, 16.0, M); box(c - 15, 129.0, 31.0, 6.0, M) }
        b.box(c - 20, 136.0, 2.0, 4.0, hex(0x3A2214)); b.box(c + 19, 136.0, 2.0, 4.0, hex(0x3A2214))
        b.box(c - 21, 120.0, 8.0, 1.0, hex(0xE8B840)); b.box(c + 14, 120.0, 8.0, 1.0, hex(0xE8B840))
    }

    private fun drawBg(look: BoothLook) {
        val b = bg; b.clear()
        b.rect(0, 0, W, 120, hex(0x3A2436))
        var x = 0; while (x < W) { b.rect(x, 0, 4, 86, hex(0x432A3E)); x += 8 }
        var y = 6
        while (y < 84) { x = (y / 8 % 2) * 4 + 2; while (x < W) { b.p(x, y, hex(0x583856)); b.p(x + 1, y + 1, hex(0x2E1C2C)); b.p(x - 1, y + 1, hex(0x2E1C2C)); x += 8 }; y += 8 }
        b.rect(0, 0, W, 3, hex(0x22141F))
        b.rect(0, 86, W, 2, hex(0x6A4A2A)); b.rect(0, 86, W, 1, hex(0x9A7444))
        b.rect(0, 88, W, 32, hex(0x4A2C1C))
        x = 4; while (x < W) { b.rect(x, 92, 20, 22, hex(0x3C2216)); b.rect(x, 92, 20, 1, hex(0x2A160E)); b.rect(x, 92, 1, 22, hex(0x2A160E)); b.rect(x, 113, 20, 1, hex(0x6A4228)); b.rect(x + 19, 92, 1, 22, hex(0x6A4228)); x += 26 }
        b.rect(0, 120, W, 30, hex(0x5A3620))
        y = 124; while (y < H) { b.rect(0, y, W, 1, hex(0x44281A)); y += 5 }
        y = 120; var i = 0
        while (y < H) { x = (i * 17) % 40; while (x < W) { b.rect(x, y, 1, 5, hex(0x3A2214)); x += 40 }; y += 5; i++ }
        b.rect(0, 120, W, 1, hex(0x24120C))
        // the wall with the port
        b.rect(156, 0, 18, 120, hex(0x4C3048)); b.rect(156, 0, 1, 120, hex(0x24141F)); b.rect(157, 0, 1, 120, hex(0x5E3C58))
        b.rect(156, 88, 18, 32, hex(0x553420)); b.rect(156, 86, 18, 2, hex(0x9A7444))
        b.rect(158, 46, 13, 15, hex(0xA87418)); b.rect(158, 46, 13, 1, hex(0xFFF0A0)); b.rect(160, 48, 9, 11, hex(0x08040C))
        // the favourite's poster
        b.rect(90, 14, 28, 38, hex(0x6A4A10)); b.rect(91, 15, 26, 36, hex(0xE8B840)); b.rect(91, 15, 26, 1, hex(0xFFF0A0))
        if (look.poster != null) b.blit(look.poster, 92, 16) else b.rect(92, 16, 24, 34, hex(0x2A1A26))
        // the bulb on its cord
        b.line(76, 0, 76, 11, hex(0x140A10)); b.rect(74, 11, 5, 3, hex(0x4A4048)); b.ell(76, 16, 2, 2, hex(0xE8D8A0))
        // the projector
        b.lit(ramp(hex(0x34403C))) { rect(30, 86, 7, 50, M); poly(M, 20.0, 139.0, 47.0, 139.0, 38.0, 130.0, 29.0, 130.0) }
        b.lit(ramp(hex(0x4A4A54))) { rect(6, 60, 13, 26, M); rect(9, 48, 7, 13, M); rect(7, 45, 11, 3, M) }
        y = 64; while (y < 84) { b.rect(8, y, 9, 1, hex(0x18161C)); y += 3 }
        b.lit(ramp(hex(0x3E5248))) { rect(18, 58, 38, 30, M); rect(20, 56, 34, 2, M) }
        b.rect(24, 66, 16, 6, hex(0xA87418)); b.rect(25, 67, 14, 4, hex(0xE8B840)); b.rect(25, 67, 14, 1, hex(0xFFF0A0))
        for (k in 0 until 4) b.p(27 + k * 3, 69, hex(0x6A4410))
        b.rect(44, 62, 7, 9, hex(0x14161A)); b.rect(45, 63, 5, 7, hex(0x2A2E34))
        b.ell(36, 80, 3, 3, hex(0x24282C)); b.line(36, 80, 41, 84, hex(0xA8B0B8)); b.rect(41, 84, 2, 2, hex(0xC83A3A))
        b.lit(ramp(hex(0xC89A3A))) { rect(56, 67, 8, 9, M); rect(63, 66, 3, 11, M) }
        b.p(65, 70, hex(0xCFE8FF)); b.p(65, 71, hex(0x8AB0D0)); b.p(65, 72, hex(0x6A90B0))
        b.line(30, 57, 26, 37, hex(0x24242A)); b.line(31, 57, 27, 37, hex(0x5A5A64)); b.line(48, 57, 52, 47, hex(0x24242A)); b.line(49, 57, 53, 47, hex(0x5A5A64))
        b.line(14, 40, 22, 58, hex(0x1A100C)); b.line(50, 58, 62, 50, hex(0x1A100C))
        // the cans, last films on top
        val cols = (List(4) { hex(0x6A5A62) } + look.cans).takeLast(4)
        for (k in 0 until 4) Icons.can(b, 80.0 + if (k % 2 == 1) 1 else -1, 136.0 - k * 5, cols[k])
        if (look.popcorn) {
            b.lit(ramp(hex(0xC82A3A))) { rect(136, 122, 19, 14, M); rect(135, 98, 21, 4, M) }
            b.rect(137, 102, 17, 20, hex(0xA8D0E0)); b.rect(138, 103, 15, 18, hex(0x20303A))
            for (yy in 110 until 121) for (xx in 138 until 153) if (dith(xx, yy, .55 + (yy - 110) / 30.0)) b.p(xx, yy, if ((xx + yy) % 3 != 0) hex(0xFFF0C0) else hex(0xF0C850))
            b.rect(137, 102, 1, 20, hex(0xE8F4FA)); b.ell(145, 97, 6, 2, hex(0xE83A4A)); b.rect(144, 94, 2, 2, hex(0xE8B840))
            for (xx in intArrayOf(138, 152)) { b.ell(xx, 137, 2, 2, hex(0x24242A)); b.p(xx, 137, hex(0x8A8A94)) }
            b.rect(139, 126, 13, 5, hex(0xFFF0C0)); b.rect(140, 127, 11, 3, hex(0xC82A3A))
        }
        if (look.neon) { neon(b, hex(0x5A2A44), look.ru); b.rect(123, 23, 21, 1, hex(0x2A1A22)); b.rect(123, 32, 21, 1, hex(0x2A1A22)) }
    }

    private fun neon(b: Buf, c: Int, ru: Boolean) {
        var x = 126
        for (g in if (ru) NEON else HUSH) { g.forEachIndexed { y, row -> row.forEachIndexed { i, ch -> if (ch == '1') b.p(x + i, 26 + y, c) } }; x += g[0].length + 1 }
    }

    private fun lightMap(mode: String, neon: Boolean): FloatArray = lights.getOrPut("$mode$neon") {
        val L = FloatArray(W * H)
        val amb = when (mode) { "eve" -> .36; "seance" -> .2; else -> .17 }
        for (y in 0 until H) for (x in 0 until W) {
            var l = amb
            if (mode == "eve") l += 1.05 * max(0.0, 1 - hypot(x - 76.0, (y - 16) * 1.1) / 112).pow(1.25)
            if (mode == "seance") {
                l += .6 * max(0.0, 1 - hypot(x - 14.0, y - 70.0) / 36).pow(1.5)
                if (x > 62) { val tt = (x - 66) / 93.0; val yc = 72 - 18 * tt; l += .4 * max(0.0, 1 - abs(y - yc) / (9 + 16 * tt)) }
                l += .45 * max(0.0, 1 - hypot(x - 164.0, y - 54.0) / 30).pow(1.3)
            }
            if (neon && mode != "eve") l += .35 * max(0.0, 1 - hypot(x - 136.0, y - 28.0) / 34).pow(1.4)
            val v = min(1.12, l) * 9; val f = kotlin.math.floor(v)
            L[y * W + x] = ((f + if (dith(x, y, v - f)) 1 else 0) / 9).toFloat()
        }
        L
    }

    private fun light(b: Buf, L: FloatArray) {
        val d = b.d
        for (i in d.indices) {
            val l = L[i]; val c = d[i]
            val r = Buf.ch(c, 16); val g = Buf.ch(c, 8); val bl = Buf.ch(c, 0)
            d[i] = if (l >= 1f) rgb((r * l).toInt(), (g * l).toInt(), (bl * l).toInt())
            else rgb((r * l + 12 * (1 - l)).toInt(), (g * l + 6 * (1 - l)).toInt(), (bl * l + 20 * (1 - l)).toInt())
        }
    }

    private fun glow(b: Buf, look: BoothLook, t: Double, mode: String) {
        if (look.garland) {
            val cs = intArrayOf(hex(0xFF5A6A), hex(0xFFD84A), hex(0x6AD0FF), hex(0x8AFF7A))
            for (x in 2 until 156) {
                val y = 4 + (4 * sin(Math.PI * ((x % 52) / 52.0))).roundToInt()
                b.p(x, y, hex(0x24141C))
                if (x % 8 == 4) {
                    val i = (x shr 3) % 4
                    val on = if (mode == "night") ((t * 1.5).toInt() + i) % 4 == 0 else ((t * 2).toInt() + i) % 4 != 0
                    b.rect(x, y + 1, 2, 2, if (on) cs[i] else mix(cs[i], hex(0x140810), .6f)); if (on) b.p(x, y + 1, hex(0xFFFFFF))
                }
            }
        }
        if (look.neon) neon(b, if ((t * 7).toInt() % 37 == 0) hex(0x5A2A44) else hex(0xFF7AB0), look.ru)
        if (mode == "eve") { b.ell(76, 16, 2, 2, hex(0xFFF8D0)); b.p(75, 15, hex(0xFFFFFF)) }
        if (look.seance != null) {
            val fl = .82 + .18 * sin(t * 37) * sin(t * 13)
            for (i in BEAM.indices) { val a = BEAM[i]; if (a > 0f) { val x = i % W; val y = i / W; if (dith(x, y, a * fl)) b.p(x, y, if (a > .45f) hex(0xFFF8DC) else hex(0xF0DCA0)) } }
            for (m in motes) {
                m.t += m.v * .09; if (m.t > 1) m.t = 0.0
                val x = 70 + m.t * 86; val t2 = (x - 66) / 93; val y = 71.5 - 17.5 * t2 + m.o * (3 + 6 * t2) + sin(t * 2 + m.v * 100)
                b.dot(x, y, hex(0xFFFFFF))
            }
            var y = 64; while (y < 84) { b.rect(8, y, 9, 1, if (sin(t * 9 + y) > 0) hex(0xFF9A3A) else hex(0xC85A1A)); y += 3 }
            b.rect(160, 48, 9, 11, mix(hex(look.seance.color.toLong()), hex(0xFFFFFF), (.25 + .35 * abs(sin(t * 3.1))).toFloat()))
            b.p(65, 71, hex(0xFFFFFF))
        }
    }

    companion object {
        const val W = 174
        const val H = 150
        private val NEON = listOf(listOf("111", "010", "010", "010", "010"), listOf("1001", "1011", "1101", "1001", "1001"),
            listOf("10101", "10101", "10101", "10101", "11111"), listOf("111", "100", "110", "100", "111"))
        private val HUSH = listOf(listOf("101", "101", "111", "101", "101"), listOf("101", "101", "101", "101", "111"),
            listOf("111", "100", "111", "001", "111"), listOf("101", "101", "111", "101", "101"))
        private val BEAM = FloatArray(W * H).also { B ->
            for (x in 66 until 160) {
                val t = (x - 66) / 93.0; val yc = 71.5 - 17.5 * t; val hw = 1.5 + 4 * t
                for (y in (yc - hw - 1).toInt()..(yc + hw + 1).toInt()) { val d = abs(y - yc); if (d <= hw + .5) B[y * W + x] = (.62 * (1 - d / (hw + 1.5)) * (1 - .35 * t)).toFloat() }
            }
        }
    }
}
