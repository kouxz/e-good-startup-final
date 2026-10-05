package com.projeto.egoodapp.views.common.session;

import android.view.KeyEvent;
import android.view.MotionEvent;
import androidx.appcompat.app.AppCompatActivity;
import com.projeto.egoodapp.views.privacy.PrivacyUi;
import com.projeto.egoodapp.security.SessionTimeoutController;

/** Guards restored sessions and counts actual interaction, not lifecycle recreation. */
public abstract class AuthenticatedActivity extends AppCompatActivity {
    @Override protected void onResume() {
        super.onResume(); SessionTimeoutController.get(this).resume(this);
    }
    @Override protected void onPostResume() {
        super.onPostResume();
        if (SessionTimeoutController.get(this).check(this)) {
            SessionInputs.watch(this, getWindow().getDecorView());
            PrivacyUi.maybeShow(this);
        }
    }
    @Override protected void onPause() {
        SessionTimeoutController.get(this).pause(this); super.onPause();
    }
    @Override public void onUserInteraction() {
        super.onUserInteraction(); SessionTimeoutController.get(this).interact(this);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (!SessionTimeoutController.get(this).check(this)) return true;
        return super.dispatchTouchEvent(event);
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (!SessionTimeoutController.get(this).check(this)) return true;
        return super.dispatchKeyEvent(event);
    }
}
