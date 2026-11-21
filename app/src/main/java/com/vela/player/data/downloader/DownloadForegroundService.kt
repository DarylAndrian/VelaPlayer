package com.vela.player.data.downloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.vela.player.MainActivity
import com.vela.player.R
import com.vela.player.VelaPlayerApp

/**
 * Foreground service keeping yt-dlp / ffmpeg / OkHttp downloads alive reliably
 * while displaying live progress in the Android notification shade.
 */
class DownloadForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val jobId = intent?.getStringExtra(EXTRA_JOB_ID)
        val initialNotification = buildNotification(
            title = "VelaPlayer Download",
            subtitle = "Preparing download...",
            progress = 0,
            indeterminate = true
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        if (jobId != null) {
            val repo = (application as VelaPlayerApp).downloadRepository
            repo.executeJob(
                jobId = jobId,
                onProgressNotify = { job ->
                    val nm = getSystemService(NotificationManager::class.java)
                    nm?.notify(
                        NOTIFICATION_ID,
                        buildNotification(
                            title = job.title,
                            subtitle = "${job.option.label} • ${job.statusDetail}",
                            progress = job.progress.toInt().coerceIn(0, 100),
                            indeterminate = job.status == DownloadStatus.QUEUED || job.status == DownloadStatus.MERGING
                        )
                    )
                },
                onAllIdle = {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            )
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.download_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.download_channel_desc)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(
        title: String,
        subtitle: String,
        progress: Int,
        indeterminate: Boolean
    ): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_vela)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, indeterminate)
            .setContentIntent(openIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "vela_downloads"
        private const val NOTIFICATION_ID = 4101
        private const val EXTRA_JOB_ID = "extra_job_id"

        fun startForJob(context: Context, jobId: String) {
            val intent = Intent(context, DownloadForegroundService::class.java)
                .putExtra(EXTRA_JOB_ID, jobId)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
