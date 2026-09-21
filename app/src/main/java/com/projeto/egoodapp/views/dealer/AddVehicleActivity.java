package com.projeto.egoodapp.views.dealer;

import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.LocalSession;
import com.projeto.egoodapp.data.local.AccountProfile;
import com.projeto.egoodapp.views.LocalProfileForms;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.models.Vehicle;


public class AddVehicleActivity extends AppCompatActivity {

    private TextInputEditText editMarca, editModelo, editAno, editQuilometragem;
    private TextInputEditText editPreco, editBateria, editAutonomia, editConsumo, editPotencia, editRecarga, editCor, editDescricao;
    private MaterialAutoCompleteTextView spinnerCategoria;
    private FrameLayout photoUploadContainer;
    private ImageView previewImage;
    private LinearLayout photoUploadPlaceholder;
    private MaterialButton btnPublicar;
    private DealerNavigation navigation;
    private final ExecutorService photoWorker = Executors.newSingleThreadExecutor();
    private AccountProfile owner;
    private boolean copyingPhoto;
    private String selectedImageUri = "";
    private ActivityResultLauncher<String[]> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        owner = LocalSession.current(this, "concessionaria");
        if (owner == null || !owner.isDealer()) { LocalSession.logout(this); return; }
        setContentView(R.layout.activity_add_vehicle);

