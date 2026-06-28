package fr.magiclockscreen.android

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var backendEdit: EditText
    private lateinit var tokenEdit: EditText
    private lateinit var intervalEdit: EditText
    private lateinit var durationEdit: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        buildUi()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 48)
            setBackgroundColor(Color.rgb(5, 8, 22))
        }

        val title = TextView(this).apply {
            text = "Magic Lockscreen"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val subtitle = TextView(this).apply {
            text = "Écoute Inject via ton backend Vercel et met à jour le fond d’écran verrouillé Android."
            textSize = 15f
            setTextColor(Color.rgb(203, 213, 225))
            setPadding(0, 20, 0, 28)
        }

        backendEdit = input("Backend base URL", MagicPrefs.backend(this))
        tokenEdit = input("Token dashboard", MagicPrefs.token(this))
        tokenEdit.minLines = 3

        intervalEdit = input("Intervalle en secondes", MagicPrefs.intervalSeconds(this).toString())
        durationEdit = input("Durée en minutes", MagicPrefs.durationMinutes(this).toString())

        val saveButton = button("Sauvegarder")
        val testButton = button("Tester maintenant")
        val listenButton = button("Activer l’écoute")
        val stopButton = button("Stopper l’écoute")

        statusText = TextView(this).apply {
            text = "Prêt."
            textSize = 16f
            setTextColor(Color.rgb(226, 232, 240))
            setPadding(0, 28, 0, 0)
        }

        saveButton.setOnClickListener {
            saveConfig()
            status("Configuration sauvegardée.")
        }

        testButton.setOnClickListener {
            saveConfig()
            status("Test en cours...")
            Thread {
                try {
                    val result = MagicWallpaperClient.updateLockscreen(
                        this,
                        backendEdit.text.toString(),
                        tokenEdit.text.toString()
                    )
                    MagicPrefs.setLastHash(this, result.hash)
                    status("Lockscreen mis à jour : ${result.value}")
                } catch (e: Exception) {
                    status("Erreur : ${e.message ?: "test impossible"}")
                }
            }.start()
        }

        listenButton.setOnClickListener {
            saveConfig()
            MagicListenService.start(this)
            status("Écoute activée. Tu peux verrouiller le téléphone.")
        }

        stopButton.setOnClickListener {
            MagicListenService.stop(this)
            status("Écoute stoppée.")
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(label("Backend"))
        root.addView(backendEdit)
        root.addView(label("Token"))
        root.addView(tokenEdit)
        root.addView(label("Intervalle"))
        root.addView(intervalEdit)
        root.addView(label("Durée"))
        root.addView(durationEdit)
        root.addView(saveButton)
        root.addView(testButton)
        root.addView(listenButton)
        root.addView(stopButton)
        root.addView(statusText)

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun input(hint: String, value: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            setText(value)
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(148, 163, 184))
            setSingleLine(false)
            setPadding(18, 12, 18, 12)
        }
    }

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.rgb(203, 213, 225))
            setPadding(0, 20, 0, 4)
        }
    }

    private fun button(text: String): Button {
        return Button(this).apply {
            this.text = text
            textSize = 16f
        }
    }

    private fun saveConfig() {
        val interval = intervalEdit.text.toString().toIntOrNull()?.coerceIn(1, 60) ?: 2
        val duration = durationEdit.text.toString().toIntOrNull()?.coerceIn(1, 240) ?: 10

        MagicPrefs.saveConfig(
            this,
            backendEdit.text.toString(),
            tokenEdit.text.toString(),
            interval,
            duration
        )
    }

    private fun status(message: String) {
        runOnUiThread {
            statusText.text = message
        }
    }
}
