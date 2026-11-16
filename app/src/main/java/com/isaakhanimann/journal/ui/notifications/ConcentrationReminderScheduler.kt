package com.isaakhanimann.journal.ui.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ConcentrationReminderScheduler {
    private const val PERIODIC_WORK_NAME = "concentration_reminder_check"
    private const val NEXT_THRESHOLD_WORK_NAME = "concentration_reminder_next_threshold"
    private const val ALARM_ACTION = "com.isaakhanimann.journal.CHECK_CONCENTRATION_REMINDER"
    private const val ALARM_REQUEST_CODE = 7101

    fun sync(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            cancelNextThreshold(context)
            return
        }
        val request = PeriodicWorkRequestBuilder<ConcentrationReminderWorker>(
            15,
            TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun runNow(context: Context) {
        context.sendBroadcast(
            Intent(context, ConcentrationReminderAlarmReceiver::class.java).apply {
                action = ALARM_ACTION
            }
        )
    }

    fun scheduleNextThreshold(context: Context, delayMillis: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(NEXT_THRESHOLD_WORK_NAME)
        val triggerAtMillis =
            System.currentTimeMillis() + delayMillis.coerceAtLeast(0L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = alarmPendingIntent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelNextThreshold(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NEXT_THRESHOLD_WORK_NAME)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPendingIntent(context))
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ConcentrationReminderAlarmReceiver::class.java).apply {
            action = ALARM_ACTION
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
