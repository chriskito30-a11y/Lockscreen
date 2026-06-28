package fr.magiclockscreen.android;

import android.content.Context;
import android.content.SharedPreferences;

public final class MagicPrefs {
    private static final String NAME = "magic_lockscreen_prefs";

    private MagicPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String backend(Context context) {
        return prefs(context).getString("backend", "");
    }

    public static String token(Context context) {
        return prefs(context).getString("token", "");
    }

    public static int intervalSeconds(Context context) {
        return prefs(context).getInt("interval", 2);
    }

    public static int durationMinutes(Context context) {
        return prefs(context).getInt("duration", 10);
    }

    public static String lastHash(Context context) {
        return prefs(context).getString("lastHash", "");
    }

    public static void setLastHash(Context context, String hash) {
        prefs(context).edit().putString("lastHash", hash).apply();
    }

    public static void saveConfig(Context context, String backend, String token, int interval, int duration) {
        String cleanBackend = backend == null ? "" : backend.trim();
        while (cleanBackend.endsWith("/")) {
            cleanBackend = cleanBackend.substring(0, cleanBackend.length() - 1);
        }

        prefs(context).edit()
                .putString("backend", cleanBackend)
                .putString("token", token == null ? "" : token.trim())
                .putInt("interval", Math.max(1, Math.min(interval, 60)))
                .putInt("duration", Math.max(1, Math.min(duration, 240)))
                .apply();
    }
}
