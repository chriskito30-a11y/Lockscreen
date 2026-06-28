package fr.magiclockscreen.android;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class MagicTileService extends TileService {
    @Override
    public void onClick() {
        super.onClick();
        if (qsTile != null) {
            qsTile.setState(Tile.STATE_ACTIVE);
            qsTile.setLabel("Magic...");
            qsTile.updateTile();
        }
        new Thread(() -> {
            try {
                String backend = MagicPrefs.backend(getApplicationContext());
                String token = MagicPrefs.token(getApplicationContext());
                if (backend.length() > 0 && token.length() > 0) {
                    MagicWallpaperClient.MagicResult result = MagicWallpaperClient.updateLockscreen(getApplicationContext(), backend, token);
                    MagicPrefs.setLastHash(getApplicationContext(), result.hash);
                }
            } catch (Exception ignored) {
            } finally {
                if (qsTile != null) {
                    qsTile.setState(Tile.STATE_INACTIVE);
                    qsTile.setLabel("Magic Lock");
                    qsTile.updateTile();
                }
            }
        }).start();
    }
}
