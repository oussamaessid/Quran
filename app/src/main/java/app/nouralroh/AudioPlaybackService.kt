package app.nouralroh

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AudioPlaybackService : Service() {

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_TITLE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Quran Audio"
                updateNotification(title)
            }
            else -> {
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Quran Audio"
                startForeground(NOTIF_ID, buildNotification(title))
            }
        }
        return START_STICKY
    }

    private fun buildNotification(title: String): Notification {
        ensureChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Playing…")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("NotificationPermission")
    private fun updateNotification(title: String) {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(NOTIF_ID, buildNotification(title))
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Quran Audio",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    companion object {
        const val NOTIF_ID            = 1001
        const val CHANNEL_ID          = "quran_audio"
        const val ACTION_STOP         = "app.quran.AUDIO_STOP"
        const val ACTION_UPDATE_TITLE = "app.quran.AUDIO_UPDATE_TITLE"
        const val EXTRA_TITLE         = "extra_title"
    }
}