package com.projeto.egoodapp.theme;

import static org.junit.Assert.assertEquals;

import androidx.appcompat.app.AppCompatDelegate;
import org.junit.Test;

public class AppThemeControllerTest {
    @Test
    public void mapsSavedPreferenceToExplicitDayNightMode() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppThemeController.modeFor(true));
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppThemeController.modeFor(false));
    }
}
