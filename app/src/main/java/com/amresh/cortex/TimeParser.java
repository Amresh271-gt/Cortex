package com.amresh.cortex;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls a due time out of typed or spoken text such as
 * "Hey XYZ, I have to do my homework before 5:00 a.m."
 * Supports: "5 pm", "5:30 a.m.", "17:30", "in 20 minutes", "after 2 minutes", "in 2 hours", "tomorrow".
 */
public class TimeParser {

    public static class Result {
        public final String title;
        public final long dueMillis;

        Result(String title, long dueMillis) {
            this.title = title;
            this.dueMillis = dueMillis;
        }
    }

    private static final Pattern AMPM = Pattern.compile(
            "(\\d{1,2})(?:[:.](\\d{2}))?\\s*([ap])\\.?\\s?m\\.?", Pattern.CASE_INSENSITIVE);
    private static final Pattern H24 = Pattern.compile("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b");
    private static final Pattern RELATIVE = Pattern.compile(
            "\\b(?:in|after)\\s+(\\d{1,3})\\s*(minute|minutes|min|mins|hour|hours|hr|hrs)\\b", Pattern.CASE_INSENSITIVE);
    // "Hey Cortex, ..." greeting. The lookahead stops it from eating "remind" in "Hey remind me ...".
    private static final Pattern PREFIX = Pattern.compile(
            "^(hey|hi|hello|ok|okay)\\s+(?!remind\\b|remember\\b|note\\b|memo\\b)\\w+[,]?\\s*",
            Pattern.CASE_INSENSITIVE);
    // Plain "Hey" / "Ok" in front of "remind me ..." (no name in between).
    private static final Pattern GREET_ONLY = Pattern.compile(
            "^(hey|hi|hello|ok|okay)[,]?\\s+(?=remind\\b|remember\\b|note\\b|memo\\b)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LEAD = Pattern.compile(
            "^(please\\s+)?(remind me to|remind me|i have to|i need to|i must|i want to|i will|i should)\\s+",
            Pattern.CASE_INSENSITIVE);

    public static Result parse(String raw) {
        String text = raw == null ? "" : raw.trim();
        String lower = text.toLowerCase();
        Calendar now = Calendar.getInstance();
        Calendar due = Calendar.getInstance();
        due.set(Calendar.SECOND, 0);
        due.set(Calendar.MILLISECOND, 0);
        boolean found = false;

        Matcher rel = RELATIVE.matcher(text);
        Matcher ap = AMPM.matcher(text);
        Matcher h24 = H24.matcher(text);

        if (rel.find()) {
            int n = Integer.parseInt(rel.group(1));
            boolean hours = rel.group(2).toLowerCase().startsWith("h");
            due.setTimeInMillis(now.getTimeInMillis() + n * (hours ? 3600000L : 60000L));
            due.set(Calendar.SECOND, 0);
            found = true;
        } else if (ap.find()) {
            int hour = Integer.parseInt(ap.group(1));
            int min = ap.group(2) == null ? 0 : Integer.parseInt(ap.group(2));
            boolean pm = ap.group(3).equalsIgnoreCase("p");
            if (hour >= 1 && hour <= 12 && min < 60) {
                if (hour == 12) hour = 0;
                if (pm) hour += 12;
                due.set(Calendar.HOUR_OF_DAY, hour);
                due.set(Calendar.MINUTE, min);
                found = true;
            }
        } else if (h24.find()) {
            due.set(Calendar.HOUR_OF_DAY, Integer.parseInt(h24.group(1)));
            due.set(Calendar.MINUTE, Integer.parseInt(h24.group(2)));
            found = true;
        }

        if (found && !rel.reset().find()) {
            if (lower.contains("tomorrow")) {
                due.add(Calendar.DAY_OF_YEAR, 1);
            } else if (due.getTimeInMillis() <= now.getTimeInMillis()) {
                due.add(Calendar.DAY_OF_YEAR, 1); // time already passed today -> next occurrence
            }
        }

        String title = GREET_ONLY.matcher(text).replaceFirst("");
        title = PREFIX.matcher(title).replaceFirst("");
        title = LEAD.matcher(title).replaceFirst("").trim();
        if (title.isEmpty()) title = text;
        if (!title.isEmpty()) {
            title = Character.toUpperCase(title.charAt(0)) + title.substring(1);
        }

        return new Result(title, found ? due.getTimeInMillis() : 0);
    }
}
