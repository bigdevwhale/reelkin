package app.reelkin.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    Page(t("Настройки", "Settings"), { if (name.trim() != c.s.petName) co.write { settings.update { it.copy(petName = name.trim()) } }; nav.close() }) {
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

        Section(t("Цель на год", "A goal for the year"))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Btn("−", { co.write { settings.update { it.copy(goal = (it.goal - 4).coerceAtLeast(4)) } } }, kind = Kind.SOFT, small = true)
            Text(Words.films(c.s.goal, ru), style = T.Strong, modifier = Modifier.weight(1f))
            Btn("+", { co.write { settings.update { it.copy(goal = (it.goal + 4).coerceAtMost(400)) } } }, kind = Kind.SOFT, small = true)
        }

        Section(t("Язык", "Language"))
        val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
        Flow {
            for ((tag, l) in listOf("" to t("как в телефоне", "as the phone"), "ru" to "Русский", "en" to "English"))
                Chip(l, cur == tag, { AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)) })
        }

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
        Text(if (ru) "Бобина ${BuildConfig.VERSION_NAME}. Всё хранится на телефоне. В интернет — только поиск по Википедии, когда ты сам ищешь фильм."
            else "Reelkin ${BuildConfig.VERSION_NAME}. Everything stays on the phone. The internet is only for the Wikipedia search, when you look a film up.", style = T.Muted)
        Btn(t("Исходный код", "Source code"), { open("https://github.com/bigdevwhale/reelkin") }, kind = Kind.GHOST, small = true)
    }
}
