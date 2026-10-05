package com.projeto.egoodapp.security;

import com.projeto.egoodapp.views.privacy.PrivacyUi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.LocalSession;
import com.projeto.egoodapp.data.local.SecurityAudit;
import com.projeto.egoodapp.data.model.SecurityAuditEvent;
import com.projeto.egoodapp.views.auth.LoginActivity;
import java.lang.ref.WeakReference;

/** One deadline for every authenticated Activity; no background service or exact alarm. */
public final class SessionTimeoutController {
    private static SessionTimeoutController instance;
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WeakReference<Activity> foreground = new WeakReference<>(null);
    private SessionDeadline deadline;
    private boolean expiring;
    private final Runnable persist = this::persistSafely;
    private final Runnable timeout = () -> {
        Activity activity = foreground.get();
        if (activity != null && !activity.isDestroyed()) check(activity);
    };

    private SessionTimeoutController(Context context) { this.context = context.getApplicationContext(); }
    public static synchronized SessionTimeoutController get(Context context) {
        if (instance == null) instance = new SessionTimeoutController(context);
        return instance;
    }
    public void begin(String uid) {
        expiring = false;
        deadline = new SessionDeadline();
        deadline.touch(uid, System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot());
        LocalRepository.get(context).saveSessionDeadline(deadline);
        PrivacyUi.resetPrompt();
    }
    public void clear() {
        handler.removeCallbacks(persist); handler.removeCallbacks(timeout);
        deadline = null; foreground.clear(); PrivacyUi.resetPrompt();
        try { LocalRepository.get(context).saveSessionDeadline(null); }
        catch (RuntimeException ignored) { /* Logout remains mandatory on storage failure. */ }
    }
    public boolean resume(Activity activity) {
        foreground = new WeakReference<>(activity);
        return check(activity);
    }
    public void pause(Activity activity) {
        if (foreground.get() != activity) return;
        handler.removeCallbacks(timeout); handler.removeCallbacks(persist);
        persistSafely(); foreground.clear();
    }
    public boolean check(Activity activity) {
        if (expiring) return false;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { expire(activity, null, false); return false; }
        try {
            if (deadline == null) deadline = LocalRepository.get(context).sessionDeadline();
            // Upgrade migration: an existing session gets one initial deadline, never one on rotation.
            if (deadline == null) begin(user.getUid());
            long remaining = deadline.remaining(user.getUid(), System.currentTimeMillis(),
                    SystemClock.elapsedRealtime(), boot());
            if (remaining == 0) { expire(activity, user.getUid(), true); return false; }
            handler.removeCallbacks(timeout);
            handler.postDelayed(timeout, remaining);
            return true;
        } catch (RuntimeException storageFailure) {
            expire(activity, user.getUid(), false); return false;
        }
    }
    public boolean interact(Activity activity) {
        if (!check(activity)) return false;
        deadline.touch(deadline.userId, System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot());
        handler.removeCallbacks(timeout); handler.postDelayed(timeout, SessionDeadline.TIMEOUT_MS);
        // Batch rapid keyboard/touch events, flush the exact latest interaction on pause.
        handler.removeCallbacks(persist); handler.postDelayed(persist, 1000);
        return true;
    }
    private void persistSafely() {
        if (deadline == null || expiring) return;
        try { LocalRepository.get(context).saveSessionDeadline(deadline); }
        catch (RuntimeException ignored) { /* Next resume re-checks the persisted deadline. */ }
    }
    private int boot() {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
    }
    private void expire(Activity activity, String uid, boolean idle) {
        if (expiring) return;
        expiring = true;
        activity.getWindow().getDecorView().setVisibility(android.view.View.INVISIBLE);
        if (idle) SecurityAudit.record(context, SecurityAuditEvent.Type.SESSION_EXPIRED,
                SecurityAuditEvent.Result.SUCCESS, uid);
        LocalSession.signOut(context, () -> {
            context.startActivity(new Intent(context, LoginActivity.class)
                    .putExtra("session_expired", idle)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });
    }
}
