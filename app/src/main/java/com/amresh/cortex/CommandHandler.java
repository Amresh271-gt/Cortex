package com.amresh.cortex;

import android.content.Context;
import android.text.format.DateFormat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single place where a spoken (or typed) command is understood and carried out.
 * The Quick Settings tile and the Assistant tab both call handle().
 *
 * Routing rules:
 *   1. "save as task" / "save as memory" said anywhere decides it.
 *   2. "remind me" means a Task; "remember", "note" or "memo" means a Memory
 *      (whichever comes first if both are said).
 *   3. No keyword at all means an Assistance request (open an app, go home ...).
 *      If Assistance cannot do it, Cortex says sorry.
 */
public class CommandHandler {

    public enum Kind { TASK, MEMORY, ASSISTANCE }

    /** What happened, in words that can be shown and spoken. */
    public static class Result {
        public final Kind kind;
        public final String message;

        Result(Kind kind, String message) {
            this.kind = kind;
            this.message = message;
        }
    }

    private static final int TITLE_MAX = 60;

    private static final Pattern GREETING = Pattern.compile(
            "^(?:hey|hi|hello|ok|okay)\\s+cortex[,.]?\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SAVE_AS = Pattern.compile(
            "\\b(?:and\\s+)?save\\s+(?:this\\s+|it\\s+)?as\\s+(?:a\\s+|an\\s+)?(task|reminder|memory|note|memo)\\b[.,]?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern REMIND = Pattern.compile("\\bremind\\s+me\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEM_KEY = Pattern.compile("\\b(?:remember|note|memo)\\b", Pattern.CASE_INSENSITIVE);
    /** "remember that ...", "note down ...", "make a note that ..." at the start of a memory. */
    private static final Pattern MEM_STRIP = Pattern.compile(
            "^(?:please\\s+)?(?:(?:make|take|add)\\s+(?:a\\s+)?)?(?:remember|note|memo)(?:\\s+down)?"
                    + "(?:\\s+(?:that|to|this))?\\s*[:,]?\\s*",
            Pattern.CASE_INSENSITIVE);

    /** Understands the text, does the work and returns what to tell the user. */
    public static Result handle(Context ctx, String spoken) {
        final Context app = ctx.getApplicationContext();
        String text = spoken == null ? "" : spoken.trim();
        text = GREETING.matcher(text).replaceFirst("").trim();

        Kind kind;
        Matcher sa = SAVE_AS.matcher(text);
        if (sa.find()) {
            String what = sa.group(1).toLowerCase();
            kind = (what.equals("task") || what.equals("reminder")) ? Kind.TASK : Kind.MEMORY;
            text = (text.substring(0, sa.start()) + " " + text.substring(sa.end())).trim();
        } else {
            int iTask = indexOf(REMIND, text);
            int iMem = indexOf(MEM_KEY, text);
            if (iTask < 0 && iMem < 0) {
                kind = Kind.ASSISTANCE;
            } else if (iMem < 0 || (iTask >= 0 && iTask <= iMem)) {
                kind = Kind.TASK;
            } else {
                kind = Kind.MEMORY;
            }
        }

        switch (kind) {
            case TASK:
                return addTask(app, text);
            case MEMORY:
                return addMemory(app, text);
            default:
                return assist(app, text);
        }
    }

    private static int indexOf(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? m.start() : -1;
    }

    // ----------------------------------------------------------------- task

    private static Result addTask(Context app, String text) {
        if (text.trim().isEmpty()) {
            return new Result(Kind.TASK, app.getString(R.string.cmd_nothing_to_save));
        }
        TimeParser.Result r = TimeParser.parse(text);
        final Task t = new Task();
        t.title = r.title;
        t.dueMillis = r.dueMillis;
        t.done = false;

        final Context appCtx = app;
        AppDatabase.IO.execute(() -> {
            t.id = AppDatabase.get(appCtx).taskDao().insert(t);
            AlarmScheduler.schedule(appCtx, t);
        });

        String message;
        if (t.dueMillis > 0) {
            message = app.getString(R.string.cmd_task_set_time, t.title,
                    DateFormat.format("EEE, d MMM, h:mm a", t.dueMillis));
        } else {
            message = app.getString(R.string.cmd_task_added, t.title);
        }
        return new Result(Kind.TASK, message);
    }

    // --------------------------------------------------------------- memory

    private static Result addMemory(Context app, String text) {
        String cleaned = MEM_STRIP.matcher(text).replaceFirst("").trim();
        if (cleaned.isEmpty()) {
            return new Result(Kind.MEMORY, app.getString(R.string.cmd_nothing_to_save));
        }
        cleaned = Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);

        final Memory m = new Memory();
        m.title = makeTitle(cleaned);
        // A short note fits in the title; a long one keeps its full text as the content.
        m.content = cleaned.length() > TITLE_MAX ? cleaned : "";
        m.tag = null;
        m.pinned = false;
        long now = System.currentTimeMillis();
        m.createdAt = now;
        m.updatedAt = now;

        final Context appCtx = app;
        AppDatabase.IO.execute(() -> AppDatabase.get(appCtx).memoryDao().insert(m));

        return new Result(Kind.MEMORY, app.getString(R.string.cmd_memory_saved, m.title));
    }

    private static String makeTitle(String s) {
        if (s.length() <= TITLE_MAX) return s;
        int cut = s.lastIndexOf(' ', TITLE_MAX);
        if (cut < 20) cut = TITLE_MAX;
        return s.substring(0, cut).trim() + "…";
    }

    // ----------------------------------------------------------- assistance

    private static Result assist(Context app, String text) {
        AssistantActions.Reply r = AssistantActions.tryHandle(app, text);
        if (r.handled) return new Result(Kind.ASSISTANCE, r.message);
        return new Result(Kind.ASSISTANCE, app.getString(R.string.assistant_sorry));
    }
}
