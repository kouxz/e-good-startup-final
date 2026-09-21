package com.projeto.egoodapp.views.user;

import android.Manifest;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.AccountProfile;
import com.google.firebase.auth.UserProfileChangeRequest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.projeto.egoodapp.R;

public class UserRegisterActivity extends AppCompatActivity {

    private TextInputEditText editNome, editEmail, editSenha, editConfSenha, editEndereco, editTelefone;
    private MaterialButton btnCadastrar;
    private TextView tvVoltar, tvLogin;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseAuth mAuth;
    
    private final ActivityResultLauncher<String> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    preencherLocalizacaoAtual();
                } else {
                    Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_register);

        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        editTelefone = findViewById(R.id.editTelefoneUsuario);
        editNome = findViewById(R.id.editNome);
        editEmail = findViewById(R.id.editEmail);
        editSenha = findViewById(R.id.editSenha);
        editConfSenha = findViewById(R.id.editConfSenha);
        editEndereco = findViewById(R.id.editEndereco);

        btnCadastrar = findViewById(R.id.btnCadastrar);
        tvVoltar = findViewById(R.id.tvVoltar);
        tvLogin = findViewById(R.id.tvLogin);

        findViewById(R.id.btnAutoLocation).setOnClickListener(v -> pedirLocalizacaoAtual());
        findViewById(R.id.btnManualLocation).setOnClickListener(v -> abrirPreenchimentoManual());

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
                        AccountProfile profile = LocalRepository.get(this).ensureAccount(mAuth.getCurrentUser(), "pessoa");
                        profile.name = nome; profile.phone = telefone;
                        LocalRepository.get(this).saveAccount(profile);
                        mAuth.getCurrentUser().updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(nome).build());
                        Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Erro: " + (task.getException() != null ? task.getException().getMessage() : "Falha no cadastro"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void pedirLocalizacaoAtual() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            preencherLocalizacaoAtual();
            return;
        }
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void preencherLocalizacaoAtual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissão de localização necessária", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(this, "Não foi possível obter o GPS agora", Toast.LENGTH_SHORT).show();
                return;
            }

            String texto = String.format(java.util.Locale.getDefault(), "Lat: %.5f, Lng: %.5f", location.getLatitude(), location.getLongitude());
            if (editEndereco != null) {
                editEndereco.setText(texto);
            }
            Toast.makeText(this, "Localização atual preenchida", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter localização", Toast.LENGTH_SHORT).show());
    }

    private void abrirPreenchimentoManual() {
        if (editEndereco != null) {
            editEndereco.setText("Digite seu endereço manualmente");
        }
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String getPassword(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }
}
