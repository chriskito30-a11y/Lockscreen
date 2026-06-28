package fr.magiclockscreen.android;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private EditText sourceEdit;
    private EditText pathEdit;
    private EditText langEdit;
    private EditText intervalEdit;
    private EditText durationEdit;
    private TextView statusText;
    private TextView scanText;
    private Spinner scanSpinner;
    private final List<ScanCandidate> candidates = new ArrayList<>();

    private final int bgTop = Color.rgb(9, 14, 35);
    private final int bgBottom = Color.rgb(28, 17, 65);
    private final int card = Color.rgb(15, 23, 42);
    private final int card2 = Color.rgb(17, 24, 50);
    private final int text = Color.WHITE;
    private final int muted = Color.rgb(203, 213, 225);
    private final int purple = Color.rgb(139, 92, 246);
    private final int cyan = Color.rgb(34, 211, 238);

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
        GradientDrawable rootBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{bgTop, bgBottom});
        scroll.setBackground(rootBg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));

        TextView logo = new TextView(this);
        logo.setText("✦");
        logo.setTextSize(34f);
        logo.setGravity(Gravity.CENTER);
        logo.setTextColor(Color.WHITE);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable logoBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{purple, Color.rgb(236, 72, 153), cyan});
        logoBg.setShape(GradientDrawable.OVAL);
        logo.setBackground(logoBg);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(logo, logoParams);

        TextView title = new TextView(this);
        title.setText("Modulys Magic Lock");
        title.setTextSize(30f);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, dp(16), 0, 0);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("APK autonome : scanne ton JSON Inject, choisis la valeur à écouter, puis affiche automatiquement la photo Wikipédia sur le lockscreen.");
        subtitle.setTextSize(15f);
        subtitle.setTextColor(muted);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, dp(10), 0, dp(22));
        root.addView(subtitle);

        LinearLayout configCard = cardLayout();
        configCard.addView(sectionTitle("Configuration"));
        configCard.addView(label("URL source Inject / WikiTest"));
        sourceEdit = input("https://11z.co/_w/MON_ID/selection", MagicPrefs.sourceUrl(this));
        configCard.addView(sourceEdit);

        Button scanButton = primaryButton("Scanner l’URL");
        configCard.addView(scanButton);

        configCard.addView(label("Choix détecté"));
        scanSpinner = new Spinner(this);
        scanSpinner.setPadding(dp(10), dp(8), dp(10), dp(8));
        scanSpinner.setBackground(inputBg());
        configCard.addView(scanSpinner);

        scanText = new TextView(this);
        scanText.setText("Aucun scan pour l’instant.");
        scanText.setTextSize(14f);
        scanText.setTextColor(Color.rgb(186, 230, 253));
        scanText.setPadding(0, dp(10), 0, dp(2));
        configCard.addView(scanText);

        configCard.addView(label("Clé JSON à écouter"));
        pathEdit = input("value ou selection", MagicPrefs.jsonPath(this));
        configCard.addView(pathEdit);

        configCard.addView(label("Langue Wikipédia"));
        langEdit = input("fr", MagicPrefs.lang(this));
        configCard.addView(langEdit);

        root.addView(configCard);

        LinearLayout actionCard = cardLayout();
        actionCard.addView(sectionTitle("Action"));
        actionCard.addView(label("Intervalle d’écoute en secondes"));
        intervalEdit = input("3", String.valueOf(MagicPrefs.intervalSeconds(this)));
        actionCard.addView(intervalEdit);
        actionCard.addView(label("Durée d’écoute en minutes"));
        durationEdit = input("10", String.valueOf(MagicPrefs.durationMinutes(this)));
        actionCard.addView(durationEdit);

        Button saveButton = secondaryButton("Sauvegarder");
        Button testValueButton = secondaryButton("Tester la valeur");
        Button applyButton = primaryButton("Appliquer la photo maintenant");
        Button listenButton = primaryButton("Activer l’écoute");
        Button stopButton = dangerButton("Stopper l’écoute");

        actionCard.addView(saveButton);
        actionCard.addView(testValueButton);
        actionCard.addView(applyButton);
        actionCard.addView(listenButton);
        actionCard.addView(stopButton);

        root.addView(actionCard);

        statusText = new TextView(this);
        statusText.setText("Prêt.");
        statusText.setTextSize(15f);
        statusText.setTextColor(Color.rgb(226, 232, 240));
        statusText.setPadding(dp(4), dp(20), dp(4), 0);
        root.addView(statusText);

        TextView footer = new TextView(this);
        footer.setText("modulys.top");
        footer.setTextSize(13f);
        footer.setTextColor(Color.rgb(148, 163, 184));
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        footer.setPadding(0, dp(22), 0, 0);
        root.addView(footer);

        scanSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position >= 0 && position < candidates.size()) {
                    ScanCandidate c = candidates.get(position);
                    pathEdit.setText(c.path == null || c.path.isEmpty() ? "raw" : c.path);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        scanButton.setOnClickListener(v -> {
            status("Scan en cours...");
            new Thread(() -> {
                try {
                    List<ScanCandidate> found = MagicStandaloneClient.scanSource(sourceEdit.getText().toString());
                    runOnUiThread(() -> applyScanResults(found));
                    status("Scan terminé : " + found.size() + " donnée(s) trouvée(s).");
                } catch (Exception e) {
                    status("Erreur scan : " + safeMessage(e));
                }
            }).start();
        });

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

        scroll.addView(root);
        setContentView(scroll);
    }

    private void applyScanResults(List<ScanCandidate> found) {
        candidates.clear();
        candidates.addAll(found);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels(found));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scanSpinner.setAdapter(adapter);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(found.size(), 12); i++) {
            sb.append("• ").append(found.get(i).toString()).append("\n");
        }
        scanText.setText(sb.toString().trim());
        if (!found.isEmpty()) pathEdit.setText(found.get(0).path);
    }

    private List<String> labels(List<ScanCandidate> found) {
        List<String> out = new ArrayList<>();
        for (ScanCandidate c : found) out.add(c.toString());
        return out;
    }

    private LinearLayout cardLayout() {
        LinearLayout cardView = new LinearLayout(this);
        cardView.setOrientation(LinearLayout.VERTICAL);
        cardView.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(card);
        bg.setCornerRadius(dp(24));
        bg.setStroke(dp(1), Color.argb(95, 148, 163, 184));
        cardView.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(12), 0, dp(12));
        cardView.setLayoutParams(params);
        return cardView;
    }

    private TextView sectionTitle(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(21f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, 0, 0, dp(10));
        return t;
    }

    private EditText input(String hint, String value) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setText(value == null ? "" : value);
        input.setTextSize(15f);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.rgb(148, 163, 184));
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        input.setBackground(inputBg());
        return input;
    }

    private GradientDrawable inputBg() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(card2);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Color.argb(100, 148, 163, 184));
        return bg;
    }

    private TextView label(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(13f);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(muted);
        label.setPadding(0, dp(14), 0, dp(6));
        return label;
    }

    private Button primaryButton(String text) { return styledButton(text, purple, Color.WHITE); }
    private Button secondaryButton(String text) { return styledButton(text, Color.rgb(30, 41, 59), Color.WHITE); }
    private Button dangerButton(String text) { return styledButton(text, Color.rgb(127, 29, 29), Color.WHITE); }

    private Button styledButton(String text, int color, int textColor) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15f);
        button.setTextColor(textColor);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(16));
        button.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        params.setMargins(0, dp(10), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private void saveConfig() {
        int interval = parseInt(intervalEdit.getText().toString(), 3);
        int duration = parseInt(durationEdit.getText().toString(), 10);
        MagicPrefs.saveConfig(this, sourceEdit.getText().toString(), pathEdit.getText().toString(), langEdit.getText().toString(), interval, duration);
    }

    private int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value.trim()); } catch (Exception e) { return fallback; }
    }

    private String safeMessage(Exception e) {
        String msg = e.getMessage();
        return msg == null || msg.isEmpty() ? e.getClass().getSimpleName() : msg;
    }

    private void status(String message) {
        runOnUiThread(() -> statusText.setText(message));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
