package fr.magiclockscreen.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        setContent {
            MagicApp()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun MagicApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var backend by remember { mutableStateOf(MagicPrefs.backend(context)) }
    var token by remember { mutableStateOf(MagicPrefs.token(context)) }
    var interval by remember { mutableStateOf(MagicPrefs.intervalSeconds(context).toString()) }
    var duration by remember { mutableStateOf(MagicPrefs.durationMinutes(context).toString()) }
    var status by remember { mutableStateOf("Prêt.") }
    var loading by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(MagicPrefs.isListening(context)) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF8B5CF6),
            secondary = Color(0xFF22D3EE),
            background = Color(0xFF050816),
            surface = Color(0xFF111827),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF050816)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF111827), Color(0xFF050816), Color(0xFF020617))
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Magic Lockscreen",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Écoute ton URL Inject via le backend, récupère l’image Wikipédia, puis met à jour le fond d’écran verrouillé Android.",
                    color = Color(0xFFCBD5E1)
                )

                MagicCard {
                    OutlinedTextField(
                        value = backend,
                        onValueChange = { backend = it },
                        label = { Text("Backend base URL") },
                        placeholder = { Text("https://ton-site.vercel.app") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("Token dashboard") },
                        placeholder = { Text("v1....") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = interval,
                            onValueChange = { interval = it.filter(Char::isDigit).take(3) },
                            label = { Text("Intervalle sec") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it.filter(Char::isDigit).take(3) },
                            label = { Text("Durée min") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        onClick = {
                            val intervalInt = interval.toIntOrNull()?.coerceIn(1, 60) ?: 2
                            val durationInt = duration.toIntOrNull()?.coerceIn(1, 240) ?: 10
                            MagicPrefs.saveConfig(context, backend, token, intervalInt, durationInt)
                            status = "Configuration sauvegardée."
                        }
                    ) {
                        Text("Sauvegarder")
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading && backend.isNotBlank() && token.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        onClick = {
                            loading = true
                            status = "Test en cours..."
                            MagicPrefs.saveConfig(
                                context,
                                backend,
                                token,
                                interval.toIntOrNull()?.coerceIn(1, 60) ?: 2,
                                duration.toIntOrNull()?.coerceIn(1, 240) ?: 10
                            )
                            scope.launch {
                                try {
                                    val result = MagicWallpaperClient.updateLockscreen(context, backend, token)
                                    MagicPrefs.setLastHash(context, result.hash)
                                    status = "Lockscreen mis à jour : ${result.value}"
                                } catch (e: Exception) {
                                    status = "Erreur : ${e.message ?: "test impossible"}"
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    ) {
                        Text("Tester maintenant")
                    }
                }

                MagicCard {
                    Text(
                        text = "Mode écoute",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "À activer avant le tour. L’app surveille l’URL même écran verrouillé grâce à une notification silencieuse Android.",
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(Modifier.height(12.dp))

                    if (!listening) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = backend.isNotBlank() && token.isNotBlank(),
                            onClick = {
                                val intervalInt = interval.toIntOrNull()?.coerceIn(1, 60) ?: 2
                                val durationInt = duration.toIntOrNull()?.coerceIn(1, 240) ?: 10
                                MagicPrefs.saveConfig(context, backend, token, intervalInt, durationInt)
                                MagicListenService.start(context, backend, token, intervalInt, durationInt)
                                listening = true
                                status = "Écoute activée pendant $durationInt min, toutes les $intervalInt sec."
                            }
                        ) {
                            Text("Activer l’écoute")
                        }
                    } else {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            onClick = {
                                MagicListenService.stop(context)
                                listening = false
                                status = "Écoute arrêtée."
                            }
                        ) {
                            Text("Stopper l’écoute")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(onClick = {
                        interval = "2"
                        duration = "10"
                        status = "Réglage prestation recommandé : 2 sec / 10 min."
                    }) {
                        Text("Réglage recommandé : 2 sec / 10 min")
                    }
                }

                MagicCard {
                    Text(
                        text = "Statut",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = status, color = Color(0xFFE2E8F0))
                }
            }
        }
    }
}

@Composable
fun MagicCard(content: @Composable Column.() -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x1FFFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
