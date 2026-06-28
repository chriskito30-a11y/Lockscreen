package fr.magiclockscreen.android;

import android.content.Context;
import android.content.SharedPreferences;

public final class MagicPrefs {
    private static final String NAME = "magic_lockscreen_standalone_prefs";

    private MagicPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String sourceUrl(Context context) {
        return prefs(context).getString("sourceUrl", "");
    }

    public static String jsonPath(Context context) {
        return prefs(context).getString("jsonPath", "value");
    }

    public static String lang(Context context) {
        return prefs(context).getString("lang", "fr");
    }

    public static int intervalSeconds(Context context) {
        return prefs(context).getInt("interval", 3);
    }

    public static int durationMinutes(Context context) {
        return prefs(context).getInt("duration", 10);
    }

    public static String lastHash(Context context) {
        return prefs(context).getString("lastHash", "");
    }

    public static void saveConfig(Context context, String sourceUrl, String jsonPath, String lang, int interval, int duration) {
        prefs(context).edit()
                .putString("sourceUrl", sourceUrl.trim())
                .putString("jsonPath", jsonPath.trim().isEmpty() ? "value" : jsonPath.trim())
                .putString("lang", lang.trim().isEmpty() ? "fr" : lang.trim())
                .putInt("interval", Math.max(1, Math.min(60, interval)))
                .putInt("duration", Math.max(1, Math.min(240, duration)))
                .apply();
    }

    public static void setLastHash(Context context, String hash) {
        prefs(context).edit().putString("lastHash", hash).apply();
    }
}
