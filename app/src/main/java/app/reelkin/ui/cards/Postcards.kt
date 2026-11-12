package app.reelkin.ui.cards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import app.reelkin.R
import app.reelkin.data.Covers
import app.reelkin.data.DAY
import app.reelkin.data.Genre
import app.reelkin.data.Stage
import app.reelkin.domain.Cinema
import app.reelkin.domain.Decor
import app.reelkin.domain.Filter
import app.reelkin.domain.Span
import app.reelkin.domain.Stats
import app.reelkin.domain.Words
import app.reelkin.ui.art.BoothLook
import app.reelkin.ui.art.BoothScene
import app.reelkin.ui.art.Buf
import app.reelkin.ui.art.PetArt
import app.reelkin.ui.art.hex
import app.reelkin.ui.art.toBitmap
import app.reelkin.ui.bill.dur
import app.reelkin.ui.poster
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** The postcards to share: each one a picture of something the cinema knows, laid out for a social feed. */
enum class Postcard(val a: Int, val b: Int, val accent: Int) {
    BOOTH(0xFF221420.toInt(), 0xFF4E1F33.toInt(), 0xFFE8B850.toInt()),
    PET(0xFF1D1830.toInt(), 0xFF46284F.toInt(), 0xFFF0A0C8.toInt()),
    SUMMARY(0xFF241612.toInt(), 0xFF5E2A22.toInt(), 0xFFE8B850.toInt()),
    FILMS(0xFF1A1420.toInt(), 0xFF3E2440.toInt(), 0xFFE8B850.toInt()),
    SERIES(0xFF14202B.toInt(), 0xFF27445A.toInt(), 0xFF7FD6E0.toInt()),
    GENRES(0xFF1A1F18.toInt(), 0xFF35472C.toInt(), 0xFFB8E07A.toInt()),
    RHYTHM(0xFF161631.toInt(), 0xFF35306A.toInt(), 0xFFB9A4FF.toInt()),
    YEAR(0xFF1C1712.toInt(), 0xFF4A3420.toInt(), 0xFFE8B850.toInt()),
    MAP(0xFF102326.toInt(), 0xFF1F5157.toInt(), 0xFF8FE3C8.toInt()),
    ZOO(0xFF1B2416.toInt(), 0xFF3F5A2C.toInt(), 0xFFF5D06A.toInt());

    fun title(ru: Boolean) = when (this) {
        BOOTH -> if (ru) "Будка" else "The booth"
        PET -> if (ru) "Питомец" else "Pet"
        SUMMARY -> if (ru) "Итоги" else "Summary"
        FILMS -> if (ru) "Фильмы" else "Films"
        SERIES -> if (ru) "Сериалы" else "Series"
        GENRES -> if (ru) "Жанры" else "Genres"
        RHYTHM -> if (ru) "Ритм" else "Rhythm"
        YEAR -> if (ru) "Год" else "The year"
        MAP -> if (ru) "Карта" else "Map"
        ZOO -> if (ru) "Зверинец" else "Menagerie"
    }

    /** Whether the period chosen changes this card. */
    val bySpan get() = this in setOf(SUMMARY, FILMS, SERIES, GENRES, RHYTHM, MAP)
}

/** A post (4:5) or a story (9:16). Everything is laid out on a 1080×1350 page; a story gets more sky above and below. */
enum class Shape(val h: Int) { POST(1350), STORY(1920) }

object Postcards {
    const val W = 1080
    private const val PAGE = 1350

    /** The cards there is something to show on. */
    fun available(c: Cinema, span: Span): List<Postcard> {
        val s = Stats(c, Filter(span = span))
        return Postcard.entries.filter { p ->
            when (p) {
                Postcard.BOOTH -> true
                Postcard.PET -> c.pet?.pet?.hatched == true
                Postcard.ZOO -> c.pets.any { it.pet.hatched }
                Postcard.YEAR -> c.seats.isNotEmpty() || c.episodes.isNotEmpty()
                Postcard.FILMS -> s.films.isNotEmpty()
                Postcard.SERIES -> s.episodes.isNotEmpty()
                Postcard.MAP -> s.countries.isNotEmpty()
                else -> s.moments.isNotEmpty()
            }
        }
    }

    /** Where the QR code on a card leads: Reelkin in Google Play. */
    const val PLAY = "https://play.google.com/store/apps/details?id=app.reelkin"

    /** What a review card shows: each part can be left out. */
    data class Opts(val pet: Boolean = true, val poster: Boolean = true, val words: Boolean = true, val popcorn: Boolean = true, val date: Boolean = true, val qr: Boolean = true)

