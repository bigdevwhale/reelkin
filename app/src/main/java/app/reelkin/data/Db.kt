package app.reelkin.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CinemaDao {
    @Query("SELECT * FROM titles ORDER BY added DESC")
    fun titles(): Flow<List<Title>>

    @Query("SELECT * FROM titles WHERE id = :id")
    suspend fun title(id: Long): Title?

    @Insert suspend fun insert(t: Title): Long
    @Insert suspend fun insertTitles(t: List<Title>): List<Long>
    @Update suspend fun update(t: Title)
    @Delete suspend fun delete(t: Title)

    @Query("SELECT * FROM tickets ORDER BY at")
    fun tickets(): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets ORDER BY at")
    suspend fun ticketList(): List<Ticket>

    @Insert suspend fun insert(t: Ticket): Long
    @Insert suspend fun insertTickets(t: List<Ticket>): List<Long>

    @Query("SELECT * FROM titles")
    suspend fun titleList(): List<Title>

    @Query("UPDATE titles SET plan = :plan, planAt = :at, show = :show WHERE id = :id")
    suspend fun setPlan(id: Long, plan: String, at: Long, show: String?)

    @Query("UPDATE titles SET cover = :cover WHERE id IN (:ids) AND cover IS NULL")
    suspend fun coverTitles(ids: List<Long>, cover: String)

    @Query("UPDATE tickets SET cover = :cover WHERE id IN (:ids) AND cover IS NULL")
    suspend fun coverTickets(ids: List<Long>, cover: String)
    /** Posters found for an import, written in one go: (poster, title ids, stub ids). */
    @androidx.room.Transaction
    suspend fun coverAll(found: List<Triple<String, List<Long>, List<Long>>>) {
        for ((name, titles, tickets) in found) {
            if (titles.isNotEmpty()) coverTitles(titles, name)
            if (tickets.isNotEmpty()) coverTickets(tickets, name)
        }
    }

    @Update suspend fun update(t: Ticket)
    @Delete suspend fun delete(t: Ticket)
    @Delete suspend fun deleteTickets(t: List<Ticket>)

    @Query("SELECT * FROM episodes ORDER BY at")
    fun episodes(): Flow<List<Episode>>

    @Insert suspend fun insert(e: Episode): Long
    @Insert suspend fun insertEpisodes(e: List<Episode>)

    @Query("SELECT * FROM episodes WHERE series = :id ORDER BY at DESC LIMIT 1")
    suspend fun lastEpisode(id: Long): Episode?

    @Delete suspend fun delete(e: Episode)

    @Query("SELECT COUNT(*) FROM tickets")
    suspend fun ticketCount(): Int

    @Query("SELECT * FROM pets ORDER BY id")
    fun pets(): Flow<List<Pet>>

    @Query("SELECT * FROM pets ORDER BY id")
    suspend fun petList(): List<Pet>

    @Insert suspend fun insert(p: Pet): Long
    @Update suspend fun update(p: Pet)

    @Query("SELECT * FROM goals ORDER BY id")
    fun goals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals ORDER BY id")
    suspend fun goalList(): List<Goal>

    @Insert suspend fun insert(g: Goal): Long
    @Update suspend fun update(g: Goal)
    @Delete suspend fun delete(g: Goal)

    @Query("SELECT COUNT(*) FROM titles")
    suspend fun titleCount(): Int
}

@Database(entities = [Title::class, Ticket::class, Episode::class, Pet::class, Goal::class], version = 7, exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6), AutoMigration(from = 6, to = 7)])
abstract class CinemaDb : RoomDatabase() {
    abstract fun dao(): CinemaDao

    companion object {
        fun create(context: Context): CinemaDb = Room.databaseBuilder(context, CinemaDb::class.java, "reelkin.db").build()
    }
}