        setupGalleryLauncher();
        initializeViews();
        setupCategoryDropdown();
        navigation = new DealerNavigation(this, () -> LocalRepository.get(this).account(owner.uid), section -> {
            if ("logout".equals(section)) LocalSession.logout(this);
            else if (!"add".equals(section)) openSection(section);
        });
        navigation.select("add");
        navigation.restore(savedInstanceState);
        setupPhotoUpload();
        setupPublishButton();
        if (!owner.hasCompanyData()) LocalProfileForms.edit(this, owner,
                () -> owner = LocalRepository.get(this).account(owner.uid));
        if (savedInstanceState != null) {
            selectedImageUri = savedInstanceState.getString("selectedImageUri", "");
            if (!selectedImageUri.isEmpty()) {
                Glide.with(this).load(Uri.parse(selectedImageUri)).centerCrop().into(previewImage);
                previewImage.setVisibility(ImageView.VISIBLE);
                photoUploadPlaceholder.setVisibility(LinearLayout.GONE);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("selectedImageUri", selectedImageUri);
        if (navigation != null) navigation.save(outState);
        super.onSaveInstanceState(outState);
    }

    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), selectedUri -> {
            if (selectedUri == null || copyingPhoto) return;
            copyingPhoto = true; btnPublicar.setEnabled(false); photoUploadContainer.setEnabled(false);
            photoWorker.execute(() -> {
                try {
                    String local = LocalRepository.get(this).copyPhoto(selectedUri);
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        selectedImageUri = local;
                        Glide.with(this).load(Uri.parse(local)).centerCrop().into(previewImage);
                        previewImage.setVisibility(ImageView.VISIBLE); photoUploadPlaceholder.setVisibility(LinearLayout.GONE);
                        copyingPhoto = false; btnPublicar.setEnabled(true); photoUploadContainer.setEnabled(true);
                    });
                } catch (java.io.IOException | SecurityException e) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        copyingPhoto = false; btnPublicar.setEnabled(true); photoUploadContainer.setEnabled(true);
                        Toast.makeText(this, "Não foi possível salvar a foto. Selecione outra imagem.", Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }
    @Override protected void onDestroy() { photoWorker.shutdown(); super.onDestroy(); }

    @Override protected void onResume() {
        super.onResume();
        if (navigation != null) navigation.refreshIdentity();
    }

    private void initializeViews() {
        editMarca = findViewById(R.id.editMarca);
        editModelo = findViewById(R.id.editModelo);
        editAno = findViewById(R.id.editAno);
        editQuilometragem = findViewById(R.id.editQuilometragem);
        editPreco = findViewById(R.id.editPreco);
        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        editBateria = findViewById(R.id.editBateria);
        editAutonomia = findViewById(R.id.editAutonomia);
        editConsumo = findViewById(R.id.editConsumo);
        editPotencia = findViewById(R.id.editPotencia);
        editRecarga = findViewById(R.id.editRecarga);
        editCor = findViewById(R.id.editCor);
        editDescricao = findViewById(R.id.editDescricao);
        photoUploadContainer = findViewById(R.id.photoUploadContainer);
        previewImage = findViewById(R.id.previewImage);
        photoUploadPlaceholder = findViewById(R.id.photoUploadPlaceholder);
        btnPublicar = findViewById(R.id.btnPublicar);

    }

    private void setupCategoryDropdown() {
        String[] categorias = {"Hatch", "SUV", "SUV compacto", "Luxo"};
        spinnerCategoria.setAdapter(new ArrayAdapter<>(
                this, R.layout.item_add_vehicle_category, categorias));
        spinnerCategoria.setDropDownBackgroundDrawable(new ColorDrawable(Color.WHITE));
        if (spinnerCategoria.getText().toString().isEmpty()) {
            spinnerCategoria.setText(categorias[0], false);
        }
    }

    private void openSection(String section) {
        startActivity(new Intent(this, DealerDashboardActivity.class).putExtra("section", section)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void setupPhotoUpload() {
        photoUploadContainer.setOnClickListener(v -> {
            galleryLauncher.launch(new String[]{"image/*"});
        });
    }

    private void setupPublishButton() {
        btnPublicar.setOnClickListener(v -> publicarVeiculo());
    }

    private void publicarVeiculo() {
        if (copyingPhoto) return;
        owner = LocalRepository.get(this).account(owner.uid);
        if (!owner.hasCompanyData()) { LocalProfileForms.edit(this, owner, () -> {}); return; }
        if (!validarFormulario()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Criar novo veículo com os dados do formulário
            Vehicle novoVeiculo = new Vehicle(
                    editMarca.getText().toString(),
                    editModelo.getText().toString(),
                    Integer.parseInt(editAno.getText().toString()),
                    editQuilometragem.getText().toString().trim().isEmpty() ? 0 : Integer.parseInt(editQuilometragem.getText().toString()),
                    Double.parseDouble(editPreco.getText().toString()),
                    spinnerCategoria.getText().toString(),
                    Integer.parseInt(editBateria.getText().toString()),
                    Integer.parseInt(editAutonomia.getText().toString()),
                    editCor.getText().toString(),
                    editDescricao.getText().toString(),
                    selectedImageUri
            );
            novoVeiculo.setConsumo(VehicleSpecifications.consumption(editConsumo.getText().toString()));
            novoVeiculo.setPotencia(VehicleSpecifications.power(editPotencia.getText().toString()));
            novoVeiculo.setCarga(VehicleSpecifications.charging(editRecarga.getText().toString()));

            if (novoVeiculo.getAno() < 1886 || novoVeiculo.getQuilometragem() < 0
                    || !Double.isFinite(novoVeiculo.getPreco()) || novoVeiculo.getPreco() <= 0
                    || novoVeiculo.getBateria() <= 0 || novoVeiculo.getAutonomia() <= 0) {
                Toast.makeText(this, "Verifique os valores numéricos do veículo", Toast.LENGTH_LONG).show(); return;
            }
            // Salvar no catálogo local compartilhado
            salvarVeiculoLocalmente(novoVeiculo);

            Toast.makeText(this, "Veículo publicado com sucesso!", Toast.LENGTH_SHORT).show();

            // Voltar para tela de veículos
            setResult(RESULT_OK);
            finish();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Erro ao processar números. Verifique os campos", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException | IllegalStateException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean validarFormulario() {
        boolean complete = !editMarca.getText().toString().trim().isEmpty() &&
                !editModelo.getText().toString().trim().isEmpty() &&
                !editAno.getText().toString().trim().isEmpty() &&
                !editPreco.getText().toString().trim().isEmpty() &&
                !spinnerCategoria.getText().toString().trim().isEmpty() &&
                !editBateria.getText().toString().trim().isEmpty() &&
                !editAutonomia.getText().toString().trim().isEmpty() &&
                !editCor.getText().toString().trim().isEmpty();
        boolean technical = true;
        try { VehicleSpecifications.consumption(editConsumo.getText().toString()); editConsumo.setError(null); }
        catch (RuntimeException invalid) { editConsumo.setError("Informe um consumo maior que zero"); technical = false; }
        try { VehicleSpecifications.power(editPotencia.getText().toString()); editPotencia.setError(null); }
        catch (RuntimeException invalid) { editPotencia.setError("Informe uma potência maior que zero"); technical = false; }
        try { VehicleSpecifications.charging(editRecarga.getText().toString()); editRecarga.setError(null); }
        catch (RuntimeException invalid) { editRecarga.setError("Informe o tipo e a potência de recarga"); technical = false; }
        return complete && technical;
    }

    private void salvarVeiculoLocalmente(Vehicle veiculo) {
        LocalRepository.get(this).addVehicle(owner.uid, veiculo);
    }
}
