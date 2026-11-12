package app.reelkin.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.reelkin.R
import app.reelkin.ui.art.Art
import app.reelkin.ui.components.t
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.T
import kotlin.math.cos
import kotlin.math.sin

/**
 * The bottom of every screen, as in Songling: four little glyphs (the booth, the bill, the saga, the goals), the chosen
 * one in a soft patch of gold, and in the middle a film reel that turns during a film; tapping it adds a film or a series.
 */
@Composable
fun Dock(tab: Tab, running: Boolean, cans: Int, onTab: (Tab) -> Unit, onReel: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(26.dp)).background(C.surface).border(1.dp, C.line, RoundedCornerShape(26.dp)).padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Slot(Tab.BOOTH, tab, t("Будка", "Booth"), R.drawable.dock_booth, onTab)
        Slot(Tab.BILL, tab, t("Афиша", "Bill"), R.drawable.dock_bill, onTab)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Reel(running, onReel) }
        Slot(Tab.SAGA, tab, t("Сага", "Saga"), R.drawable.dock_saga, onTab)
        Slot(Tab.PETS, tab, t("Питомцы", "Pets"), R.drawable.act_can, onTab, badge = cans)
    }
}

@Composable
private fun RowScope.Slot(me: Tab, tab: Tab, label: String, icon: Int, onTab: (Tab) -> Unit, badge: Int = 0) {
    val on = me == tab
    val patch by animateColorAsState(if (on) C.gold.copy(alpha = .14f) else Color.Transparent, tween(220), label = "patch")
    val ink by animateColorAsState(if (on) C.gold else C.faint, tween(220), label = "ink")
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable(remember { MutableInteractionSource() }, null) { onTab(me) }.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(Modifier.width(52.dp).height(34.dp).clip(RoundedCornerShape(17.dp)).background(patch), contentAlignment = Alignment.Center) {
            Image(ImageBitmap.imageResource(icon), null, Modifier.size(24.dp), filterQuality = FilterQuality.Low, colorFilter = ColorFilter.tint(ink))
            // A can is ready to open.
            if (badge > 0) Box(Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 8.dp).size(8.dp).clip(CircleShape).background(C.velvet))
        }
        Text(label, style = T.Small.copy(color = ink, fontSize = T.Small.fontSize * .95f), maxLines = 1)
    }
}

@Composable
private fun Reel(running: Boolean, onClick: () -> Unit) {
    val spin = rememberInfiniteTransition(label = "spin")
    val turn by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "turn")
    val angle = if (running && !Art.still) turn else 0f
    val label = t("Что делаем", "What now")
    Box(Modifier.size(52.dp).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(52.dp).rotate(angle)) {
            val c = center; val r = size.minDimension / 2
            drawCircle(Color(0xFF3A2E36), r, c)
            drawCircle(Color(0xFF5A4A52), r * .92f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
            for (k in 0 until 6) {
                val a = k * Math.PI / 3
                drawCircle(Color(0xFF221820), r * .17f, Offset(c.x + cos(a).toFloat() * r * .58f, c.y + sin(a).toFloat() * r * .58f))
            }
            drawCircle(C.velvet, r * .3f, c)
        }
        Canvas(Modifier.size(12.dp)) {
            val w = 2.4.dp.toPx()
            drawLine(Color.White, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), w, StrokeCap.Round)
            drawLine(Color.White, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), w, StrokeCap.Round)
        }
    }
}
