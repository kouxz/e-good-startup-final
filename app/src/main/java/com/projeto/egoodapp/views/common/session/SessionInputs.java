package com.projeto.egoodapp.views.common.session;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.security.SessionTimeoutController;

/** IME commitText often sends no KeyEvent, so focused edits also count as activity. */
final class SessionInputs {
    private SessionInputs() {}
    static void watch(Activity activity, View root) {
        if (root instanceof EditText && root.getTag(R.id.session_input_watched) == null) {
            root.setTag(R.id.session_input_watched, true);
            EditText field = (EditText) root;
            field.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (field.hasFocus() && field.hasWindowFocus()) SessionTimeoutController.get(activity).interact(activity);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) watch(activity, group.getChildAt(i));
        }
    }
}
