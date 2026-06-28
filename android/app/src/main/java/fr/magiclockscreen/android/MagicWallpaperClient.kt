package fr.magiclockscreen.android

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class MagicValue(
    val value: String,
    val hash: String
)

object MagicWallpaperClient {
    suspend fun fetchValue(backendBaseUrl: String, token: String): MagicValue = withContext(Dispatchers.IO) {
        val url = buildUrl(backendBaseUrl, "/api/value", token)
        val text = httpGetText(url)
        val json = JSONObject(text)
        if (!json.optBoolean("ok", false)) {
            throw IllegalStateException(json.optString("error", "Erreur valeur"))
        }
        MagicValue(
            value = json.optString("value", "").trim(),
            hash = json.optString("hash", "").trim()
        )
    }

    suspend fun updateLockscreen(context: Context, backendBaseUrl: String, token: String): MagicValue = withContext(Dispatchers.IO) {
        val value = fetchValue(backendBaseUrl, token)
        if (value.value.isBlank()) throw IllegalStateException("Révélation vide")

        val wallpaperUrl = buildUrl(backendBaseUrl, "/api/wallpaper", token)
        val bitmap = httpGetBitmap(wallpaperUrl)
        val manager = WallpaperManager.getInstance(context.applicationContext)

        manager.setBitmap(
            bitmap,
            null,
            true,
            WallpaperManager.FLAG_LOCK
        )

        value
    }

    private fun buildUrl(base: String, path: String, token: String): String {
        val cleanBase = base.trim().trimEnd('/')
        val encodedToken = URLEncoder.encode(token.trim(), StandardCharsets.UTF_8.toString())
        return "$cleanBase$path?token=$encodedToken"
    }

    private fun httpGetText(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            useCaches = false
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) throw IllegalStateException("HTTP $code : $text")
            text
        } finally {
            connection.disconnect()
        }
    }

    private fun httpGetBitmap(url: String) = with(URL(url).openConnection() as HttpURLConnection) {
        requestMethod = "GET"
        connectTimeout = 12_000
        readTimeout = 30_000
        setRequestProperty("Accept", "image/png,image/*")
        useCaches = false
        try {
            val code = responseCode
            if (code !in 200..299) {
                val text = errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw IllegalStateException("HTTP $code wallpaper : $text")
            }
            BufferedInputStream(inputStream).use { input ->
                BitmapFactory.decodeStream(input) ?: throw IllegalStateException("Image wallpaper illisible")
            }
        } finally {
            disconnect()
        }
    }
}
