package com.projeto.egoodapp.views.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.views.common.form.PasswordFields;
import com.projeto.egoodapp.views.auth.RegisterActivity;
import com.projeto.egoodapp.views.home.HomeActivity;
import com.projeto.egoodapp.views.dealer.DealerDashboardActivity;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText editEmail, editSenha;
    private MaterialButton btnEntrar, btnGoogle;
    private TextView btnTogglePessoa, btnToggleConcessionaria, tvRegister, tvForgot;
    private boolean isPessoa = true;
    private boolean authInProgress;
    private LoginFlowModel loginFlow;
    private AlertDialog typeDialog;
    private final android.os.Handler cooldownHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private com.projeto.egoodapp.security.LoginAttemptLimiter limiter;
    private final Runnable cooldownTick = new Runnable() {
        @Override public void run() { renderCooldown(); cooldownHandler.postDelayed(this, 1000); }
    };
    private void renderCooldown() {
        if (limiter == null || btnEntrar == null || authInProgress) return;
        try {
            long seconds = (limiter.remaining() + 999) / 1000;
            btnEntrar.setEnabled(seconds == 0);
            btnEntrar.setText(seconds == 0 ? "Entrar" : "Tente novamente em " + seconds + " s");
        } catch (RuntimeException storageFailure) {
            btnEntrar.setEnabled(false); btnEntrar.setText("Acesso indisponível no momento");
        }
    }
    @Override protected void onResume() {
        super.onResume(); cooldownHandler.removeCallbacks(cooldownTick); cooldownTick.run();
    }
    @Override protected void onPause() { cooldownHandler.removeCallbacks(cooldownTick); super.onPause(); }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        limiter = new com.projeto.egoodapp.security.LoginAttemptLimiter(this);
        com.projeto.egoodapp.views.common.form.InputLimits.account(this);
        if (savedInstanceState == null && getIntent().getBooleanExtra("session_expired", false)) {
            Toast.makeText(this, "Sua sessão foi encerrada após 6 horas sem atividade", Toast.LENGTH_LONG).show();
        }

        // Vinculação dos componentes
        editEmail = findViewById(R.id.editEmail);
        editSenha = findViewById(R.id.editSenha);
        PasswordFields.configure(findViewById(R.id.inputLayoutSenha), editSenha);
        btnEntrar = findViewById(R.id.btnEntrar);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnTogglePessoa = findViewById(R.id.btnTogglePessoa);
        btnToggleConcessionaria = findViewById(R.id.btnToggleConcessionaria);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgot = findViewById(R.id.tvForgot);

        btnTogglePessoa.setCompoundDrawablePadding(8);
        btnToggleConcessionaria.setCompoundDrawablePadding(8);

        // Ações de clique
        btnTogglePessoa.setOnClickListener(v -> setToggleState(true));
        btnToggleConcessionaria.setOnClickListener(v -> setToggleState(false));
        btnEntrar.setOnClickListener(v -> realizarLogin());
        setToggleState(savedInstanceState == null || savedInstanceState.getBoolean("isPessoa", true));

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            Toast.makeText(this, "Indo para Cadastro", Toast.LENGTH_SHORT).show();
        });

        tvForgot.setOnClickListener(v -> abrirRecuperacaoSenha());
        loginFlow = new ViewModelProvider(this).get(LoginFlowModel.class);
        loginFlow.attach(this);
        btnGoogle.setOnClickListener(v -> {
            if (!authInProgress) loginFlow.googleLogin(this, isPessoa ? "pessoa" : "concessionaria");
        });
        loginFlow.state().observe(this, this::renderLoginState);
    }

    private void setToggleState(boolean selecionarPessoa) {
        isPessoa = selecionarPessoa;

        int selectedColor = ContextCompat.getColor(this, R.color.teal_primary);
        int defaultColor = ContextCompat.getColor(this, R.color.white);

        if (isPessoa) {
            btnTogglePessoa.setBackgroundResource(R.drawable.bg_toggle_selected);
            btnTogglePessoa.setTextColor(selectedColor);
            btnToggleConcessionaria.setBackground(null);
            btnToggleConcessionaria.setTextColor(defaultColor);
            setToggleIcon(btnTogglePessoa, R.drawable.ic_person, selectedColor);
            setToggleIcon(btnToggleConcessionaria, R.drawable.ic_user_dealership, defaultColor);
        } else {
            btnToggleConcessionaria.setBackgroundResource(R.drawable.bg_toggle_selected);
            btnToggleConcessionaria.setTextColor(selectedColor);
            btnTogglePessoa.setBackground(null);
            btnTogglePessoa.setTextColor(defaultColor);
            setToggleIcon(btnTogglePessoa, R.drawable.ic_person, defaultColor);
            setToggleIcon(btnToggleConcessionaria, R.drawable.ic_user_dealership, selectedColor);
        }
    }

    private void setToggleIcon(TextView textView, int drawableRes, int tintColor) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableRes);
        if (drawable == null) {
            return;
        }

        drawable = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTint(drawable, tintColor);
        textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        textView.setCompoundDrawablePadding(8);
    }

    private void realizarLogin() {
        if (authInProgress) return;
        String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
        // Passwords must reach Firebase exactly as entered, including spaces.
        String senha = editSenha.getText() != null ? editSenha.getText().toString() : "";

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Informe um e-mail válido");
            return;
        }
        final String selectedType = isPessoa ? "pessoa" : "concessionaria";
        loginFlow.passwordLogin(email, senha, selectedType);
    }

    private void renderLoginState(LoginFlowModel.State state) {
        setBusy(state.busy(), state.busy() ? "Entrando…" : "Entrar");
        btnGoogle.setText(state.busy() && loginFlow.googleAttempt() ? "Conectando…" : "Continuar com Google");
        if (state.phase != LoginFlowModel.Phase.CONFIRM_TYPE && typeDialog != null) {
            typeDialog.dismiss();
            typeDialog = null;
        }
        switch (state.phase) {
            case CONFIRM_TYPE:
                mostrarConfirmacaoTipo();
                break;
            case READY:
                loginFlow.acknowledge();
                startActivity(new Intent(this, state.profile.isDealer() ? DealerDashboardActivity.class : HomeActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
                break;
            case ERROR:
                boolean google = loginFlow.googleAttempt();
                loginFlow.acknowledge();
                if (state.message != null) {
                    new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(this, R.style.ThemeOverlay_EGood_LocalDialog)
                            .setTitle("Não foi possível entrar").setMessage(state.message)
                            .setPositiveButton("OK", null).show();
                } else {
                    mostrarErroLogin(state.error, google);
                }
                break;
            default:
                break;
        }
    }

    private void mostrarConfirmacaoTipo() {
        if (typeDialog != null && typeDialog.isShowing()) return;
        typeDialog = new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(this, R.style.ThemeOverlay_EGood_LocalDialog)
                .setTitle("Confirme o tipo da conta")
                .setSingleChoiceItems(new String[]{"Pessoa", "Concessionária"},
                        "pessoa".equals(loginFlow.candidateType()) ? 0 : 1,
                        (dialog, index) -> loginFlow.setCandidateType(index == 0 ? "pessoa" : "concessionaria"))
                .setPositiveButton("Confirmar", (dialog, which) -> loginFlow.confirmType(loginFlow.candidateType()))
                .setNegativeButton("Cancelar", (dialog, which) -> loginFlow.cancelTypeConfirmation())
                .setOnCancelListener(dialog -> loginFlow.cancelTypeConfirmation()).create();
        typeDialog.setCanceledOnTouchOutside(false);
        typeDialog.show();
    }

    private void setBusy(boolean busy, String label) {
        authInProgress = busy;
        btnEntrar.setEnabled(!busy);
        btnGoogle.setEnabled(!busy);
        btnEntrar.setText(label);
        if (!busy) renderCooldown();
        editEmail.setEnabled(!busy);
        editSenha.setEnabled(!busy);
        btnTogglePessoa.setEnabled(!busy);
        btnToggleConcessionaria.setEnabled(!busy);
        tvRegister.setEnabled(!busy);
        tvForgot.setEnabled(!busy);
    }

    private void mostrarErroLogin(Exception error, boolean google) {
        String code = error instanceof FirebaseAuthException
                ? ((FirebaseAuthException) error).getErrorCode() : "UNKNOWN";
        // Do not log credentials, email addresses or complete server responses.
        Log.w("EGoodAuth", "Login failed: " + code);
        String message = com.projeto.egoodapp.views.auth.AuthErrorMessages.login(error, google);
        MaterialAlertDialogBuilder dialog = new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(this, R.style.ThemeOverlay_EGood_LocalDialog)
                .setTitle("Não foi possível entrar")
                .setMessage(message);
        if (!google && erroCredenciais(error)) {
            dialog.setPositiveButton("Recuperar senha", (d, which) -> abrirRecuperacaoSenha())
                    .setNegativeButton("Fechar", null);
        } else {
            dialog.setPositiveButton("OK", null);
        }
        dialog.show();
    }

    private boolean erroCredenciais(Exception error) {
        if (!(error instanceof FirebaseAuthException)) return false;
        String code = ((FirebaseAuthException) error).getErrorCode();
        return code.equals("ERROR_INVALID_CREDENTIAL") || code.equals("ERROR_INVALID_LOGIN_CREDENTIALS")
                || code.equals("ERROR_WRONG_PASSWORD") || code.equals("ERROR_USER_NOT_FOUND");
    }

    private String mensagemErro(Exception error) {
        return com.projeto.egoodapp.views.auth.AuthErrorMessages.login(error, false);
    }

    private void abrirRecuperacaoSenha() {
        if (authInProgress) return;
        MaterialAlertDialogBuilder builder = new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(this, R.style.ThemeOverlay_EGood_LocalDialog);
        LinearLayout fields = new LinearLayout(builder.getContext());
        fields.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        fields.setPadding(padding, 0, padding, 0);
        TextInputLayout input = new TextInputLayout(builder.getContext());
        input.setHint("E-mail da conta");
        input.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        TextInputEditText emailField = new TextInputEditText(input.getContext());
        emailField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        com.projeto.egoodapp.views.common.form.InputLimits.field(emailField, com.projeto.egoodapp.security.InputRules.EMAIL);
        emailField.setTextColor(ContextCompat.getColor(this, R.color.app_text_primary));
        emailField.setHintTextColor(ContextCompat.getColor(this, R.color.app_text_secondary));
        emailField.setText(editEmail.getText());
        input.addView(emailField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        fields.addView(input);
        androidx.appcompat.app.AlertDialog dialog = builder.setTitle("Recuperar senha")
                .setMessage("Informe o e-mail da conta para receber o link de redefinição.")
                .setView(fields).setPositiveButton("Enviar", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String email = emailField.getText() == null ? "" : emailField.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                input.setError("Informe um e-mail válido");
                return;
            }
            editEmail.setText(email);
            dialog.dismiss();
            setBusy(true, "Aguarde…");
            mAuth.setLanguageCode("pt");
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(this, task -> {
                setBusy(false, "Entrar");
                com.projeto.egoodapp.data.local.SecurityAudit.record(this,
                        com.projeto.egoodapp.data.model.SecurityAuditEvent.Type.PASSWORD_RESET,
                        task.isSuccessful() ? com.projeto.egoodapp.data.model.SecurityAuditEvent.Result.SUCCESS
                        : com.projeto.egoodapp.data.model.SecurityAuditEvent.Result.FAILURE, null);
                if (task.isSuccessful() || com.projeto.egoodapp.views.auth.AuthErrorMessages.invalidCredentials(task.getException())) {
                    new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(this, R.style.ThemeOverlay_EGood_LocalDialog)
                            .setTitle("Solicitação concluída")
                            .setMessage("Se esse e-mail estiver cadastrado, você receberá um link para redefinir a senha. Verifique também a caixa de spam.")
                            .setPositiveButton("OK", null).show();
                } else {
                    new com.projeto.egoodapp.views.common.session.SessionDialogBuilder(this, R.style.ThemeOverlay_EGood_LocalDialog)
                            .setTitle("Não foi possível recuperar a senha")
                            .setMessage(mensagemErro(task.getException())).setPositiveButton("OK", null).show();
                }
            });
        }));
        dialog.show();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isPessoa", isPessoa);
    }
    @Override protected void onDestroy() {
        if (typeDialog != null) typeDialog.dismiss();
        if (loginFlow != null) loginFlow.detach(this);
        super.onDestroy();
    }
}


