package fr.magiclockscreen.android

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class MagicTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val backend = MagicPrefs.backend(this)
        val token = MagicPrefs.token(this)

        if (MagicPrefs.isListening(this)) {
            MagicListenService.stop(this)
        } else if (backend.isNotBlank() && token.isNotBlank()) {
            MagicListenService.start(
                this,
                backend,
                token,
                MagicPrefs.intervalSeconds(this),
                MagicPrefs.durationMinutes(this)
            )
        } else {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(android.app.PendingIntent.getActivity(this, 10, intent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT))
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }

        updateTile()
    }

    private fun updateTile() {
        qsTile?.let { tile ->
            val active = MagicPrefs.isListening(this)
            tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = if (active) "Magic ON" else "Magic Lock"
            tile.updateTile()
        }
    }
}
