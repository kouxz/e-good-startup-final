package com.projeto.egoodapp.theme;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class AppThemeController {
    private static final String PREFERENCES = "ui_preferences";
    private static final String DARK_MODE_ENABLED = "dark_mode_enabled";

    private AppThemeController() {}

    public static boolean isDarkModeEnabled(Context context) {
        return preferences(context).getBoolean(DARK_MODE_ENABLED, false);
    }

    public static void applySavedMode(Context context) {
        AppCompatDelegate.setDefaultNightMode(modeFor(isDarkModeEnabled(context)));
    }

    public static void setDarkModeEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(DARK_MODE_ENABLED, enabled).apply();
        AppCompatDelegate.setDefaultNightMode(modeFor(enabled));
    }

    static int modeFor(boolean enabled) {
        return enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}
