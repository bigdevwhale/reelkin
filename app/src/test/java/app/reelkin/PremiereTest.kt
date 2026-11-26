package app.reelkin

import app.reelkin.data.Catalog
import app.reelkin.data.DAY
import app.reelkin.data.Kind
import app.reelkin.data.Premieres
import app.reelkin.data.Status
import app.reelkin.data.Title
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

/** Films not out yet (#14). Robolectric for org.json. */
@RunWith(RobolectricTestRunner::class)
class PremiereTest {
    private val zone = ZoneId.of("Europe/Moscow")
    private val tz = TimeZone.getTimeZone(zone)
    private fun at(d: LocalDate, h: Int) = d.atTime(h, 0).atZone(zone).toInstant().toEpochMilli()
    private val today = LocalDate.of(2026, 11, 26)
    private val now = at(today, 13)
    private fun film(id: Long, d: LocalDate, status: Status = Status.WATCH, kind: Kind = Kind.FILM) =
        Title(id = id, title = "F$id", kind = kind, status = status, premiere = Premieres.of(d.year, d.monthValue, d.dayOfMonth))

    @Test fun aheadUntilItsDay() {
        assertTrue(Premieres.ahead(film(1, today.plusDays(1)), now, tz))
        assertFalse(Premieres.ahead(film(1, today), now, tz))
        assertFalse(Premieres.ahead(Title(title = "x"), now, tz))
        assertFalse(Premieres.ahead(film(1, today.plusDays(5), kind = Kind.SERIES), now, tz))
        // late in the evening in Moscow it's still the same day, though UTC is past midnight elsewhere
        assertTrue(Premieres.ahead(film(1, today.plusDays(1)), at(today, 23), tz))
        assertEquals(listOf(2L, 1L), Premieres.soon(listOf(film(1, today.plusDays(9)), film(2, today.plusDays(2)), film(3, today.minusDays(1))), now, tz).map { it.id })
    }

    @Test fun ringsInTheMorningOfTheNearestDay() {
        val titles = listOf(film(1, today.plusDays(9)), film(2, today.plusDays(2)), film(3, today.plusDays(1), status = Status.SHELVED))
        assertEquals(at(today.plusDays(2), Premieres.HOUR), Premieres.next(titles, now, { false }, zone))
        assertEquals(at(today.plusDays(9), Premieres.HOUR), Premieres.next(titles, now, { it.id == 2L }, zone))
        assertNull(Premieres.next(titles, now, { true }, zone))
        // a morning missed: ring in a minute; one long past isn't news any more
        assertEquals(now + 60_000L, Premieres.next(listOf(film(4, today.minusDays(1))), now, { false }, zone))
        assertNull(Premieres.next(listOf(film(4, today.minusDays(5))), now, { false }, zone))
        assertEquals(listOf(4L, 5L), Premieres.due(listOf(film(4, today.minusDays(1)), film(5, today), film(6, today.plusDays(1)), film(7, today.minusDays(4))), now, { false }, tz).map { it.id })
        assertTrue(Premieres.key(film(1, today)) != Premieres.key(film(1, today.plusDays(1))))
    }

    @Test fun theCataloguesDayInRussiaFirst() {
        fun date(t: String, precision: Int = 11, place: String? = null) = JSONObject().put("mainsnak", JSONObject().put("datavalue", JSONObject()
            .put("value", JSONObject().put("time", t).put("precision", precision))))
            .apply { if (place != null) put("qualifiers", JSONObject().put("P291", org.json.JSONArray().put(JSONObject().put("datavalue", JSONObject().put("value", JSONObject().put("id", place)))))) }
        val claims = JSONObject().put("P577", org.json.JSONArray()
            .put(date("+2026-12-16T00:00:00Z", place = "Q30"))
            .put(date("+2026-12-18T00:00:00Z", place = "Q159"))
            .put(date("+2026-12-01T00:00:00Z", precision = 10)))
        assertEquals(Premieres.of(2026, 12, 18), Catalog.premiere(claims, ru = true, now = now))
        assertEquals(Premieres.of(2026, 12, 16), Catalog.premiere(claims, ru = false, now = now))
        // out already
        assertNull(Catalog.premiere(claims, ru = true, now = now + 40 * DAY))
        assertNull(Catalog.premiere(JSONObject(), ru = true, now = now))
    }
}
