package fr.magiclockscreen.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import java.util.concurrent.atomic.AtomicBoolean

class MagicListenService : Service() {
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopListening()
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startListening()

        return START_STICKY
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }

    private fun startListening() {
        if (running.getAndSet(true)) return

        val appContext = applicationContext
        val backend = MagicPrefs.backend(appContext)
        val token = MagicPrefs.token(appContext)
        val intervalMs = MagicPrefs.intervalSeconds(appContext).coerceIn(1, 60) * 1000L
        val durationMs = MagicPrefs.durationMinutes(appContext).coerceIn(1, 240) * 60_000L
        val endAt = System.currentTimeMillis() + durationMs

        worker = Thread {
            while (running.get() && System.currentTimeMillis() < endAt) {
                try {
                    val current = MagicWallpaperClient.fetchValue(backend, token)
                    val lastHash = MagicPrefs.lastHash(appContext)

                    if (current.hash.isNotBlank() && current.hash != lastHash) {
                        val updated = MagicWallpaperClient.updateLockscreen(appContext, backend, token)
                        MagicPrefs.setLastHash(appContext, updated.hash)
                    }
                } catch (_: Exception) {
                    // On ignore en prestation : nouvelle tentative au cycle suivant.
                }

                try {
                    Thread.sleep(intervalMs)
                } catch (_: InterruptedException) {
                    break
                }
            }

            running.set(false)
            stopSelf()
        }

        worker?.start()
    }

    private fun stopListening() {
        running.set(false)
        worker?.interrupt()
        worker = null
    }

    private fun createNotification(): Notification {
        val channelId = "magic_lockscreen_listen"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Magic Lockscreen",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("Magic Lockscreen actif")
            .setContentText("Écoute Inject en cours")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 3026
        private const val ACTION_STOP = "fr.magiclockscreen.android.STOP"

        fun start(context: Context) {
            val intent = Intent(context, MagicListenService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MagicListenService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
