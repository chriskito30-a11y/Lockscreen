package fr.magiclockscreen.android;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private EditText backendEdit;
    private EditText tokenEdit;
    private EditText intervalEdit;
    private EditText durationEdit;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNotificationPermission();
        buildUi();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36, 48, 36, 48);
        root.setBackgroundColor(Color.rgb(5, 8, 22));

        TextView title = label("Magic Lockscreen", 28, Color.WHITE);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);
        root.addView(label("Écoute Inject via ton backend Vercel et met à jour le fond d’écran verrouillé Android.", 15, Color.rgb(203, 213, 225)));

        backendEdit = input("Backend base URL", MagicPrefs.backend(this));
        tokenEdit = input("Token dashboard", MagicPrefs.token(this));
        tokenEdit.setMinLines(3);
        intervalEdit = input("Intervalle en secondes", String.valueOf(MagicPrefs.intervalSeconds(this)));
        durationEdit = input("Durée en minutes", String.valueOf(MagicPrefs.durationMinutes(this)));

        root.addView(label("Backend", 14, Color.rgb(203, 213, 225)));
        root.addView(backendEdit);
        root.addView(label("Token", 14, Color.rgb(203, 213, 225)));
        root.addView(tokenEdit);
        root.addView(label("Intervalle", 14, Color.rgb(203, 213, 225)));
        root.addView(intervalEdit);
        root.addView(label("Durée", 14, Color.rgb(203, 213, 225)));
        root.addView(durationEdit);

        Button save = button("Sauvegarder");
        Button test = button("Tester maintenant");
        Button listen = button("Activer l’écoute");
        Button stop = button("Stopper l’écoute");
        root.addView(save);
        root.addView(test);
        root.addView(listen);
        root.addView(stop);

        statusText = label("Prêt.", 16, Color.rgb(226, 232, 240));
        root.addView(statusText);

        save.setOnClickListener(v -> {
            saveConfig();
            status("Configuration sauvegardée.");
        });

        test.setOnClickListener(v -> {
            saveConfig();
            status("Test en cours...");
            new Thread(() -> {
                try {
                    MagicWallpaperClient.MagicResult result = MagicWallpaperClient.updateLockscreen(this, backendEdit.getText().toString(), tokenEdit.getText().toString());
                    MagicPrefs.setLastHash(this, result.hash);
                    status("Lockscreen mis à jour : " + result.value);
                } catch (Exception e) {
                    status("Erreur : " + (e.getMessage() == null ? "test impossible" : e.getMessage()));
                }
            }).start();
        });

        listen.setOnClickListener(v -> {
            saveConfig();
            MagicListenService.start(this);
            status("Écoute activée. Tu peux verrouiller le téléphone.");
        });

        stop.setOnClickListener(v -> {
            MagicListenService.stop(this);
            status("Écoute stoppée.");
        });

        scroll.addView(root);
        setContentView(scroll);
    }

    private EditText input(String hint, String value) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setText(value == null ? "" : value);
        edit.setTextSize(16);
        edit.setTextColor(Color.WHITE);
        edit.setHintTextColor(Color.rgb(148, 163, 184));
        edit.setSingleLine(false);
        edit.setPadding(18, 12, 18, 12);
        return edit;
    }

    private TextView label(String text, int size, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(color);
        tv.setPadding(0, 14, 0, 8);
        return tv;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        return b;
    }

    private void saveConfig() {
        int interval = parse(intervalEdit.getText().toString(), 2, 1, 60);
        int duration = parse(durationEdit.getText().toString(), 10, 1, 240);
        MagicPrefs.saveConfig(this, backendEdit.getText().toString(), tokenEdit.getText().toString(), interval, duration);
    }

    private int parse(String value, int fallback, int min, int max) {
        try {
            int n = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(n, max));
        } catch (Exception e) {
            return fallback;
        }
    }

    private void status(String message) {
        runOnUiThread(() -> statusText.setText(message));
    }
}
