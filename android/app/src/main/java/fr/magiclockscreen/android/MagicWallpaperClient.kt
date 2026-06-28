package fr.magiclockscreen.android

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class MagicResult(
    val value: String,
    val hash: String
)

object MagicWallpaperClient {
    private fun enc(value: String): String =
        URLEncoder.encode(value, "UTF-8")

    private fun getText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 20000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "MagicLockscreenAndroid/0.2")

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream.bufferedReader().use { it.readText() }

        if (code !in 200..299) {
            throw IllegalStateException("HTTP $code : $text")
        }

        return text
    }

    private fun getBytes(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 30000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "MagicLockscreenAndroid/0.2")

        val code = connection.responseCode
        if (code !in 200..299) {
            val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw IllegalStateException("Image HTTP $code : $err")
        }

        return connection.inputStream.use { it.readBytes() }
    }

    private fun extractJsonString(json: String, key: String): String {
        val regex = Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"")
        return regex.find(json)?.groupValues?.get(1)
            ?.replace("\\\"", "\"")
            ?.replace("\\n", "\n")
            ?.replace("\\/", "/")
            ?: ""
    }

    fun fetchValue(backend: String, token: String): MagicResult {
        val base = backend.trim().trimEnd('/')
        val url = "$base/api/value?token=${enc(token)}"
        val json = getText(url)

        val ok = json.contains("\"ok\"") && json.contains("true")
        if (!ok) {
            throw IllegalStateException("Réponse backend invalide : $json")
        }

        val value = extractJsonString(json, "value")
        val hash = extractJsonString(json, "hash").ifBlank { value }

        if (value.isBlank()) {
            throw IllegalStateException("Aucune révélation trouvée")
        }

        return MagicResult(value, hash)
    }

    fun updateLockscreen(context: Context, backend: String, token: String): MagicResult {
        val result = fetchValue(backend, token)
        val base = backend.trim().trimEnd('/')
        val imageUrl = "$base/api/wallpaper?token=${enc(token)}&t=${System.currentTimeMillis()}"
        val bytes = getBytes(imageUrl)

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("Image illisible")

        WallpaperManager.getInstance(context).setBitmap(
            bitmap,
            null,
            true,
            WallpaperManager.FLAG_LOCK
        )

        return result
    }
}
