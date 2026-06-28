package fr.magiclockscreen.android;

import android.content.Context;
import android.content.SharedPreferences;

public final class MagicPrefs {
    private static final String NAME = "magic_lockscreen_standalone_prefs";

    private MagicPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String sourceUrl(Context context) { return prefs(context).getString("sourceUrl", ""); }
    public static String jsonPath(Context context) { return prefs(context).getString("jsonPath", "value"); }
    public static String lang(Context context) { return prefs(context).getString("lang", "fr"); }

    public static String imageProvider(Context context) {
        String value = prefs(context).getString("imageProvider", "wikipedia");
        if ("peek".equalsIgnoreCase(value)) return "peek";
        return "wikipedia";
    }

    public static int intervalSeconds(Context context) { return prefs(context).getInt("interval", 3); }
    public static int durationMinutes(Context context) { return prefs(context).getInt("duration", 10); }
    public static String lastHash(Context context) { return prefs(context).getString("lastHash", ""); }

    public static String peekImageUri(Context context) { return prefs(context).getString("peekImageUri", ""); }
    public static float peekX(Context context) { return prefs(context).getFloat("peekX", 0.12f); }
    public static float peekY(Context context) { return prefs(context).getFloat("peekY", 0.70f); }
    public static float peekW(Context context) { return prefs(context).getFloat("peekW", 0.76f); }
    public static float peekH(Context context) { return prefs(context).getFloat("peekH", 0.14f); }
    public static int peekTextSize(Context context) { return prefs(context).getInt("peekTextSize", 46); }
    public static int peekTextColor(Context context) { return prefs(context).getInt("peekTextColor", 0xFFFFFFFF); }
    public static int peekOpacity(Context context) { return prefs(context).getInt("peekOpacity", 100); }
    public static boolean peekBold(Context context) { return prefs(context).getBoolean("peekBold", true); }
    public static boolean peekItalic(Context context) { return prefs(context).getBoolean("peekItalic", false); }
    public static String peekAlign(Context context) { return prefs(context).getString("peekAlign", "center"); }
    public static boolean peekShadow(Context context) { return prefs(context).getBoolean("peekShadow", true); }

    public static void saveConfig(
            Context context,
            String sourceUrl,
            String jsonPath,
            String lang,
            String imageProvider,
            int interval,
            int duration
    ) {
        prefs(context).edit()
                .putString("sourceUrl", sourceUrl == null ? "" : sourceUrl.trim())
                .putString("jsonPath", jsonPath == null || jsonPath.trim().isEmpty() ? "value" : jsonPath.trim())
                .putString("lang", lang == null || lang.trim().isEmpty() ? "fr" : lang.trim())
                .putString("imageProvider", imageProvider == null || imageProvider.trim().isEmpty() ? "wikipedia" : imageProvider.trim())
                .putInt("interval", Math.max(1, Math.min(60, interval)))
                .putInt("duration", Math.max(1, Math.min(240, duration)))
                .apply();
    }

    public static void savePeekImageUri(Context context, String uri) {
        prefs(context).edit().putString("peekImageUri", uri == null ? "" : uri).apply();
    }

    public static void savePeekBox(Context context, float x, float y, float w, float h) {
        prefs(context).edit()
                .putFloat("peekX", clamp(x, 0f, 0.98f))
                .putFloat("peekY", clamp(y, 0f, 0.98f))
                .putFloat("peekW", clamp(w, 0.08f, 1f))
                .putFloat("peekH", clamp(h, 0.04f, 1f))
                .apply();
    }

    public static void savePeekStyle(
            Context context,
            int textSize,
            int textColor,
            int opacity,
            boolean bold,
            boolean italic,
            String align,
            boolean shadow
    ) {
        prefs(context).edit()
                .putInt("peekTextSize", Math.max(8, Math.min(220, textSize)))
                .putInt("peekTextColor", textColor)
                .putInt("peekOpacity", Math.max(0, Math.min(100, opacity)))
                .putBoolean("peekBold", bold)
                .putBoolean("peekItalic", italic)
                .putString("peekAlign", align == null || align.trim().isEmpty() ? "center" : align.trim())
                .putBoolean("peekShadow", shadow)
                .apply();
    }

    public static void setLastHash(Context context, String hash) {
        prefs(context).edit().putString("lastHash", hash == null ? "" : hash).apply();
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
