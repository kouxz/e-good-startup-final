package com.projeto.egoodapp;

import com.projeto.egoodapp.theme.AppThemeController;

import android.app.Application;

public class EGoodApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppThemeController.applySavedMode(this);
        com.projeto.egoodapp.security.SessionTimeoutController.get(this);
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(auth ->
                com.projeto.egoodapp.chat.ChatSession.syncUser(auth.getCurrentUser() == null
                        ? null : auth.getCurrentUser().getUid()));
    }
}
