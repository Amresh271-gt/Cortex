package com.amresh.cortex;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * The Quick Settings tile. Swipe down from anywhere, tap the tile, speak.
 * The system keeps this service alive, so the tile stays available even after
 * the app is swiped out of Recents.
 */
public class CommandTileService extends TileService {

    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setLabel(getString(R.string.cmd_tile_label));
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_mic));
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }

    @Override
    public void onClick() {
        if (isLocked()) {
            unlockAndRun(this::launchVoice);
        } else {
            launchVoice();
        }
    }

    private void launchVoice() {
        Intent i = new Intent(this, VoiceCommandActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires the PendingIntent version for apps targeting 34.
            startActivityAndCollapse(PendingIntent.getActivity(
                    this, 0, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            startActivityAndCollapse(i);
        }
    }
}
