package app.reelkin

import app.reelkin.data.Genre
import app.reelkin.data.Seance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** #21: a film on the projector can be paused and goes on from the same frame. */
@RunWith(RobolectricTestRunner::class)
class SeanceTest {
    private val min = 60_000L
    private val s = Seance("Фильм", 120, Genre.DRAMA, 2020, "", null, 1, false, started = 0)

    @Test fun pauseKeepsThePlace() {
        val p = s.pause(30 * min)
        assertFalse(p.on)
        // an hour of interval and the film is still at 30 minutes
        assertEquals(30 * min, p.played(90 * min))
        val r = p.resume(90 * min)
        assertTrue(r.on)
        assertEquals(30 * min, r.played(90 * min))
        assertEquals(40 * min, r.played(100 * min))
        // pausing twice, or going on while it runs, changes nothing
        assertEquals(p, p.pause(50 * min))
        assertEquals(r, r.resume(120 * min))
    }

    @Test fun survivesSaving() {
        val p = s.pause(30 * min)
        assertEquals(p, Seance.of(p.json()))
        // a seance saved before pauses existed runs
        assertTrue(Seance.of(s.json().replace(",\"p\":0", ""))!!.on)
    }

    @Test fun anEpisodeStaysAnEpisode() {
        val e = s.copy(episode = true, minutes = 45)
        assertEquals(e, Seance.of(e.json()))
        // a seance saved before episodes went on the projector is a film
        assertFalse(Seance.of(s.json().replace(",\"ep\":false", ""))!!.episode)
    }
}
