package app.reelkin.remind

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.reelkin.MainActivity
import app.reelkin.R
import app.reelkin.container
import app.reelkin.data.Premieres
import app.reelkin.data.Title
import app.reelkin.ru
import kotlinx.coroutines.launch

/**
 * The morning reminder of a film coming out (#14). One alarm at a time, set for the nearest day: it rings, says
 * which films are out, and sets the next one. Set again whenever the bill changes, the app starts or the phone boots.
 * Not exact on purpose: within an hour is fine for a morning reminder, and needs no exact-alarm permission.
 */
object Reminders {
    private const val CHANNEL = "premieres"
    private const val WINDOW = 60 * 60_000L

    fun canNotify(ctx: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun told(ctx: Context): suspend (Title) -> Boolean = { t -> ctx.container.settings.done(Premieres.key(t)) }

    /** Sets the alarm for the nearest film coming out, or takes it away when nothing is waiting. */
    suspend fun schedule(ctx: Context, titles: List<Title>) {
        val app = ctx.applicationContext
        val toldOnes = titles.filter { it.premiere > 0 }.filter { told(app)(it) }.map { it.id to it.premiere }.toHashSet()
        val at = Premieres.next(titles, System.currentTimeMillis(), { (it.id to it.premiere) in toldOnes })
        val am = app.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(app, 0, Intent(app, PremiereReceiver::class.java).setAction(RING), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        // A window, not a plain inexact alarm: Android stretches one set weeks ahead by up to three quarters of the wait.
        if (at == null) am.cancel(pi) else am.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW, pi)
    }

    /** Tells of the films out today (or a day or two ago, unheard), once each; then sets the next alarm. */
    suspend fun ring(ctx: Context) {
        val app = ctx.applicationContext
        val c = app.container
        val titles = c.db.dao().titleList()
        val toldOnes = titles.filter { it.premiere > 0 }.filter { told(app)(it) }.map { it.id }.toHashSet()
        val due = Premieres.due(titles, System.currentTimeMillis(), { it.id in toldOnes })
        if (due.isNotEmpty() && canNotify(app)) {
            val ru = app.ru
            val nm = app.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(NotificationChannel(CHANNEL, if (ru) "Премьеры" else "Premieres", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = if (ru) "Утром в день, когда выходит фильм из афиши" else "In the morning of the day a film on your bill comes out"
            })
            val open = PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE)
            for (t in due) {
                val n = NotificationCompat.Builder(app, CHANNEL)
                    .setSmallIcon(R.drawable.ic_stat_premiere)
                    .setContentTitle(if (ru) "«${t.title}» вышел" else "${t.title} is out")
                    .setContentText(if (ru) "Фильм из твоей афиши уже в прокате. Попкорн готов." else "A film from your bill is out now. The popcorn's ready.")
                    .setContentIntent(open).setAutoCancel(true)
                    .build()
                // checked again right here, where lint can see it
                if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                    runCatching { NotificationManagerCompat.from(app).notify(("premiere" + t.id).hashCode(), n) }
            }
        }
        // told even when notifications are off: no pile of old news once they're turned on
        for (t in due) c.settings.markDone(Premieres.key(t))
        schedule(app, c.db.dao().titleList())
    }

    const val RING = "app.reelkin.PREMIERE"
}

/** The alarm, and the phone starting again (alarms don't outlive a reboot). */
class PremiereReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val c = context.container
        c.scope.launch {
            try {
                if (intent.action == Reminders.RING) Reminders.ring(context) else Reminders.schedule(context, c.db.dao().titleList())
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }
}
