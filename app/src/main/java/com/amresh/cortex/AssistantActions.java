package com.amresh.cortex;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the Assistant can do for a request that has no task or memory keyword.
 *
 * Cortex 1.0:  "open <app>"  and  "go home" / "close" (closing = pressing Home, nothing is killed).
 * Not yet:     opening files (answered with a polite sorry).
 */
public class AssistantActions {

    /** handled = false means "I do not know this command", and the caller says sorry. */
    public static class Reply {
        public final boolean handled;
        public final String message;

        Reply(boolean handled, String message) {
            this.handled = handled;
            this.message = message;
        }
    }

    /** "go home", "home screen", "close", "close WhatsApp", "exit" ... all mean: press Home. */
    private static final Pattern HOME = Pattern.compile(
            "^(?:please\\s+)?(?:go\\s+(?:to\\s+)?(?:the\\s+)?home(?:\\s*screen)?|take\\s+me\\s+home"
                    + "|home(?:\\s*screen)?|(?:close|exit|quit)\\b.*)$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern OPEN = Pattern.compile(
            "^(?:please\\s+)?(?:open|launch|start|run)\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern LOOKS_LIKE_FILE = Pattern.compile(".*\\.[a-z0-9]{2,5}$", Pattern.CASE_INSENSITIVE);

    public static Reply tryHandle(Context app, String raw) {
        String text = raw == null ? "" : raw.trim();
        // Speech results sometimes end with a full stop or similar.
        text = text.replaceAll("[.!?]+$", "").trim();
        if (text.isEmpty()) return new Reply(false, "");

        if (HOME.matcher(text).matches()) {
            goHome(app);
            return new Reply(true, app.getString(R.string.assistant_home));
        }

        Matcher open = OPEN.matcher(text);
        if (open.matches()) {
            String target = open.group(1).trim();
            // "open WhatsApp app" -> "WhatsApp"
            target = target.replaceAll("(?i)\\s+(?:app|application)$", "").trim();
            target = target.replaceAll("(?i)^(?:the|my)\\s+", "").trim();
            if (target.isEmpty()) return new Reply(false, "");

            if (LOOKS_LIKE_FILE.matcher(target).matches()) {
                return new Reply(true, app.getString(R.string.assistant_file_not_ready));
            }
            return openApp(app, target);
        }

        return new Reply(false, "");
    }

    // ------------------------------------------------------------- go home

    private static void goHome(Context app) {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        app.startActivity(home);
    }

    // ------------------------------------------------------------- open app

    private static Reply openApp(Context app, String target) {
        PackageManager pm = app.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);

        String wanted = normalise(target);
        if (wanted.isEmpty()) return new Reply(false, "");

        String bestPackage = null;
        String bestLabel = null;
        int bestScore = 0;

        for (ResolveInfo ri : apps) {
            String label = String.valueOf(ri.loadLabel(pm));
            String n = normalise(label);
            if (n.isEmpty()) continue;

            int score = 0;
            if (n.equals(wanted)) {
                score = 4;
            } else if (n.startsWith(wanted)) {
                score = 3;
            } else if (n.contains(wanted)) {
                score = 2;
            } else if (n.length() >= 3 && wanted.contains(n)) {
                score = 1;
            }
            if (score == 0) continue;

            boolean better = score > bestScore
                    || (score == bestScore && bestLabel != null && label.length() < bestLabel.length());
            if (better) {
                bestScore = score;
                bestPackage = ri.activityInfo.packageName;
                bestLabel = label;
            }
        }

        if (bestPackage != null) {
            Intent launch = pm.getLaunchIntentForPackage(bestPackage);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                app.startActivity(launch);
                return new Reply(true, app.getString(R.string.assistant_opening, bestLabel));
            }
        }
        return new Reply(true, app.getString(R.string.assistant_app_not_found, target));
    }

    /** Lower case, letters and digits only: "What's App" and "whatsapp" match each other. */
    private static String normalise(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
