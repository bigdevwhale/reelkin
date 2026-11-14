package app.reelkin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.reelkin.R
import app.reelkin.container
import app.reelkin.data.Settings
import app.reelkin.ui.LocalRu
import app.reelkin.ui.goals.Glyph
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T

/** The first-visit hints, one per screen. */
enum class Tip(val icon: Int) {
    BOOTH(R.drawable.dock_booth), BILL(R.drawable.dock_bill), SAGA(R.drawable.dock_saga), PETS(R.drawable.act_can), CAN(R.drawable.act_can),
    PET(R.drawable.sec_star), SHOP(R.drawable.act_buffet), MENU(R.drawable.act_seance), GOALS(R.drawable.sec_target), FILM(R.drawable.act_seance),
    STUB(R.drawable.dock_bill), LOG(R.drawable.act_log), PICK(R.drawable.act_pick), CARDS(R.drawable.sec_star), SETTINGS(R.drawable.sec_medal),
}

private fun text(tip: Tip, ru: Boolean): Pair<String, List<String>> = if (ru) when (tip) {
    Tip.BOOTH -> "Это будка киномеханика" to listOf("Здесь живёт питомец. Каждая минута кино, которое ты отмечаешь, растит его.",
        "«Сегодня вечером» — что питомец предлагает посмотреть из афиши. «На проектор» начинает сеанс: питомец смотрит вместе с тобой.",
        "Круглая кнопка внизу — всё остальное: отметить фильм, серию, добавить в афишу. Попкорн сверху — это буфет.")
    Tip.BILL -> "Это афиша" to listOf("«Ждут» — что собираешься посмотреть, «Идут» — сериалы, «Корешки» — всё, что уже посмотрел.",
        "Лупа ищет и в афише, и в каталоге фильмов: по названию, режиссёру или актёру.", "«Фильтр» сужает по жанру, попкорну, году, компании и людям.")
    Tip.SAGA -> "Это сага" to listOf("Твоё кино в цифрах и картинках. Сверху выбираешь период и что считать: всё, фильмы или сериалы.",
        "Каждая карточка открывается — там полный список или график.", "Ниже — цели, календарь года, зал со зрителями и премии.")
    Tip.PETS -> "Это питомцы" to listOf("«Мои» — кого ты уже вырастил. Есть только тот, кто сейчас в будке; остальные отдыхают, их можно позвать.",
        "«Коробки» — все 49 питомцев. Каждая коробка открывается тем, как ты смотришь кино: хорроры, старые фильмы, кино после полуночи…",
        "Нажми на коробку — увидишь, кто в ней, что он любит и сколько осталось до открытия.")
    Tip.CAN -> "Коробка с плёнкой" to listOf("Вверху — четыре стадии, в которые вырастет питомец. Пока коробка закрыта, они в тени.",
        "«Как открыть» — условие и сколько осталось. Открытую коробку можно забрать в будку.")
    Tip.PET -> "Карточка питомца" to listOf("Здесь его имя — его можно поменять, — стадии роста, любимое, характер и форма.",
        "Любимое кино кормит в полтора раза сытнее и приносит попкорн. Взрослый навсегда принимает форму по тому, чем его кормили.")
    Tip.SHOP -> "Это буфет" to listOf("Попкорн приходит за фильмы, серии, задания недели и цели. Здесь он тратится на вещи и обои для будки.",
        "Убранная вещь возвращает попкорн целиком, так что пробовать можно смело.")
    Tip.MENU -> "Что делаем?" to listOf("Отсюда — всё главное: отметить фильм, начать сеанс, спросить питомца, что посмотреть, добавить в афишу.",
        "Внизу — сериалы, которые идут: «+1» отмечает следующую серию.")
    Tip.GOALS -> "Каталог целей" to listOf("Отметь сколько угодно целей — от «4 фильма за месяц» до «кино 7 дней подряд» — и добавь разом.",
        "Нажатием на цель потом меняются число и срок. Разовая цель приносит 15 попкорна.")
    Tip.FILM -> "Карточка фильма" to listOf("«На проектор» начинает сеанс, «Уже посмотрел» сразу отрывает корешок.",
        "Режиссёр и актёры находятся сами. Нажми на имя — увидишь всё с этим человеком в афише и корешках.")
    Tip.STUB -> "Корешок" to listOf("Здесь можно поменять попкорн, дописать три слова и поправить день просмотра.",
        "«Пересмотреть» ставит фильм на проектор ещё раз.")
    Tip.LOG -> "Оторви корешок" to listOf("Попкорн от одного до пяти — твоя оценка. Три слова — чтобы вспомнить фильм через год.",
        "С кем смотрел и была ли сцена после титров — пригодится для целей, коробок и статистики.")
    Tip.PICK -> "Что ставим?" to listOf("Скажи, сколько есть времени и какое настроение, — питомец выберет из афиши.",
        "Он предпочитает десятилетия и страны, которых у тебя ещё нет.")
    Tip.CARDS -> "Открытки" to listOf("Будка, питомец, итоги, жанры, год квадратиками — картинкой для ленты или сторис.",
        "Листай в сторону, выбери формат и период, сохрани или отправь.")
    Tip.SETTINGS -> "Настройки" to listOf("Здесь все питомцы, перенос истории из MyShows и Кинопоиска, поиск режиссёров и актёров для всей коллекции.",
        "Во «Виде» — фон за экранами.")
} else when (tip) {
    Tip.BOOTH -> "This is the projection booth" to listOf("Your pet lives here. Every minute of film you log helps it grow.",
        "«Tonight» is the pet's pick from the bill. «Put it on» starts a show and the pet watches with you.",
        "The round button below has the rest: log a film, an episode, add to the bill. The popcorn on top is the buffet.")
    Tip.BILL -> "This is the bill" to listOf("«Waiting» is what you mean to watch, «Running» your series, «Stubs» everything watched.",
        "The glass searches the bill and the film catalogue: by title, director or actor.", "«Filter» narrows it by genre, popcorn, year, company and people.")
    Tip.SAGA -> "This is the saga" to listOf("Your films in numbers and pictures. Pick the span and what to count on top.",
        "Each card opens into the whole list or chart.", "Below: goals, the calendar, the hall and the awards.")
    Tip.PETS -> "These are the pets" to listOf("«Mine» are the ones you've raised. Only the one in the booth eats; call the others back any time.",
        "«Cans» are all 49 pets. Each can opens by the way you watch: horror, old films, films after midnight…",
        "Tap a can to see who's in it, what it loves and how near it is to opening.")
    Tip.CAN -> "A film can" to listOf("On top, the four stages the pet grows through, in shadow while the can is shut.",
        "«How it opens» says what it takes and how much is left. An open can goes to the booth.")
    Tip.PET -> "The pet's card" to listOf("Its name (change it if you like), its stages, favourites, nature and form.",
        "Favourite films feed it one and a half times as much and pay popcorn. Grown up, it takes a form for good from what it ate.")
    Tip.SHOP -> "This is the buffet" to listOf("Popcorn comes for films, episodes, weekly tasks and goals. Here it buys things and wallpaper for the booth.",
        "Taking a thing away pays all its popcorn back, so try anything.")
    Tip.MENU -> "What now?" to listOf("Everything that matters: log a film, start a show, ask the pet what to watch, add to the bill.",
        "Below are your running series: «+1» logs the next episode.")
    Tip.GOALS -> "Goals to pick" to listOf("Tick as many as you like, from «4 films a month» to «films 7 days in a row», and add them at once.",
        "Tap a goal later to change its number and span. A one-off goal pays 15 popcorn.")
    Tip.FILM -> "A film's card" to listOf("«Put it on» starts a show; «Watched it» tears a stub at once.",
        "The director and the cast are found by themselves. Tap a name to see everything with them.")
    Tip.STUB -> "A stub" to listOf("Change the popcorn, add three words, fix the day you watched it.", "«Watch again» puts it on the projector once more.")
    Tip.LOG -> "Tear the stub" to listOf("One to five popcorn is your mark. Three words help you remember it a year from now.",
        "Who you watched with and a post-credits scene count for goals, cans and the stats.")
    Tip.PICK -> "What's on?" to listOf("Say how much time you have and the mood; the pet picks from the bill.", "It leans to decades and countries you don't have yet.")
    Tip.CARDS -> "Postcards" to listOf("The booth, the pet, the numbers, genres, the year in squares, as a picture for a feed or a story.",
        "Swipe, pick the shape and the span, save or share.")
    Tip.SETTINGS -> "Settings" to listOf("All the pets, history from MyShows and Kinopoisk, and finding directors and actors for the whole collection.",
        "«Look» has the background behind the screens.")
}

/** A note the first time a screen is opened; «Понятно» puts it away for good. */
@Composable
fun FirstVisit(tip: Tip, s: Settings, modifier: Modifier = Modifier) {
    if (tip.name in s.tips) return
    val ru = LocalRu.current
    val co = LocalContext.current.container
    var shown by remember { mutableStateOf(true) }
    val (title, lines) = text(tip, ru)
    AnimatedVisibility(shown, exit = shrinkVertically() + fadeOut()) {
        Column(modifier.fillMaxWidth().clip(R8.Lg).background(C.goldSoft).border(1.dp, C.gold.copy(alpha = .35f), R8.Lg).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Glyph(tip.icon, C.gold, 18, back = C.surface, box = 34)
                Text(title, style = T.Name, modifier = Modifier.weight(1f))
            }
            for (l in lines) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•", style = T.Body.copy(color = C.gold))
                Text(l, style = T.Body.copy(color = C.ink))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(t("Понятно", "Got it"), style = T.Button.copy(color = C.onGold), modifier = Modifier.clip(CircleShape).background(C.gold)
                    .clickable { shown = false; co.write { settings.update { it.copy(tips = it.tips + tip.name) } } }.padding(horizontal = 18.dp, vertical = 8.dp))
            }
        }
    }
}
