package app.reelkin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelkin.data.Genre
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T

/** Bars with labels under them; tap one to read what it holds ([describe]), the [highlight] one in velvet. */
@Composable
fun Bars(values: List<Long>, labels: List<String>, modifier: Modifier = Modifier, color: Color = C.gold, highlight: Int = -1, every: Int = 1, wide: Boolean = false, describe: ((Int) -> String)? = null) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    var picked by remember(values) { mutableIntStateOf(-1) }
    Column(modifier.fillMaxWidth()) {
        if (describe != null) Text(if (picked in values.indices) describe(picked) else t("Нажми на столбик", "Tap a bar"),
            style = T.Small.copy(color = if (picked in values.indices) C.ink else C.faint), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 4.dp))
        val tap = if (describe == null) Modifier else Modifier.pointerInput(values) {
            detectTapGestures { o -> val i = (o.x / (size.width / values.size.coerceAtLeast(1).toFloat())).toInt().coerceIn(0, values.size - 1); picked = if (picked == i) -1 else i }
        }
        Canvas(Modifier.fillMaxWidth().height(110.dp).then(tap)) {
            val n = values.size.coerceAtLeast(1)
            val w = size.width / n
            values.forEachIndexed { i, v ->
                val h = (v.toFloat() / max) * (size.height - 10)
                val c = if (i == highlight) C.velvet else color
                val dim = picked >= 0 && i != picked
                drawRoundRect((if (v == 0L) C.line else c).let { if (dim) it.copy(alpha = it.alpha * .35f) else it },
                    Offset(i * w + w * .15f, size.height - h.coerceAtLeast(2f)), Size(w * .7f, h.coerceAtLeast(2f)), CornerRadius(w * .2f))
                if (i == picked) drawCircle(C.ink, (w * .22f).coerceIn(2.5f, 6f), Offset(i * w + w / 2, size.height - h.coerceAtLeast(2f) - 7f))
            }
        }
        if (wide) Row(Modifier.fillMaxWidth()) {
            val shown = labels.filter { it.isNotEmpty() }
            shown.forEachIndexed { i, l -> Text(l, style = T.Small.copy(fontSize = T.Small.fontSize * .9f), maxLines = 1, modifier = Modifier.weight(1f),
                textAlign = when (i) { 0 -> TextAlign.Start; shown.size - 1 -> TextAlign.End; else -> TextAlign.Center }) }
        } else Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, l ->
                Text(if (i % every == 0) l else "", style = T.Small.copy(fontSize = T.Small.fontSize * .85f), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** One line of a top list: rank, a poster, name, sub and a bar for its share. */
@Composable
fun TopRow(rank: Int, name: String, sub: String, value: String, share: Float, modifier: Modifier = Modifier, cover: String? = null, genre: Genre? = null, poster: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().clip(R8.Sm).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("$rank", style = T.Small.copy(color = if (rank <= 3) C.gold else C.faint), modifier = Modifier.size(width = 20.dp, height = 18.dp), textAlign = TextAlign.End)
        if (poster && genre != null) PosterImage(name, genre, cover, 24, 34, Modifier.clip(R8.Sm), scale = 1.3.dp)
        Column(Modifier.weight(1f)) {
            Text(name, style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub.isNotEmpty()) Text(sub, style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.fillMaxWidth().padding(top = 5.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(C.surface2)) {
                Box(Modifier.fillMaxWidth(share.coerceIn(0.02f, 1f)).height(5.dp).clip(RoundedCornerShape(3.dp)).background(genre?.let { C.genre(it) } ?: C.gold))
            }
        }
        Text(value, style = T.Small.copy(color = C.muted))
    }
}

/** A ring of shares, each in its genre's colour. */
@Composable
fun GenreRing(g: List<Pair<Genre, Int>>) {
    val total = g.sumOf { it.second }.toFloat().coerceAtLeast(1f)
    Canvas(Modifier.fillMaxWidth().aspectRatio(3.2f)) {
        val d = size.height * .9f
        var a = -90f
        for ((genre, n) in g) {
            val sweep = n / total * 360f
            drawArc(C.genre(genre), a, (sweep - 1.2f).coerceAtLeast(.5f), false, Offset((size.width - d) / 2, (size.height - d) / 2), Size(d, d), style = Stroke(d * .16f))
            a += sweep
        }
    }
}

/** A big number with what it counts under it, on a soft tile. */
@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier, color: Color = C.gold) {
    Column(modifier.clip(R8.Md).background(C.surface).padding(horizontal = 10.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = T.Number.copy(color = color), maxLines = 1)
        Text(label, style = T.Small.copy(textAlign = TextAlign.Center), maxLines = 2)
    }
}
