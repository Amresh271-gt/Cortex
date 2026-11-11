package com.amresh.cortex;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Schedules the first reminder at the task's due time and, if the task is still not done
 * when it fires, one second reminder a little later (see ReminderReceiver).
 */
public class AlarmScheduler {

    public static final String EXTRA_ID = "task_id";
    public static final String EXTRA_TITLE = "task_title";
    public static final String EXTRA_REPEAT = "is_repeat";

    /** The second reminder uses a different action so it is a separate alarm from the first. */
    private static final String ACTION_REPEAT = "com.amresh.cortex.REPEAT_REMINDER";

    private static PendingIntent pendingIntent(Context ctx, long id, String title,
                                               boolean repeat, int extraFlags) {
        Intent i = new Intent(ctx, ReminderReceiver.class);
        if (repeat) i.setAction(ACTION_REPEAT);
        i.putExtra(EXTRA_ID, id);
        i.putExtra(EXTRA_TITLE, title);
        i.putExtra(EXTRA_REPEAT, repeat);
        return PendingIntent.getBroadcast(ctx, (int) id, i,
                PendingIntent.FLAG_IMMUTABLE | extraFlags);
    }

    private static void set(Context ctx, Task task, long triggerMillis, boolean repeat) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = pendingIntent(ctx, task.id, task.title, repeat,
                PendingIntent.FLAG_UPDATE_CURRENT);

        boolean exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
        if (exactAllowed) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
        } else {
            // Exact-alarm permission not granted: still fire, just possibly a few minutes late.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
        }
    }

    /** First reminder, at the task's due time. */
    public static void schedule(Context ctx, Task task) {
        if (task.dueMillis <= System.currentTimeMillis() || task.done) return;
        set(ctx, task, task.dueMillis, false);
    }

    /** Second reminder, at the given time. */
    public static void scheduleRepeat(Context ctx, Task task, long triggerMillis) {
        if (task.done || task.deletedAt > 0) return;
        set(ctx, task, triggerMillis, true);
    }

    /** Cancels both the first and the second reminder of a task. */
    public static void cancel(Context ctx, Task task) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(pendingIntent(ctx, task.id, task.title, false, PendingIntent.FLAG_UPDATE_CURRENT));
        am.cancel(pendingIntent(ctx, task.id, task.title, true, PendingIntent.FLAG_UPDATE_CURRENT));
    }
}
