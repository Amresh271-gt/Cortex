package com.amresh.cortex;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Exports tasks, memories and settings to a JSON string and restores them again. */
public class BackupManager {

    private static final String APP_ID = "Cortex";
    /** Backups made by the old Task Reminder app have the same layout and can be imported too. */
    private static final String OLD_APP_ID = "TaskReminder";

    public static class Result {
        public int added;
        public int skipped;
        public int memAdded;
        public int memSkipped;
        public String userName;   // null when the backup had no name
        public Integer nightMode; // null when the backup had no valid value
        public Integer accent;    // null when the backup had no valid value
    }

    public static String export(Context ctx, List<Task> tasks, List<Memory> memories) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("app", APP_ID);
        root.put("formatVersion", 2);
        root.put("exportedAt", System.currentTimeMillis());

        JSONObject settings = new JSONObject();
        settings.put("userName", Prefs.getName(ctx));
        settings.put("nightMode", Prefs.getNightMode(ctx));
        settings.put("accent", Prefs.getAccent(ctx));
        root.put("settings", settings);

        JSONArray arr = new JSONArray();
        for (Task t : tasks) {
            JSONObject o = new JSONObject();
            o.put("title", t.title == null ? "" : t.title);
            o.put("notes", t.notes == null ? "" : t.notes);
            o.put("dueMillis", t.dueMillis);
            o.put("done", t.done);
            o.put("deletedAt", t.deletedAt);
            arr.put(o);
        }
        root.put("tasks", arr);

        JSONArray memArr = new JSONArray();
        for (Memory m : memories) {
            JSONObject o = new JSONObject();
            o.put("title", m.title == null ? "" : m.title);
            o.put("content", m.content == null ? "" : m.content);
            o.put("tag", m.tag == null ? "" : m.tag);
            o.put("pinned", m.pinned);
            o.put("createdAt", m.createdAt);
            o.put("updatedAt", m.updatedAt);
            memArr.put(o);
        }
        root.put("memories", memArr);
        return root.toString(2);
    }

    /**
     * Adds the tasks from a backup. Tasks that already exist (same title, time and bin state)
     * are skipped, so restoring twice does not create duplicates. Must run on a background thread.
     */
    public static Result importJson(Context ctx, AppDatabase db, String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        String appId = root.optString("app");
        if (!APP_ID.equals(appId) && !OLD_APP_ID.equals(appId)) {
            throw new JSONException("Not a Cortex or Task Reminder backup");
        }

        Result r = new Result();

        JSONObject s = root.optJSONObject("settings");
        if (s != null) {
            String name = s.optString("userName", "").trim();
            if (!name.isEmpty()) r.userName = name;
            if (s.has("nightMode")) {
                int m = s.optInt("nightMode", Integer.MIN_VALUE);
                if (m == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        || m == AppCompatDelegate.MODE_NIGHT_NO
                        || m == AppCompatDelegate.MODE_NIGHT_YES) {
                    r.nightMode = m;
                }
            }
            if (s.has("accent")) {
                int a = s.optInt("accent", -1);
                if (a >= Prefs.ACCENT_PURPLE && a <= Prefs.ACCENT_PINK) r.accent = a;
            }
        }

        Set<String> existing = new HashSet<>();
        for (Task t : db.taskDao().getAll()) {
            existing.add(key(t.title, t.dueMillis, t.deletedAt));
        }

        long now = System.currentTimeMillis();
        JSONArray arr = root.optJSONArray("tasks");
        if (arr == null) arr = new JSONArray();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String title = o.optString("title", "").trim();
            if (title.isEmpty()) continue;

            long due = o.optLong("dueMillis", 0);
            long deletedAt = o.optLong("deletedAt", 0);
            // Bin items past the 15-day limit are not worth bringing back.
            if (deletedAt > 0 && deletedAt < now - AppDatabase.BIN_RETENTION_MILLIS) continue;

            if (!existing.add(key(title, due, deletedAt))) {
                r.skipped++;
                continue;
            }

            Task t = new Task();
            t.title = title;
            String notes = o.optString("notes", "");
            t.notes = notes.isEmpty() ? null : notes;
            t.dueMillis = due;
            t.done = o.optBoolean("done", false);
            t.deletedAt = deletedAt;
            t.id = db.taskDao().insert(t);
            if (t.deletedAt == 0 && !t.done) {
                AlarmScheduler.schedule(ctx, t);
            }
            r.added++;
        }

        // Memories (older v1 backups have none, which is fine).
        Set<String> existingMem = new HashSet<>();
        for (Memory m : db.memoryDao().getAll()) {
            existingMem.add(memKey(m.title, m.content));
        }
        JSONArray memArr = root.optJSONArray("memories");
        if (memArr != null) {
            for (int i = 0; i < memArr.length(); i++) {
                JSONObject o = memArr.optJSONObject(i);
                if (o == null) continue;
                String title = o.optString("title", "").trim();
                if (title.isEmpty()) continue;
                String content = o.optString("content", "");
                if (!existingMem.add(memKey(title, content))) {
                    r.memSkipped++;
                    continue;
                }
                String tag = o.optString("tag", "").trim();
                Memory m = new Memory();
                m.title = title;
                m.content = content;
                m.tag = tag.isEmpty() ? null : tag;
                m.pinned = o.optBoolean("pinned", false);
                m.createdAt = o.optLong("createdAt", now);
                m.updatedAt = o.optLong("updatedAt", m.createdAt);
                db.memoryDao().insert(m);
                r.memAdded++;
            }
        }
        return r;
    }

    private static String memKey(String title, String content) {
        return (title == null ? "" : title) + "|" + (content == null ? "" : content);
    }

    private static String key(String title, long due, long deletedAt) {
        return title + "|" + due + "|" + (deletedAt > 0 ? "bin" : "active");
    }
}
