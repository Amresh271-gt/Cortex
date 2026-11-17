package com.amresh.cortex;

import android.content.Context;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps the 7-day schedule (today + the next 6 days) in step with the task list.
 * Always call {@link #sync} from the database thread ({@link AppDatabase#IO}).
 */
public class ScheduleManager {

    public static final int DAYS = 7;

    /** Past schedules stay in the schedule bin this long (same 15 days as deleted tasks). */
    public static final long BIN_RETENTION_MILLIS = AppDatabase.BIN_RETENTION_MILLIS;

    /** Midnight (local time) of the day that contains the given time. */
    public static long startOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Midnight of the day that is {@code days} days after the given midnight. */
    public static long addDays(long dayStart, int days) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dayStart);
        c.add(Calendar.DAY_OF_YEAR, days);
        return c.getTimeInMillis();
    }

    /**
     * 1. Moves schedules of past days to the schedule bin and clears bin entries older than 15 days.
     * 2. Adds an entry for every active task due within the 7 days (once per task and day).
     * 3. Updates / drops task entries when the task was edited, finished or deleted.
     * Entries the user added by hand are never touched here, and the task list is never changed.
     */
    public static void sync(Context context) {
        AppDatabase db = AppDatabase.get(context);
        long now = System.currentTimeMillis();
        long today = startOfDay(now);
        long end = addDays(today, DAYS); // exclusive

        ScheduleDao sd = db.scheduleDao();
        sd.archiveBefore(today, now);
        sd.purgeArchivedBefore(now - BIN_RETENTION_MILLIS);

        Map<Long, Task> activeTasks = new HashMap<>();
        for (Task t : db.taskDao().getAll()) {
            if (t.deletedAt == 0) activeTasks.put(t.id, t);
        }

        Set<String> have = new HashSet<>();
        for (ScheduleEntry e : sd.getActiveTaskEntries()) {
            Task t = activeTasks.get(e.taskId);
            boolean stillValid = t != null && t.dueMillis > 0 && startOfDay(t.dueMillis) == e.dayStart;
            if (!stillValid) {
                // Task deleted, its time removed, or moved to another day.
                sd.delete(e);
                continue;
            }
            have.add(key(e.taskId, e.dayStart));
            if (!equal(e.title, t.title) || e.timeMillis != t.dueMillis || e.done != t.done) {
                e.title = t.title;
                e.timeMillis = t.dueMillis;
                e.done = t.done;
                sd.update(e);
            }
        }

        for (Task t : activeTasks.values()) {
            if (t.dueMillis <= 0) continue; // tasks without a time have no day to be scheduled on
            long day = startOfDay(t.dueMillis);
            if (day < today || day >= end) continue;
            if (have.contains(key(t.id, day))) continue;
            ScheduleEntry e = new ScheduleEntry();
            e.dayStart = day;
            e.timeMillis = t.dueMillis;
            e.title = t.title;
            e.taskId = t.id;
            e.done = t.done;
            sd.insert(e);
        }
    }

    private static String key(long taskId, long dayStart) {
        return taskId + "@" + dayStart;
    }

    private static boolean equal(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
