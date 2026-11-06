package app.reelkin.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import app.reelkin.ui.BackdropPicker
import app.reelkin.ui.Backdrops
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import app.reelkin.ui.components.Tabs
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.reelkin.BuildConfig
import app.reelkin.container
import app.reelkin.domain.Cinema
import app.reelkin.domain.Words
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.Card
import app.reelkin.ui.components.Chip
import app.reelkin.ui.components.Field
import app.reelkin.ui.components.Flow
import app.reelkin.ui.components.Kind
import app.reelkin.ui.components.Page
import app.reelkin.ui.components.Section
import app.reelkin.ui.components.t
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.T

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "reelkin"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

@Composable
fun SettingsPage(c: Cinema, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val co = ctx.container
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    var name by remember { mutableStateOf(c.s.petName) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pickOwn = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) co.write {
            if (Backdrops.importPicked(ctx, uri)) settings.update { it.copy(backdrop = Backdrops.OWN, backdropStamp = System.currentTimeMillis()) }
        }
    }
    Page(t("Настройки", "Settings"), { if (name.trim() != c.s.petName) co.write { settings.update { it.copy(petName = name.trim()) } }; nav.close() }) {
        Tabs(listOf(t("Кинотеатр", "Cinema"), t("Вид", "Look"), t("Ещё", "More")), page, { page = it })
        if (page == 0) {
            Section(t("Питомец", "The pet"))
            Field(name, { name = it.take(16) }, Words.defaultName(ru))
            Text(t("Пустое поле — имя по умолчанию.", "Leave it empty for the default name."), style = T.Small)
            if (c.traits.isNotEmpty()) {
                Text(t("Носит черты — сними любую, она не потеряется", "Traits worn: take any off, it won't be lost"), style = T.Muted)
                Flow {
                    for (tr in c.traits) {
                        val on = tr !in c.s.off
                        Chip(Words.trait(tr, ru), on, { co.write { settings.update { s -> s.copy(off = if (on) s.off + tr else s.off - tr) } } })
                    }
                }
            }

            Section(t("История", "History"))
            Card(Modifier.fillMaxWidth(), onClick = { nav.go(app.reelkin.ui.Overlay.Import) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("Перенести историю", "Bring your history"), style = T.Strong)
                        Text(t("из MyShows или Кинопоиска", "from MyShows or Kinopoisk"), style = T.Muted)
                    }
                    Text("›", style = T.Title.copy(color = C.velvet))
                }
            }

            Section(t("Цель на год", "A goal for the year"))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn("−", { co.write { settings.update { it.copy(goal = (it.goal - 4).coerceAtLeast(4)) } } }, kind = Kind.SOFT, small = true)
                Text(Words.films(c.s.goal, ru), style = T.Strong, modifier = Modifier.weight(1f))
                Btn("+", { co.write { settings.update { it.copy(goal = (it.goal + 4).coerceAtMost(400)) } } }, kind = Kind.SOFT, small = true)
            }

        }
        if (page == 1) {
            Section(t("Обложки", "Posters"))
            Text(t("Пиксели в тон будке; «как есть» показывает настоящую картинку в афише и корешках. Будка и зал остаются пиксельными.",
                "Pixels match the booth; «as they are» shows the real picture on the bill and the stubs. The booth and the hall stay pixel."), style = T.Muted)
            Flow {
                Chip(t("в пикселях", "in pixels"), c.s.pixelCovers, { co.write { settings.update { it.copy(pixelCovers = true) } } })
                Chip(t("как есть", "as they are"), !c.s.pixelCovers, { co.write { settings.update { it.copy(pixelCovers = false) } } })
            }

            Section(t("Фон", "Background"))
            Text(t("Картинка за всеми экранами, приглушённая, чтобы текст читался. «По сеансу» — жанр фильма на проекторе или сериала, который идёт.",
                "A picture behind every screen, dimmed so the text stays clear. «What's on» follows the genre of the film on the projector or the series running."), style = T.Muted)
            BackdropPicker(c, onPick = { k -> co.write { settings.update { it.copy(backdrop = k) } } },
                onOwn = { pickOwn.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
            if (c.s.backdrop == Backdrops.OWN) Btn(t("Выбрать другую картинку", "Pick another picture"), { pickOwn.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, kind = Kind.GHOST, small = true)

            Section(t("Язык", "Language"))
            val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
            Flow {
                for ((tag, l) in listOf("" to t("как в телефоне", "as the phone"), "ru" to "Русский", "en" to "English"))
                    Chip(l, cur == tag, { AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)) })
            }

        }
        if (page == 2) {
            Btn(t("Как это устроено", "How it works"), { co.write { settings.update { it.copy(onboarded = false) } }; nav.close() }, Modifier.fillMaxWidth(), kind = Kind.SOFT)

            Section("StillCraft")
            Text(t("Бобина — одно из приложений StillCraft. Расскажи, что сломалось или чего не хватает.", "Reelkin is one of the StillCraft apps. Tell us what's broken or what's missing."), style = T.Muted)
            for ((label, sub, url) in listOf(
                Triple(t("Сообщить об ошибке", "Report a bug"), t("откроется бот в Телеграме", "opens the bot in Telegram"), Community.BUG),
                Triple(t("Предложить идею", "Suggest an idea"), t("чего не хватает кинотеатру", "what the cinema is missing"), Community.IDEA),
                Triple(t("Следить за обновлениями", "Follow updates"), t("сообщение, когда выйдет новая версия", "a message when a new version is out"), Community.FOLLOW),
                Triple(t("Чат StillCraft", "StillCraft chat"), t("люди, которые пользуются этими приложениями", "people who use these apps"), Community.CHAT),
            )) Card(Modifier.fillMaxWidth(), onClick = { open(url) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(label, style = T.Strong); Text(sub, style = T.Muted) }
                    Text("›", style = T.Title.copy(color = C.velvet))
                }
            }

            Section(t("О приложении", "About"))
            Text(if (ru) "Бобина ${BuildConfig.VERSION_NAME}. Всё хранится на телефоне. В интернет — только когда ты сам ищешь фильм (Википедия), открываешь сериал (сезоны с TVmaze) или переносишь историю."
                else "Reelkin ${BuildConfig.VERSION_NAME}. Everything stays on the phone. The internet is only for when you look a film up (Wikipedia), open a series (seasons from TVmaze) or bring your history over.", style = T.Muted)
            Btn(t("Исходный код", "Source code"), { open("https://github.com/bigdevwhale/reelkin") }, kind = Kind.GHOST, small = true)
        }
    }
}
