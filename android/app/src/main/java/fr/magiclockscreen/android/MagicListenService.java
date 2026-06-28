package fr.magiclockscreen.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.util.concurrent.atomic.AtomicBoolean;

public class MagicListenService extends Service {
    private static final int NOTIFICATION_ID = 3026;
    private static final String ACTION_STOP = "fr.magiclockscreen.android.STOP";

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopListening();
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, createNotification());
        startListening();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopListening();
        super.onDestroy();
    }

    private void startListening() {
        if (running.getAndSet(true)) return;
        final Context appContext = getApplicationContext();
        final String backend = MagicPrefs.backend(appContext);
        final String token = MagicPrefs.token(appContext);
        final long intervalMs = Math.max(1, Math.min(MagicPrefs.intervalSeconds(appContext), 60)) * 1000L;
        final long endAt = System.currentTimeMillis() + Math.max(1, Math.min(MagicPrefs.durationMinutes(appContext), 240)) * 60000L;

        worker = new Thread(() -> {
            while (running.get() && System.currentTimeMillis() < endAt) {
                try {
                    MagicWallpaperClient.MagicResult current = MagicWallpaperClient.fetchValue(backend, token);
                    String last = MagicPrefs.lastHash(appContext);
                    if (current.hash != null && current.hash.length() > 0 && !current.hash.equals(last)) {
                        MagicWallpaperClient.MagicResult updated = MagicWallpaperClient.updateLockscreen(appContext, backend, token);
                        MagicPrefs.setLastHash(appContext, updated.hash);
                    }
                } catch (Exception ignored) {
                }
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    break;
                }
            }
            running.set(false);
            stopSelf();
        });
        worker.start();
    }

    private void stopListening() {
        running.set(false);
        if (worker != null) worker.interrupt();
        worker = null;
    }

    private Notification createNotification() {
        String channelId = "magic_lockscreen_listen";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Magic Lockscreen", NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, channelId)
                : new Notification.Builder(this);
        return builder
                .setContentTitle("Magic Lockscreen actif")
                .setContentText("Écoute Inject en cours")
                .setSmallIcon(android.R.drawable.ic_menu_upload)
                .setOngoing(true)
                .build();
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, MagicListenService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent);
        else context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, MagicListenService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }
}
