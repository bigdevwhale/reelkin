package app.reelkin.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore("settings")

/** A film on the projector right now. It may be a title from the bill or a rewatch of a stub. */
data class Seance(
    val title: String,
    val minutes: Int,
    val genre: Genre,
    val year: Int?,
    val country: String,
    val cover: String?,
    val titleId: Long,
    val rewatch: Boolean,
    val started: Long,
) {
    fun json(): String = JSONObject().put("t", title).put("m", minutes).put("g", genre.name).put("y", year ?: 0).put("c", country)
        .put("cv", cover ?: "").put("id", titleId).put("re", rewatch).put("s", started).toString()

    companion object {
        fun of(s: String?): Seance? = runCatching {
            val o = JSONObject(s ?: return null)
            Seance(o.getString("t"), o.getInt("m"), Genre.valueOf(o.getString("g")), o.optInt("y").takeIf { it > 0 }, o.optString("c"),
                o.optString("cv").ifBlank { null }, o.optLong("id", -1), o.optBoolean("re"), o.getLong("s"))
        }.getOrNull()
    }
}

/** A statuette on the shelf: the year, the category and the film. */
data class Award(val year: Int, val cat: Int, val title: String)

data class Settings(
    val onboarded: Boolean = false,
    /** Empty means the default name in the phone's language. */
    val petName: String = "",
    /** The pet of 1.x, kept only to bring it into the pets table once. */
    val species: Species? = null,
    val shiny: Boolean = false,
    /** The pet in the booth (Pet.id); 0 for the last one taken out. */
    val pet: Long = 0,
    /** First-visit hints already read: Tip names. */
    val tips: Set<String> = emptySet(),
    /** The booth's wallpaper: a Wall name. */
    val wall: String = "",
    /** Weekly tasks whose popcorn was taken, as «week:TASK». */
    val claimed: Set<String> = emptySet(),
    /** The name on the sign, built from the letters you own. */
    val cinema: String = "",
    /** Popcorn spent in the buffet. */
    val spent: Int = 0,
    val bought: Set<String> = emptySet(),
    val awards: List<Award> = emptyList(),
    val goal: Int = 52,
    /** Seasons of series to finish this year; 0 = no such goal (#6). */
    val goalSeasons: Int = 0,
    /** Posters in lists redrawn in pixels, or shown as they are. */
    val pixelCovers: Boolean = false,
    val seance: Seance? = null,
    /** The picture behind the screens: see Backdrops. Empty for none. */
    val backdrop: String = "",
    /** Changes when a picture of one's own is picked, so it's read again. */
    val backdropStamp: Long = 0,
    /** How the stubs are ordered on the bill: a Browse.Sort name. */
    val stubSort: String = "",
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        petName = this[PET] ?: "",
        species = this[SPECIES]?.let { s -> runCatching { Species.valueOf(s) }.getOrNull() },
        shiny = this[SHINY] ?: false,
        cinema = this[CINEMA] ?: "",
        spent = this[SPENT] ?: 0,
        bought = this[BOUGHT]?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
        pet = this[PET_ID] ?: 0L,
        wall = this[WALL] ?: "",
        tips = this[TIPS]?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
        claimed = this[CLAIMED]?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
        awards = this[AWARDS]?.lines()?.mapNotNull { l -> l.split('\t').takeIf { it.size == 3 }?.let { Award(it[0].toInt(), it[1].toInt(), it[2]) } } ?: emptyList(),
        goal = this[GOAL] ?: 52,
        goalSeasons = this[GOAL_SEASONS] ?: 0,
        pixelCovers = this[PIXEL] ?: false,
        seance = Seance.of(this[SEANCE]),
        backdrop = this[BACKDROP] ?: "",
        backdropStamp = this[BACKDROP_STAMP] ?: 0L,
        stubSort = this[STUB_SORT] ?: "",
    )

    suspend fun now(): Settings = settings.first()

    /** A one-off chore done once per install, such as a fix after an update. */
    suspend fun done(chore: String): Boolean = store.data.first()[booleanPreferencesKey("done_$chore")] ?: false
    suspend fun markDone(chore: String) { store.edit { it[booleanPreferencesKey("done_$chore")] = true } }

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[PET] = s.petName
            if (s.species != null) it[SPECIES] = s.species.name else it.remove(SPECIES)
            it[SHINY] = s.shiny
            it[CINEMA] = s.cinema
            it[SPENT] = s.spent
            it[BOUGHT] = s.bought.sorted().joinToString(",")
            it[PET_ID] = s.pet
            it[WALL] = s.wall
            it[TIPS] = s.tips.sorted().joinToString(",")
            it[CLAIMED] = s.claimed.sorted().joinToString(",")
            it[AWARDS] = s.awards.joinToString("\n") { a -> "${a.year}\t${a.cat}\t${a.title.replace('\t', ' ').replace('\n', ' ')}" }
            it[GOAL] = s.goal
            it[GOAL_SEASONS] = s.goalSeasons
            it[PIXEL] = s.pixelCovers
            if (s.seance != null) it[SEANCE] = s.seance.json() else it.remove(SEANCE)
            it[BACKDROP] = s.backdrop
            it[BACKDROP_STAMP] = s.backdropStamp
            it[STUB_SORT] = s.stubSort
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val PET = stringPreferencesKey("pet")
        val SPECIES = stringPreferencesKey("species")
        val SHINY = booleanPreferencesKey("shiny")
        val CINEMA = stringPreferencesKey("cinema")
        val SPENT = intPreferencesKey("spent")
        val BOUGHT = stringPreferencesKey("bought")
        val CLAIMED = stringPreferencesKey("claimed")
        val WALL = stringPreferencesKey("wall")
        val TIPS = stringPreferencesKey("tips")
        val PET_ID = androidx.datastore.preferences.core.longPreferencesKey("pet_id")
        val AWARDS = stringPreferencesKey("awards")
        val GOAL = intPreferencesKey("goal")
        val GOAL_SEASONS = intPreferencesKey("goal_seasons")
        // "pixel" was written by every save while pixels were the default, so the choice starts afresh under a new key.
        val PIXEL = booleanPreferencesKey("pixel_covers")
        val SEANCE = stringPreferencesKey("seance")
        val BACKDROP = stringPreferencesKey("backdrop")
        val BACKDROP_STAMP = androidx.datastore.preferences.core.longPreferencesKey("backdrop_stamp")
        val STUB_SORT = stringPreferencesKey("stub_sort")
    }
}
