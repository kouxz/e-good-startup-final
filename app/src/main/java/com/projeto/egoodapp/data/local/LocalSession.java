package com.projeto.egoodapp.data.local;

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
import com.projeto.egoodapp.views.user.LoginActivity;

public final class LocalSession {
    private static final long CREDENTIAL_CLEANUP_TIMEOUT_MS = 2000;
    private LocalSession() {}
    public static AccountProfile current(Activity activity, String legacyType) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? null : LocalRepository.get(activity).ensureAccount(user, legacyType);
    }
    public static void logout(Activity activity) {
        signOut(activity, () -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            activity.startActivity(new Intent(activity, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            activity.finish();
        });
    }
    public static void signOut(Context context, Runnable completed) {
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
