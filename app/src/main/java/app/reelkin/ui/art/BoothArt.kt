package app.reelkin.ui.art

import android.graphics.BitmapFactory
import app.reelkin.data.Form
import app.reelkin.data.Genre
import app.reelkin.data.Species
import app.reelkin.data.Stage
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

private operator fun IntArray.component1() = this[0]
private operator fun IntArray.component2() = this[1]
private operator fun IntArray.component3() = this[2]
private operator fun IntArray.component4() = this[3]

/** What the booth needs to know to draw itself. */
data class BoothLook(
    /** The pet in the booth; null when there's none yet. */
    val species: Species?,
    val stage: Stage = Stage.YOUNG,
    val shiny: Boolean = false,
    val form: Form? = null,
    /** 0 awake .. 1 drowsy after days without films. */
    val windup: Float = 0f,
    /** A can: how near it is to opening, 0..1. */
    val hatch: Float = 0f,
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
    val rug: Boolean = false,
    val plant: Boolean = false,
    val clock: Boolean = false,
    /** The wallpaper's picture: room, room_green… */
    val wall: String = "room",
)

enum class Spot { PET, PROJECTOR, POSTER, CANS, POPCORN, PORT, NONE }

/**
 * The booth's pictures, drawn ahead of time (tools/scene.py cuts them from Codex's drawings): the room, the projector,
 * the popcorn cart, the armchair, the poster's frame and the hall's stage, with where each stands.
 */
object Scenery {
    class Pic(val buf: Buf, val x: Int, val y: Int)

    private fun read(name: String): Buf? = runCatching {
        val bmp = Scenery::class.java.getResourceAsStream("/booth/$name.png")?.use { BitmapFactory.decodeStream(it) } ?: return null
        val b = Buf(bmp.width / Buf.FINE, bmp.height / Buf.FINE, Buf.FINE)
        bmp.getPixels(b.d, 0, b.fw, 0, 0, b.fw, b.fh)
        bmp.recycle()
        b
    }.getOrNull()

    /** name → numbers, from booth.txt; reels come twice, so they're kept in order. */
    val table: List<Pair<String, IntArray>> by lazy {
        (Scenery::class.java.getResourceAsStream("/booth/booth.txt")?.bufferedReader()?.use { it.readText() }.orEmpty()).lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { l -> val p = l.trim().split(Regex("\\s+")); p[0] to p.drop(1).map { it.toInt() }.toIntArray() }
    }
    fun at(name: String): IntArray? = table.firstOrNull { it.first == name }?.second

    private val pics = HashMap<String, Pic?>()
    fun pic(name: String): Pic? = synchronized(pics) {
        pics.getOrPut(name) { read(name)?.let { b -> val p = at(name); Pic(b, p?.getOrNull(0) ?: 0, p?.getOrNull(1) ?: 0) } }
    }
    /** The poster's window in the frame: x, y, w, h. */
    val poster: IntArray by lazy { at("poster") ?: intArrayOf(92, 16, 24, 34) }
    private val rooms = HashMap<String, Buf?>()
    fun room(file: String): Buf? = synchronized(rooms) { rooms.getOrPut(file) { read(file) ?: if (file != "room") read("room") else null } }
    /** Where the port is on a wallpaper, in art pixels: x, y, w, h. */
    fun port(file: String): IntArray = (if (file == "room") at("port") else at("port_" + file.removePrefix("room_"))) ?: at("port") ?: intArrayOf(160, 48, 9, 11)
    val stage: Buf? by lazy { read("stage") }
}

/**
 * The projectionist's booth: a projector with two turning reels, the stack of film cans, a bulb on a cord,
 * the port into the hall, the favourite's poster and the pet. Lit by the bulb in the evening, by the projector's
 * beam during a film and by almost nothing at night. Drawn [Buf.FINE] times finer than its art pixels.
 */
class BoothScene {
    private val bg = Buf(W, H, F)
    private var bgKey: Any? = null
    private val pet = Buf(PetArt.W, PetArt.H, F)
    private val lights = HashMap<String, FloatArray>()

    // the pet's walk and moods
    var x = 112.0; private var tx = 112.0; private var dir = 1; private var walk = false
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
    private var port = intArrayOf(160, 48, 9, 11)
    private val portC get() = port[0] + port[2] / 2.0 to port[1] + port[3] / 2.0

