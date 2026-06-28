package fr.magiclockscreen.android;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MagicWallpaperClient {
    private MagicWallpaperClient() {}

    public static final class MagicResult {
        public final String value;
        public final String hash;

        public MagicResult(String value, String hash) {
            this.value = value;
            this.hash = hash;
        }
    }

    private static String enc(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }

    private static String getText(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(20000);
        c.setRequestMethod("GET");
        c.setRequestProperty("User-Agent", "MagicLockscreenAndroid/0.3");
        int code = c.getResponseCode();
        InputStream stream = code >= 200 && code <= 299 ? c.getInputStream() : c.getErrorStream();
        String text = new String(readAll(stream), StandardCharsets.UTF_8);
        if (code < 200 || code > 299) throw new IllegalStateException("HTTP " + code + " : " + text);
        return text;
    }

    private static byte[] getBytes(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(30000);
        c.setRequestMethod("GET");
        c.setRequestProperty("User-Agent", "MagicLockscreenAndroid/0.3");
        int code = c.getResponseCode();
        if (code < 200 || code > 299) {
            String err = c.getErrorStream() == null ? "" : new String(readAll(c.getErrorStream()), StandardCharsets.UTF_8);
            throw new IllegalStateException("Image HTTP " + code + " : " + err);
        }
        return readAll(c.getInputStream());
    }

    private static byte[] readAll(InputStream stream) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = stream.read(buffer)) != -1) out.write(buffer, 0, n);
        stream.close();
        return out.toByteArray();
    }

    private static String jsonString(String json, String key) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
        Matcher m = p.matcher(json);
        if (!m.find()) return "";
        return m.group(1).replace("\\\"", "\"").replace("\\n", "\n").replace("\\/", "/");
    }

    public static MagicResult fetchValue(String backend, String token) throws Exception {
        String base = backend == null ? "" : backend.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String json = getText(base + "/api/value?token=" + enc(token));
        if (!json.contains("\"ok\"") || !json.contains("true")) throw new IllegalStateException("Réponse backend invalide : " + json);
        String value = jsonString(json, "value");
        String hash = jsonString(json, "hash");
        if (hash.length() == 0) hash = value;
        if (value.trim().length() == 0) throw new IllegalStateException("Aucune révélation trouvée");
        return new MagicResult(value, hash);
    }

    public static MagicResult updateLockscreen(Context context, String backend, String token) throws Exception {
        MagicResult result = fetchValue(backend, token);
        String base = backend == null ? "" : backend.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        byte[] bytes = getBytes(base + "/api/wallpaper?token=" + enc(token) + "&t=" + System.currentTimeMillis());
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap == null) throw new IllegalStateException("Image illisible");
        WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
        return result;
    }
}
