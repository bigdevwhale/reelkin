package app.reelkin.ui.art

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Drawing in fractional coordinates, rounded the way the prototype's canvas rounds them, so the
 * Kotlin pictures match the design pixel for pixel.
 */

const val M = Buf.OPAQUE

fun Buf.box(x: Double, y: Double, w: Double, h: Double, c: Int) {
    val X = x.roundToInt(); val Y = y.roundToInt(); val W = w.roundToInt(); val H = h.roundToInt()
    if (W <= 0 || H <= 0) return
    for (j in Y until Y + H) for (i in X until X + W) p(i, j, c)
}

fun Buf.dot(x: Double, y: Double, c: Int) = p(x.roundToInt(), y.roundToInt(), c)

fun Buf.el(cx: Double, cy: Double, rx: Double, ry: Double, c: Int) {
    val r = max(0.0, rx)
    val k = max(0, ry.roundToInt())
    for (y in -k..k) {
        val w = (r * sqrt(max(0.0, 1 - (y * y) / ((k + .5) * (k + .5))))).roundToInt()
        val x0 = (cx - w).roundToInt()
        val yy = (cy + y).roundToInt()
        for (i in 0..2 * w) p(x0 + i, yy, c)
    }
}

fun Buf.ln(x0: Double, y0: Double, x1: Double, y1: Double, c: Int) = line(x0.roundToInt(), y0.roundToInt(), x1.roundToInt(), y1.roundToInt(), c)

/** A filled polygon by scanlines, from points as x, y pairs. */
fun Buf.poly(c: Int, vararg pts: Double) {
    val n = pts.size / 2
    var y0 = Double.MAX_VALUE; var y1 = -Double.MAX_VALUE
    for (i in 0 until n) { y0 = minOf(y0, pts[2 * i + 1]); y1 = maxOf(y1, pts[2 * i + 1]) }
    val xs = DoubleArray(n)
    for (y in kotlin.math.floor(y0).toInt()..kotlin.math.ceil(y1).toInt()) {
        val yy = y + .5
        var k = 0
        for (i in 0 until n) {
            val ax = pts[2 * i]; val ay = pts[2 * i + 1]; val j = (i + 1) % n; val bx = pts[2 * j]; val by = pts[2 * j + 1]
            if ((ay <= yy && by > yy) || (by <= yy && ay > yy)) xs[k++] = ax + (yy - ay) * (bx - ax) / (by - ay)
        }
        xs.sort(0, k)
        var i = 0
        while (i + 1 < k) { val a = xs[i].roundToInt(); val b = xs[i + 1].roundToInt(); for (x in a until b) p(x, y, c); i += 2 }
    }
}

fun hex(c: Long) = rgb(c)
