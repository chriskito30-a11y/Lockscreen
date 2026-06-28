package fr.magiclockscreen.android;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MagicStandaloneClient {
    private MagicStandaloneClient() {}

    private static String enc(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(25000);
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "ModulysMagicLock/2.0 Android standalone");
        return connection;
    }

    private static String getText(String url) throws Exception {
        HttpURLConnection connection = open(url);
        connection.setRequestProperty("Accept", "application/json,text/plain,*/*");
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code <= 299 ? connection.getInputStream() : connection.getErrorStream();
        byte[] bytes = readAll(stream);
        String text = new String(bytes, "UTF-8");
        if (code < 200 || code > 299) {
            throw new IllegalStateException("HTTP " + code + " : " + text);
        }
        return text.trim();
    }

    private static byte[] getBytes(String url) throws Exception {
        HttpURLConnection connection = open(url);
        connection.setRequestProperty("Accept", "image/jpeg,image/png,image/webp,image/*,*/*");
        int code = connection.getResponseCode();
        if (code < 200 || code > 299) {
            InputStream err = connection.getErrorStream();
            String text = err == null ? "" : new String(readAll(err), "UTF-8");
            throw new IllegalStateException("Image HTTP " + code + " : " + text);
        }
        return readAll(connection.getInputStream());
    }

    private static byte[] readAll(InputStream input) throws Exception {
        if (input == null) return new byte[0];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = input.read(buf)) >= 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private static String unescapeJson(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .trim();
    }

    private static String extractByPath(String json, String path) {
        String key = path == null || path.trim().isEmpty() ? "value" : path.trim();
        try {
            Object root = json.trim().startsWith("[") ? new JSONArray(json) : new JSONObject(json);
            Object value = objectByPath(root, key);
            if (value != null && value != JSONObject.NULL) return normalizeQuery(String.valueOf(value));
        } catch (Exception ignored) {
        }

        if (key.contains(".")) {
            String[] parts = key.split("\\.");
            String current = json;
            for (String part : parts) {
                String extracted = extractDirectKey(current, part);
                if (extracted.isEmpty()) return "";
                current = extracted;
            }
            return normalizeQuery(current);
        }
        return normalizeQuery(extractDirectKey(json, key));
    }

    private static Object objectByPath(Object root, String path) throws Exception {
        Object current = root;
        for (String part : path.split("\\.")) {
            if (current instanceof JSONObject) {
                JSONObject obj = (JSONObject) current;
                if (!obj.has(part)) return null;
                current = obj.get(part);
            } else if (current instanceof JSONArray) {
                int idx = Integer.parseInt(part);
                JSONArray arr = (JSONArray) current;
                if (idx < 0 || idx >= arr.length()) return null;
                current = arr.get(idx);
            } else {
                return null;
            }
        }
        return current;
    }

    private static String extractDirectKey(String json, String key) {
        String quotedKey = Pattern.quote(key);
        Pattern stringPattern = Pattern.compile("\\\"" + quotedKey + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
        Matcher sm = stringPattern.matcher(json);
        if (sm.find()) return unescapeJson(sm.group(1));
        Pattern valuePattern = Pattern.compile("\\\"" + quotedKey + "\\\"\\s*:\\s*([^,}\\]]+)");
        Matcher vm = valuePattern.matcher(json);
        if (vm.find()) return vm.group(1).replaceAll("^[\\\"]|[\\\"]$", "").trim();
        return "";
    }

    private static String normalizeQuery(String value) {
        return value == null ? "" : value.replace('_', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String sha(String value) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(value.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(8, digest.length); i++) sb.append(String.format(Locale.US, "%02x", digest[i]));
        return sb.toString();
    }

    public static List<ScanCandidate> scanSource(String sourceUrl) throws Exception {
        if (sourceUrl == null || sourceUrl.trim().isEmpty()) throw new IllegalStateException("URL vide");
        String response = getText(sourceUrl.trim());
        String trimmed = response.trim();
        List<ScanCandidate> out = new ArrayList<>();

        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            String raw = normalizeQuery(trimmed);
            if (!raw.isEmpty()) out.add(new ScanCandidate("raw", raw));
            return out;
        }

        try {
            Object root = trimmed.startsWith("[") ? new JSONArray(trimmed) : new JSONObject(trimmed);
            flattenJson(root, "", out, 0);
        } catch (Exception e) {
            String value = extractByPath(trimmed, "value");
            if (!value.isEmpty()) out.add(new ScanCandidate("value", value));
            String selection = extractByPath(trimmed, "selection");
            if (!selection.isEmpty()) out.add(new ScanCandidate("selection", selection));
        }

        if (out.isEmpty()) throw new IllegalStateException("Aucune donnée lisible dans le JSON");
        return out;
    }

    private static void flattenJson(Object node, String path, List<ScanCandidate> out, int depth) throws Exception {
        if (out.size() >= 40 || depth > 6) return;

        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            JSONArray names = obj.names();
            if (names == null) return;
            for (int i = 0; i < names.length(); i++) {
                String key = names.getString(i);
                Object child = obj.get(key);
                String childPath = path.isEmpty() ? key : path + "." + key;
                flattenJson(child, childPath, out, depth + 1);
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < Math.min(arr.length(), 8); i++) {
                String childPath = path.isEmpty() ? String.valueOf(i) : path + "." + i;
                flattenJson(arr.get(i), childPath, out, depth + 1);
            }
        } else if (node != null && node != JSONObject.NULL) {
            String value = normalizeQuery(String.valueOf(node));
            if (!value.isEmpty() && value.length() <= 300) out.add(new ScanCandidate(path, value));
        }
    }

    public static MagicResult fetchValue(Context context) throws Exception {
        String source = MagicPrefs.sourceUrl(context);
        String path = MagicPrefs.jsonPath(context);
        if (source.isEmpty()) throw new IllegalStateException("URL Inject vide");
        String response = getText(source);
        String value;
        String trimmed = response.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            if ("raw".equalsIgnoreCase(path)) value = trimmed;
            else value = extractByPath(trimmed, path);
            if (value.isEmpty()) value = extractByPath(trimmed, "value");
            if (value.isEmpty()) value = extractByPath(trimmed, "selection");
            if (value.isEmpty()) value = extractByPath(trimmed, "query");
            if (value.isEmpty()) value = extractByPath(trimmed, "name");
        } else {
            value = trimmed;
        }
        value = normalizeQuery(value);
        if (value.isEmpty()) throw new IllegalStateException("Aucune révélation trouvée");
        return new MagicResult(value, sha(value), "", value);
    }

    public static MagicResult findWikipediaImage(Context context, String query) throws Exception {
        String lang = MagicPrefs.lang(context);
        if (lang == null || lang.trim().isEmpty()) lang = "fr";
        lang = lang.toLowerCase(Locale.US).replaceAll("[^a-z-]", "");
        if (lang.isEmpty()) lang = "fr";
        MagicResult result = searchWiki(query, lang);
        if (result.imageUrl != null && !result.imageUrl.isEmpty()) return result;
        if (!"en".equals(lang)) {
            MagicResult en = searchWiki(query, "en");
            if (en.imageUrl != null && !en.imageUrl.isEmpty()) return en;
        }
        return result;
    }

    private static MagicResult searchWiki(String query, String lang) throws Exception {
        String api = "https://" + lang + ".wikipedia.org/w/api.php"
                + "?action=query"
                + "&generator=search"
                + "&gsrsearch=" + enc(query)
                + "&gsrlimit=1"
                + "&prop=pageimages%7Cinfo"
                + "&inprop=url"
                + "&pithumbsize=2200"
                + "&format=json"
                + "&formatversion=2"
                + "&origin=*";
        String json = getText(api);
        String title = extractDirectKey(json, "title");
        String image = extractDirectKey(json, "source");
        return new MagicResult(query, sha(query), image, title.isEmpty() ? query : title);
    }

    public static MagicResult updateLockscreen(Context context) throws Exception {
        MagicResult value = fetchValue(context);
        MagicResult wiki = findWikipediaImage(context, value.value);
        if (wiki.imageUrl == null || wiki.imageUrl.isEmpty()) throw new IllegalStateException("Photo Wikipédia introuvable pour : " + value.value);
        byte[] bytes = getBytes(wiki.imageUrl);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap == null) throw new IllegalStateException("Image illisible");
        WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
        return new MagicResult(value.value, value.hash, wiki.imageUrl, wiki.title);
    }
}
