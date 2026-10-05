package com.projeto.egoodapp.data.local;

import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.model.SecurityAuditEvent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projeto.egoodapp.views.auth.LoginActivity;

public final class LocalSession {
    private static final long CREDENTIAL_CLEANUP_TIMEOUT_MS = 2000;
    private LocalSession() {}
    public static AccountProfile current(Activity activity, String expectedType) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !AccountAccess.knownType(expectedType)) return null;
        AccountProfile profile = LocalRepository.get(activity).account(user.getUid());
        return AccountAccess.evaluate(profile, expectedType) == AccountAccess.Decision.ALLOW
                ? profile : null;
    }
    public static void logout(Activity activity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) SecurityAudit.record(activity, SecurityAuditEvent.Type.LOGOUT,
                SecurityAuditEvent.Result.SUCCESS, user.getUid());
        signOut(activity, () -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            activity.startActivity(new Intent(activity, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            activity.finish();
        });
    }
    public static void signOut(Context context, Runnable completed) {
        com.projeto.egoodapp.chat.ChatSession.clear();
        com.projeto.egoodapp.security.SessionTimeoutController.get(context).clear();
        FirebaseAuth.getInstance().signOut();
        Context app = context.getApplicationContext();
        Handler handler = new Handler(Looper.getMainLooper());
        CancellationSignal cancellation = new CancellationSignal();
        CleanupDeadline.run(done -> {
            CredentialManager.create(app).clearCredentialStateAsync(new ClearCredentialStateRequest(),
                    cancellation, ContextCompat.getMainExecutor(app),
                    new CredentialManagerCallback<Void, ClearCredentialException>() {
                        @Override public void onResult(Void result) { done.run(); }
                        @Override public void onError(ClearCredentialException error) { done.run(); }
                    });
        }, new CleanupDeadline.Scheduler() {
            @Override public void schedule(Runnable timeout) {
                handler.postDelayed(timeout, CREDENTIAL_CLEANUP_TIMEOUT_MS);
            }
            @Override public void cancel(Runnable timeout) { handler.removeCallbacks(timeout); }
        }, cancellation::cancel, completed);
    }
}
