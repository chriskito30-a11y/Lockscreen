package fr.magiclockscreen.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

class MagicListenService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var listenJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopListening()
            else -> startListening(intent)
        }
        return START_STICKY
    }

    private fun startListening(intent: Intent?) {
        val backend = intent?.getStringExtra(EXTRA_BACKEND)?.takeIf { it.isNotBlank() } ?: MagicPrefs.backend(this)
        val token = intent?.getStringExtra(EXTRA_TOKEN)?.takeIf { it.isNotBlank() } ?: MagicPrefs.token(this)
        val intervalSeconds = intent?.getIntExtra(EXTRA_INTERVAL_SECONDS, MagicPrefs.intervalSeconds(this)) ?: MagicPrefs.intervalSeconds(this)
        val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, MagicPrefs.durationMinutes(this)) ?: MagicPrefs.durationMinutes(this)

        if (backend.isBlank() || token.isBlank()) {
            stopSelf()
            return
        }

        MagicPrefs.saveConfig(this, backend, token, intervalSeconds, durationMinutes)
        MagicPrefs.setListening(this, true)
        startForeground(NOTIFICATION_ID, buildNotification("Écoute active"))

        listenJob?.cancel()
        listenJob = scope.launch {
            val endAt = System.currentTimeMillis() + max(1, durationMinutes) * 60_000L
            val delayMs = max(1, intervalSeconds) * 1000L
            var lastHash = MagicPrefs.lastHash(applicationContext)

            while (isActive && System.currentTimeMillis() < endAt) {
                try {
                    val current = MagicWallpaperClient.fetchValue(backend, token)
                    if (current.hash.isNotBlank() && current.hash != lastHash) {
                        acquireShortWakeLock()
                        val updated = MagicWallpaperClient.updateLockscreen(applicationContext, backend, token)
                        lastHash = updated.hash
                        MagicPrefs.setLastHash(applicationContext, updated.hash)
                        notifyStatus("Lockscreen mis à jour : ${updated.value.take(28)}")
                    } else {
                        notifyStatus("Écoute active : ${current.value.take(28)}")
                    }
                } catch (e: Exception) {
                    notifyStatus("Erreur : ${e.message ?: "mise à jour impossible"}")
                }
                delay(delayMs)
            }
            stopListening()
        }
    }

    private fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        MagicPrefs.setListening(this, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notifyStatus(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MagicListenService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_magic_tile)
            .setContentTitle("Magic Lockscreen")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_magic_tile, "Stop", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Magic Lockscreen écoute",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification silencieuse nécessaire pour écouter l’URL écran verrouillé."
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun acquireShortWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MagicLockscreen:Update")
            wakeLock.acquire(20_000L)
            wakeLock.release()
        } catch (_: Exception) {
            // Best effort only.
        }
    }

    override fun onDestroy() {
        listenJob?.cancel()
        MagicPrefs.setListening(this, false)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "magic_lockscreen_listen"
        private const val NOTIFICATION_ID = 15999
        const val ACTION_STOP = "fr.magiclockscreen.android.STOP"
        const val EXTRA_BACKEND = "backend"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_INTERVAL_SECONDS = "interval_seconds"
        const val EXTRA_DURATION_MINUTES = "duration_minutes"

        fun start(context: Context, backend: String, token: String, intervalSeconds: Int, durationMinutes: Int) {
            val intent = Intent(context, MagicListenService::class.java).apply {
                putExtra(EXTRA_BACKEND, backend)
                putExtra(EXTRA_TOKEN, token)
                putExtra(EXTRA_INTERVAL_SECONDS, intervalSeconds)
                putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MagicListenService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
