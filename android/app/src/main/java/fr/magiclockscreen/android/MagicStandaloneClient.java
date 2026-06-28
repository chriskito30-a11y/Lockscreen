package fr.magiclockscreen.android;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;

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
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(25000);
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "ModulysMagicLock/2.3 Android standalone");
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

    private static MagicResult findWikipediaImage(Context context, String query) throws Exception {
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

    private static Bitmap tryDownloadBitmap(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) return null;
            if (imageUrl.toLowerCase(Locale.US).endsWith(".svg")) return null;
            byte[] bytes = getBytes(imageUrl);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static MagicResult updateLockscreen(Context context) throws Exception {
        MagicResult value = fetchValue(context);
        String provider = MagicPrefs.imageProvider(context);
        if (provider == null || provider.trim().isEmpty()) provider = "wikipedia";
        provider = provider.toLowerCase(Locale.US);

        if ("peek".equals(provider)) {
            Bitmap bitmap = renderPeekBitmap(context, value.value);
            WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
            return new MagicResult(value.value, value.hash, MagicPrefs.peekImageUri(context), "Peek");
        }

        MagicResult imageResult = findWikipediaImage(context, value.value);
        Bitmap bitmap = tryDownloadBitmap(imageResult.imageUrl);
        if (bitmap != null) {
            WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
            return new MagicResult(value.value, value.hash, imageResult.imageUrl, imageResult.title);
        }
        throw new IllegalStateException("Aucune photo Wikipédia trouvée pour : " + value.value);
    }

    public static Bitmap renderPeekBitmap(Context context, String text) throws Exception {
        String uriText = MagicPrefs.peekImageUri(context);
        if (uriText == null || uriText.trim().isEmpty()) {
            throw new IllegalStateException("Aucune image Peek choisie");
        }

        Bitmap base;
        try (InputStream input = context.getContentResolver().openInputStream(Uri.parse(uriText))) {
            base = BitmapFactory.decodeStream(input);
        }
        if (base == null) throw new IllegalStateException("Image Peek illisible");

        int outW = Math.max(720, base.getWidth());
        int outH = Math.max(1280, base.getHeight());
        Bitmap out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);

        Rect src = new Rect(0, 0, base.getWidth(), base.getHeight());
        Rect dst = coverRect(base.getWidth(), base.getHeight(), outW, outH);
        Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(base, src, dst, bitmapPaint);

        float bx = MagicPrefs.peekX(context);
        float by = MagicPrefs.peekY(context);
        float bw = MagicPrefs.peekW(context);
        float bh = MagicPrefs.peekH(context);
        Rect clip = new Rect(
                Math.round((bx) * outW),
                Math.round((by) * outH),
                Math.round((bx + bw) * outW),
                Math.round((by + bh) * outH)
        );
        canvas.save();
        canvas.rotate(MagicPrefs.peekRotation(context), clip.exactCenterX(), clip.exactCenterY());
        drawPeekText(canvas, clip, text == null || text.trim().isEmpty() ? "Exemple" : text.trim(), context);
        canvas.restore();
        return out;
    }

    private static Rect coverRect(int imgW, int imgH, int outW, int outH) {
        float imgRatio = imgW / (float) imgH;
        float outRatio = outW / (float) outH;
        if (imgRatio > outRatio) {
            int h = outH;
            int w = Math.round(h * imgRatio);
            int left = (outW - w) / 2;
            return new Rect(left, 0, left + w, outH);
        } else {
            int w = outW;
            int h = Math.round(w / imgRatio);
            int top = (outH - h) / 2;
            return new Rect(0, top, outW, top + h);
        }
    }

    private static void drawPeekText(Canvas canvas, Rect rect, String text, Context context) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        int opacity = Math.max(0, Math.min(100, MagicPrefs.peekOpacity(context)));
        int alpha = Math.round(opacity * 2.55f);
        int color = MagicPrefs.peekTextColor(context);
        paint.setColor((color & 0x00FFFFFF) | (alpha << 24));
        paint.setTextSize(spToPx(context, MagicPrefs.peekTextSize(context)));
        boolean bold = MagicPrefs.peekBold(context);
        boolean italic = MagicPrefs.peekItalic(context);
        int style = bold && italic ? Typeface.BOLD_ITALIC : bold ? Typeface.BOLD : italic ? Typeface.ITALIC : Typeface.NORMAL;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, style));
        String align = MagicPrefs.peekAlign(context);
        if ("left".equalsIgnoreCase(align)) paint.setTextAlign(Paint.Align.LEFT);
        else if ("right".equalsIgnoreCase(align)) paint.setTextAlign(Paint.Align.RIGHT);
        else paint.setTextAlign(Paint.Align.CENTER);
        if (MagicPrefs.peekShadow(context)) paint.setShadowLayer(dpToPx(context, 4), dpToPx(context, 2), dpToPx(context, 2), Color.argb(alpha, 0, 0, 0));

        Paint.FontMetrics fm = paint.getFontMetrics();
        float tx = paint.getTextAlign() == Paint.Align.LEFT ? rect.left + dpToPx(context, 10) : paint.getTextAlign() == Paint.Align.RIGHT ? rect.right - dpToPx(context, 10) : rect.centerX();
        float ty = rect.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.save();
        canvas.clipRect(rect);
        canvas.drawText(text, tx, ty, paint);
        canvas.restore();
    }

    private static float spToPx(Context context, int sp) {
        return sp * context.getResources().getDisplayMetrics().scaledDensity;
    }

    private static float dpToPx(Context context, int dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

}