    /** A card for one stub: the film, its poster, the popcorn and the words, maybe the pet's view of it. */
    fun review(ctx: Context, c: Cinema, tk: app.reelkin.data.Ticket, o: Opts, shape: Shape, ru: Boolean): Bitmap = frame(ctx, Postcard.FILMS, shape, ru, o.qr) { p ->
        p.head(if (o.date) (if (ru) "мой корешок · " else "my stub · ") + fmt(tk.at, if (ru) "d MMMM yyyy" else "MMMM d, yyyy", ru) else (if (ru) "мой корешок" else "my stub"), "")
        val left = 60
        var y = 250f
        val textX = if (o.poster) 520f else 60f
        val textW = if (o.poster) 500 else 960
        if (o.poster) p.poster(ctx, tk.cover, tk.title, tk.genre, left, 250, 420, 620)
        val h = p.wrap(tk.title, textX, y, textW, if (o.poster) 64f else 84f, p.cream, p.display, lines = 4)
        y += h + 30f
        val facts = listOfNotNull(tk.year?.toString(), tk.country.ifBlank { null }, if (tk.minutes > 0) dur(tk.minutes.toLong(), ru) else null, Words.genre(tk.genre, ru)).joinToString(" · ")
        y += p.wrap(facts, textX, y, textW, 32f, p.soft, p.semi, lines = 2) + 30f
        if (o.popcorn && tk.stars > 0) {
            p.text("🍿".repeat(tk.stars) + "  " + (if (ru) "${tk.stars} из 5" else "${tk.stars} of 5"), textX, y + 50f, 50f, p.accent, p.bold)
            y += 90f
        }
        if (o.words && tk.words.isNotBlank()) {
            p.text("«", textX - 6f, y + 70f, 110f, p.accent, p.display)
            y += p.wrap(tk.words, textX + 50f, y + 10f, textW - 50, 46f, p.cream, p.display, lines = 4) + 40f
        }
        val badges = listOfNotNull(
            (if (ru) "пересмотр" else "a rewatch").takeIf { tk.rewatch },
            (if (ru) "сцена после титров" else "post-credits scene").takeIf { tk.credits == app.reelkin.data.Credits.SCENE },
            when (tk.company) { app.reelkin.data.Company.TWO -> if (ru) "вдвоём" else "for two"; app.reelkin.data.Company.MANY -> if (ru) "компанией" else "with friends"; else -> null },
        )
        val people = app.reelkin.data.People.of(tk.director).firstOrNull()
        if (people != null) { y += p.wrap((if (ru) "режиссёр " else "directed by ") + people, textX, y, textW, 30f, p.soft, p.semi, lines = 2) + 16f }
        if (badges.isNotEmpty()) p.chips(badges, maxOf(y + 60f, if (o.poster) 960f else y + 60f), left = true)
        if (o.pet) c.pet?.takeIf { it.pet.hatched }?.let { pt -> petSays(p, pt, c, if (tk.stars >= 4) (if (ru) "Мне очень понравилось!" else "I loved it!") else if (tk.stars in 1..2) (if (ru) "Ну… бывает." else "Well… it happens.") else (if (ru) "Хорошее было кино." else "That was a good one."), ru) }
    }

    /** A card for one achievement: its picture, its name, the steps climbed. */
    fun feat(ctx: Context, c: Cinema, fs: app.reelkin.domain.FeatState, o: Opts, shape: Shape, ru: Boolean): Bitmap = frame(ctx, Postcard.YEAR, shape, ru, o.qr) { p ->
        p.head(if (ru) "достижение" else "achievement", app.reelkin.domain.Feats.name(fs.f, ru))
        p.glow(540f, 700f, 400f)
        runCatching { Postcards::class.java.getResourceAsStream("/feats/${fs.f.file}.png")?.use { android.graphics.BitmapFactory.decodeStream(it) } }.getOrNull()?.let {
            p.pixels(it, 540 - 220, 360, 440, 440)
        }
        val dots = fs.max
        for (i in 0 until dots) p.dot(540f + (i - (dots - 1) / 2f) * 44f, 850f, 14f, if (i < fs.level) p.accent else 0x33FFFFFF)
        val last = fs.f.tiers.getOrElse(fs.level - 1) { fs.f.tiers.first() }
        p.text(app.reelkin.domain.Feats.goal(fs.f, last, ru).replaceFirstChar { it.uppercase() }, 540f, 940f, 46f, p.cream, p.display, Paint.Align.CENTER, 960f)
        p.text(if (ru) "ступень ${fs.level} из ${fs.max}" else "step ${fs.level} of ${fs.max}", 540f, 995f, 32f, p.soft, p.semi, Paint.Align.CENTER)
        if (o.pet) c.pet?.takeIf { it.pet.hatched }?.let { pt -> petSays(p, pt, c, if (ru) "Я горжусь тобой!" else "So proud of you!", ru) }
    }

    /** The pet in the lower corner with a word in a bubble. */
    private fun petSays(p: Pen, pt: app.reelkin.domain.PetState, c: Cinema, line: String, ru: Boolean) {
        val buf = PetArt.portrait(pt.species, pt.stage, pt.pet.shiny, pt.form)
        val k = 230f / buf.h
        val w = (buf.w * k).toInt(); val h = (buf.h * k).toInt()
        p.pixels(buf.toBitmap(), 1020 - w, 1290 - h, w, h)
        val bw = p.width(line, 34f, p.bold) + 60f
        p.bubble(1020f - w - bw - 10f, 1290f - h + 10f, bw, 80f)
        p.text(line, 1020f - w - bw / 2f - 10f, 1290f - h + 62f, 34f, 0xFF2A1A20.toInt(), p.bold, Paint.Align.CENTER)
        p.text(c.name(pt, ru), 1020f - w / 2f, 1325f, 28f, p.soft, p.semi, Paint.Align.CENTER)
    }

