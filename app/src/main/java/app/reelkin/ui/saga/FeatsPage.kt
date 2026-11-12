package app.reelkin.ui.saga

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reelkin.domain.Cinema
import app.reelkin.domain.Feat
import app.reelkin.domain.FeatGroup
import app.reelkin.domain.FeatState
import app.reelkin.domain.Feats
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.components.Bar
import app.reelkin.ui.components.Page
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import app.reelkin.ui.components.Sheet
import app.reelkin.ui.components.t
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T

private val icons = HashMap<Feat, ImageBitmap?>()
private val grey = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** An achievement's picture, drawn by Codex (tools/feat_prompts.py, cut by tools/icons_cut.py). */
fun featIcon(f: Feat): ImageBitmap? = synchronized(icons) {
    icons.getOrPut(f) { runCatching { Feat::class.java.getResourceAsStream("/feats/${f.file}.png")?.use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull() }
}

@Composable
fun FeatIcon(f: Feat, got: Boolean, size: Dp) {
    val img = featIcon(f) ?: return
    Image(img, null, Modifier.size(size).alpha(if (got) 1f else .45f), filterQuality = FilterQuality.Low, colorFilter = if (got) null else grey)
}

/** Every achievement on its shelf: the picture, the steps climbed, how far to the next. */
@Composable
fun FeatsPage(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    var open by remember { mutableStateOf<Feat?>(null) }
    var shelf by remember { mutableStateOf(0) }
    val steps = c.feats.sumOf { it.level }; val most = c.feats.sumOf { it.max }
    Page(t("Достижения", "Achievements"), nav::close) {
        // Totals and a bar for all of them.
        Column(Modifier.fillMaxWidth().clip(R8.Lg).background(C.surface).border(1.dp, C.line, R8.Lg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$steps", style = T.Display.copy(color = C.gold))
                Text(if (ru) " из $most ступеней" else " of $most steps", style = T.Muted, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
            }
            Bar(steps / most.toFloat(), Modifier.fillMaxWidth())
            Text(if (ru) "получено: ${c.feats.count { it.level > 0 }} из ${c.feats.size} · за каждую ступень +${Feats.REWARD} 🍿 · уже ${steps * Feats.REWARD} 🍿"
                else "${c.feats.count { it.level > 0 }} of ${c.feats.size} earned · +${Feats.REWARD} 🍿 a step · ${steps * Feats.REWARD} 🍿 so far", style = T.Small)
        }
        // The nearest ones: what's almost there.
        val near = c.feats.filter { !it.done }.sortedByDescending { it.part }.take(3)
        if (near.isNotEmpty()) {
            Text(t("Почти", "Almost there").uppercase(), style = T.Eyebrow)
            for (fs in near) Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface).clickable { open = fs.f }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatIcon(fs.f, fs.level > 0, 44.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(Feats.name(fs.f, ru), style = T.Strong)
                    Text(Feats.goal(fs.f, fs.need, ru), style = T.Small)
                    Bar(fs.part, Modifier.fillMaxWidth())
                }
                Text("${fs.have}/${fs.need}", style = T.Small.copy(color = C.gold))
            }
        }
        val groups = FeatGroup.entries
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (listOf(t("Все", "All")) + groups.map { Feats.group(it, ru) }).forEachIndexed { i, n -> app.reelkin.ui.components.Chip(n, shelf == i, { shelf = i }) }
        }
        for (g in groups) if (shelf == 0 || groups[shelf - 1] == g) {
            Text(Feats.group(g, ru).uppercase(), style = T.Eyebrow, modifier = Modifier.padding(top = 6.dp))
            val list = c.feats.filter { it.f.group == g }
            for (row in list.chunked(3)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (fs in row) Tile(fs, Modifier.weight(1f)) { open = fs.f }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    open?.let { f -> c.feats.firstOrNull { it.f == f }?.let { FeatSheet(it, { ff -> open = null; nav.go(app.reelkin.ui.Overlay.ShareIt(app.reelkin.ui.cards.Share.Achievement(ff))) }) { open = null } } }
}

@Composable
private fun Tile(fs: FeatState, modifier: Modifier, onClick: () -> Unit) {
    val ru = LocalRu.current
    Column(modifier.clip(R8.Lg).background(C.surface).border(1.dp, if (fs.done) C.gold.copy(alpha = .6f) else C.line, R8.Lg).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FeatIcon(fs.f, fs.level > 0, 52.dp)
        Text(Feats.name(fs.f, ru), style = T.Small.copy(color = if (fs.level > 0) C.ink else C.muted), textAlign = TextAlign.Center, maxLines = 2)
        Dots(fs)
    }
}

/** The steps as dots: gold for reached. */
@Composable
private fun Dots(fs: FeatState) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(fs.max) { i -> Box(Modifier.size(6.dp).clip(CircleShape).background(if (i < fs.level) C.gold else C.line)) }
    }
}

@Composable
private fun FeatSheet(fs: FeatState, onShare: (Feat) -> Unit, onClose: () -> Unit) {
    val ru = LocalRu.current
    Sheet(onClose) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FeatIcon(fs.f, fs.level > 0, 88.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(Feats.name(fs.f, ru), style = T.Title)
                Text(Feats.group(fs.f.group, ru), style = T.Small)
                Dots(fs)
            }
        }
        Text(if (ru) "Сейчас: ${fs.have}" else "Now: ${fs.have}", style = T.Strong)
        if (fs.level > 0) app.reelkin.ui.sheets.ShareLine(t("Поделиться достижением", "Share the achievement"), t("открытка со значком и ступенью", "a card with the badge and the step")) { onShare(fs.f) }
        for ((i, n) in fs.f.tiers.withIndex()) {
            val got = fs.have >= n
            Row(Modifier.fillMaxWidth().clip(R8.Md).background(if (got) C.goldSoft else C.surface2).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (got) "✓" else "${i + 1}", style = T.Strong.copy(color = if (got) C.gold else C.faint))
                Text(Feats.goal(fs.f, n, ru).replaceFirstChar { it.uppercase() }, style = T.Body, modifier = Modifier.weight(1f))
                Text("+${Feats.REWARD} 🍿", style = T.Small.copy(color = if (got) C.gold else C.faint))
            }
        }
    }
}
