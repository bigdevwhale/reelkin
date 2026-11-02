package app.reelkin.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.reelkin.container
import app.reelkin.data.Species
import app.reelkin.data.Trait
import app.reelkin.ui.LocalRu
import app.reelkin.ui.art.Art
import app.reelkin.ui.art.BoothLook
import app.reelkin.ui.art.BoothScene
import app.reelkin.ui.art.PixelCanvas
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.Kind
import app.reelkin.ui.components.Says
import app.reelkin.ui.components.t
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.R8
import app.reelkin.ui.theme.T

/** Three pages: the booth and the can, how films become the hall, and what the pet takes from them. */
@Composable
fun Onboarding() {
    val c = LocalContext.current.container
    val ru = LocalRu.current
    var page by rememberSaveable { mutableIntStateOf(0) }
    val scene = remember { BoothScene() }
    val look = when (page) {
        0 -> BoothLook(null, false, 1, emptySet(), null, emptyList(), garland = true, chair = false, neon = false, popcorn = false, night = false, seance = null, ru = ru)
        1 -> BoothLook(Species.FOX, false, 1, emptySet(), null, emptyList(), garland = true, chair = false, neon = false, popcorn = false, night = false, seance = app.reelkin.data.Genre.ANIMATION, ru = ru)
        else -> BoothLook(Species.CAT, false, 2, setOf(Trait.HAT, Trait.NOSE), null, emptyList(), garland = true, chair = true, neon = true, popcorn = false, night = false, seance = null, ru = ru)
    }
    Column(Modifier.fillMaxSize().background(C.bg)) {
        Text(t("Бобина", "Reelkin"), style = T.Display.copy(color = C.gold), modifier = Modifier.statusBarsPadding().padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().clip(R8.Lg).border(1.dp, C.line, R8.Lg)) {
                PixelCanvas(BoothScene.W, BoothScene.H, Modifier.fillMaxWidth()) { b, tick -> scene.draw(b, look, tick * Art.FRAME_MS / 1000.0, Art.FRAME_MS / 1000.0) }
            }
            when (page) {
                0 -> {
                    Says(t("Это будка киномеханика. А в коробке с плёнкой кто-то спит.", "This is a projectionist's booth. And something is asleep in the film can."), t("будка", "the booth"))
                    Text(t("Отмечай фильмы и сериалы, которые смотришь. После третьего фильма коробка откроется, и кто из неё вылезет, решат жанры: нуар, хоррор, мультфильмы, комедии…",
                        "Log the films and series you watch. After the third film the can opens, and the genres decide who climbs out: noir, horror, animation, comedy…"), style = T.Body)
                }
                1 -> {
                    Says(t("Ставь фильм на проектор — я буду смотреть с тобой из окошка.", "Put a film on the projector and I'll watch it with you from the port."), t("питомец", "the pet"))
                    Text(t("Каждая минута — 24 кадра в секунду для питомца. Каждый фильм — корешок билета и зритель в зале на сто мест. Первая буква названия идёт на вывеску, страна — в кругосветку, десятилетие — в машину времени.",
                        "Each minute is 24 frames a second for the pet. Each film is a ticket stub and a spectator in a hundred-seat hall. Its first letter goes on the sign, its country on the map, its decade into the time machine."), style = T.Body)
                }
                else -> {
                    Says(t("Третий нуар — и у меня шляпа. Третья комедия — красный нос.", "A third noir and I get a hat. A third comedy, a red nose."), t("питомец", "the pet"))
                    Text(t("Сериалы идут по абонементу: серия — корешок, финал сезона — салют. Брошенный сериал можно снять с проката без угрызений. Никто не голодает и не обижается: смотри, когда хочется.",
                        "Series run on a season ticket: an episode tears a ticket, a finale sets off fireworks. A dropped series can be taken off with no guilt. Nobody starves or sulks: watch when you feel like it."), style = T.Body)
                }
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (page > 0) Btn(t("Назад", "Back"), { page-- }, kind = Kind.GHOST)
            Box(Modifier.weight(1f))
            Btn(if (page == 2) t("Открыть кинотеатр", "Open the cinema") else t("Дальше", "Next"), {
                if (page < 2) page++ else c.write { settings.update { it.copy(onboarded = true) } }
            }, kind = if (page == 2) Kind.VELVET else Kind.GOLD)
        }
    }
}
