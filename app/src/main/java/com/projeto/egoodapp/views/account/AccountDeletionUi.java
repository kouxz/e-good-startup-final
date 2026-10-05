package com.projeto.egoodapp.views.account;

import com.projeto.egoodapp.views.common.form.PasswordFields;
import com.projeto.egoodapp.views.common.session.SessionDialogBuilder;

import android.content.DialogInterface;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.local.LocalSession;

/** Shared destructive-account action used by person and dealership screens. */
public final class AccountDeletionUi implements DefaultLifecycleObserver {
    private final AppCompatActivity activity;
    private final MaterialButton button;
    private final boolean dealer;
    private final AccountDeletionViewModel viewModel;
    private String displayedError;

    public AccountDeletionUi(AppCompatActivity activity, MaterialButton button, boolean dealer) {
        this.activity = activity;
        this.button = button;
        this.dealer = dealer;
        viewModel = new ViewModelProvider(activity).get(AccountDeletionViewModel.class);
        activity.getLifecycle().addObserver(this);
        viewModel.attach(activity);
        button.setOnClickListener(view -> confirmDeletion());
        viewModel.state().observe(activity, this::render);
    }

    private void confirmDeletion() {
        String message = dealer
                ? "Esta ação excluirá permanentemente o perfil da concessionária, veículos, fotos, contatos, visualizações e avaliações armazenados pelo e-good."
                : "Esta ação excluirá permanentemente seu perfil, interesses, visualizações e avaliações armazenados pelo e-good.";
        new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(activity, R.style.ThemeOverlay_EGood_LocalDialog)
                .setTitle("Excluir conta?")
                .setMessage(message + "\n\nSua identidade será confirmada antes da exclusão. Esta ação não pode ser desfeita.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Continuar", (dialog, which) -> chooseProvider())
                .show();
    }

    private void chooseProvider() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean password = AccountDeletionViewModel.hasProvider(user, EmailAuthProvider.PROVIDER_ID);
        boolean google = AccountDeletionViewModel.hasProvider(user, GoogleAuthProvider.PROVIDER_ID);
        if (password && google) {
            new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(activity, R.style.ThemeOverlay_EGood_LocalDialog)
                    .setTitle("Confirmar identidade")
                    .setItems(new String[]{"Usar senha", "Usar Conta Google"},
                            (dialog, index) -> {
                                if (index == 0) showPasswordDialog();
                                else viewModel.deleteWithGoogle(activity);
                            })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else if (password) {
            showPasswordDialog();
        } else if (google) {
            viewModel.deleteWithGoogle(activity);
        } else {
            showMessage("Não foi possível excluir", "O provedor de acesso desta conta não é compatível com a confirmação de identidade.");
        }
    }

    private void showPasswordDialog() {
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_delete_account_password, null);
        TextInputLayout layout = content.findViewById(R.id.inputDeleteAccountPassword);
        TextInputEditText password = content.findViewById(R.id.editDeleteAccountPassword);
        PasswordFields.configure(layout, password);
        AlertDialog dialog = new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(activity, R.style.ThemeOverlay_EGood_LocalDialog)
                .setTitle("Confirme sua senha")
                .setMessage("Digite a senha atual para excluir permanentemente a conta.")
                .setView(content)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir conta", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String value = password.getText() == null ? "" : password.getText().toString();
                    if (value.isEmpty()) {
                        layout.setError("Informe sua senha atual");
                        return;
                    }
                    layout.setError(null);
                    dialog.dismiss();
                    viewModel.deleteWithPassword(value);
                }));
        dialog.show();
    }

    private void render(AccountDeletionViewModel.State state) {
        boolean busy = state.busy();
        button.setEnabled(!busy);
        button.setText(busy ? "Excluindo conta…" : "Excluir conta");
        if (state.phase == AccountDeletionViewModel.Phase.IDLE) displayedError = null;
        if (state.phase == AccountDeletionViewModel.Phase.ERROR
                && state.message != null && !state.message.equals(displayedError)) {
            displayedError = state.message;
            new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(activity, R.style.ThemeOverlay_EGood_LocalDialog)
                    .setTitle("Não foi possível excluir")
                    .setMessage(state.message)
                    .setPositiveButton("OK", (dialog, which) -> viewModel.acknowledgeError())
                    .show();
        } else if (state.phase == AccountDeletionViewModel.Phase.SUCCESS) {
            String warning = state.message;
            viewModel.consumeSuccess();
            if (warning == null) {
                finishDeletion();
            } else {
                new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(activity, R.style.ThemeOverlay_EGood_LocalDialog)
                        .setTitle("Conta excluída")
                        .setMessage(warning)
                        .setCancelable(false)
                        .setPositiveButton("OK", (dialog, which) -> finishDeletion())
                        .show();
            }
        }
    }

    private void showMessage(String title, String message) {
        new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(activity, R.style.ThemeOverlay_EGood_LocalDialog)
                .setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }

    private void finishDeletion() {
        if (!activity.isFinishing() && !activity.isDestroyed()) LocalSession.logout(activity);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        viewModel.detach(activity);
    }
}
