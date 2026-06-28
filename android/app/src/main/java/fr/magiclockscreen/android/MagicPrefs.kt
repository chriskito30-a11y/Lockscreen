package fr.magiclockscreen.android

import android.content.Context

object MagicPrefs {
    private const val NAME = "magic_lockscreen_prefs"

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun backend(context: Context): String =
        prefs(context).getString("backend", "") ?: ""

    fun token(context: Context): String =
        prefs(context).getString("token", "") ?: ""

    fun intervalSeconds(context: Context): Int =
        prefs(context).getInt("interval", 2)

    fun durationMinutes(context: Context): Int =
        prefs(context).getInt("duration", 10)

    fun lastHash(context: Context): String =
        prefs(context).getString("lastHash", "") ?: ""

    fun saveConfig(context: Context, backend: String, token: String, interval: Int, duration: Int) {
        prefs(context).edit()
            .putString("backend", backend.trim().trimEnd('/'))
            .putString("token", token.trim())
            .putInt("interval", interval.coerceIn(1, 60))
            .putInt("duration", duration.coerceIn(1, 240))
            .apply()
    }

    fun setLastHash(context: Context, hash: String) {
        prefs(context).edit().putString("lastHash", hash).apply()
    }
}
