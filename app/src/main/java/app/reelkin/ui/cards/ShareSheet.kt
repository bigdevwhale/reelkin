package app.reelkin.ui.cards

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reelkin.domain.Cinema
import app.reelkin.domain.Feat
import app.reelkin.domain.Span
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Nav
import app.reelkin.ui.components.Btn
import app.reelkin.ui.components.Chip
import app.reelkin.ui.components.Flow
import app.reelkin.ui.components.Kind
import app.reelkin.ui.components.Sheet
import app.reelkin.ui.components.Tabs
import app.reelkin.ui.components.t
import app.reelkin.ui.theme.C
import app.reelkin.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What can be shared from where it's shown. */
sealed class Share {
    data class Review(val stub: Long) : Share()
    data class Achievement(val feat: Feat) : Share()
    data class Card(val card: Postcard) : Share()
}

/** A round button with the share glyph: three dots joined by two lines. */
@Composable
fun ShareButton(onClick: () -> Unit, size: Dp = 40.dp) {
    val label = t("Поделиться", "Share")
    Box(Modifier.size(size).clip(CircleShape).background(C.surface).border(1.dp, C.line, CircleShape).clickable(onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size * .42f)) {
            val r = this.size.width * .16f; val w = 1.8.dp.toPx()
            val a = Offset(this.size.width - r, r); val b = Offset(r, this.size.height / 2); val c = Offset(this.size.width - r, this.size.height - r)
            drawLine(C.gold, a, b, w); drawLine(C.gold, b, c, w)
            for (o in listOf(a, b, c)) drawCircle(C.gold, r, o)
        }
    }
}

/** A postcard of one thing — a stub, an achievement, the pet, the booth — with what to show on it, then saved or sent. */
@Composable
fun ShareSheet(c: Cinema, what: Share, nav: Nav) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    val scope = rememberCoroutineScope()
    var shape by remember { mutableStateOf(Shape.POST) }
    var o by remember { mutableStateOf(Postcards.Opts()) }
    var busy by remember { mutableStateOf(false) }
    val tk = (what as? Share.Review)?.let { r -> c.seats.firstOrNull { it.id == r.stub } }
    val fs = (what as? Share.Achievement)?.let { a -> c.feats.firstOrNull { it.f == a.feat } }
    fun draw(): Bitmap? = runCatching {
        when (what) {
            is Share.Review -> tk?.let { Postcards.review(ctx, c, it, o, shape, ru) }
            is Share.Achievement -> fs?.let { Postcards.feat(ctx, c, it, o, shape, ru) }
            is Share.Card -> Postcards.draw(ctx, c, what.card, Span.ALL, shape, ru, o.qr)
        }
    }.getOrNull()
    val name = when (what) { is Share.Review -> "review"; is Share.Achievement -> "achievement"; is Share.Card -> what.card.name.lowercase() }
    Sheet(nav::close) {
        Text(when (what) {
            is Share.Review -> t("Поделиться отзывом", "Share a review")
            is Share.Achievement -> t("Поделиться достижением", "Share an achievement")
            is Share.Card -> t("Поделиться открыткой", "Share a postcard")
        }, style = T.Title)
        val bmp by produceState<Bitmap?>(null, shape, o, c) { value = withContext(Dispatchers.Default) { draw() } }
        Box(Modifier.fillMaxWidth().aspectRatio(Postcards.W / shape.h.toFloat()).clip(RoundedCornerShape(16.dp)).background(C.surface2), contentAlignment = Alignment.Center) {
            bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) } ?: Text(t("Рисую…", "Drawing…"), style = T.Muted)
        }
        Tabs(listOf(t("Пост 4:5", "Post 4:5"), t("История 9:16", "Story 9:16")), shape.ordinal, { shape = Shape.entries[it] })
        Text(t("Что показать", "What to show").uppercase(), style = T.Eyebrow)
        Flow {
            if (what is Share.Review) {
                Chip(t("постер", "poster"), o.poster, { o = o.copy(poster = !o.poster) })
                Chip(t("попкорн", "popcorn"), o.popcorn, { o = o.copy(popcorn = !o.popcorn) })
                Chip(t("три слова", "three words"), o.words, { o = o.copy(words = !o.words) })
                Chip(t("дата", "date"), o.date, { o = o.copy(date = !o.date) })
            }
            if (what !is Share.Card) Chip(t("питомец", "the pet"), o.pet, { o = o.copy(pet = !o.pet) })
            Chip(t("QR на Google Play", "QR to Google Play"), o.qr, { o = o.copy(qr = !o.qr) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("Сохранить", "Save"), {
                busy = true
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { draw()?.let { runCatching { save(ctx, it, name) }.getOrDefault(false) } ?: false }
                    busy = false
                    nav.toast(if (ok) (if (ru) "Открытка в галерее, в папке Reelkin" else "Saved to the gallery, in Reelkin") else (if (ru) "Не получилось сохранить" else "Couldn't save it"))
                }
            }, Modifier.weight(1f), kind = Kind.GHOST, enabled = !busy)
            Btn(t("Поделиться", "Share"), {
                busy = true
                scope.launch {
                    val f = withContext(Dispatchers.IO) { draw()?.let { runCatching { file(ctx, it, name) }.getOrNull() } }
                    busy = false
                    if (f != null) share(ctx, f, ru)
                }
            }, Modifier.weight(1f), kind = Kind.GOLD, enabled = !busy)
        }
    }
}