    /** A card's frame: the background, the page laid out by [body], the name on top and, if wanted, the QR code. */
    private fun frame(ctx: Context, look: Postcard, shape: Shape, ru: Boolean, qr: Boolean, body: (Pen) -> Unit): Bitmap {
        val bmp = Bitmap.createBitmap(W, shape.h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val p = Pen(ctx, cv, look.accent)
        p.background(look, shape.h)
        cv.save(); cv.translate(0f, (shape.h - PAGE) / 2f)
        body(p)
        cv.restore()
        p.brand(shape.h, ru, qr)
        return bmp
    }

    fun draw(ctx: Context, c: Cinema, card: Postcard, span: Span, shape: Shape, ru: Boolean, qr: Boolean = true): Bitmap {
        val bmp = Bitmap.createBitmap(W, shape.h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val p = Pen(ctx, cv, card.accent)
        p.background(card, shape.h)
        cv.save()
        cv.translate(0f, (shape.h - PAGE) / 2f)
        val s = Stats(c, Filter(span = span))
        val period = periodName(span, c, ru)
        when (card) {
            Postcard.BOOTH -> booth(ctx, p, c, ru)
            Postcard.PET -> pet(p, c, ru)
            Postcard.SUMMARY -> summary(p, c, s, period, ru)
            Postcard.FILMS -> films(ctx, p, s, period, ru)
            Postcard.SERIES -> series(ctx, p, s, period, ru)
            Postcard.GENRES -> genres(p, s, period, ru)
            Postcard.RHYTHM -> rhythm(p, s, period, ru)
            Postcard.YEAR -> year(p, c, ru)
            Postcard.MAP -> map(p, s, period, ru)
            Postcard.ZOO -> zoo(p, c, ru)
        }
        cv.restore()
        p.brand(shape.h, ru, qr)
        return bmp
    }

    private fun periodName(span: Span, c: Cinema, ru: Boolean): String = when (span) {
        Span.WEEK -> if (ru) "последние 7 дней" else "the last 7 days"
        Span.MONTH -> if (ru) "последние 30 дней" else "the last 30 days"
        Span.YEAR -> if (ru) "${c.year} год" else "${c.year}"
        Span.LAST_YEAR -> if (ru) "${c.year - 1} год" else "${c.year - 1}"
        Span.ALL -> if (ru) "всё время" else "all time"
    }

    private fun fmt(ms: Long, pattern: String, ru: Boolean) = SimpleDateFormat(pattern, if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH).format(Date(ms))

    // ---------------------------------------------------------------- cards

    private fun booth(ctx: Context, p: Pen, c: Cinema, ru: Boolean) {
        p.head(if (ru) "кино и питомец" else "films and a pet", if (ru) "Моя будка" else "My booth")
        val pt = c.pet
        val fav = c.favourite
        val look = BoothLook(
            species = pt?.species, stage = if (pt != null && !pt.pet.hatched) Stage.EGG else pt?.stage ?: Stage.EGG,
            shiny = pt?.pet?.shiny == true, form = pt?.form, hatch = pt?.let { (it.fed / Stage.BABY.from.toFloat()).coerceIn(0f, 1f) } ?: 0f,
            poster = fav?.let { poster(ctx, it.title, it.genre, it.cover, app.reelkin.ui.art.Scenery.poster[2], app.reelkin.ui.art.Scenery.poster[3]) }, cans = c.seats.takeLast(4).map { hex(it.genre.color.toLong()) },
            garland = c.has(Decor.GARLAND), chair = c.has(Decor.CHAIR), neon = c.has(Decor.NEON), popcorn = c.has(Decor.POPCORN),
            rug = c.has(Decor.RUG), plant = c.has(Decor.PLANT), clock = c.has(Decor.CLOCK), wall = c.wall.file,
            night = false, seance = null, ru = ru,
        )
        val b = Buf(BoothScene.W, BoothScene.H, BoothScene.F)
        val scene = BoothScene()
        scene.x = BoothScene.CHAIR_X
        scene.draw(b, look, 3.0, 0.0)
        val w = 960; val h = w * BoothScene.H / BoothScene.W
        p.framed(b.toBitmap(), 60, 370, w, h, 40f)
        if (pt != null && pt.pet.hatched) {
            p.text(c.name(pt, ru), 60f, 1250f, 60f, p.cream, p.display, max = 960f)
            p.text("${Words.species(pt.species, ru)} · ${Words.stage(pt.stage, ru)}", 60f, 1300f, 34f, p.soft, p.semi)
        }
        p.text(if (ru) "${Words.minutes(c.minutes, true)} кино · ${Words.films(c.seats.count { !it.season }, true)}" else "${Words.minutes(c.minutes, false)} of film · ${Words.films(c.seats.count { !it.season }, false)}",
            1020f, 1300f, 30f, p.accent, p.bold, Paint.Align.RIGHT)
    }

    private fun pet(p: Pen, c: Cinema, ru: Boolean) {
        val pt = c.pet ?: return
        p.head(if (ru) "мой питомец из коробки с плёнкой" else "my pet from a film can", c.name(pt, ru))
        p.glow(540f, 720f, 380f)
        val buf = PetArt.still(pt.species, pt.stage, pt.pet.shiny, pt.form)
        val k = 10
        p.pixels(buf.toBitmap(), 540 - PetArt.W * k / 2, 720 - PetArt.H * k / 2, PetArt.W * k, PetArt.H * k)
        val chips = listOfNotNull(Words.species(pt.species, ru), Words.stage(pt.stage, ru), if (pt.pet.shiny) (if (ru) "техниколор" else "technicolor") else null)
        p.chips(chips, 1150f)
        p.text(if (ru) "Съел ${Words.minutes(pt.fed.toLong(), true)} кино · любит ${Words.loves(pt.species, true)}" else "Ate ${Words.minutes(pt.fed.toLong(), false)} of film · loves ${Words.loves(pt.species, false)}",
            540f, 1250f, 32f, p.soft, p.semi, Paint.Align.CENTER, 960f)
    }

    private fun summary(p: Pen, c: Cinema, s: Stats, period: String, ru: Boolean) {
        p.head(period, if (ru) "Моё кино" else "My films")
        val big = if (s.hours >= 1) Words.fmt(s.hours) else "${s.minutes}"
        val unit = if (s.hours >= 1) (if (ru) Words.plural((s.hours % 100).toInt(), "час", "часа", "часов") else if (s.hours == 1L) "hour" else "hours") else (if (ru) "мин" else "min")
        p.text(big, 60f, 620f, 230f, p.accent, p.display)
        val bw = p.width(big, 230f, p.display)
        p.text(unit, 60f + bw + 24f, 620f, 64f, p.cream, p.display)
        p.text(if (ru) "у экрана · дней с кино: ${s.days} · в среднем ${dur(s.perDay.toLong(), true)} в день" else "watched · ${s.days} days with films · ${dur(s.perDay.toLong(), false)} a day",
            60f, 690f, 34f, p.soft, p.semi, max = 960f)
        val tiles = listOf(
            "${s.films.size}" to (if (ru) "фильмов" else "films"),
            "${s.episodes.size}" to (if (ru) "серий" else "episodes"),
            (s.avgPopcorn?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "–") to (if (ru) "попкорна в среднем" else "popcorn on average"),
            "${Stats.streaks(c).first}" to (if (ru) "дней подряд — рекорд" else "days in a row – best"),
        )
        tiles.forEachIndexed { i, (v, l) ->
            val x = 60f + (i % 2) * 490f; val y = 760f + (i / 2) * 190f
            p.panel(x, y, 470f, 170f)
            p.text(v, x + 32f, y + 88f, 64f, p.cream, p.display)
            p.text(l, x + 32f, y + 136f, 30f, p.soft, p.semi, max = 410f)
        }
        var y = 1180f
        s.topFilms.firstOrNull()?.let { p.text((if (ru) "Лучший фильм: " else "Best film: ") + it.name, 60f, y, 38f, p.cream, p.bold, max = 960f); y += 56f }
        s.genres.firstOrNull()?.let { p.text((if (ru) "Главный жанр: " else "Top genre: ") + Words.genre(it.first, ru), 60f, y, 38f, p.cream, p.bold, max = 960f) }
    }

    private fun films(ctx: Context, p: Pen, s: Stats, period: String, ru: Boolean) {
        p.head(period, if (ru) "Лучшие фильмы" else "Best films")
        val rows = s.topFilms.take(9)
        val n = if (rows.size >= 9) 3 else if (rows.size >= 4) 2 else 1
        val gap = 24
        val pw = (780 - gap * (n - 1)) / n
        val ph = pw * 3 / 2
        val scale = min(1f, 820f / (ph * ((rows.size.coerceAtMost(n * n) + n - 1) / n) + gap))
        val w = (pw * scale).toInt(); val h = (ph * scale).toInt()
        val x0 = (W - (w * n + gap * (n - 1))) / 2
        rows.take(n * n).forEachIndexed { i, r ->
            val x = x0 + (i % n) * (w + gap); val y = 380 + (i / n) * (h + gap)
            p.poster(ctx, r.cover, r.name, r.genre, x, y, w, h)
            p.badge("${i + 1}", x + 14f, y + 14f, if (i == 0) p.accent else 0xCC1A1014.toInt())
        }
        rows.firstOrNull()?.let {
            p.text("№1 — ${it.name}", 90f, 1250f, 40f, p.cream, p.display, max = 900f)
            p.text(listOf(it.sub, if (it.stars > 0) (if (ru) "попкорн: ${it.stars} из 5" else "popcorn: ${it.stars} of 5") else "").filter { s -> s.isNotEmpty() }.joinToString(" · "), 90f, 1298f, 30f, p.soft, p.semi, max = 900f)
        }
    }

    private fun series(ctx: Context, p: Pen, s: Stats, period: String, ru: Boolean) {
        p.head(period, if (ru) "Мои сериалы" else "My series")
        val rows = s.topSeries.take(6)
        val top = rows.firstOrNull()?.minutes?.coerceAtLeast(1) ?: 1
        rows.forEachIndexed { i, r ->
            val y = 400 + i * 145
            p.poster(ctx, r.cover, r.name, r.genre, 60, y, 84, 126)
            p.text(r.name, 180f, y + 46f, 42f, p.cream, p.display, max = 640f)
            p.text(dur(r.minutes.toLong(), ru), 180f, y + 92f, 30f, p.soft, p.semi, max = 640f)
            p.text(if (ru) "${r.count} сер." else "${r.count} ep.", 1020f, y + 46f, 34f, if (i == 0) p.accent else p.soft, p.bold, Paint.Align.RIGHT)
            p.bar(180f, y + 108f, 840f, 10f, r.minutes / top.toFloat())
        }
        p.text(if (ru) "Серий: ${s.episodes.size} · сезонов досмотрено: ${s.seasons}" else "${s.episodes.size} episodes · ${s.seasons} seasons finished", 60f, 1300f, 34f, p.accent, p.bold)
    }

    private fun genres(p: Pen, s: Stats, period: String, ru: Boolean) {
        p.head(period, if (ru) "Мои жанры" else "My genres")
        val g = s.genres.take(8)
        val total = g.sumOf { it.second }.coerceAtLeast(1).toFloat()
        val cx = 540f; val cy = 690f; val r = 230f
        var a = -90f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 96f }
        for ((genre, m) in g) {
            val sweep = m / total * 360f
            ring.color = 0xFF000000.toInt() or genre.color
            p.c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), a, (sweep - 1.5f).coerceAtLeast(.5f), false, ring)
            a += sweep
        }
        g.firstOrNull()?.let { (top, m) ->
            p.text("${(m * 100 / total).toInt()}%", cx, cy + 10f, 84f, p.cream, p.display, Paint.Align.CENTER)
            p.text(Words.genre(top, ru), cx, cy + 62f, 34f, p.soft, p.semi, Paint.Align.CENTER, 300f)
        }
        g.take(6).forEachIndexed { i, (genre, m) ->
            val x = 90f + (i % 2) * 470f; val y = 1030f + (i / 2) * 76f
            p.dot(x + 14f, y - 12f, 14f, 0xFF000000.toInt() or genre.color)
            p.text(Words.genre(genre, ru), x + 44f, y, 32f, p.cream, p.bold, max = 270f)
            p.text("${(m * 100 / total).toInt()}%", x + 420f, y, 34f, p.soft, p.semi, Paint.Align.RIGHT)
        }
    }

    private fun rhythm(p: Pen, s: Stats, period: String, ru: Boolean) {
        val peak = s.peakHour ?: 21
        val (who, what) = when (peak) {
            in 5..11 -> (if (ru) "Утренний зритель" else "Morning viewer") to (if (ru) "Кино — с первым кофе" else "Films with the first coffee")
            in 12..17 -> (if (ru) "Дневной сеанс" else "Matinee lover") to (if (ru) "Лучшие места на дневном сеансе" else "The best seats are at the matinee")
            in 18..22 -> (if (ru) "Вечерний зритель" else "Evening viewer") to (if (ru) "Свет гаснет — начинается главное" else "The lights go down and it begins")
            else -> (if (ru) "Полуночник" else "Midnight movie fan") to (if (ru) "Самое интересное — когда все спят" else "The best bit is when everyone's asleep")
        }
        p.head(period, who)
        p.text(what, 60f, 400f, 40f, p.soft, p.semi)
        p.text((if (ru) "по часам" else "by hour").uppercase(), 60f, 500f, 28f, p.accent, p.bold, spacing = .12f)
        p.bars(s.hoursOfDay.toList(), 60f, 530f, 960f, 300f, peak)
        for (h in listOf(0, 6, 12, 18, 23)) p.text("$h", 60f + (h + .5f) * 40f, 870f, 26f, p.faint, p.semi, Paint.Align.CENTER)
        p.text((if (ru) "по дням недели" else "by weekday").uppercase(), 60f, 960f, 28f, p.accent, p.bold, spacing = .12f)
        val fav = s.favouriteWeekday ?: -1
        p.bars(s.weekdays.toList(), 60f, 990f, 960f, 190f, fav)
        val names = if (ru) listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс") else listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
        for (i in 0 until 7) p.text(names[i], 60f + (i + .5f) * 960f / 7, 1222f, 28f, if (fav == i) p.accent else p.faint, p.semi, Paint.Align.CENTER)
        p.text(if (ru) "Пик — около $peak:00" else "Peak around $peak:00", 60f, 1290f, 34f, p.cream, p.bold)
    }

    private fun year(p: Pen, c: Cinema, ru: Boolean) {
        val y = c.year
        p.head(if (ru) "каждый день — квадратик" else "a square for every day", if (ru) "Мой $y" else "My $y")
        val byDay = Stats.byDay(c)
        val cal = Calendar.getInstance()
        fun epochDay(m: Int, d: Int): Long { cal.clear(); cal.set(y, m, d); val ms = cal.timeInMillis; return (ms + cal.timeZone.getOffset(ms)).floorDiv(DAY) }
        val today = (c.now + cal.timeZone.getOffset(c.now)).floorDiv(DAY)
        val first = epochDay(0, 1); val last = epochDay(12, 1)
        val inYear = byDay.filterKeys { it in first until last }
        val max = inYear.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        val cell = 26f; val gap = 4f; val x0 = 130f; val y0 = 420f; val row = 52f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (m in 0 until 12) {
            cal.clear(); cal.set(y, m, 1)
            val len = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val yy = y0 + m * row
            p.text(fmt(cal.timeInMillis, "LLL", ru).trimEnd('.'), 60f, yy + 22f, 26f, p.faint, p.semi)
            val start = epochDay(m, 1)
            for (d in 0 until len) {
                val day = start + d
                val v = inYear[day] ?: 0L
                val x = x0 + d * (cell + gap)
                paint.color = if (day > today) 0x14FFFFFF else if (v == 0L) 0x22FFFFFF else blend(0x55E8B850, p.accent, (v.toFloat() / max).coerceIn(0f, 1f))
                p.c.drawRoundRect(RectF(x, yy, x + cell, yy + cell), 7f, 7f, paint)
            }
        }
        p.text(if (ru) "Дней с кино: ${inYear.size} · ${dur(inYear.values.sum(), true)}" else "${inYear.size} days with films · ${dur(inYear.values.sum(), false)}", 60f, 1100f, 40f, p.cream, p.display, max = 960f)
        val (best, _) = Stats.streaks(c)
        p.text(if (ru) "Рекорд — $best ${Words.plural(best, "день", "дня", "дней")} подряд" else "Best streak – $best days in a row", 60f, 1160f, 34f, p.soft, p.semi)
        inYear.maxByOrNull { it.value }?.let { (d, m) ->
            p.text(if (ru) "Самый киношный день — ${fmt(d * DAY, "d MMMM", true)}, ${dur(m, true)}" else "Biggest day – ${fmt(d * DAY, "MMMM d", false)}, ${dur(m, false)}", 60f, 1215f, 34f, p.soft, p.semi, max = 960f)
        }
    }

    private fun map(p: Pen, s: Stats, period: String, ru: Boolean) {
        p.head(period, if (ru) "Кино без границ" else "Films without borders")
        val list = s.countries.take(7)
        val top = list.firstOrNull()?.second?.coerceAtLeast(1) ?: 1
        list.forEachIndexed { i, (cn, n) ->
            val y = 430f + i * 105f
            p.text(cn, 60f, y + 30f, 40f, p.cream, p.display, max = 700f)
            p.text(Words.films(n, ru), 1020f, y + 30f, 30f, p.soft, p.semi, Paint.Align.RIGHT)
            p.bar(60f, y + 52f, 960f, 12f, n / top.toFloat())
        }
        if (s.decades.isNotEmpty()) {
            p.text((if (ru) "десятилетия" else "decades").uppercase(), 60f, 1190f, 28f, p.accent, p.bold, spacing = .12f)
            p.chips(s.decades.sortedByDescending { it.second }.take(6).map { "’" + (it.first % 100).toString().padStart(2, '0') }, 1250f, left = true)
        }
    }

    private fun zoo(p: Pen, c: Cinema, ru: Boolean) {
        val pets = c.pets.filter { it.pet.hatched }.sortedByDescending { it.fed }.take(9)
        p.head(if (ru) "${pets.size} ${Words.plural(pets.size, "питомец", "питомца", "питомцев")} · коробок открыто ${c.hints.count { it.open }} из ${c.hints.size}" else "${pets.size} pets · ${c.hints.count { it.open }} of ${c.hints.size} cans open",
            if (ru) "Мой зверинец" else "My menagerie")
        val n = if (pets.size > 4) 3 else if (pets.size > 1) 2 else 1
        val rows = (pets.size + n - 1) / n
        val cell = 960 / n
        val rowH = min(cell + 50, 880 / max(1, rows))
        val pic = rowH - 64
        pets.forEachIndexed { i, ps ->
            val x = 60 + (i % n) * cell; val y = 390 + (i / n) * rowH
            p.panel(x + 8f, y + 6f, cell - 16f, rowH - 12f)
            val k = pic / PetArt.H.toFloat()
            val buf = PetArt.still(ps.species, ps.stage, ps.pet.shiny, ps.form)
            val w = (PetArt.W * k).toInt()
            p.pixels(buf.toBitmap(), x + (cell - w) / 2, y + 8, w, pic)
            p.text(c.name(ps, ru), x + cell / 2f, y + rowH - 24f, if (n == 3) 30f else 40f, p.cream, p.display, Paint.Align.CENTER, cell - 30f)
        }
    }

    // ---------------------------------------------------------------- helpers

    fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(c: Int, s: Int) = (c shr s) and 0xFF
        fun one(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * t).toInt().coerceIn(0, 255)
        return (one(24) shl 24) or (one(16) shl 16) or (one(8) shl 8) or one(0)
    }

    /** What draws on a card: the fonts, the colours and the shapes they share. */
    class Pen(private val ctx: Context, val c: Canvas, val accent: Int) {
        val display: Typeface = ResourcesCompat.getFont(ctx, R.font.prata) ?: Typeface.SERIF
        val bold: Typeface = ResourcesCompat.getFont(ctx, R.font.manrope_bold) ?: Typeface.DEFAULT_BOLD
        val semi: Typeface = ResourcesCompat.getFont(ctx, R.font.manrope_semibold) ?: Typeface.DEFAULT
        val narrow: Typeface = ResourcesCompat.getFont(ctx, R.font.pt_sans_narrow_bold) ?: Typeface.DEFAULT_BOLD
        val cream = 0xFFFFF3E0.toInt()
        val soft = 0xCCF3E3D0.toInt()
        val faint = 0x80F3E3D0.toInt()
        private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        fun background(card: Postcard, h: Int) {
            fill.shader = LinearGradient(0f, 0f, W.toFloat() * .4f, h.toFloat(), card.a, card.b, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, W.toFloat(), h.toFloat(), fill)
            fill.shader = null
            val rnd = java.util.Random(card.ordinal * 31L + 7)
            repeat(5) {
                val x = rnd.nextFloat() * W; val y = rnd.nextFloat() * h; val r = 160f + rnd.nextFloat() * 260f
                fill.shader = RadialGradient(x, y, r, intArrayOf((accent and 0xFFFFFF) or 0x26000000, accent and 0xFFFFFF), null, Shader.TileMode.CLAMP)
                c.drawCircle(x, y, r, fill)
            }
            fill.shader = null
            // Film perforations along the edges, and a scatter of little stars.
            fill.color = 0x16FFFFFF
            var y = 20f
            while (y < h) { c.drawRoundRect(RectF(14f, y, 34f, y + 28f), 5f, 5f, fill); c.drawRoundRect(RectF(W - 34f, y, W - 14f, y + 28f), 5f, 5f, fill); y += 52f }
            fill.color = 0x1FFFFFFF
            repeat(24) { val x = rnd.nextFloat() * W; val yy = rnd.nextFloat() * h; c.drawCircle(x, yy, 2f + rnd.nextFloat() * 3f, fill) }
        }

        fun brand(h: Int, ru: Boolean, qr: Boolean = false) {
            val top = (h - PAGE) / 2f
            if (qr) {
                // A code to the app in Google Play, top right, on a white card so any camera reads it.
                val side = 132; val x = W - 60f - side; val y = top + 34f
                fill.color = 0xFFFFFFFF.toInt()
                c.drawRoundRect(RectF(x - 8f, y - 8f, x + side + 8f, y + side + 8f), 14f, 14f, fill)
                runCatching {
                    val m = com.google.zxing.qrcode.QRCodeWriter().encode(PLAY, com.google.zxing.BarcodeFormat.QR_CODE, 0, 0, mapOf(com.google.zxing.EncodeHintType.MARGIN to 0))
                    val cell = side / m.width.toFloat()
                    fill.color = 0xFF1A1014.toInt()
                    for (yy in 0 until m.height) for (xx in 0 until m.width) if (m.get(xx, yy)) c.drawRect(x + xx * cell, y + yy * cell, x + (xx + 1) * cell + .5f, y + (yy + 1) * cell + .5f, fill)
                }
                fill.color = accent
                c.drawCircle(78f, top + 92f, 16f, fill)
                text("Reelkin", 108f, top + 104f, 40f, cream, display)
                text(if (ru) "скачать в Google Play →" else "get it on Google Play →", x - 22f, top + 108f, 26f, faint, semi, Paint.Align.RIGHT)
                return
            }
            fill.color = accent
            c.drawCircle(78f, top + 92f, 16f, fill)
            fill.color = 0xFF1A1014.toInt()
            for (k in 0 until 3) { val a = Math.toRadians(k * 120.0 - 90); c.drawCircle(78f + 8f * Math.cos(a).toFloat(), top + 92f + 8f * Math.sin(a).toFloat(), 3.5f, fill) }
            text("Reelkin", 108f, top + 104f, 40f, cream, display)
            text(if (ru) "кино и питомец" else "films and a pet", W - 60f, top + 104f, 28f, faint, semi, Paint.Align.RIGHT)
        }

        fun head(eyebrow: String, title: String) {
            text(eyebrow.uppercase(), 60f, 210f, 30f, accent, bold, spacing = .14f, max = 960f)
            text(title, 60f, 300f, 78f, cream, display, max = 960f)
        }

        fun width(s: String, size: Float, font: Typeface): Float { tp.textSize = size; tp.typeface = font; tp.letterSpacing = 0f; return tp.measureText(s) }

        fun text(s: String, x: Float, y: Float, size: Float, color: Int, font: Typeface, align: Paint.Align = Paint.Align.LEFT, max: Float? = null, spacing: Float = 0f) {
            tp.textSize = size; tp.typeface = font; tp.color = color; tp.textAlign = align; tp.letterSpacing = spacing
            val t = if (max != null) TextUtils.ellipsize(s, tp, max, TextUtils.TruncateAt.END).toString() else s
            c.drawText(t, x, y, tp)
            tp.letterSpacing = 0f
        }

        fun wrap(s: String, x: Float, y: Float, width: Int, size: Float, color: Int, font: Typeface, lines: Int = 3): Float {
            tp.textSize = size; tp.typeface = font; tp.color = color; tp.textAlign = Paint.Align.LEFT; tp.letterSpacing = 0f
            val l = android.text.StaticLayout.Builder.obtain(s, 0, s.length, tp, width).setMaxLines(lines).setEllipsize(TextUtils.TruncateAt.END).setLineSpacing(0f, .95f).build()
            c.save(); c.translate(x, y); l.draw(c); c.restore()
            return l.height.toFloat()
        }

        fun bubble(x: Float, y: Float, w: Float, h: Float) {
            fill.color = 0xFFF4E6C8.toInt()
            c.drawRoundRect(RectF(x, y, x + w, y + h), 30f, 30f, fill)
            c.drawPath(Path().apply { moveTo(x + w - 30f, y + h - 6f); lineTo(x + w + 18f, y + h + 14f); lineTo(x + w - 6f, y + h - 30f); close() }, fill)
        }

        fun panel(x: Float, y: Float, w: Float, h: Float) {
            fill.color = 0x1AFFFFFF
            c.drawRoundRect(RectF(x, y, x + w, y + h), 32f, 32f, fill)
        }

        fun dot(x: Float, y: Float, r: Float, col: Int) { fill.color = col; c.drawCircle(x, y, r, fill) }

        fun bar(x: Float, y: Float, w: Float, h: Float, part: Float) {
            fill.color = 0x1FFFFFFF
            c.drawRoundRect(RectF(x, y, x + w, y + h), h / 2, h / 2, fill)
            fill.color = accent
            c.drawRoundRect(RectF(x, y, x + max(h, w * part.coerceIn(0f, 1f)), y + h), h / 2, h / 2, fill)
        }

        fun bars(values: List<Long>, x: Float, y: Float, w: Float, h: Float, hi: Int) {
            val top = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
            val bw = w / values.size
            values.forEachIndexed { i, v ->
                val bh = max(6f, v.toFloat() / top * h)
                fill.color = if (i == hi) accent else blend(0x40FFFFFF, accent, .35f)
                c.drawRoundRect(RectF(x + i * bw + bw * .14f, y + h - bh, x + (i + 1) * bw - bw * .14f, y + h), bw * .2f, bw * .2f, fill)
            }
        }

        fun chips(list: List<String>, y: Float, left: Boolean = false) {
            tp.textSize = 34f; tp.typeface = bold; tp.letterSpacing = 0f
            val widths = list.map { tp.measureText(it) + 56f }
            val total = widths.sum() + (list.size - 1) * 16f
            var x = if (left) 60f else (W - total) / 2
            list.forEachIndexed { i, s ->
                fill.color = if (i == 0) accent else 0x26FFFFFF
                c.drawRoundRect(RectF(x, y - 50f, x + widths[i], y + 14f), 32f, 32f, fill)
                text(s, x + widths[i] / 2, y - 6f, 34f, if (i == 0) 0xFF231A14.toInt() else cream, bold, Paint.Align.CENTER)
                x += widths[i] + 16f
            }
        }

        fun badge(s: String, x: Float, y: Float, col: Int) {
            fill.color = col
            c.drawCircle(x + 28f, y + 28f, 30f, fill)
            text(s, x + 28f, y + 40f, 32f, if (col == accent) 0xFF231A14.toInt() else cream, display, Paint.Align.CENTER)
        }

        fun glow(x: Float, y: Float, r: Float) {
            fill.shader = RadialGradient(x, y, r, intArrayOf((accent and 0xFFFFFF) or 0x44000000, accent and 0xFFFFFF), null, Shader.TileMode.CLAMP)
            c.drawCircle(x, y, r, fill)
            fill.shader = null
        }

        /** Pixel art, blown up with hard edges. */
        fun pixels(b: Bitmap, x: Int, y: Int, w: Int, h: Int) {
            c.drawBitmap(b, null, Rect(x, y, x + w, y + h), Paint().apply { isFilterBitmap = false; isAntiAlias = false })
        }

        /** A picture in a rounded frame with a soft shadow. */
        fun framed(b: Bitmap, x: Int, y: Int, w: Int, h: Int, r: Float, hard: Boolean = true) {
            val box = RectF(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat())
            fill.color = 0x66000000
            fill.maskFilter = BlurMaskFilter(28f, BlurMaskFilter.Blur.NORMAL)
            c.drawRoundRect(RectF(box.left + 6, box.top + 18, box.right - 6, box.bottom + 18), r, r, fill)
            fill.maskFilter = null
            c.save()
            c.clipPath(Path().apply { addRoundRect(box, r, r, Path.Direction.CW) })
            c.drawBitmap(b, null, Rect(x, y, x + w, y + h), Paint().apply { isFilterBitmap = !hard })
            c.restore()
        }

        /** A film's poster, or a pixel poster of its genre when there's no picture. */
        fun poster(ctx: Context, file: String?, name: String, genre: Genre?, x: Int, y: Int, w: Int, h: Int) {
            val bmp = Covers.file(ctx, file)?.let { f -> runCatching { BitmapFactory.decodeFile(f.path) }.getOrNull() }
            if (bmp != null) { framed(bmp, x, y, w, h, w * .06f, hard = false); return }
            // No picture: the genre's colour, darkening down, and the title, as the app shows it.
            val box = RectF(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat())
            val base = 0xFF000000.toInt() or (genre ?: Genre.DRAMA).color
            fill.shader = LinearGradient(0f, box.top, 0f, box.bottom, base, 0xFF1A1014.toInt(), Shader.TileMode.CLAMP)
            c.drawRoundRect(box, w * .06f, w * .06f, fill)
            fill.shader = null
            wrap(name, x + w * .08f, y + h * .62f, (w * .84f).toInt(), w * .11f, cream, display, lines = 3)
        }
    }
}
