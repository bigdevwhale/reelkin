package app.reelkin

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import app.reelkin.data.DemoSeed
import app.reelkin.data.Settings
import app.reelkin.domain.Cinema
import app.reelkin.domain.gains
import app.reelkin.domain.Span
import app.reelkin.ui.cards.Postcards
import app.reelkin.ui.cards.Shape
import app.reelkin.ui.components.Tip
import app.reelkin.ui.art.Pose as ArtPose
import app.reelkin.ui.art.Mood as ArtMood
import app.reelkin.ui.art.Buf
import app.reelkin.ui.art.PetArt
import app.reelkin.data.Stage
import app.reelkin.data.Species
import app.reelkin.ui.LocalRu
import app.reelkin.ui.Overlay
import app.reelkin.ui.ReelkinRoot
import app.reelkin.ui.Tab
import app.reelkin.ui.art.Art
import app.reelkin.ui.onboarding.Onboarding
import app.reelkin.ui.sheets.seance
import app.reelkin.ui.theme.ReelkinTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp

/** Every main screen over the demo cinema, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    /** Today at half past eight in the evening. */
    private val evening: Long = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 30) }.timeInMillis

    @Before fun still() { Art.still = true }

    private val app get() = ApplicationProvider.getApplicationContext<ReelkinApp>()
    private val ru get() = lang == "ru"

    private fun seed(change: (Settings) -> Settings = { it }): Settings = runBlocking {
        DemoSeed.seedIfEmpty(app, app.container)
        app.container.settings.update { change(it.copy(seance = null, bought = setOf("GARLAND"), onboarded = true, tips = Tip.entries.map { t -> t.name }.toSet())) }
        app.container.settings.now()
    }

    private fun shot(name: String, wait: String? = null, scroll: Int = 0, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { ReelkinTheme { CompositionLocalProvider(LocalRu provides ru) { content() } } }
        if (wait != null) rule.waitUntil(15_000) { rule.mainClock.advanceTimeBy(100); rule.onAllNodesWithText(wait, substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.advanceTimeBy(2_000)
        repeat(scroll) { rule.onAllNodes(androidx.compose.ui.test.hasScrollAction())[0].performTouchInput { swipeUp(startY = centerY + height / 8f, endY = top + height / 6f) }; rule.mainClock.advanceTimeBy(1_000) }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun screen(name: String, tab: Tab, change: (Settings) -> Settings = { it }, scroll: Int = 0, overlay: (Cinema) -> Overlay? = { null }) {
        val s = seed(change)
        val c = runBlocking { app.container.cinema() }
        shot(name, "Reelkin", scroll) { ReelkinRoot(s, evening, tab, overlay(c)) }
    }

    @Test fun welcome() = shot("0-welcome") { Onboarding() }
    @Test fun booth() = screen("1-booth", Tab.BOOTH, { it.copy(bought = setOf("GARLAND", "CHAIR", "NEON", "POPCORN", "RUG", "PLANT", "CLOCK", "WALL_GREEN"), wall = "GREEN", seance = null) })
    @Test fun tonight() = screen("1-tonight", Tab.BOOTH, scroll = 1)
    @Test fun seance() = screen("2-seance", Tab.BOOTH, { s ->
        val f = runBlocking { app.container.cinema() }.watch.first { it.title.startsWith(if (ru) "Касаб" else "Casab") }
        s.copy(seance = f.seance().copy(started = System.currentTimeMillis() - 42 * 60_000L))
    })
    @Test fun bill() = screen("3-bill", Tab.BILL)
    @Test fun hall() = screen("4-saga", Tab.SAGA)
    @Test fun petsTab() = screen("12-pets", Tab.PETS)
    @Test fun menu() = screen("13-menu", Tab.BOOTH) { Overlay.Menu }
    @Test fun shop() = screen("14-shop", Tab.BOOTH) { Overlay.Shop }
    @Test fun feats() = screen("15-feats", Tab.SAGA) { Overlay.Feats }
    @Test fun reward() = screen("5-reward", Tab.BOOTH) { c ->
        val t = c.seats.last()
        Overlay.Reward(t, gains(Cinema(c.titles, c.seats.dropLast(1), c.episodes, c.s, c.now), c, t))
    }
    @Test fun pick() = screen("6-pick", Tab.BOOTH) { Overlay.Pick }
    @Test fun pet() = screen("7-pet", Tab.BOOTH) { Overlay.Pet }
    @Test fun pets() = screen("9-pets", Tab.BOOTH) { Overlay.Pets }
    @Test fun can() = screen("9-can", Tab.BOOTH) { Overlay.Can(Species.OWL) }
    @Test fun stats() = screen("10-stats", Tab.BILL) { Overlay.Stats }
    @Test fun cards() = screen("11-cards", Tab.BILL) { Overlay.Cards }
    /** Every postcard as a picture of its own, a post and a story. */
    @Test fun postcards() {
        seed()
        val c = runBlocking { app.container.cinema() }
        for (card in Postcards.available(c, Span.ALL)) {
            val b = Postcards.draw(app, c, card, Span.ALL, Shape.POST, ru)
            File(out, "card-${card.name.lowercase()}.png").outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
        val tk = c.seats.filter { !it.season && it.words.isNotBlank() }.maxBy { it.stars }
        Postcards.review(app, c, tk, Postcards.Opts(), Shape.POST, ru).let { b -> File(out, "card-review.png").outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } }
        Postcards.feat(app, c, c.feats.maxBy { it.level }, Postcards.Opts(), Shape.POST, ru).let { b -> File(out, "card-feat.png").outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } }
    }
    /** The cat at every stage, eyes open, blinking, asleep and happy, side by side. */
    @Test fun eyesAll() {
        val f = Buf.FINE
        val all = Species.entries
        val sheet = android.graphics.Bitmap.createBitmap(PetArt.W * f * 8, PetArt.H * f * ((all.size + 3) / 4), android.graphics.Bitmap.Config.ARGB_8888)
        val cv = android.graphics.Canvas(sheet); cv.drawColor(0xFF2A2030.toInt())
        for ((i, sp) in all.withIndex()) for (k in 0 until 2) {
            val b = PetArt.still(ArtPose(sp, Stage.ADULT, mood = ArtMood.IDLE, blink = k == 1))
            val bmp = android.graphics.Bitmap.createBitmap(b.fw, b.fh, android.graphics.Bitmap.Config.ARGB_8888).also { it.setPixels(b.d, 0, b.fw, 0, 0, b.fw, b.fh) }
            cv.drawBitmap(bmp, ((i % 4 * 2 + k) * b.fw).toFloat(), (i / 4 * b.fh).toFloat(), null)
        }
        File(out, "eyesAll.png").outputStream().use { sheet.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun eyes() {
        val sts = listOf(Stage.BABY, Stage.YOUNG, Stage.ADULT, Stage.ANCIENT)
        val moods = listOf(ArtMood.IDLE to false, ArtMood.IDLE to true, ArtMood.SLEEP to false, ArtMood.HAPPY to false)
        val f = Buf.FINE
        val sheet = android.graphics.Bitmap.createBitmap(PetArt.W * f * 4, PetArt.H * f * 4, android.graphics.Bitmap.Config.ARGB_8888)
        val cv = android.graphics.Canvas(sheet); cv.drawColor(0xFF2A2030.toInt())
        for ((r, st) in sts.withIndex()) for ((k, m) in moods.withIndex()) {
            val b = PetArt.still(ArtPose(Species.CAT, st, mood = m.first, blink = m.second))
            val bmp = android.graphics.Bitmap.createBitmap(b.fw, b.fh, android.graphics.Bitmap.Config.ARGB_8888).also { it.setPixels(b.d, 0, b.fw, 0, 0, b.fw, b.fh) }
            cv.drawBitmap(bmp, (k * b.fw).toFloat(), (r * b.fh).toFloat(), null)
        }
        File(out, "eyes.png").outputStream().use { sheet.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun stub() = screen("8-stub", Tab.BILL) { c -> Overlay.Stub(c.seats.first { it.title.startsWith(if (ru) "Бегущ" else "Blade") }.id) }
}

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }
