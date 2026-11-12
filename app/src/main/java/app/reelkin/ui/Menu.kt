package app.reelkin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelkin.R
import app.reelkin.domain.Cinema
import app.reelkin.ui.booth.episode
import app.reelkin.ui.components.Sheet
import app.reelkin.ui.components.t
import app.reelkin.ui.goals.Glyph
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T

/** What the reel in the middle of the dock opens: everything there is to do, in big rows, with the series a tap from the next episode. */
@Composable
fun MenuSheet(c: Cinema, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    Sheet(nav::close) {
        Text(t("Что делаем?", "What now?"), style = T.Title)
        app.reelkin.ui.components.FirstVisit(app.reelkin.ui.components.Tip.MENU, c.s)
        Item(R.drawable.act_log, t("Посмотрел фильм", "I watched a film"), t("оторвать корешок", "tear a stub")) { nav.go(Overlay.Choose(true)) }
        if (c.s.seance == null) Item(R.drawable.act_seance, t("Начать сеанс", "Start a show"), t("питомец смотрит вместе с тобой", "the pet watches with you")) { nav.go(Overlay.Choose(false)) }
        Item(R.drawable.act_pick, t("Что посмотреть?", "What to watch?"), t("питомец выберет из афиши", "the pet picks from the bill")) { nav.go(Overlay.Pick) }
        Item(R.drawable.dock_bill, t("Добавить в афишу", "Add to the bill"), t("фильм или сериал", "a film or a series")) { nav.go(Overlay.Add()) }
        if (c.running.isNotEmpty()) {
            Text(t("Следующая серия", "Next episode").uppercase(), style = T.Eyebrow, modifier = Modifier.padding(top = 6.dp))
            for (s in c.running) Item(R.drawable.act_can, s.title, if (ru) "сезон ${s.season} · серия ${s.episode + 1}" else "season ${s.season} · episode ${s.episode + 1}", trailing = "+1") {
                episode(s, nav, ctx, ru); nav.close()
            }
        }
    }
}

@Composable
private fun Item(icon: Int, title: String, sub: String, trailing: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface2).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Glyph(icon, C.gold, 22, back = C.surface, box = 42)
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = T.Small, maxLines = 1)
        }
        Text(trailing ?: "›", style = T.Strong.copy(color = if (trailing != null) C.velvet else C.faint))
    }
}
