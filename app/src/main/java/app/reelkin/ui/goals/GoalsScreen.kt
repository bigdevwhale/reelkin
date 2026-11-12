package app.reelkin.ui.goals

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import app.reelkin.R
import app.reelkin.container
import app.reelkin.data.Genre
import app.reelkin.data.Goal
import app.reelkin.data.GoalKind
import app.reelkin.data.Period
import app.reelkin.domain.Cinema
import app.reelkin.domain.Goals
import app.reelkin.domain.Quests
import app.reelkin.domain.Words
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.Overlay
import app.reelkin.ui.components.Bar
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.Chip
import app.reelkin.ui.components.Flow
import app.reelkin.ui.components.Kind
import app.reelkin.ui.components.Section
import app.reelkin.ui.components.Sheet
import app.reelkin.ui.components.Stepper
import app.reelkin.ui.components.t
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T

/** The little glyph a goal of this kind goes with. */
fun goalIcon(k: GoalKind): Int = when (k) {
    GoalKind.FILMS -> R.drawable.act_seance
    GoalKind.HOURS, GoalKind.LONG -> R.drawable.sec_clock
    GoalKind.EPISODES -> R.drawable.dock_bill
    GoalKind.SEASONS -> R.drawable.act_can
    GoalKind.GENRE -> R.drawable.sec_palette
    GoalKind.NEW_COUNTRIES -> R.drawable.sec_globe
    GoalKind.NEW_DECADES, GoalKind.OLD -> R.drawable.sec_hourglass
    GoalKind.DAYS -> R.drawable.sec_calendar
    GoalKind.FIVE_STARS -> R.drawable.sec_star
    GoalKind.STREAK -> R.drawable.sec_medal
    GoalKind.WORDS -> R.drawable.act_log
    GoalKind.COMPANY -> R.drawable.sec_seats
    GoalKind.REWATCHES -> R.drawable.act_pick
    GoalKind.SCENES -> R.drawable.sec_trophy
    GoalKind.RATED -> R.drawable.sec_popcorn
}

@Composable
fun Glyph(icon: Int, tint: Color = C.muted, size: Int = 20, back: Color = C.surface2, box: Int = 36) {
    Box(Modifier.size(box.dp).clip(CircleShape).background(back), contentAlignment = Alignment.Center) {
        Image(ImageBitmap.imageResource(icon), null, Modifier.size(size.dp), filterQuality = FilterQuality.Low, colorFilter = ColorFilter.tint(tint))
    }
}

/** The goals, as a block of the saga: each with its bar, and a way to add more from the catalogue. */
@Composable
fun GoalsBlock(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    val goals = c.goals.filter { !it.archived }
    if (goals.isEmpty()) Text(t("Целей пока нет. Выбери из каталога — сколько хочешь: фильмы за месяц, новые страны, кино без пропусков…",
        "No goals yet. Pick as many as you like from the catalogue: films a month, new countries, films without a gap…"), style = T.Muted)
    for (g in goals.sortedBy { Goals.progress(c, it).done }) {
        val p = Goals.progress(c, g)
        Row(Modifier.fillMaxWidth().clip(R8.Md).clickable { nav.go(Overlay.GoalEdit(g.id)) }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Glyph(goalIcon(g.kind), if (p.done) C.gold else C.muted)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Goals.title(g, ru).replaceFirstChar { it.uppercase() }, style = T.Strong, modifier = Modifier.weight(1f), maxLines = 2)
                    Text(if (p.done) t("готово", "done") else "${p.have}/${p.need}", style = T.Small.copy(color = if (p.done) C.gold else C.muted))
                }
                Bar(p.part, Modifier.fillMaxWidth())
            }
        }
    }
    Btn(t("+ Выбрать цели", "+ Pick goals"), { nav.go(Overlay.GoalEdit(-1)) }, Modifier.fillMaxWidth().padding(top = 4.dp), kind = Kind.SOFT, small = true)
    val archived = c.goals.count { it.archived }
    if (archived > 0) Text(t("В архиве: $archived", "Archived: $archived"), style = T.Small)
}

