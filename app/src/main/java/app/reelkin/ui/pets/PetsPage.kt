package app.reelkin.ui.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelkin.container
import app.reelkin.data.Species
import app.reelkin.data.Stage
import app.reelkin.domain.Cinema
import app.reelkin.domain.Hint
import app.reelkin.domain.PetState
import app.reelkin.domain.Pets
import app.reelkin.domain.Words
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.Tab
import app.reelkin.ui.art.PetArt
import app.reelkin.ui.components.Bar
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.Card
import app.reelkin.ui.components.BufImage
import app.reelkin.ui.components.Page
import app.reelkin.ui.components.Sheet
import app.reelkin.ui.components.Tabs
import app.reelkin.ui.components.t
import app.reelkin.ui.sheets.PetCard
import app.reelkin.ui.sheets.PetBlock
import app.reelkin.ui.sheets.Perk
import app.reelkin.ui.sheets.lovesWhy
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.T

/** Every pet: the ones raised, and the cans, each with what opens it and what its pet loves. */
@Composable
fun PetsPage(c: Cinema, nav: Nav) = Page(t("Питомцы", "Pets"), nav::close) { PetsBody(c, nav) }

/** The pets as a tab of their own. */
@Composable
fun PetsScreen(c: Cinema, nav: Nav) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PetsBody(c, nav)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PetsBody(c: Cinema, nav: Nav) {
    val ru = LocalRu.current
    var page by rememberSaveable { mutableStateOf(if (c.pets.isEmpty()) 1 else 0) }
    val open = c.hints.count { it.open }
    run {
        app.reelkin.ui.components.FirstVisit(app.reelkin.ui.components.Tip.PETS, c.s)
        app.reelkin.ui.components.Pills(listOf(t("Мои", "Mine"), t("Коробки", "Cans")), page, { page = it }, counts = listOf(c.pets.size, open),
            icons = listOf(app.reelkin.R.drawable.dock_booth, app.reelkin.R.drawable.act_can))
        if (page == 0) {
            if (c.pets.isEmpty()) Text(t("Пока никого. Выбери коробку во вкладке «Коробки»: в каждой кто-то спит.", "No one yet. Pick a can under «Cans»: someone sleeps in each."), style = T.Muted)
            val sorted = c.pets.sortedWith(compareByDescending<PetState> { it.pet.id == c.pet?.pet?.id }.thenByDescending { it.fed })
            for (row in sorted.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (p in row) PetTile(c, p, Modifier.weight(1f)) { nav.go(app.reelkin.ui.Overlay.PetOf(p.pet.id)) }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        } else {
            Text(if (ru) "Открыто ${open} из ${c.hints.size}. Коробки открываются тем, как ты смотришь кино; в каждой свой питомец со своими вкусами."
                else "$open of ${c.hints.size} open. Cans open by the way you watch; each holds a pet with tastes of its own.", style = T.Muted)
            val order = c.hints.sortedWith(compareByDescending<Hint> { it.open }.thenByDescending { it.have.toFloat() / it.need })
            for (row in order.chunked(3)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (h in row) CanTile(c, h, Modifier.weight(1f)) { nav.go(app.reelkin.ui.Overlay.Can(h.species)) }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PetTile(c: Cinema, p: PetState, modifier: Modifier, onClick: () -> Unit) {
    val ru = LocalRu.current
    val here = p.pet.id == c.pet?.pet?.id
    val shape = RoundedCornerShape(18.dp)
    Column(modifier.clip(shape).background(C.surface).then(if (here) Modifier.border(2.dp, C.gold, shape) else Modifier.border(1.dp, C.line, shape))
        .clickable(onClick = onClick).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.surface2), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().height(124.dp), contentAlignment = Alignment.Center) { app.reelkin.ui.components.FitBuf(PetArt.portrait(p.species, if (p.pet.hatched) p.stage else Stage.EGG, p.pet.shiny, p.form), Modifier.fillMaxSize().padding(8.dp)) }
            if (here) Text(t("в будке", "in the booth"), style = T.Small.copy(color = C.onGold),
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(CircleShape).background(C.gold).padding(horizontal = 8.dp, vertical = 2.dp))
            if (p.pet.shiny) Text("✦", style = T.Strong.copy(color = C.gold), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        Text(if (p.pet.hatched) c.name(p, ru) else t("Коробка", "A can"), style = T.Name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(Words.species(p.species, ru) + " · " + Words.stage(if (p.pet.hatched) p.stage else Stage.EGG, ru), style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Bar(if (p.pet.hatched) p.progress else p.fed / Stage.BABY.from.toFloat(), Modifier.fillMaxWidth())
    }
}

@Composable
private fun CanTile(c: Cinema, h: Hint, modifier: Modifier, onClick: () -> Unit) {
    val ru = LocalRu.current
    val have = c.pets.any { it.species == h.species }
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).background(C.surface).border(1.dp, if (h.open && !have) C.gold else C.line, shape).clickable(onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.fillMaxWidth().height(62.dp), contentAlignment = Alignment.Center) { app.reelkin.ui.components.FitBuf(PetArt.portrait(h.species, Stage.EGG, dim = !h.open), Modifier.fillMaxSize().padding(2.dp)) }
        Text(Words.species(h.species, ru), style = T.Small.copy(color = C.ink), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (h.open) Text(if (have) t("уже есть", "you have one") else t("можно открыть", "ready"), style = T.Small.copy(color = if (have) C.muted else C.gold))
        else Bar(h.have / h.need.toFloat(), Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp))
    }
}

/**
 * One can, opened up: who sleeps in it and how it grows (all four stages, in shadow until the can opens), what it loves and
 * what that gives, what opens it and how near that is, and a way to take it.
 */
@Composable
fun CanSheet(c: Cinema, sp: Species, nav: Nav) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val h = c.hints.first { it.species == sp }
    val have = c.pets.any { it.species == sp }
    val known = h.open || have
    Sheet(nav::close) {
        app.reelkin.ui.components.FirstVisit(app.reelkin.ui.components.Tip.CAN, c.s)
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.surface2).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) { app.reelkin.ui.components.FitBuf(PetArt.portrait(sp, if (known) Stage.ADULT else Stage.EGG), Modifier.fillMaxSize().padding(10.dp)) }
        }
        Text(Words.species(sp, ru).replaceFirstChar { it.uppercase() }, style = T.Title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(when {
            have -> t("уже живёт у тебя", "already yours")
            h.open -> t("коробка открыта — можно забрать", "the can is open: it's yours to take")
            else -> t("коробка закрыта", "the can is closed")
        }, style = T.Small.copy(color = if (h.open && !have) C.gold else C.muted), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

        // How it grows, stage by stage.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (st in listOf(Stage.BABY, Stage.YOUNG, Stage.ADULT, Stage.ANCIENT)) Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(C.surface2).padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(60.dp), contentAlignment = Alignment.Center) { BufImage(PetArt.portrait(sp, st, dim = !known), scale = .9.dp) }
                Text(Words.stage(st, ru), style = T.Small, maxLines = 1)
                Text(app.reelkin.ui.bill.dur(st.from.toLong(), ru), style = T.Small.copy(color = C.faint, fontSize = T.Small.fontSize * .85f), maxLines = 1)
            }
        }

        // The same blocks as on a pet's own card.
        PetBlock(app.reelkin.R.drawable.act_can, t("Как открыть", "How it opens")) {
            Text(Words.opens(h, ru).replaceFirstChar { it.uppercase() }, style = T.Body)
            if (!h.open) {
                Bar(h.have / h.need.toFloat(), Modifier.fillMaxWidth())
                Text(if (ru) "${minOf(h.have, h.need)} из ${h.need} · осталось ${h.need - h.have}" else "${minOf(h.have, h.need)} of ${h.need} · ${h.need - h.have} to go", style = T.Small)
            } else Text(t("Готово ✓", "Done ✓"), style = T.Small.copy(color = C.gold))
        }
        PetBlock(app.reelkin.R.drawable.sec_star, t("Любимое кино", "Favourite films")) {
            Text(Words.loves(sp, ru).replaceFirstChar { it.uppercase() }, style = T.Name)
            val genres = app.reelkin.domain.Pets.genres(sp)
            if (genres.isNotEmpty()) app.reelkin.ui.components.Flow { for (g in genres) app.reelkin.ui.components.Tag(Words.genre(g, ru), C.genre(g)) }
            Text(lovesWhy(sp, ru), style = T.Small)
        }
        PetBlock(app.reelkin.R.drawable.sec_popcorn, t("Что это даёт", "What it gives")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Perk(app.reelkin.R.drawable.sec_chart, "×1,5", t("растёт быстрее", "grows faster"), Modifier.weight(1f))
                Perk(app.reelkin.R.drawable.act_seance, "+5 🍿", t("за фильм", "a film"), Modifier.weight(1f))
                Perk(app.reelkin.R.drawable.act_can, "+1 🍿", t("за серию", "an episode"), Modifier.weight(1f))
            }
        }
        PetBlock(app.reelkin.R.drawable.sec_medal, t("Характер", "Nature")) {
            Text(t("Выпадает при открытии", "Set when the can opens"), style = T.Name)
            Text(t("Спокойный, соня, пугливый, любопытный (+5 🍿 за новую страну и десятилетие), застенчивый, болтун или обжора (растёт на десятую часть быстрее). Иногда — в техниколоре, один на шестнадцать.",
                "Calm, sleepy, jumpy, curious (+5 🍿 for each new country and decade), shy, chatty or greedy (grows a tenth faster). Now and then in technicolor, one in sixteen."), style = T.Body)
        }
        PetBlock(app.reelkin.R.drawable.sec_palette, t("Форма", "Form")) {
            Text(t("Выберется, когда вырастет", "Set when it grows up"), style = T.Name)
            Text(t("Навсегда — по фильмам, которыми его кормили: Тень, Звезда, Сердце, Смех или Пыль дорог.",
                "For good, by the films it was fed: Shadow, Star, Heart, Laugh or Road dust."), style = T.Body)
        }
        if (h.open && !have) {
            Text(t("Питомец пойдёт в будку, а тот, кто там сейчас, отдохнёт — его можно позвать обратно когда угодно. Ест только тот, кто в будке.",
                "The pet goes into the booth and whoever is there now rests; call them back any time. Only the one in the booth eats."), style = T.Small)
            Btn(t("Открыть коробку", "Open the can"), { co.write { office.takePet(sp) }; nav.close(); nav.tab(Tab.BOOTH) }, Modifier.fillMaxWidth())
        }
    }
}
