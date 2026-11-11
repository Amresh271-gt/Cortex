package com.amresh.cortex;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/** Applies the saved light/dark choice as soon as the app process starts. */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(Prefs.getNightMode(this));
    }
}
