package com.projeto.egoodapp.views.auth;

import com.projeto.egoodapp.views.home.HomeActivity;

import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.model.AccountProfile;
import com.google.firebase.auth.UserProfileChangeRequest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.views.common.form.PasswordFields;
import com.projeto.egoodapp.views.auth.AuthErrorMessages;

public class UserRegisterActivity extends AppCompatActivity {

    private TextInputEditText editNome, editEmail, editSenha, editConfSenha, editTelefone;
    private MaterialButton btnCadastrar;
    private TextView tvVoltar, tvLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_register);

        mAuth = FirebaseAuth.getInstance();

        editTelefone = findViewById(R.id.editTelefoneUsuario);
        editNome = findViewById(R.id.editNome);
        editEmail = findViewById(R.id.editEmail);
        editSenha = findViewById(R.id.editSenha);
        editConfSenha = findViewById(R.id.editConfSenha);
        PasswordFields.configure(findViewById(R.id.inputLayoutUserPassword), editSenha);
        PasswordFields.configure(findViewById(R.id.inputLayoutUserConfirmPassword), editConfSenha);

        com.projeto.egoodapp.views.common.form.InputLimits.account(this);
        com.projeto.egoodapp.views.common.form.PasswordPolicyUi.bind(editSenha, editConfSenha, new TextView[]{
                findViewById(R.id.passwordRuleLength), findViewById(R.id.passwordRuleUpper),
                findViewById(R.id.passwordRuleLower), findViewById(R.id.passwordRuleDigit), findViewById(R.id.passwordRuleSpecial)});
        btnCadastrar = findViewById(R.id.btnCadastrar);
        tvVoltar = findViewById(R.id.tvVoltar);
        tvLogin = findViewById(R.id.tvLogin);

        tvVoltar.setOnClickListener(v -> finish());
        tvLogin.setOnClickListener(v -> finish());

        btnCadastrar.setOnClickListener(v -> realizarCadastro());
    }

    private void realizarCadastro() {
        String nome = getText(editNome);
        String email = getText(editEmail);
        String senha = getPassword(editSenha);
        String confSenha = getPassword(editConfSenha);

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confSenha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!com.projeto.egoodapp.security.PasswordPolicy.valid(senha)) {
            editSenha.setError("Atenda aos requisitos de senha indicados abaixo"); return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Informe um e-mail em formato válido"); return;
        }
        if (!senha.equals(confSenha)) {
            Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show();
            return;
        }

        String telefone = getText(editTelefone);
        if (!AccountProfile.validPhone(telefone)) { editTelefone.setError("Informe telefone válido com DDD"); return; }
        btnCadastrar.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, task -> {
                    btnCadastrar.setEnabled(true);
                    if (task.isSuccessful()) {
                        try {
                            AccountProfile profile = LocalRepository.get(this).ensureAccount(mAuth.getCurrentUser(), "pessoa");
                            profile.name = nome; profile.phone = telefone;
                            LocalRepository.get(this).saveAccount(profile);
                            com.projeto.egoodapp.security.SessionTimeoutController.get(this).begin(profile.uid);
                            com.projeto.egoodapp.data.local.SecurityAudit.record(this,
                                    com.projeto.egoodapp.data.model.SecurityAuditEvent.Type.LOGIN,
                                    com.projeto.egoodapp.data.model.SecurityAuditEvent.Result.SUCCESS, profile.uid);
                            mAuth.getCurrentUser().updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(nome).build());
                            Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, HomeActivity.class));
                            finish();
                        } catch (RuntimeException storageFailure) {
                            com.projeto.egoodapp.data.local.LocalSession.signOut(this, () -> {
                                Toast.makeText(this, "Sua conta foi criada, mas não foi possível salvar o perfil. Entre novamente.", Toast.LENGTH_LONG).show();
                                finish();
                            });
                        }
                    } else {
                        Toast.makeText(this, AuthErrorMessages.registration(task.getException()),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? com.projeto.egoodapp.security.InputRules.clean(editText.getText().toString(), 2000) : "";
    }

    private String getPassword(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }
}