/** The catalogue: every kind of goal in groups, tick as many as you like and add them at once. */
@Composable
fun GoalCatalogue(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var picked by remember { mutableStateOf(setOf<Int>()) }
    val all = remember { Goals.catalogue().flatMap { (n, l) -> l.map { n to it } } }
    Sheet(nav::close) {
        Text(t("Каталог целей", "Goals to pick"), style = T.Title)
        app.reelkin.ui.components.FirstVisit(app.reelkin.ui.components.Tip.GOALS, c.s)
        Text(t("Отметь сколько угодно. Цифру и срок потом можно поменять, нажав на цель.", "Tick as many as you like. Tap a goal later to change its number and span."), style = T.Muted)
        var group = -1
        all.forEachIndexed { i, (n, g) ->
            if (n != group) { group = n; Section(Goals.group(n, ru)) }
            val have = c.goals.any { !it.archived && Goals.same(it, g) }
            val on = i in picked
            Row(Modifier.fillMaxWidth().clip(R8.Md).background(if (on) C.goldSoft else C.surface2).clickable(enabled = !have) { picked = if (on) picked - i else picked + i }
                .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Glyph(goalIcon(g.kind), if (on) C.gold else C.muted, back = if (on) C.surface else C.surface)
                Text(Goals.title(g, ru).replaceFirstChar { it.uppercase() }, style = T.Body.copy(color = if (have) C.faint else C.ink), modifier = Modifier.weight(1f))
                Text(if (have) t("уже есть", "set") else if (on) "✓" else "", style = T.Strong.copy(color = C.gold))
            }
        }
        Btn(if (picked.isEmpty()) t("Отметь цели", "Tick some goals") else if (ru) "Добавить · ${picked.size}" else "Add · ${picked.size}", {
            val now = System.currentTimeMillis()
            co.write { for (i in picked.sorted()) db.dao().insert(all[i].second.copy(created = now)) }
            nav.toast(if (ru) "Целей добавлено: ${picked.size}" else "${picked.size} goals added")
            nav.close()
        }, Modifier.fillMaxWidth(), enabled = picked.isNotEmpty())
    }
}

/** The week's three tasks, each with popcorn to take once it's done. */
@Composable
fun QuestList(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    for (q in c.quests) {
        val id = "${c.weekId}:${q.q.name}"
        val claimed = id in c.s.claimed
        Row(Modifier.fillMaxWidth().clip(R8.Md).background(C.surface).border(1.dp, if (q.p.done && !claimed) C.gold else C.line, R8.Md).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(Quests.name(q.q, ru), style = T.Strong.copy(color = if (claimed) C.faint else C.ink))
                Bar(q.p.part, Modifier.fillMaxWidth().padding(top = 6.dp), color = if (claimed) C.line else C.gold)
                Text("${q.p.have.coerceAtMost(q.p.need)} / ${q.p.need}", style = T.Small, modifier = Modifier.padding(top = 2.dp))
            }
            when {
                claimed -> Text(t("получено", "taken"), style = T.Small)
                q.p.done -> Btn("+${q.q.reward}", {
                    co.write { settings.update { it.copy(claimed = it.claimed + id) } }
                    nav.toast(if (ru) "+${q.q.reward} попкорна" else "+${q.q.reward} popcorn")
                }, kind = Kind.GOLD, small = true)
                else -> Text("+${q.q.reward}", style = T.Small.copy(color = C.gold))
            }
        }
    }
}

@Composable
fun QuestsSheet(c: Cinema, nav: Nav) {
    Sheet(nav::close) {
        Text(t("Задания недели", "This week's tasks"), style = T.Title)
        Text(t("Новые — каждый понедельник. За каждое — попкорн для буфета.", "New ones every Monday. Each pays popcorn for the buffet."), style = T.Muted)
        QuestList(c, nav)
    }
}

/** One goal: its number, span and genre, or out of the way. [id] -1 opens the catalogue instead. */
@Composable
fun GoalSheet(c: Cinema, id: Long?, nav: Nav) {
    if (id == null || id < 0) { GoalCatalogue(c, nav); return }
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val old = c.goals.firstOrNull { it.id == id } ?: run { nav.close(); return }
    var period by remember { mutableStateOf(old.period) }
    var target by remember { mutableIntStateOf(old.target) }
    var genre by remember { mutableStateOf(Genre.entries.firstOrNull { it.name == old.ref } ?: Genre.HORROR) }
    Sheet(nav::close) {
        val draft = old.copy(target = target, period = period, ref = if (old.kind == GoalKind.GENRE) genre.name else old.ref)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Glyph(goalIcon(old.kind), C.gold, 24, box = 44)
            Text(Goals.title(draft, ru).replaceFirstChar { it.uppercase() }, style = T.Name, modifier = Modifier.weight(1f))
        }
        val p = Goals.progress(c, draft)
        Bar(p.part, Modifier.fillMaxWidth())
        Text("${p.have} / ${p.need}", style = T.Small)
        Section(t("Сколько", "How many"))
        Stepper("$target") { d -> target = (target + d).coerceIn(1, 999) }
        if (old.kind != GoalKind.STREAK) {
            Section(t("За какой срок", "Over what span"))
            Flow { for (pp in Period.entries) Chip(Goals.period(pp, ru), pp == period, { period = pp }) }
        }
        if (old.kind == GoalKind.GENRE) {
            Section(t("Жанр", "Genre"))
            Flow { for (g in Genre.entries) Chip(Words.genre(g, ru), g == genre, { genre = g }, color = C.genre(g)) }
        }
        Btn(t("Сохранить", "Save"), { co.write { db.dao().update(draft) }; nav.close() }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(if (old.archived) t("Вернуть", "Restore") else t("В архив", "Archive"), { co.write { db.dao().update(old.copy(archived = !old.archived)) }; nav.close() }, Modifier.weight(1f), kind = Kind.SOFT)
            Btn(t("Удалить", "Delete"), { co.write { db.dao().delete(old) }; nav.close() }, Modifier.weight(1f), kind = Kind.GHOST)
        }
    }
}
