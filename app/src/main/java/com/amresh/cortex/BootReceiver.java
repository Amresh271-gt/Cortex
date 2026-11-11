package com.amresh.cortex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

/** Re-creates all pending alarms after the phone restarts (alarms are wiped on reboot). */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context ctx, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        final PendingResult result = goAsync();
        final Context app = ctx.getApplicationContext();
        AppDatabase.IO.execute(() -> {
            try {
                List<Task> pending = AppDatabase.get(app).taskDao().getPendingWithTime();
                for (Task t : pending) {
                    AlarmScheduler.schedule(app, t);
                }
            } finally {
                result.finish();
            }
        });
    }
}
