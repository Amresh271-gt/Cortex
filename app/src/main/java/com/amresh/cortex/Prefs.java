package com.amresh.cortex;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Small wrapper around SharedPreferences for the user name, theme and reminder settings. */
public class Prefs {

    public static final int ACCENT_PURPLE = 0;
    public static final int ACCENT_TEAL = 1;
    public static final int ACCENT_BLUE = 2;
    public static final int ACCENT_ORANGE = 3;
    public static final int ACCENT_PINK = 4;

    /** Default gap between the first reminder and the second one. */
    public static final int DEFAULT_SECOND_GAP_MIN = 30;

    private static final String FILE = "cortex_prefs";
    private static final String K_NAME = "user_name";
    private static final String K_NAME_ASKED = "name_asked";
    private static final String K_NIGHT = "night_mode";
    private static final String K_ACCENT = "accent";
    private static final String K_SECOND_ON = "second_reminder_on";
    private static final String K_SECOND_GAP = "second_reminder_gap_min";
    private static final String K_SPOKEN = "spoken_replies";

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String getName(Context c) {
        return sp(c).getString(K_NAME, "");
    }

    public static void setName(Context c, String name) {
        sp(c).edit().putString(K_NAME, name == null ? "" : name.trim()).apply();
    }

    public static boolean wasNameAsked(Context c) {
        return sp(c).getBoolean(K_NAME_ASKED, false);
    }

    public static void setNameAsked(Context c) {
        sp(c).edit().putBoolean(K_NAME_ASKED, true).apply();
    }

    public static int getNightMode(Context c) {
        return sp(c).getInt(K_NIGHT, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setNightMode(Context c, int mode) {
        sp(c).edit().putInt(K_NIGHT, mode).apply();
    }

    public static int getAccent(Context c) {
        return sp(c).getInt(K_ACCENT, ACCENT_PURPLE);
    }

    public static void setAccent(Context c, int accent) {
        sp(c).edit().putInt(K_ACCENT, accent).apply();
    }

    // ------------------------------------------------------- second reminder

    /** One extra reminder for a task that is still not marked done. On by default. */
    public static boolean isSecondReminderOn(Context c) {
        return sp(c).getBoolean(K_SECOND_ON, true);
    }

    public static void setSecondReminderOn(Context c, boolean on) {
        sp(c).edit().putBoolean(K_SECOND_ON, on).apply();
    }

    /** Minutes between the first and the second reminder. */
    public static int getSecondReminderGapMin(Context c) {
        int m = sp(c).getInt(K_SECOND_GAP, DEFAULT_SECOND_GAP_MIN);
        return m > 0 ? m : DEFAULT_SECOND_GAP_MIN;
    }

    public static void setSecondReminderGapMin(Context c, int minutes) {
        sp(c).edit().putInt(K_SECOND_GAP, minutes).apply();
    }

    // -------------------------------------------------------- spoken replies

    /** When on, voice commands are answered out loud as well as on screen. */
    public static boolean isSpokenOn(Context c) {
        return sp(c).getBoolean(K_SPOKEN, true);
    }

    public static void setSpokenOn(Context c, boolean on) {
        sp(c).edit().putBoolean(K_SPOKEN, on).apply();
    }
}
