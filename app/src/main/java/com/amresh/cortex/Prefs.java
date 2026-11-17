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
    private static final String K_BIRTH = "birth_millis";
    private static final String K_BIRTH_HAS_TIME = "birth_has_time";
    private static final String K_LIFE_UPDATED = "life_updated_at";
    private static final String K_FAB_X = "fab_x";
    private static final String K_FAB_Y = "fab_y";

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

    // ------------------------------------------------------- profile / life counter

    /** Date (and time, if known) of birth in epoch millis, or 0 when not set yet. */
    public static long getBirthMillis(Context c) {
        return sp(c).getLong(K_BIRTH, 0);
    }

    /** True when the user also gave a time of birth (otherwise midnight is used). */
    public static boolean hasBirthTime(Context c) {
        return sp(c).getBoolean(K_BIRTH_HAS_TIME, false);
    }

    public static void setBirth(Context c, long millis, boolean hasTime) {
        sp(c).edit().putLong(K_BIRTH, millis).putBoolean(K_BIRTH_HAS_TIME, hasTime).apply();
    }

    /** The moment the life counter was last updated by the user, or 0 if never. */
    public static long getLifeUpdatedAt(Context c) {
        return sp(c).getLong(K_LIFE_UPDATED, 0);
    }

    public static void setLifeUpdatedAt(Context c, long millis) {
        sp(c).edit().putLong(K_LIFE_UPDATED, millis).apply();
    }

    // ------------------------------------------------------- floating schedule button

    /** Saved position of the floating button as fractions (0..1) of the free space, or -1 if never moved. */
    public static float getFabX(Context c) {
        return sp(c).getFloat(K_FAB_X, -1f);
    }

    public static float getFabY(Context c) {
        return sp(c).getFloat(K_FAB_Y, -1f);
    }

    public static void setFabPosition(Context c, float x, float y) {
        sp(c).edit().putFloat(K_FAB_X, x).putFloat(K_FAB_Y, y).apply();
    }
}
