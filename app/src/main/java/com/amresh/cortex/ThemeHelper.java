package com.amresh.cortex;

import android.app.Activity;

/** Applies the saved accent colour. Call before super.onCreate() in every activity. */
public class ThemeHelper {

    public static void apply(Activity a) {
        switch (Prefs.getAccent(a)) {
            case Prefs.ACCENT_TEAL:
                a.setTheme(R.style.Theme_Cortex_Teal);
                break;
            case Prefs.ACCENT_BLUE:
                a.setTheme(R.style.Theme_Cortex_Blue);
                break;
            case Prefs.ACCENT_ORANGE:
                a.setTheme(R.style.Theme_Cortex_Orange);
                break;
            case Prefs.ACCENT_PINK:
                a.setTheme(R.style.Theme_Cortex_Pink);
                break;
            default:
                a.setTheme(R.style.Theme_Cortex);
        }
    }
}
