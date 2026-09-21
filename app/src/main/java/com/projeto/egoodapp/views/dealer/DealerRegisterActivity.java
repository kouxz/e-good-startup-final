package com.projeto.egoodapp.views.dealer;

import android.Manifest;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.AccountProfile;
import com.google.firebase.auth.UserProfileChangeRequest;
import android.content.Intent;
import android.content.pm.PackageManager;
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

public class DealerRegisterActivity extends AppCompatActivity {

    private TextInputEditText editNomeEmpresa, editCnpj, editEmailEmpresa, editSenhaEmpresa,
            editConfSenhaEmpresa, editTelefone, editEndereco, editCidade, editEstado, editCep;
    private MaterialButton btnCadastrar;
    private TextView tvVoltar, tvLogin;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseAuth mAuth;
    private Double companyLatitude, companyLongitude;

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
        setContentView(R.layout.activity_dealer_register);

        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        editNomeEmpresa = findViewById(R.id.editNomeEmpresa);
        editCnpj = findViewById(R.id.editCnpj);
        editEmailEmpresa = findViewById(R.id.editEmailEmpresa);
        editSenhaEmpresa = findViewById(R.id.editSenhaEmpresa);
        editConfSenhaEmpresa = findViewById(R.id.editConfSenhaEmpresa);
        editTelefone = findViewById(R.id.editTelefone);
        editEndereco = findViewById(R.id.editEndereco);
        editCidade = findViewById(R.id.editCidade);
        editEstado = findViewById(R.id.editEstado);
        editCep = findViewById(R.id.editCep);

        btnCadastrar = findViewById(R.id.btnCadastrar);
        tvVoltar = findViewById(R.id.tvVoltar);
        tvLogin = findViewById(R.id.tvLogin);

        findViewById(R.id.btnAutoLocationConcessionaria).setOnClickListener(v -> pedirLocalizacaoAtual());
        findViewById(R.id.btnManualLocationConcessionaria).setOnClickListener(v -> abrirPreenchimentoManual());

        tvVoltar.setOnClickListener(v -> finish());
        tvLogin.setOnClickListener(v -> finish());

        btnCadastrar.setOnClickListener(v -> realizarCadastro());
    }

    private void realizarCadastro() {
        String nomeEmpresa = getText(editNomeEmpresa);
        String cnpj = getText(editCnpj);
        String email = getText(editEmailEmpresa);
        String senha = editSenhaEmpresa.getText() == null ? "" : editSenhaEmpresa.getText().toString();
        String confSenha = editConfSenhaEmpresa.getText() == null ? "" : editConfSenhaEmpresa.getText().toString();
        String telefone = getText(editTelefone);
        String endereco = getText(editEndereco);
        String cidade = getText(editCidade);
        String estado = getText(editEstado);
        String cep = getText(editCep);

        if (nomeEmpresa.isEmpty() || cnpj.isEmpty() || email.isEmpty() || senha.isEmpty()
                || confSenha.isEmpty() || telefone.isEmpty() || endereco.isEmpty()
                || cidade.isEmpty() || estado.isEmpty() || cep.isEmpty()) {
            Toast.makeText(this, "Preencha todos os dados da concessionária", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!senha.equals(confSenha)) {
            Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!AccountProfile.validPhone(telefone)) { editTelefone.setError("Informe telefone válido com DDD"); return; }
        if (estado.length() != 2) { editEstado.setError("Informe a UF"); return; }
        btnCadastrar.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, task -> {
                    btnCadastrar.setEnabled(true);
                    if (task.isSuccessful()) {
                        AccountProfile profile = LocalRepository.get(this).ensureAccount(mAuth.getCurrentUser(), "concessionaria");
                        profile.name = nomeEmpresa; profile.cnpj = cnpj; profile.phone = telefone;
                        profile.address = endereco; profile.city = cidade; profile.state = estado.toUpperCase(java.util.Locale.ROOT); profile.zip = cep; profile.latitude = companyLatitude; profile.longitude = companyLongitude;
                        LocalRepository.get(this).saveAccount(profile);
                        mAuth.getCurrentUser().updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(nomeEmpresa).build());
                        Toast.makeText(this, "Cadastro de concessionária realizado com sucesso!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, DealerDashboardActivity.class));
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

            companyLatitude = location.getLatitude(); companyLongitude = location.getLongitude();
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
}
