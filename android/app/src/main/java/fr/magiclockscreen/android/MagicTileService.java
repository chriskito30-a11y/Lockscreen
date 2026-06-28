package fr.magiclockscreen.android;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class MagicTileService extends TileService {
    @Override
    public void onClick() {
        super.onClick();

        setTileState(Tile.STATE_ACTIVE, "Magic...");

        new Thread(() -> {
            try {
                String backend = MagicPrefs.backend(getApplicationContext());
                String token = MagicPrefs.token(getApplicationContext());

                if (!backend.isEmpty() && !token.isEmpty()) {
                    MagicResult result = MagicWallpaperClient.updateLockscreen(
                            getApplicationContext(),
                            backend,
                            token
                    );
                    MagicPrefs.setLastHash(getApplicationContext(), result.hash);
                }
            } catch (Exception ignored) {
            } finally {
                setTileState(Tile.STATE_INACTIVE, "Magic Lock");
            }
        }).start();
    }

    private void setTileState(int state, String label) {
        Tile tile = getQsTile();

        if (tile != null) {
            tile.setState(state);
            tile.setLabel(label);
            tile.updateTile();
        }
    }
}
