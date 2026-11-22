package to.wim.watchlist.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import to.wim.watchlist.R
import to.wim.watchlist.WatchlistApp
import to.wim.watchlist.data.SubscriptionRepository
import to.wim.watchlist.data.model.SubscriptionRules
import to.wim.watchlist.ui.components.Format
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Reminds the user [SubscriptionRules.REMINDER_DAYS_BEFORE] days before the free trial ends that
 * Google Play will start charging, with a tap to Google Play's page to manage or cancel it. It only
 * asks Google Play (on the phone) whether the subscription still renews, so someone who already
 * cancelled isn't reminded; it makes no network calls of its own.
 */
class TrialReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WatchlistApp
        val end = app.settings.trialEndsAt() ?: return Result.success()
        if (System.currentTimeMillis() >= end) return Result.success()
        // Google Play can't be asked right now: try again later (WorkManager backs off), a few times.
        val renewing = app.subscription.renewing() ?: return if (runAttemptCount < 5) Result.retry() else Result.success()
        // Cancelled already: no reminder.
        if (!renewing) return Result.success()
        notify(applicationContext, end)
        return Result.success()
    }

    companion object {
        private const val WORK = "trial_reminder"
        const val CHANNEL = "subscription"
        private const val NOTIFICATION_ID = 7_001

        fun createChannel(context: Context) {
            val channel =
                NotificationChannel(CHANNEL, context.getString(R.string.sub_notif_channel), NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = context.getString(R.string.sub_notif_channel_description)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        /** Schedules the reminder for a trial that ends at [trialEnd] (epoch ms), replacing an earlier one. */
        fun schedule(
            context: Context,
            trialEnd: Long,
        ) {
            val now = System.currentTimeMillis()
            val at = SubscriptionRules.reminderAt(trialEnd, now) ?: return
            val request =
                OneTimeWorkRequestBuilder<TrialReminderWorker>()
                    .setInitialDelay(at - now, TimeUnit.MILLISECONDS)
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
        }

        @SuppressLint("MissingPermission")
        private fun notify(
            context: Context,
            trialEnd: Long,
        ) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val manage = Intent(Intent.ACTION_VIEW, SubscriptionRepository.manageUrl(context.packageName).toUri())
            val intent = PendingIntent.getActivity(context, NOTIFICATION_ID, manage, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val date = Format.date(Instant.ofEpochMilli(trialEnd))
            val text = context.getString(R.string.sub_notif_text)
            val builder =
                NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.sub_notif_title, date))
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(intent)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }
}
