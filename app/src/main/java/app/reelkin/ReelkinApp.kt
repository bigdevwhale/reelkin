package app.reelkin

import android.app.Application
import android.content.Context
import app.reelkin.data.BoxOffice
import app.reelkin.data.CinemaDb
import app.reelkin.data.DemoSeed
import app.reelkin.data.SettingsStore
import app.reelkin.domain.Cinema
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = CinemaDb.create(app)
    val settings = SettingsStore(app)
    val office = BoxOffice(app, db.dao(), settings)
    /** On the main thread: Room and DataStore are main-safe, and screens' state is written from here. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The cinema as it is in the store right now, for comparing before and after a stub. */
    suspend fun cinema(): Cinema = Cinema(db.dao().titles().first(), db.dao().ticketList(), db.dao().episodes().first(), settings.now(), System.currentTimeMillis(), db.dao().petList(), db.dao().goalList())

    /** True while the demo build fills its cinema, so the first screen doesn't jump. */
    val seeding = MutableStateFlow(BuildConfig.DEMO)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class ReelkinApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (BuildConfig.DEMO) container.scope.launch { try { DemoSeed.seedIfEmpty(this@ReelkinApp, container) } finally { container.seeding.value = false } }
        else container.scope.launch { runCatching { container.office.mendCovers() } }
    }
}

val Context.container: AppContainer get() = (applicationContext as ReelkinApp).container

val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
