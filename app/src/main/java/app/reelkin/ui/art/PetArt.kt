package app.reelkin.ui.art

import app.reelkin.data.Species
import app.reelkin.data.Trait
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class Mood { IDLE, HAPPY, SCARED, SLEEP, WATCH, SAD }

data class Pose(
    val sp: Species,
    val shiny: Boolean = false,
    val stage: Int = 2,
    val traits: Set<Trait> = emptySet(),
    val t: Double = 0.0,
    val mood: Mood = Mood.IDLE,
    val blink: Boolean = false,
    val hop: Double = 0.0,
    val look: Int = 0,
)

/** The pet from the film can: one round body, a species' ears and tail, and the traits it picked up from films. */
object PetArt {
    const val W = 64
    const val H = 66
    val INK = hex(0x1A0E14)

    private class Look(val f: Long, val b: Long, val inner: Long, val shiny: Long)
    private val LOOKS = mapOf(
        Species.CAT to Look(0x5C6A8C, 0xD4D8E6, 0xE890A8, 0xD8609A),
        Species.FOX to Look(0xE47A2E, 0xFBEEDC, 0x4A2014, 0x4AA8E0),
        Species.COON to Look(0x8C8A96, 0xE2DED8, 0x5A4A50, 0xD8B840),
        Species.BAT to Look(0x5C3C78, 0xA684BC, 0xE07A9A, 0x34A884),
        Species.OWL to Look(0x9C6A38, 0xECD8AC, 0x5A3A1A, 0xD84A4A),
    )

    fun fur(sp: Species, shiny: Boolean): Int = LOOKS.getValue(sp).let { hex(if (shiny) it.shiny else it.f) }

