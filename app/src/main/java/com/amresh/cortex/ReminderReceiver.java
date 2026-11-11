package com.amresh.cortex;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Fires when a reminder alarm goes off. It shows the notification, and after the FIRST
 * reminder it schedules one second reminder (if the task is still not done).
 */
public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "task_reminders";

    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, ctx.getString(R.string.notif_channel),
                    NotificationManager.IMPORTANCE_HIGH);
            ch.enableVibration(true);
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        final long id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, 0);
        final String fallbackTitle = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE);
        final boolean repeat = intent.getBooleanExtra(AlarmScheduler.EXTRA_REPEAT, false);
        final Context app = ctx.getApplicationContext();
        final PendingResult result = goAsync();

        AppDatabase.IO.execute(() -> {
            try {
                Task t = AppDatabase.get(app).taskDao().getById(id);
                // The task was finished, deleted or removed after this alarm was set: stay quiet.
                if (t == null || t.done || t.deletedAt > 0) return;

                String title = (t.title != null && !t.title.isEmpty())
                        ? t.title : (fallbackTitle == null ? "" : fallbackTitle);

                createChannel(app);
                showNotification(app, id, title, repeat);

                // One second reminder only: it is not repeated again.
                if (!repeat && Prefs.isSecondReminderOn(app)) {
                    long at = System.currentTimeMillis()
                            + Prefs.getSecondReminderGapMin(app) * 60L * 1000L;
                    AlarmScheduler.scheduleRepeat(app, t, at);
                }
            } finally {
                result.finish();
            }
        });
    }

    private static void showNotification(Context ctx, long id, String title, boolean repeat) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(ctx, (int) id, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(ctx.getString(repeat ? R.string.notif_title_again : R.string.notif_title))
                .setContentText(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(title))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(content)
                .build();

        NotificationManagerCompat.from(ctx).notify((int) id, n);
    }
}
