package app.reelkin.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import app.reelkin.BuildConfig
import app.reelkin.ReelkinApp
import app.reelkin.AppContainer
import app.reelkin.data.Species
import app.reelkin.data.Stage
import app.reelkin.domain.Pets
import app.reelkin.domain.Words
import app.reelkin.ui.art.hashI
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The bridge to Lingvale, the village where the pets of all the -ling apps live together.
 *
 * Lingvale asks through ContentResolver.call() and nobody else gets an answer: the caller must be Lingvale signed
 * with its release key (a debug or demo build also answers a Lingvale built with the same debug key). It only reads;
 * nothing here changes the app's data. Protocol v1, the same in every -ling app:
 *  - "hello"    → {v}
 *  - "snapshot" → {json}: pets, species, days of activity, the last week's events
 *  - "art"      → {bytes}: one picture from res/drawable-nodpi (pet_*, egg_*, item_*, *_bg)
 */
class VillageProvider : ContentProvider() {
    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        if (!trusted(ctx)) throw SecurityException("Only Lingvale may ask")
        return when (method) {
            "hello" -> Bundle().apply { putInt("v", VERSION) }
            "snapshot" -> Bundle().apply { putString("json", runBlocking { snapshot(ctx, container(ctx)) }.toString()) }
            "art" -> art(ctx, arg)
            else -> null
        }
    }

    private fun trusted(ctx: Context): Boolean {
        val pkg = callingPackage ?: return false
        if (pkg != HUB && !pkg.startsWith("$HUB.")) return false
        val pm = ctx.packageManager
        if (HUB_CERTS.any { pm.hasSigningCertificate(pkg, it, PackageManager.CERT_INPUT_SHA256) }) return true
        return BuildConfig.DEBUG && pm.checkSignatures(pkg, ctx.packageName) == PackageManager.SIGNATURE_MATCH
    }

    private fun art(ctx: Context, name: String?): Bundle? {
        if (name == null || !ART.matches(name)) return null
        val bytes = resArt(name) ?: return null
        return Bundle().apply { putByteArray("bytes", bytes) }
    }

    /** The provider can be asked before the application has finished starting; wait for it a little. */
    private fun container(ctx: Context): AppContainer {
        repeat(100) {
            runCatching { return (ctx.applicationContext as ReelkinApp).container }
            Thread.sleep(30)
        }
        error("not ready")
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    companion object {
        const val VERSION = 1
        const val HUB = "app.lingvale"
        /** SHA-256 of Lingvale's release certificate. */
        private val HUB_CERTS = listOf(hex("CEF40A1786896D829CFA53115693AC9A53FC0349EAB84BAF64EFA648592D1ABF"))
        private val ART = Regex("""(pet|egg|item|find)_[a-z0-9_]+|[a-z]+_bg""")
        private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        private fun color(c: Long) = "#%06X".format(c and 0xFFFFFF)
        private fun dayOf(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

        /** Reelkin's pets live in java resources (/pets/<kind>_<stage>.png), the booth in /booth/room.png. */
        private fun resArt(name: String): ByteArray? {
            val path = when {
                name.startsWith("egg_") -> "/pets/${name.removePrefix("egg_")}_egg.png"
                name.startsWith("pet_") -> {
                    val sp = name.removePrefix("pet_").substringBeforeLast('_')
                    val stage = when (name.substringAfterLast('_')) { "0" -> "baby"; "1" -> "young"; else -> "adult" }
                    "/pets/${sp}_$stage.png"
                }
                name == "room_bg" -> "/booth/room.png"
                else -> return null
            }
            return VillageProvider::class.java.getResourceAsStream(path)?.use { it.readBytes() }
        }

        private val COLORS = listOf(0xC8553D, 0x3E8FB0, 0x5E8A4E, 0xD99A2B, 0x7A5BB5, 0xB5523E, 0x4A6FA5, 0x8A6440)

        /** Everything Lingvale shows of Reelkin: films and episodes as days, pets by what they've watched. */
        suspend fun snapshot(ctx: Context, c: AppContainer): JSONObject {
            val dao = c.db.dao()
            val cinema = c.cinema()
            val watches = cinema.watches
            val today = LocalDate.now().toEpochDay()
            val inBooth = cinema.petList.filter { it.spans.endsWith("-0") }.sortedBy { it.laid }

            val petsJson = JSONArray()
            cinema.petList.forEach { p ->
                val st = Pets.state(p, watches)
                val slot = inBooth.indexOfFirst { it.id == p.id }.takeIf { it in 0..2 } ?: -1
                petsJson.put(JSONObject()
                    .put("id", p.id).put("species", p.species.file).put("name", p.name)
                    .put("stage", if (!p.hatched && st.stage == Stage.EGG) 0 else st.stage.ordinal).put("progress", st.progress.toDouble())
                    .put("slot", slot).put("shiny", p.shiny).put("found", dayOf(p.laid)).put("hatched", if (st.stage == Stage.EGG) 0 else dayOf(p.laid)))
            }
            val speciesJson = JSONArray()
            Species.entries.forEachIndexed { i, s ->
                speciesJson.put(JSONObject().put("id", s.file).put("ru", Words.species(s, true)).put("en", Words.species(s, false))
                    .put("color", color(COLORS[(hashI(s.name)) % COLORS.size].toLong())).put("aboutRu", "любит " + Words.loves(s, true)).put("aboutEn", "loves " + Words.loves(s, false)).put("rare", i >= 18))
            }
            val daysJson = JSONArray()
            watches.groupBy { dayOf(it.at) }.toSortedMap().forEach { (d, l) ->
                if (d > today - 400) daysJson.put(JSONObject().put("d", d).put("n", l.size).put("min", l.sumOf { it.minutes }))
            }
            val titles = dao.titleList().associateBy { it.id }
            val eventsJson = JSONArray()
            cinema.tickets.filter { dayOf(it.at) > today - 8 }.forEach { t ->
                eventsJson.put(JSONObject().put("at", t.at).put("min", t.minutes).put("ru", t.title).put("en", t.title))
            }
            cinema.episodes.filter { dayOf(it.at) > today - 8 }.forEach { e ->
                val name = titles[e.series]?.title.orEmpty()
                val line = "$name · S${e.season}E${e.number}"
                eventsJson.put(JSONObject().put("at", e.at).put("min", e.minutes).put("ru", line).put("en", line))
            }
            return JSONObject()
                .put("v", VERSION).put("app", "reelkin").put("version", BuildConfig.VERSION_NAME)
                .put("ru", true).put("scene", "room_bg")
                .put("pets", petsJson).put("species", speciesJson).put("days", daysJson).put("events", eventsJson)
        }
    }
}
