package com.eisenhower.matrix.reminder

import android.Manifest
import android.annotation.SuppressLint
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
import com.eisenhower.matrix.R
import com.eisenhower.matrix.data.Prefs
import com.eisenhower.matrix.data.Quadrant
import com.eisenhower.matrix.data.TaskStore
import com.eisenhower.matrix.ui.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderScheduler.ACTION_FIRE -> {
                ReminderNotifier.show(context, force = false)
                ReminderScheduler.schedule(context) // programma il successivo
            }
            ReminderScheduler.ACTION_SNOOZE_FIRE -> {
                ReminderNotifier.show(context, force = false)
            }
            ReminderScheduler.ACTION_SNOOZE_TAP -> {
                NotificationManagerCompat.from(context).cancel(ReminderNotifier.NOTIFICATION_ID)
                ReminderScheduler.snooze(context, 60)
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            ReminderScheduler.schedule(context)
        }
    }
}

object ReminderNotifier {
    const val NOTIFICATION_ID = 4001
    private const val CHANNEL_ID = "reminders"

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * Mostra l'avviso con le attività da svolgere.
     * [force] = true ignora l'opzione "solo se ci sono attività" (usato per la prova).
     */
    @SuppressLint("MissingPermission")
    fun show(context: Context, force: Boolean) {
        if (!hasPermission(context)) return

        val pending = TaskStore.all(context)
            .filter { !it.done }
            .sortedWith(compareBy({ it.quadrant }, { it.createdAt }))

        if (pending.isEmpty() && !force && Prefs.reminder(context).onlyIfPending) return

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Promemoria attività", NotificationManager.IMPORTANCE_DEFAULT)
        )

        val title: String
        val text: String
        if (pending.isEmpty()) {
            title = "Matrice di Eisenhower"
            text = "Nessuna attività in sospeso. Ottimo lavoro!"
        } else {
            title = if (pending.size == 1) "Hai 1 attività da svolgere"
            else "Hai ${pending.size} attività da svolgere"
            text = pending.first().title
        }

        val inbox = NotificationCompat.InboxStyle()
        pending.take(5).forEach { inbox.addLine("${Quadrant.from(it.quadrant).dot} ${it.title}") }
        if (pending.size > 5) inbox.setSummaryText("+${pending.size - 5} altre")

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val snooze = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderScheduler.ACTION_SNOOZE_TAP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_grid)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Rimanda di 1 ora", snooze)
        if (pending.isNotEmpty()) builder.setStyle(inbox)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }
}
