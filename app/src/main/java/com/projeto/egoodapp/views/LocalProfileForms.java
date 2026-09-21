package com.projeto.egoodapp.views;

import android.app.Activity;
import android.graphics.Color;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.local.AccountProfile;
import com.projeto.egoodapp.data.local.LocalRepository;

public final class LocalProfileForms {
    private LocalProfileForms() {}
    public static void edit(Activity activity, AccountProfile profile, Runnable saved) {
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setBackgroundColor(Color.WHITE);
        int padding = (int) (20 * activity.getResources().getDisplayMetrics().density);
        form.setPadding(padding, padding / 2, padding, padding);
        EditText name = field(form, "Nome", profile.name, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText phone = field(form, "Telefone com DDD", profile.phone, InputType.TYPE_CLASS_PHONE);
        EditText cnpj = null, address = null, city = null, state = null, description = null;
        if (profile.isDealer()) {
            cnpj = field(form, "CNPJ", profile.cnpj, InputType.TYPE_CLASS_PHONE);
            cnpj.setEnabled(profile.cnpj.isEmpty());
            address = field(form, "Endereço", profile.address, InputType.TYPE_CLASS_TEXT);
            city = field(form, "Cidade", profile.city, InputType.TYPE_CLASS_TEXT);
            state = field(form, "Estado (UF)", profile.state, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            description = field(form, "Descrição", profile.description, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        }
        ScrollView scroll = new ScrollView(activity); scroll.addView(form);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_EGood_LocalDialog)
                .setTitle(profile.isDealer() ? "Editar concessionária" : "Editar perfil")
                .setView(scroll).setNegativeButton("Cancelar", null).setPositiveButton("Salvar", null).create();
        EditText finalCnpj = cnpj, finalAddress = address, finalCity = city, finalState = state, finalDescription = description;
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError("Informe o nome"); return; }
            if (!AccountProfile.validPhone(phone.getText().toString())) { phone.setError("Informe telefone válido com DDD"); return; }
            if (profile.isDealer()) {
                for (EditText required : new EditText[]{finalCnpj, finalAddress, finalCity, finalState}) {
                    if (required.getText().toString().trim().isEmpty()) { required.setError("Campo obrigatório"); return; }
                }
                if (finalState.getText().toString().trim().length() != 2) { finalState.setError("Informe a UF com duas letras"); return; }
                if (!profile.address.equals(finalAddress.getText().toString().trim())
                        || !profile.city.equals(finalCity.getText().toString().trim())
                        || !profile.state.equalsIgnoreCase(finalState.getText().toString().trim())) {
                    profile.latitude = null; profile.longitude = null;
                }
                profile.cnpj = finalCnpj.getText().toString().trim();
                profile.address = finalAddress.getText().toString().trim();
                profile.city = finalCity.getText().toString().trim();
                profile.state = finalState.getText().toString().trim().toUpperCase(java.util.Locale.ROOT);
                profile.description = finalDescription.getText().toString().trim();
            }
            profile.name = name.getText().toString().trim(); profile.phone = phone.getText().toString().trim();
            try { LocalRepository.get(activity).saveAccount(profile); }
            catch (IllegalStateException e) { Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show(); return; }
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                FirebaseAuth.getInstance().getCurrentUser().updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(profile.name).build());
            }
            dialog.dismiss(); saved.run();
        }));
        dialog.show();
    }
    private static EditText field(LinearLayout form, String hint, String value, int inputType) {
        TextView label = new TextView(form.getContext()); label.setText(hint);
        label.setTextSize(12); label.setTextColor(Color.parseColor("#64748B"));
        int spacing = (int) (12 * form.getResources().getDisplayMetrics().density);
        label.setPadding(0, spacing, 0, 0); form.addView(label);
        EditText input = new EditText(form.getContext()); input.setHint(hint); input.setText(value);
        input.setInputType(inputType); input.setTextColor(Color.parseColor("#0F172A"));
        input.setHintTextColor(Color.parseColor("#64748B")); input.setTextSize(14);
        input.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0ABFBC")));
        input.setMinHeight((int) (48 * form.getResources().getDisplayMetrics().density));
        form.addView(input, new LinearLayout.LayoutParams(-1, -2));
        return input;
    }
}
