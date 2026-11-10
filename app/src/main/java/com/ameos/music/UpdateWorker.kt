package com.ameos.music

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

// Every few hours (with internet) this looks at GitHub. When a newer VAME is out, you get one
// notification for that version. Tapping it opens the app, which then shows the update dialog.
// Settings > Storage and about > Updates > Update notifications turns it off.
class UpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val c = applicationContext
        if (!enabled(c)) return Result.success()
        try {
            val info = Updater.latest() ?: return Result.success()
            if (!Updater.isNewer(info.version, Updater.currentVersion(c))) return Result.success()
            val sp = c.getSharedPreferences("vame", Context.MODE_PRIVATE)
            if (sp.getString("notifiedVersion", "") == info.version) return Result.success()
            if (notify(c, info)) sp.edit().putString("notifiedVersion", info.version).apply()
        } catch (e: Exception) {
            return Result.retry()
        }
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "vame_updates"
        private const val NAME = "vame_update_check"

        fun enabled(c: Context): Boolean =
            c.getSharedPreferences("vame", Context.MODE_PRIVATE).getBoolean("updateNotify", true)

        fun setEnabled(c: Context, on: Boolean) {
            c.getSharedPreferences("vame", Context.MODE_PRIVATE).edit().putBoolean("updateNotify", on).apply()
        }

        fun schedule(c: Context) {
            try {
                val req = PeriodicWorkRequestBuilder<UpdateWorker>(3, TimeUnit.HOURS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                WorkManager.getInstance(c).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
            } catch (e: Exception) {
            }
        }

        private fun notify(c: Context, info: Updater.Info): Boolean {
            if (Build.VERSION.SDK_INT >= 33 &&
                c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return false
            val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "App updates", NotificationManager.IMPORTANCE_DEFAULT)
            )
            val open = Intent(c, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                c, 71, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_w_play)
                .setContentTitle("VAME " + info.version + " is out")
                .setContentText("Tap to update")
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    if (info.notes.isNotBlank()) info.notes.take(300) else "A new version of VAME is ready. Tap to update."
                ))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            nm.notify(7101, n)
            return true
        }
    }
}