    fun hop(v: Double = 26.0) { if (hop == 0.0) { vy = v; hop = .01 } }
    fun mood(m: Mood, sec: Double = 2.5) { mood = m; moodUntil = now + sec }
    fun burst(n: Int = 24) {
        val cols = intArrayOf(hex(0xFFF4C0), hex(0xE8B840), hex(0xFF7AA8), hex(0xFFFFFF))
        repeat(n) { i -> val a = Random.nextDouble() * 6.28; val v = 20 + Random.nextDouble() * 40; sparks += Spark(x, 110.0, cos(a) * v, sin(a) * v - 20, 0.0, .6 + Random.nextDouble() * .6, cols[i % 4]) }
    }
    /** Plays the can opening; [done] runs when the pet is out. */
    fun hatch(done: () -> Unit) { if (!hatching) { hatching = true; hatchAt = now; onHatched = done } }

    fun hit(px: Double, py: Double, look: BoothLook): Spot = when {
        abs(px - x) < 20 && py > 84 && py < 142 -> Spot.PET
        px < 66 && py > 20 && py < 140 -> Spot.PROJECTOR
        px in POSTER_X - 4.0..POSTER_X + 28.0 && py in 8.0..54.0 -> Spot.POSTER
        px > 68 && px < 94 && py > 112 -> Spot.CANS
        look.popcorn && px > 128 && px < 158 && py > 90 -> Spot.POPCORN
        px > 156 -> Spot.PORT
        else -> Spot.NONE
    }

