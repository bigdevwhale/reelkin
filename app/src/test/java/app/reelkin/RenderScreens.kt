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
        app.container.settings.update { change(it.copy(seance = null, bought = setOf("GARLAND"), onboarded = true)) }
        app.container.settings.now()
    }

    private fun shot(name: String, wait: String? = null, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { ReelkinTheme { CompositionLocalProvider(LocalRu provides ru) { content() } } }
        if (wait != null) rule.waitUntil(15_000) { rule.mainClock.advanceTimeBy(100); rule.onAllNodesWithText(wait, substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun screen(name: String, tab: Tab, change: (Settings) -> Settings = { it }, overlay: (Cinema) -> Overlay? = { null }) {
        val s = seed(change)
        val c = runBlocking { app.container.cinema() }
        shot(name, if (ru) "попкорна" else "popcorn") { ReelkinRoot(s, evening, tab, overlay(c)) }
    }

    @Test fun welcome() = shot("0-welcome") { Onboarding() }
    @Test fun booth() = screen("1-booth", Tab.BOOTH, { it.copy(bought = setOf("GARLAND", "CHAIR", "NEON", "POPCORN"), seance = null) })
    @Test fun seance() = screen("2-seance", Tab.BOOTH, { s ->
        val f = runBlocking { app.container.cinema() }.watch.first { it.title.startsWith(if (ru) "Касаб" else "Casab") }
        s.copy(seance = f.seance().copy(started = System.currentTimeMillis() - 42 * 60_000L))
    })
    @Test fun bill() = screen("3-bill", Tab.BILL)
    @Test fun hall() = screen("4-hall", Tab.HALL)
    @Test fun reward() = screen("5-reward", Tab.BOOTH) { c ->
        val t = c.seats.last()
        Overlay.Reward(t, gains(Cinema(c.titles, c.seats.dropLast(1), c.episodes, c.s, c.now), c, t))
    }
    @Test fun pick() = screen("6-pick", Tab.BOOTH) { Overlay.Pick }
    @Test fun pet() = screen("7-pet", Tab.BOOTH) { Overlay.Pet }
    @Test fun stub() = screen("8-stub", Tab.BILL) { c -> Overlay.Stub(c.seats.first { it.title.startsWith(if (ru) "Бегущ" else "Blade") }.id) }
}

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }
