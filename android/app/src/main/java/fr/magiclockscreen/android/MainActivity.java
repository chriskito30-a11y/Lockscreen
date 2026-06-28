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
    private EditText sourceEdit;
    private EditText pathEdit;
    private EditText langEdit;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36, 48, 36, 48);
        root.setBackgroundColor(Color.rgb(5, 8, 22));

        TextView title = new TextView(this);
        title.setText("Magic Lockscreen");
        title.setTextSize(28f);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView subtitle = new TextView(this);
        subtitle.setText("Version autonome : colle ton URL Inject, l’app lit la valeur, cherche la photo Wikipédia et change le lockscreen. Aucun serveur.");
        subtitle.setTextSize(15f);
        subtitle.setTextColor(Color.rgb(203, 213, 225));
        subtitle.setPadding(0, 20, 0, 28);

        sourceEdit = input("URL Inject /selection", MagicPrefs.sourceUrl(this));
        pathEdit = input("Clé JSON, ex : value ou selection", MagicPrefs.jsonPath(this));
        langEdit = input("Langue Wikipédia, ex : fr ou en", MagicPrefs.lang(this));
        intervalEdit = input("Intervalle en secondes", String.valueOf(MagicPrefs.intervalSeconds(this)));
        durationEdit = input("Durée en minutes", String.valueOf(MagicPrefs.durationMinutes(this)));

        Button saveButton = button("Sauvegarder");
        Button testValueButton = button("Tester la valeur Inject");
        Button applyButton = button("Appliquer la photo maintenant");
        Button listenButton = button("Activer l’écoute");
        Button stopButton = button("Stopper l’écoute");

        statusText = new TextView(this);
        statusText.setText("Prêt.");
        statusText.setTextSize(16f);
        statusText.setTextColor(Color.rgb(226, 232, 240));
        statusText.setPadding(0, 28, 0, 0);

        saveButton.setOnClickListener(v -> {
            saveConfig();
            status("Configuration sauvegardée.");
        });

        testValueButton.setOnClickListener(v -> {
            saveConfig();
            status("Lecture Inject...");
            new Thread(() -> {
                try {
                    MagicResult result = MagicStandaloneClient.fetchValue(this);
                    status("Valeur détectée : " + result.value);
                } catch (Exception e) {
                    status("Erreur valeur : " + safeMessage(e));
                }
            }).start();
        });

        applyButton.setOnClickListener(v -> {
            saveConfig();
            status("Recherche photo et application...");
            new Thread(() -> {
                try {
                    MagicResult result = MagicStandaloneClient.updateLockscreen(this);
                    MagicPrefs.setLastHash(this, result.hash);
                    status("Lockscreen mis à jour : " + result.value + " / " + result.title);
                } catch (Exception e) {
                    status("Erreur photo : " + safeMessage(e));
                }
            }).start();
        });

        listenButton.setOnClickListener(v -> {
            saveConfig();
            MagicListenService.start(this);
            status("Écoute activée. Tu peux verrouiller le téléphone.");
        });

        stopButton.setOnClickListener(v -> {
            MagicListenService.stop(this);
            status("Écoute stoppée.");
        });

        root.addView(title);
        root.addView(subtitle);
        root.addView(label("URL source Inject"));
        root.addView(sourceEdit);
        root.addView(label("Clé JSON"));
        root.addView(pathEdit);
        root.addView(label("Langue Wikipédia"));
        root.addView(langEdit);
        root.addView(label("Intervalle"));
        root.addView(intervalEdit);
        root.addView(label("Durée"));
        root.addView(durationEdit);
        root.addView(saveButton);
        root.addView(testValueButton);
        root.addView(applyButton);
        root.addView(listenButton);
        root.addView(stopButton);
        root.addView(statusText);

        scroll.addView(root);
        setContentView(scroll);
    }

    private EditText input(String hint, String value) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setText(value == null ? "" : value);
        input.setTextSize(16f);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.rgb(148, 163, 184));
        input.setSingleLine(false);
        input.setPadding(18, 12, 18, 12);
        return input;
    }

    private TextView label(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(14f);
        label.setTextColor(Color.rgb(203, 213, 225));
        label.setPadding(0, 20, 0, 4);
        return label;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16f);
        return button;
    }

    private void saveConfig() {
        int interval = parseInt(intervalEdit.getText().toString(), 3);
        int duration = parseInt(durationEdit.getText().toString(), 10);
        MagicPrefs.saveConfig(
                this,
                sourceEdit.getText().toString(),
                pathEdit.getText().toString(),
                langEdit.getText().toString(),
                interval,
                duration
        );
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String safeMessage(Exception e) {
        String msg = e.getMessage();
        return msg == null || msg.isEmpty() ? e.getClass().getSimpleName() : msg;
    }

    private void status(String message) {
        runOnUiThread(() -> statusText.setText(message));
    }
}
