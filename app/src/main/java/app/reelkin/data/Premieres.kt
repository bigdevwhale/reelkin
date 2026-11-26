package app.reelkin.data

import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

/**
 * Films not out yet (#14): a film on the bill can carry the day it comes out ([Title.premiere], as that day's UTC
 * midnight). Until then it waits on the bill under «coming soon», and on the morning of the day a reminder rings.
 */
object Premieres {
    /** The hour of the morning the reminder rings at. */
    const val HOUR = 10

    /** How many days late a reminder still makes sense: the phone was off that morning. */
    private const val LATE = 2

    /** The local day [now] falls on, counted from 1970-01-01. */
    fun today(now: Long, tz: TimeZone = TimeZone.getDefault()): Long = (now + tz.getOffset(now)).floorDiv(DAY)

    fun dayOf(premiere: Long): Long = premiere.floorDiv(DAY)

    /** The premiere value of a calendar date ([month] from 1). */
    fun of(year: Int, month: Int, day: Int): Long = LocalDate.of(year, month, day).toEpochDay() * DAY

    /** A film whose day hasn't come yet. */
    fun ahead(t: Title, now: Long, tz: TimeZone = TimeZone.getDefault()): Boolean =
        t.kind == Kind.FILM && t.premiere > 0 && dayOf(t.premiere) > today(now, tz)

    /** The films still waiting to come out, the nearest first. */
    fun soon(titles: List<Title>, now: Long, tz: TimeZone = TimeZone.getDefault()): List<Title> =
        titles.filter { it.status == Status.WATCH && ahead(it, now, tz) }.sortedBy { it.premiere }

    /** What a reminder is remembered by: the film and its day, so a film moved to another day rings again. */
    fun key(t: Title): String = "premiere_${t.id}_${dayOf(t.premiere)}"

    /** Films waiting on the bill that come out today, or came out a day or two ago, and weren't reminded of yet. */
    fun due(titles: List<Title>, now: Long, told: (Title) -> Boolean, tz: TimeZone = TimeZone.getDefault()): List<Title> {
        val d = today(now, tz)
        return titles.filter { it.kind == Kind.FILM && it.status == Status.WATCH && it.premiere > 0 && dayOf(it.premiere) in (d - LATE)..d && !told(it) }
    }

    /**
     * When to ring next: [HOUR] in the morning of the nearest day still to be reminded of, or in a minute when one
     * is due already (its morning passed while nothing was set). Null when nothing is waiting.
     */
    fun next(titles: List<Title>, now: Long, told: (Title) -> Boolean, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val tz = TimeZone.getTimeZone(zone)
        val d = today(now, tz)
        val day = titles.filter { it.kind == Kind.FILM && it.status == Status.WATCH && it.premiere > 0 && dayOf(it.premiere) >= d - LATE && !told(it) }
            .minOfOrNull { dayOf(it.premiere) } ?: return null
        val at = LocalDate.ofEpochDay(day).atTime(HOUR, 0).atZone(zone).toInstant().toEpochMilli()
        return if (at > now) at else now + 60_000L
    }
}
