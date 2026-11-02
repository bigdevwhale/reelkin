package app.reelkin.data

import android.content.Context
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
    @Insert suspend fun insertTitles(t: List<Title>)
    @Update suspend fun update(t: Title)
    @Delete suspend fun delete(t: Title)

    @Query("SELECT * FROM tickets ORDER BY at")
    fun tickets(): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets ORDER BY at")
    suspend fun ticketList(): List<Ticket>

    @Insert suspend fun insert(t: Ticket): Long
    @Insert suspend fun insertTickets(t: List<Ticket>)
    @Update suspend fun update(t: Ticket)
    @Delete suspend fun delete(t: Ticket)

    @Query("SELECT * FROM episodes ORDER BY at")
    fun episodes(): Flow<List<Episode>>

    @Insert suspend fun insert(e: Episode): Long
    @Insert suspend fun insertEpisodes(e: List<Episode>)

    @Query("SELECT * FROM episodes WHERE series = :id ORDER BY at DESC LIMIT 1")
    suspend fun lastEpisode(id: Long): Episode?

    @Delete suspend fun delete(e: Episode)

    @Query("SELECT COUNT(*) FROM tickets")
    suspend fun ticketCount(): Int

    @Query("SELECT COUNT(*) FROM titles")
    suspend fun titleCount(): Int
}

@Database(entities = [Title::class, Ticket::class, Episode::class], version = 1, exportSchema = true)
abstract class CinemaDb : RoomDatabase() {
    abstract fun dao(): CinemaDao

    companion object {
        fun create(context: Context): CinemaDb = Room.databaseBuilder(context, CinemaDb::class.java, "reelkin.db").build()
    }
}
