package app.nouralroh.adhan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.nouralroh.MainActivity
import app.nouralroh.R

class AdhanPlaybackService : Service() {
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change < 0) stopPlayback()
    }
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlayback()
            return START_NOT_STICKY
        }
        if (!AdhanPreferences.isEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val prayer = intent?.getStringExtra(EXTRA_PRAYER) ?: "Salat"
        createChannel()
        startForeground(NOTIFICATION_ID, notification(prayer))
        val mode = AdhanPreferences.soundMode(this)
        if (mode == "silent") {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID + 1, notification(prayer, false))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!requestAudioFocus()) {
            stopPlayback()
            return START_NOT_STICKY
        }
        player?.release()
        val audioFile = resources.openRawResourceFd(R.raw.adhan)
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            setDataSource(audioFile.fileDescriptor, audioFile.startOffset, audioFile.length)
            audioFile.close()
            setOnCompletionListener { stopPlayback() }
            setOnErrorListener { _, _, _ -> stopPlayback(); true }
            prepare()
            start()
        }
        if (mode == "short") handler.postDelayed({ stopPlayback() }, 8_000L)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        abandonAudioFocus()
        super.onDestroy()
    }

    private fun stopPlayback() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        abandonAudioFocus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun requestAudioFocus(): Boolean {
        val manager = getSystemService(AudioManager::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            return manager.requestAudioFocus(
                focusListener, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener).build()
        return manager.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        val manager = getSystemService(AudioManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { manager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(focusListener)
        }
        focusRequest = null
    }

    private fun notification(prayer: String, ongoing: Boolean = true) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("Adhan · $prayer")
        .setContentText(getString(R.string.adhan_time))
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .addAction(
            android.R.drawable.ic_media_pause,
            getString(R.string.adhan_stop),
            PendingIntent.getService(
                this, 1, Intent(this, AdhanPlaybackService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .setOngoing(ongoing)
        .setAutoCancel(!ongoing)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Adhan", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = getString(R.string.adhan_channel_desc)
                    setSound(null, null)
                },
            )
        }
    }

    companion object {
        const val EXTRA_PRAYER = "prayer_name"
        const val ACTION_STOP = "app.nouralroh.STOP_ADHAN"
        private const val CHANNEL_ID = "adhan_playback"
        private const val NOTIFICATION_ID = 2201
    }
}
