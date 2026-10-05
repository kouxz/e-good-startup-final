package com.projeto.egoodapp.views.common.form;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.security.PasswordPolicy;

public final class PasswordPolicyUi {
    private PasswordPolicyUi() {}
    public static void bind(EditText password, EditText confirm, TextView[] labels) {
        password.setFilters(new InputFilter[]{new InputFilter.LengthFilter(PasswordPolicy.MAX_LENGTH)});
        confirm.setFilters(new InputFilter[]{new InputFilter.LengthFilter(PasswordPolicy.MAX_LENGTH)});
        Runnable update = () -> {
            boolean[] met = PasswordPolicy.rules(password.getText().toString());
            for (int i = 0; i < labels.length; i++) {
                String requirement = labels[i].getTag().toString();
                labels[i].setText((met[i] ? "✓ " : "○ ") + requirement);
                labels[i].setTextColor(ContextCompat.getColor(password.getContext(),
                        met[i] ? R.color.primary : R.color.app_text_secondary));
                labels[i].setContentDescription(requirement + (met[i] ? ", atendido" : ", pendente"));
            }
        };
        password.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { update.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        update.run();
    }
}