    /** Draws the moment [t] seconds in; [dt] moves the pet and the reels. */
    fun draw(b: Buf, look: BoothLook, t: Double, dt: Double) {
        now = t
        port = Scenery.port(look.wall)
        val key = listOf(look.poster, look.cans, look.neon, look.popcorn, look.chair, look.rug, look.plant, look.clock, look.wall)
        if (key != bgKey) { drawBg(look); bgKey = key }
        b.copyFrom(bg)
        if (look.seance != null) ang += dt * 4.2
        val reels = Scenery.table.filter { it.first == "reel" }.map { it.second }
        if (reels.size == 2) for ((k, r) in reels.withIndex()) reel(b, r[0].toDouble(), r[1].toDouble(), r[2].toDouble(), if (k == 0) ang else ang * 1.27 + .4)
        else { reel(b, 26.0, 36.0, 14.0, ang); reel(b, 52.0, 46.0, 11.0, ang * 1.27 + .4) }
        val chair = look.chair && !hatching
        update(look, t, dt)
        val gy = if (chair && !walk && abs(x - CHAIR_X) < 2) 128 else 141
        if (look.species == null || look.stage == Stage.EGG || hatching) drawCan(b, look, t, gy) else {
            val m = when {
                moodUntil > t -> mood
                look.seance != null -> Mood.WATCH
                look.night || look.windup >= .8f -> Mood.SLEEP
                else -> Mood.IDLE
            }
            PetArt.draw(pet, Pose(look.species, look.stage, look.shiny, look.form, t, m, t < blinkUntil,
                hop + if (walk && hop == 0.0) abs(sin(t * 12)) * 1.5 else 0.0, look.windup))
            val px = (x - PetArt.W / 2.0).roundToInt()
            if (dir < 0 && walk) blitFlipped(b, pet, px, gy - PetArt.H) else b.blit(pet, px, gy - PetArt.H)
            if (m == Mood.SLEEP && Random.nextDouble() < dt * .8) zz += Z(x + 12, gy - 50.0, 0.0)
        }
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
            look.species == null || look.stage == Stage.EGG -> tx = CHAIR_X
            look.seance != null -> tx = if (chair) CHAIR_X else 118.0
            chair && t > next && Random.nextDouble() < .6 -> { tx = CHAIR_X; next = t + 8 + Random.nextDouble() * 8 }
            look.night -> tx = x
            t > next -> { tx = 96 + Random.nextDouble() * 30; next = t + 4 + Random.nextDouble() * 6 }
        }
        val dx = tx - x
        walk = abs(dx) > .5 && look.species != null && look.stage != Stage.EGG && !hatching
        if (walk) { x += sign(dx) * min(abs(dx), dt * 14); dir = sign(dx).toInt() }
        if (hop > 0) { vy -= dt * 130; hop = max(0.0, hop + vy * dt); if (hop == 0.0) vy = 0.0 }
        if (t > nextBlink) { blinkUntil = t + .13; nextBlink = t + 2 + Random.nextDouble() * 3.5 }
    }

    private fun drawCan(b: Buf, look: BoothLook, t: Double, gy: Int) {
        val sp = look.species
        if (sp == null) { Icons.can(b, CHAIR_X, 135.0, hex(0xB8243E)); return }
        val px = (CHAIR_X - PetArt.W / 2.0).roundToInt()
        if (!hatching) {
            PetArt.draw(pet, Pose(sp, Stage.EGG, t = t, hatch = look.hatch))
            b.blit(pet, px, 141 - PetArt.H)
            return
        }
        val ht = t - hatchAt
        if (ht < 2.5) {
            PetArt.draw(pet, Pose(sp, Stage.EGG, t = t, hatch = 1f))
            b.blit(pet, px, 141 - PetArt.H)
        } else {
            val lt = ht - 2.5
            PetArt.draw(pet, Pose(sp, Stage.BABY, look.shiny, t = t, mood = Mood.HAPPY, hop = max(0.0, sin(lt * 6) * 4)))
            b.blit(pet, px, 141 - PetArt.H)
            if (lt < 1.2) { val ly = 128 - lt * 80 + lt * lt * 60; val lx = CHAIR_X + lt * 30; b.el(lx, ly, 12.0, 2.0, hex(0xB0B6C0)); b.el(lx, ly, 5.0, 1.0, hex(0x868C96)) }
            if (lt < .05) burst(36)
        }
        if (ht > 4.2) { hatching = false; x = CHAIR_X; tx = CHAIR_X; onHatched?.invoke(); onHatched = null }
    }

    private fun blitFlipped(b: Buf, o: Buf, x: Int, y: Int) {
        for (j in 0 until o.fh) for (i in 0 until o.fw) {
            val c = o.d[j * o.fw + (o.fw - 1 - i)]
            if (c ushr 24 != 0) b.pf(x * F + i, y * F + j, c)
        }
    }

    /** A reel drawn on the fine grid, so its spokes turn smoothly. */
    private fun reel(b: Buf, cx: Double, cy: Double, r: Double, a: Double) {
        val s = F.toDouble()
        val n = ((r + 1) * 2 * s).toInt() + 2
        val t = Buf(n, n)
        val c = n / 2.0; val R = r * s
        fun disc(rr: Double, col: Int) = t.el(c, c, rr, rr, col)
        disc(R, hex(0x1E1E24)); disc(R - s, hex(0x8A909A)); disc(R * .78, hex(0x3A2A22))
        val hole = (if (r > 12) 2.5 else 2.0) * s
        for (i in 0 until 3) { val an = a + i * 2.094; t.el(c + cos(an) * R * .5, c + sin(an) * R * .5, hole, hole, hex(0x120A10)) }
        for (i in 0 until 3) {
            val an = a + i * 2.094 + 1.047
            for (w in -1..0) t.ln(c + w, c, c + w + cos(an) * (R - 2 * s), c + sin(an) * (R - 2 * s), hex(0xB8C0CA))
        }
        disc(2 * s, hex(0xDCE0E8)); disc(s * .6, hex(0x3A3A40))
        var an = -2.7; while (an < -.5) { t.dot(c + cos(an) * (R - s), c + sin(an) * (R - s), hex(0xD8E0E8)); an += .03 }
        val ox = ((cx - r - 1) * s).roundToInt() - 1; val oy = ((cy - r - 1) * s).roundToInt() - 1
        for (j in 0 until n) for (i in 0 until n) { val col = t.d[j * n + i]; if (col ushr 24 != 0) b.pf(ox + i, oy + j, col) }
    }

    private fun drawBg(look: BoothLook) {
        val b = bg; b.clear()
        val room = Scenery.room(look.wall)
        if (room != null) b.blit(room, 0, 0) else {
            b.rect(0, 0, W, 120, hex(0x3A2436)); b.rect(0, 86, W, 34, hex(0x4A2C1C)); b.rect(0, 120, W, 30, hex(0x5A3620))
        }
        // the favourite's poster, behind its frame
        // Exactly in the frame's window, as tools/scene.py measured it; the frame's rim covers the edges.
        val (wx, wy, ww, wh) = Scenery.poster
        if (look.poster != null) b.blitScaled(look.poster, wx, wy, ww, wh) else b.rect(wx, wy, ww, wh, hex(0x2A1A26))
        Scenery.pic("frame")?.let { b.blit(it.buf, it.x, it.y) }
        // the bulb on its cord
        b.line(76, 0, 76, 11, hex(0x140A10)); b.rect(74, 11, 5, 3, hex(0x4A4048)); b.ell(76, 16, 2, 2, hex(0xE8D8A0))
        if (look.clock) Scenery.pic("clock")?.let { b.blit(it.buf, it.x, it.y) }
        if (look.rug) Scenery.pic("rug")?.let { b.blit(it.buf, it.x, it.y) }
        if (look.plant) Scenery.pic("plant")?.let { b.blit(it.buf, it.x, it.y) }
        Scenery.pic("projector")?.let { b.blit(it.buf, it.x, it.y) }
        // the cans, last films on top
        val stack = Scenery.pic("cans")
        if (stack != null) b.blit(stack.buf, stack.x, stack.y) else {
            val cols = (List(4) { hex(0x6A5A62) } + look.cans).takeLast(4)
            for (k in 0 until 4) Icons.can(b, 80.0 + if (k % 2 == 1) 1 else -1, 136.0 - k * 5, cols[k])
        }
        if (look.chair) Scenery.pic("chair")?.let { b.blit(it.buf, it.x, it.y) }
        if (look.popcorn) Scenery.pic("popcorn")?.let { b.blit(it.buf, it.x, it.y) }
        if (look.neon) { neon(b, hex(0x5A2A44), look.ru); b.rect(123, 23, 21, 1, hex(0x2A1A22)); b.rect(123, 32, 21, 1, hex(0x2A1A22)) }
    }

    private fun neon(b: Buf, c: Int, ru: Boolean) {
        var x = 126
        for (g in if (ru) NEON else HUSH) { g.forEachIndexed { y, row -> row.forEachIndexed { i, ch -> if (ch == '1') b.p(x + i, 26 + y, c) } }; x += g[0].length + 1 }
    }

    /** How lit each fine pixel is, dithered on the fine grid. */
    private fun lightMap(mode: String, neon: Boolean): FloatArray = lights.getOrPut("$mode$neon${port.joinToString()}") {
        val fw = W * F; val fh = H * F
        val L = FloatArray(fw * fh)
        val amb = when (mode) { "eve" -> .42; "seance" -> .24; else -> .19 }
        val (lx, ly) = LENS; val (px, py) = portC
        for (fy in 0 until fh) for (fx in 0 until fw) {
            val x = (fx + .5) / F; val y = (fy + .5) / F
            var l = amb
            if (mode == "eve") l += 1.0 * max(0.0, 1 - hypot(x - 76.0, (y - 16) * 1.1) / 115).pow(1.25)
            if (mode == "seance") {
                l += .55 * max(0.0, 1 - hypot(x - 22.0, y - 70.0) / 38).pow(1.5)
                if (x > lx) { val tt = (x - lx) / (px - lx); val yc = ly + (py - ly) * tt; l += .4 * max(0.0, 1 - abs(y - yc) / (9 + 16 * tt)) }
                l += .45 * max(0.0, 1 - hypot(x - px, y - py) / 30).pow(1.3)
            }
            if (neon && mode != "eve") l += .35 * max(0.0, 1 - hypot(x - 136.0, y - 28.0) / 34).pow(1.4)
            val v = min(1.12, l) * 12; val f = kotlin.math.floor(v)
            L[fy * fw + fx] = ((f + if (dith(fx, fy, v - f)) 1 else 0) / 12).toFloat()
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
            // The wire on the fine grid, the bulbs a little rounder.
            for (fx in 2 * F until 156 * F) {
                val x = fx / F.toDouble()
                val y = 4 + 4 * sin(Math.PI * ((x % 52) / 52.0))
                b.pf(fx, (y * F).roundToInt(), hex(0x24141C))
            }
            for (x in 4 until 156 step 8) {
                val y = 4 + 4 * sin(Math.PI * ((x % 52) / 52.0))
                val i = (x shr 3) % 4
                val on = if (mode == "night") ((t * 1.5).toInt() + i) % 4 == 0 else ((t * 2).toInt() + i) % 4 != 0
                val c = if (on) cs[i] else mix(cs[i], hex(0x140810), .6f)
                val fx = x * F; val fy = (y * F).roundToInt() + 1
                for (j in 0 until 5) for (k in 0 until 4) if (!(j == 4 && (k == 0 || k == 3))) b.pf(fx + k - 1, fy + j, c)
                if (on) { b.pf(fx, fy + 1, hex(0xFFFFFF)); for (j in -2..6) for (k in -3..5) if (b.getF(fx + k, fy + j) != 0) b.pf(fx + k, fy + j, cs[i], .12f) }
            }
        }
        if (look.neon) neon(b, if ((t * 7).toInt() % 37 == 0) hex(0x5A2A44) else hex(0xFF7AB0), look.ru)
        if (mode == "eve") { b.ell(76, 16, 2, 2, hex(0xFFF8D0)); b.p(75, 15, hex(0xFFFFFF)) }
        if (look.seance != null) {
            val fl = .82 + .18 * sin(t * 37) * sin(t * 13)
            val fw = W * F
            val beam = beam(portC)
            for (i in beam.indices) { val a = beam[i]; if (a > 0f) { val x = i % fw; val y = i / fw; if (dith(x, y, a * fl)) b.pf(x, y, if (a > .45f) hex(0xFFF8DC) else hex(0xF0DCA0)) } }
            val (lx, ly) = LENS; val (px, py) = portC
            for (m in motes) {
                m.t += m.v * .09; if (m.t > 1) m.t = 0.0
                val x = lx + 4 + m.t * (px - lx - 6); val t2 = (x - lx) / (px - lx); val y = ly + (py - ly) * t2 + m.o * (3 + 6 * t2) + sin(t * 2 + m.v * 100)
                b.pf((x * F).roundToInt(), (y * F).roundToInt(), hex(0xFFFFFF))
            }
            b.rect(port[0], port[1], port[2], port[3], mix(hex(look.seance.color.toLong()), hex(0xFFFFFF), (.25 + .35 * abs(sin(t * 3.1))).toFloat()))
        }
    }

    companion object {
        const val W = 174
        const val H = 150
        const val F = Buf.FINE
        const val CHAIR_X = 113.0
        const val POSTER_X = 92
        private val NEON = listOf(listOf("111", "010", "010", "010", "010"), listOf("1001", "1011", "1101", "1001", "1001"),
            listOf("10101", "10101", "10101", "10101", "11111"), listOf("111", "100", "110", "100", "111"))
        private val HUSH = listOf(listOf("101", "101", "111", "101", "101"), listOf("101", "101", "101", "101", "111"),
            listOf("111", "100", "111", "001", "111"), listOf("101", "101", "111", "101", "101"))
        /** Where the beam leaves the lens and where it goes in the port, in art pixels. */
        private val LENS: Pair<Double, Double> by lazy { Scenery.at("lens")?.let { it[0].toDouble() to it[1].toDouble() } ?: (66.0 to 72.0) }
        private val beams = HashMap<Pair<Double, Double>, FloatArray>()
        /** The projector's beam from the lens to the port, how bright each fine pixel is. */
        private fun beam(to: Pair<Double, Double>): FloatArray = synchronized(beams) { beams.getOrPut(to) {
            val fw = W * F; val fh = H * F
            val (lx, ly) = LENS; val (px, py) = to
            FloatArray(fw * fh).also { B ->
                for (fx in (lx * F).toInt() until (px * F).toInt()) {
                    val x = fx / F.toDouble()
                    val t = (x - lx) / (px - lx); val yc = ly + (py - ly) * t; val hw = 1.5 + 4 * t
                    for (fy in ((yc - hw - 1) * F).toInt()..((yc + hw + 1) * F).toInt()) {
                        if (fy !in 0 until fh) continue
                        val d = abs(fy / F.toDouble() - yc)
                        if (d <= hw + .5) B[fy * fw + fx] = (.62 * (1 - d / (hw + 1.5)) * (1 - .35 * t)).toFloat()
                    }
                }
            }
        } }
    }
}
