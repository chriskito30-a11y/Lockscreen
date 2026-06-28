package fr.magiclockscreen.android

import android.content.Context

object MagicPrefs {
    private const val FILE = "magic_lockscreen_prefs"
    private const val KEY_BACKEND = "backend_base_url"
    private const val KEY_TOKEN = "token"
    private const val KEY_INTERVAL = "interval_seconds"
    private const val KEY_DURATION = "duration_minutes"
    private const val KEY_LAST_HASH = "last_hash"
    private const val KEY_LISTENING = "listening"

    fun saveConfig(context: Context, backend: String, token: String, intervalSeconds: Int, durationMinutes: Int) {
        prefs(context).edit()
            .putString(KEY_BACKEND, backend.trim().trimEnd('/'))
            .putString(KEY_TOKEN, token.trim())
            .putInt(KEY_INTERVAL, intervalSeconds)
            .putInt(KEY_DURATION, durationMinutes)
            .apply()
    }

    fun backend(context: Context): String = prefs(context).getString(KEY_BACKEND, "") ?: ""
    fun token(context: Context): String = prefs(context).getString(KEY_TOKEN, "") ?: ""
    fun intervalSeconds(context: Context): Int = prefs(context).getInt(KEY_INTERVAL, 2)
    fun durationMinutes(context: Context): Int = prefs(context).getInt(KEY_DURATION, 10)

    fun lastHash(context: Context): String = prefs(context).getString(KEY_LAST_HASH, "") ?: ""
    fun setLastHash(context: Context, hash: String) = prefs(context).edit().putString(KEY_LAST_HASH, hash).apply()

    fun setListening(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_LISTENING, value).apply()
    fun isListening(context: Context): Boolean = prefs(context).getBoolean(KEY_LISTENING, false)

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