    fun draw(b: Buf, o: Pose) {
        b.clear()
        val lk = LOOKS.getValue(o.sp)
        val fur = fur(o.sp, o.shiny); val R = ramp(fur); val belly = hex(lk.b); val inner = hex(lk.inner)
        val k = doubleArrayOf(0.0, .9, 1.1, 1.3)[o.stage.coerceIn(1, 3)]
        val T = o.traits; val t = o.t
        val sleep = o.mood == Mood.SLEEP
        val shake = if (o.mood == Mood.SCARED) (if ((t * 30).roundToInt() % 2 == 1) 1 else -1) else 0
        val cx = 32.0 + shake
        val gy = 64.0 - o.hop.roundToInt()
        val br = if (sleep) (sin(t * 1.4) * .6 + .4).roundToInt().toDouble() else (sin(t * 2.4) * .5 + .5).roundToInt().toDouble()
        val brx = ((if (sleep) 14 else 12) * k).roundToInt().toDouble()
        val bry = ((if (sleep) 8 else 10) * k).roundToInt().toDouble()
        val by = gy - bry - 1
        val hrx = (14 * k).roundToInt().toDouble(); val hry = (12 * k).roundToInt().toDouble()
        val hy = by - bry - hry + (if (sleep) (10 * k).roundToInt() else (7 * k).roundToInt()) + br
        val ht = hy - hry
        val sw = sin(t * 1.6)
        val bat = o.sp == Species.BAT; val owl = o.sp == Species.OWL

        // wings, behind
        if (bat || owl) b.lit(ramp(mix(fur, hex(0x1C0C18), .25f))) {
            for (s in intArrayOf(-1, 1)) {
                val ex = cx + s * (brx - 2)
                val flap = if (o.mood == Mood.HAPPY) (sin(t * 14) * 2).roundToInt() else 0
                if (bat) poly(M, ex, by - bry + 3, ex + s * 9 * k, by - 6 * k - flap, ex + s * 8 * k, by + 2, ex + s * 6 * k, by + 1, ex + s * 5 * k, by + 5 * k, ex, by + bry - 2)
                else el(ex + s * 2, by + 1, 4 * k, 8 * k, M)
            }
        }
        val ex0 = cx + if (sleep) 3 else 0
        // silhouette, lit as one shape
        b.lit(R) {
            for (i in 0..10) {
                val s = i / 10.0
                when (o.sp) {
                    Species.CAT -> el(cx + brx - 3 + 9 * s * k + sw * s * s * 2, by + 4 - 16 * s * k + 4 * s * s * k, 1.6 * k, 1.6 * k, M)
                    Species.FOX -> el(cx + brx - 4 + 10 * s * k + sw * s * 2, by + 3 - 12 * s * k, (2 + 3.4 * sin(s * PI * .9)) * k, (2 + 3 * sin(s * PI * .9)) * k, M)
                    Species.COON -> el(cx + brx - 3 + 9 * s * k + sw * s * 2, by + 4 - 14 * s * k, 2.6 * k, 2.6 * k, M)
                    else -> Unit
                }
            }
            el(cx, by, brx, bry, M)
            for (s in intArrayOf(-1, 1)) el(cx + s * 5 * k, gy - 1, 3 * k, 1.5, M)
            el(ex0, hy, hrx, hry, M)
            for (s in intArrayOf(-1, 1)) when (o.sp) {
                Species.CAT -> poly(M, ex0 + s * (hrx - 1), ht + 6 * k, ex0 + s * (hrx - 2), ht - 5 * k, ex0 + s * 3 * k, ht + 2)
                Species.FOX -> poly(M, ex0 + s * hrx, ht + 7 * k, ex0 + s * (hrx - 1), ht - 7 * k, ex0 + s * 3 * k, ht + 2)
                Species.COON -> el(ex0 + s * (hrx - 4 * k), ht + 2, 3.4 * k, 3.4 * k, M)
                Species.BAT -> poly(M, ex0 + s * (hrx + 1), ht + 7 * k, ex0 + s * (hrx + 4 * k), ht - 6 * k, ex0 + s * 2 * k, ht + 3)
                Species.OWL -> poly(M, ex0 + s * (hrx - 1), ht + 4 * k, ex0 + s * (hrx + 2 * k), ht - 4 * k, ex0 + s * (hrx - 6 * k), ht + 2)
            }
        }
        // tail tip and rings
        if (o.sp == Species.FOX) b.el(cx + brx - 4 + 10 * k + sw * 2, by + 3 - 12 * k, 2.4 * k, 2.2 * k, hex(0xFBF3E6))
        if (o.sp == Species.COON) for (s in doubleArrayOf(.35, .65, .95)) b.el(cx + brx - 3 + 9 * s * k + sw * s * 2, by + 4 - 14 * s * k, 2.2 * k, 1.0, hex(0x3A3440))
        // belly and face
        b.el(cx, by + 2 * k, brx - 4 * k, bry - 3 * k, belly)
        if (owl) {
            var y = (by - 2).roundToInt()
            while (y < by + bry - 2) { var x = (cx - brx + 6).roundToInt(); while (x < cx + brx - 5) { b.p(x + (y and 1), y, mix(belly, fur, .5f)); x += 3 }; y += 3 }
        }
        for (s in intArrayOf(-1, 1)) {
            if (o.sp == Species.CAT || o.sp == Species.FOX) b.poly(inner, ex0 + s * (hrx - 3 * k), ht + 5 * k, ex0 + s * (hrx - 3 * k), ht - 2 * k, ex0 + s * 5 * k, ht + 3)
            if (bat) b.poly(inner, ex0 + s * hrx, ht + 5 * k, ex0 + s * (hrx + 3 * k), ht - 3 * k, ex0 + s * 3 * k, ht + 3)
        }
        val ey = hy + (1 * k).roundToInt()
        val eL = ex0 - (5 * k).roundToInt(); val eR = ex0 + (5 * k).roundToInt()
        if (o.sp == Species.COON) { b.el(ex0, ey, hrx - 2, 2.4 * k, hex(0x3A3440)); b.dot(ex0, ey - 2, mix(fur, hex(0xFFFFFF), .3f)) }
        if (owl) for (x in doubleArrayOf(eL, eR)) b.el(x, ey, 4 * k, 4 * k, mix(belly, hex(0xFFFFFF), .2f))
        else if (!bat) b.el(ex0, hy + 5 * k, 5 * k, 3 * k, belly)
        // eyes
        val watch = o.mood == Mood.WATCH
        val white = hex(0xFFFFFF)
        for (x0 in doubleArrayOf(eL, eR)) {
            val x = x0 + o.look
            if (sleep || o.blink) { b.box(x - 1, ey + 1, 3.0, 1.0, INK); continue }
            when (o.mood) {
                Mood.HAPPY -> { b.dot(x - 1, ey + 1, INK); b.dot(x, ey, INK); b.dot(x + 1, ey + 1, INK); continue }
                Mood.SCARED -> { b.box(x - 1, ey - 1, 4.0, 4.0, white); b.dot(x, ey, INK); continue }
                else -> Unit
            }
            if (owl) b.el(x0, ey, 2.4 * k, 2.4 * k, hex(0xF8C830))
            val up = if (watch) 1.0 else 0.0
            b.box(x - 1, ey - 1 - up, 3.0, 4 + up, INK)
            b.dot(x - 1, ey - 1 - up, if (watch) hex(0xFFF2C0) else white)
            if (watch) b.dot(x + 1, ey + 2, hex(0xFFD870))
        }
        if (o.mood == Mood.SAD && !sleep) { b.dot(eL - 1, ey + 3, hex(0x8AD0FF)); b.dot(eL - 1, ey + 4 + (t * 4).roundToInt() % 3, hex(0x5AA8F0)) }
        // nose and mouth
        val ny = ey + (3 * k).roundToInt()
        if (owl) b.poly(hex(0xF0A030), ex0 - 2, ny - 1, ex0 + 2, ny - 1, ex0, ny + 2)
        else if (Trait.NOSE !in T) b.dot(ex0, ny, if (bat) hex(0x2A1830) else if (o.sp == Species.CAT) hex(0xD06080) else INK)
        if (!owl && !sleep) { b.dot(ex0 - 1, ny + 1, INK); b.dot(ex0 + 1, ny + 1, INK); if (o.mood == Mood.HAPPY) b.dot(ex0, ny + 2, hex(0xC83A5A)) }
        if (bat && !sleep) { b.dot(ex0 - 1, ny + 2, white); b.dot(ex0 + 1, ny + 2, white) }
        // traits
        val neck = hy + hry - 1
        if (Trait.BLUSH in T) for (x in doubleArrayOf(eL - 2, eR + 1)) { b.dot(x, ey + 2, hex(0xFF8AA8)); b.dot(x + 1, ey + 2, hex(0xF06A8A)) }
        if (Trait.NOSE in T) { b.el(ex0, ny, 1.5, 1.5, hex(0xE82A2A)); b.dot(ex0 - 1, ny - 1, hex(0xFFB0A0)) }
        if (Trait.GLASSES in T) {
            for (x in doubleArrayOf(eL, eR)) { var a = 0.0; while (a < 6.3) { b.dot(x + (cos(a) * 3).roundToInt(), ey + .5 + (sin(a) * 3).roundToInt(), hex(0x2A1A14)); a += .35 } }
            b.ln(eL + 3, ey, eR - 3, ey, hex(0x2A1A14))
        }
        if (Trait.BOLTS in T && !sleep) for (s in intArrayOf(-1, 1)) {
            val x = ex0 + s * (hrx - 2) + if (s > 0) 0 else -2
            b.box(x, neck - 2, 3.0, 2.0, hex(0xA8B0B8)); b.dot(x + if (s > 0) 2 else 0, neck - 2, hex(0xE8F0F8))
        }
        if (Trait.BANDANA in T && !sleep) {
            b.poly(hex(0xC83030), cx - 7 * k, neck, cx + 7 * k, neck, cx, neck + 6 * k)
            b.dot(cx - 2, neck + 1, white); b.dot(cx + 2, neck + 1, white); b.dot(cx, neck + 3, white)
        }
        if (Trait.BOW in T && !sleep) {
            b.poly(hex(0xC83A8A), cx, neck + 1, cx - 4, neck - 1, cx - 4, neck + 3); b.poly(hex(0xC83A8A), cx, neck + 1, cx + 4, neck - 1, cx + 4, neck + 3)
            b.box(cx - 1, neck, 2.0, 2.0, hex(0x6A1A4A))
        }
        if (Trait.GLOVES in T) for (s in intArrayOf(-1, 1)) {
            val x = cx + s * (brx - 1); val y = by + 2 + if (o.mood == Mood.HAPPY) -4 else 0
            b.el(x, y, 2.6, 2.4, white); b.dot(x - 1, y + 2, hex(0xC8C0D0)); b.dot(x + 1, y + 2, hex(0xC8C0D0)); b.box(x - 2, y - 3, 5.0, 1.0, hex(0xF0ECF4))
        }
        if (Trait.ANTENNA in T) {
            val ax = ex0 + 4; val a0 = ht + 1
            b.ln(ax, a0, ax + 3, a0 - 7, hex(0x8A90A0)); b.box(ax + 2, a0 - 9, 3.0, 3.0, if ((t * 2).toInt() % 2 == 1) hex(0xFF4A5A) else hex(0x6AFF8A))
        }
        if (Trait.HAT in T) {
            val top = ht + 2
            b.box(ex0 - 11 * k, top, 22 * k, 2.0, hex(0x5A5A68)); b.box(ex0 - 11 * k, top, 22 * k, 1.0, hex(0x7A7A88))
            b.box(ex0 - 6 * k, top - 6, 12 * k + 1, 6.0, hex(0x5A5A68)); b.box(ex0 - 6 * k, top - 6, 1.0, 6.0, hex(0x7A7A88)); b.box(ex0 + 6 * k, top - 6, 1.0, 6.0, hex(0x3A3A46))
            b.clearAt(ex0.roundToInt() - 1, (top - 6).roundToInt(), 3, 1)
            b.box(ex0 - 6 * k, top - 2, 12 * k + 1, 1.0, hex(0x1A1A20)); b.box(ex0 - 6 * k, top - 1, 12 * k + 1, 1.0, hex(0xA02030))
        }
        b.outline(INK)
    }

    /** A still portrait for sheets. */
    fun still(o: Pose): Buf = Buf(W, H).also { draw(it, o) }
}
