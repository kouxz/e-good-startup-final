package com.projeto.egoodapp.views.common.session;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.projeto.egoodapp.security.SessionTimeoutController;

/** Dialogs have their own Window: their taps and keyboard events also renew the idle deadline. */
public final class SessionDialogBuilder extends MaterialAlertDialogBuilder {
    private final Activity activity;
    public SessionDialogBuilder(Context context, int theme) { super(context, theme); activity = activity(context); }
    public SessionDialogBuilder(Context context) { super(context); activity = activity(context); }
    @Override public AlertDialog create() {
        AlertDialog dialog = super.create();
        Window window = dialog.getWindow();
        if (activity instanceof AuthenticatedActivity && window != null) {
            window.setCallback(new WindowEvents(window.getCallback()) {
                @Override public boolean dispatchTouchEvent(MotionEvent event) {
                    if (!SessionTimeoutController.get(activity).interact(activity)) return true;
                    return super.dispatchTouchEvent(event);
                }
                @Override public boolean dispatchKeyEvent(KeyEvent event) {
                    if (!SessionTimeoutController.get(activity).interact(activity)) return true;
                    return super.dispatchKeyEvent(event);
                }
                @Override public void onWindowFocusChanged(boolean focused) {
                    super.onWindowFocusChanged(focused);
                    if (focused) SessionInputs.watch(activity, window.getDecorView());
                }
            });
        }
        return dialog;
    }
    private static Activity activity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) break;
            context = next;
        }
        return null;
    }
}
