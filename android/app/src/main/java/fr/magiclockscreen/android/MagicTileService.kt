package fr.magiclockscreen.android

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class MagicTileService : TileService() {
    override fun onClick() {
        super.onClick()

        qsTile?.state = Tile.STATE_ACTIVE
        qsTile?.label = "Magic..."
        qsTile?.updateTile()

        Thread {
            try {
                val backend = MagicPrefs.backend(applicationContext)
                val token = MagicPrefs.token(applicationContext)

                if (backend.isNotBlank() && token.isNotBlank()) {
                    val result = MagicWallpaperClient.updateLockscreen(applicationContext, backend, token)
                    MagicPrefs.setLastHash(applicationContext, result.hash)
                }
            } catch (_: Exception) {
            } finally {
                qsTile?.state = Tile.STATE_INACTIVE
                qsTile?.label = "Magic Lock"
                qsTile?.updateTile()
            }
        }.start()
    }
}
