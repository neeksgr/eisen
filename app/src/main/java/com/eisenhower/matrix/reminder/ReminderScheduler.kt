package com.eisenhower.matrix.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.eisenhower.matrix.data.Prefs
import com.eisenhower.matrix.data.ReminderConfig
import java.util.Calendar

object ReminderScheduler {
    const val ACTION_FIRE = "com.eisenhower.matrix.REMINDER_FIRE"
    const val ACTION_SNOOZE_TAP = "com.eisenhower.matrix.REMINDER_SNOOZE_TAP"
    const val ACTION_SNOOZE_FIRE = "com.eisenhower.matrix.REMINDER_SNOOZE_FIRE"

    private const val REQ_MAIN = 101
    private const val REQ_SNOOZE = 102

    /** Calcola il prossimo istante di avviso a partire da [now]. */
    fun nextTrigger(cfg: ReminderConfig, now: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        return if (cfg.mode == ReminderConfig.MODE_DAILY) {
            cal.set(Calendar.HOUR_OF_DAY, cfg.hour)
            cal.set(Calendar.MINUTE, cfg.minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.timeInMillis
        } else {
            cal.timeInMillis = now + cfg.everyHours * 3_600_000L
            val h = cal.get(Calendar.HOUR_OF_DAY)
            when {
                h >= ReminderConfig.WINDOW_END -> {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    cal.set(Calendar.HOUR_OF_DAY, ReminderConfig.WINDOW_START)
                    cal.set(Calendar.MINUTE, 0)
                }
                h < ReminderConfig.WINDOW_START -> {
                    cal.set(Calendar.HOUR_OF_DAY, ReminderConfig.WINDOW_START)
                    cal.set(Calendar.MINUTE, 0)
                }
            }
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
    }

    /** Legge le impostazioni e (ri)programma il prossimo avviso. */
    fun schedule(context: Context) {
        val cfg = Prefs.reminder(context)
        cancelMain(context)
        if (!cfg.enabled) {
            // Disattivato: annulla anche un eventuale "rimanda" ancora in sospeso
            context.getSystemService(AlarmManager::class.java)
                .cancel(pending(context, REQ_SNOOZE, ACTION_SNOOZE_FIRE))
            return
        }
        setAlarm(context, nextTrigger(cfg, System.currentTimeMillis()), REQ_MAIN, ACTION_FIRE)
    }

    fun snooze(context: Context, minutes: Int) {
        setAlarm(
            context,
            System.currentTimeMillis() + minutes * 60_000L,
            REQ_SNOOZE,
            ACTION_SNOOZE_FIRE
        )
    }

    private fun cancelMain(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(pending(context, REQ_MAIN, ACTION_FIRE))
    }

    private fun setAlarm(context: Context, at: Long, req: Int, action: String) {
        val am = context.getSystemService(AlarmManager::class.java)
        // Allarme inesatto ma affidabile anche in Doze: non richiede permessi speciali.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context, req, action))
    }

    private fun pending(context: Context, req: Int, action: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            req,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
