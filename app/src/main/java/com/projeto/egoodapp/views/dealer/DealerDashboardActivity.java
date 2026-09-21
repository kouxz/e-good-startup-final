package com.projeto.egoodapp.views.dealer;

import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.AccountProfile;
import com.projeto.egoodapp.data.local.LocalSession;
import com.projeto.egoodapp.views.LocalProfileForms;
import com.projeto.egoodapp.views.VehiclePhotos;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.models.Vehicle;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DealerDashboardActivity extends AppCompatActivity {

    private DealerNavigation navigation;
    private FrameLayout containerDashboard, containerVehicles, containerProfile, containerSettings, containerContacts;
    private MaterialButton btnAddVehicle;
    private ActivityResultLauncher<Intent> addVehicleLauncher;
    
    private AccountProfile owner;
    private DealerSections sections;
    private DealerSettings settings;
    private String activeSection = "dashboard";
    private final java.util.concurrent.ExecutorService migrationWorker = java.util.concurrent.Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        owner = LocalSession.current(this, "concessionaria");
        if (owner == null || !owner.isDealer()) { LocalSession.logout(this); return; }
        setContentView(R.layout.activity_dealer_dashboard);
        LocalRepository.get(this).migrateLegacy(owner.uid);
        sections = new DealerSections(this, owner.uid, this::showContacts);
        migrationWorker.execute(() -> {
            LocalRepository.get(this).preserveLegacyPhotos(owner.uid);
            runOnUiThread(() -> { if (!isFinishing() && !isDestroyed() && "vehicles".equals(activeSection)) loadVehicles(); });
        });

        addVehicleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        showVehicles();
                    }
                });

        initializeViews();
        navigation = new DealerNavigation(this, () -> LocalRepository.get(this).account(owner.uid), this::navigate);
        settings = new DealerSettings(this, owner.uid, () -> LocalSession.logout(this));
        
        // Mostrar dashboard por padrão
        openSection(savedInstanceState == null ? getIntent().getStringExtra("section") : savedInstanceState.getString("section"));
        if (!owner.hasCompanyData()) {
            showProfile();
            LocalProfileForms.edit(this, owner, () -> { owner = LocalRepository.get(this).account(owner.uid); sections.profile(); navigation.refreshIdentity(); });
        }
        navigation.restore(savedInstanceState);
    }

    private void initializeViews() {
        containerContacts = findViewById(R.id.containerContacts);
        containerDashboard = findViewById(R.id.containerDashboard);
        containerVehicles = findViewById(R.id.containerVehicles);
        containerProfile = findViewById(R.id.containerProfile);
        containerSettings = findViewById(R.id.containerSettings);
        
        btnAddVehicle = findViewById(R.id.btnAddVehicle);
        if (btnAddVehicle != null) {
            btnAddVehicle.setOnClickListener(v -> addVehicleLauncher.launch(new Intent(this, AddVehicleActivity.class)));
        }
        
    }

    private void navigate(String section) {
        if ("add".equals(section)) addVehicleLauncher.launch(new Intent(this, AddVehicleActivity.class));
        else if ("logout".equals(section)) LocalSession.logout(this);
        else openSection(section);
    }

    private void showDashboard() {
        sections.dashboard();
        activeSection = "dashboard";
        containerContacts.setVisibility(View.GONE);
        updateNavigation();
        containerDashboard.setVisibility(View.VISIBLE);
        containerVehicles.setVisibility(View.GONE);
        containerProfile.setVisibility(View.GONE);
        containerSettings.setVisibility(View.GONE);
    }

    private void showVehicles() {
        activeSection = "vehicles";
        containerContacts.setVisibility(View.GONE);
        updateNavigation();
        containerDashboard.setVisibility(View.GONE);
        containerVehicles.setVisibility(View.VISIBLE);
        containerProfile.setVisibility(View.GONE);
        containerSettings.setVisibility(View.GONE);
        loadVehicles();
    }

    private void loadVehicles() {
        LinearLayout vehiclesContainer = containerVehicles.findViewById(R.id.vehiclesContainer);
        vehiclesContainer.removeAllViews();
        List<Vehicle> vehicles = LocalRepository.get(this).dealerVehicles(owner.uid);
        for (Vehicle vehicle : vehicles) addVehicleCard(vehiclesContainer, vehicle);
        if (vehicles.isEmpty()) {
            TextView empty = new TextView(this); empty.setText("Você ainda não publicou veículos");
            empty.setTextColor(getColor(R.color.text_gray)); empty.setPadding(24, 32, 24, 32); vehiclesContainer.addView(empty);
        }
    }

    private void addVehicleCard(LinearLayout container, Vehicle veiculo) {
        View card = getLayoutInflater().inflate(R.layout.item_dealer_vehicle, container, false);
        ImageView imageView = card.findViewById(R.id.vehiclePhoto);
        imageView.setContentDescription(veiculo.getMarca() + " " + veiculo.getModelo());
        VehiclePhotos.load(veiculo, imageView);

        TextView marca = card.findViewById(R.id.vehicleBrand);
        TextView modelo = card.findViewById(R.id.vehicleModel);
        TextView preco = card.findViewById(R.id.vehiclePrice);
        TextView anoKm = card.findViewById(R.id.vehicleYearMileage);
        marca.setText(veiculo.getMarca());
        modelo.setText(veiculo.getModelo());

        Locale locale = Locale.forLanguageTag("pt-BR");
        NumberFormat currency = NumberFormat.getCurrencyInstance(locale);
        currency.setMinimumFractionDigits(0);
        currency.setMaximumFractionDigits(2);
        preco.setText(currency.format(veiculo.getPreco()));
        anoKm.setText(veiculo.getAno() + " • "
                + NumberFormat.getIntegerInstance(locale).format(veiculo.getQuilometragem()) + " km");
        MaterialButton deleteButton = card.findViewById(R.id.btnDeleteVehicle);
        String vehicleName = veiculo.getMarca() + " " + veiculo.getModelo();
        deleteButton.setContentDescription(getString(R.string.dealer_delete_vehicle_description, vehicleName));
        deleteButton.setOnClickListener(v -> new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_EGood_LocalDialog)
                .setTitle(R.string.dealer_delete_vehicle_title)
                .setMessage(getString(R.string.dealer_delete_vehicle_message, vehicleName))
                .setNegativeButton(R.string.dealer_delete_vehicle_cancel, null)
                .setPositiveButton(R.string.dealer_delete_vehicle_action,
                        (dialog, which) -> deleteVehicle(veiculo.getId()))
                .show());
        container.addView(card);
    }

    private void deleteVehicle(String vehicleId) {
        boolean removed = LocalRepository.get(this).deleteVehicle(owner.uid, vehicleId);
        loadVehicles();
        Toast.makeText(this, removed ? R.string.dealer_delete_vehicle_success
                : R.string.dealer_delete_vehicle_not_found, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AccountProfile current = LocalSession.current(this, "concessionaria");
        if (owner == null || current == null || !current.isDealer() || !owner.uid.equals(current.uid)) { LocalSession.logout(this); return; }
        owner = current;
        navigation.refreshIdentity();
        if ("dashboard".equals(activeSection)) sections.dashboard();
        if ("vehicles".equals(activeSection)) loadVehicles();
        if ("contacts".equals(activeSection)) sections.contacts();
        if ("profile".equals(activeSection)) sections.profile();
        if ("settings".equals(activeSection)) settings.refresh();
    }

    private void showProfile() {
        activeSection = "profile";
        containerContacts.setVisibility(View.GONE);
        updateNavigation();
        sections.profile();
        containerDashboard.setVisibility(View.GONE);
        containerVehicles.setVisibility(View.GONE);
        containerProfile.setVisibility(View.VISIBLE);
        containerSettings.setVisibility(View.GONE);
    }

    private void showSettings() {
        activeSection = "settings";
        containerContacts.setVisibility(View.GONE);
        updateNavigation();
        containerDashboard.setVisibility(View.GONE);
        containerVehicles.setVisibility(View.GONE);
        containerProfile.setVisibility(View.GONE);
        containerSettings.setVisibility(View.VISIBLE);
        settings.refresh();
    }

    private void updateNavigation() {
        navigation.select(activeSection);
    }

    private void showContacts() {
        activeSection = "contacts"; updateNavigation();
        containerDashboard.setVisibility(View.GONE); containerVehicles.setVisibility(View.GONE);
        containerProfile.setVisibility(View.GONE); containerSettings.setVisibility(View.GONE);
        containerContacts.setVisibility(View.VISIBLE); sections.contacts();
    }
    private void openSection(String section) {
        if ("settings".equals(section)) showSettings();
        else if ("contacts".equals(section)) showContacts();
        else if ("profile".equals(section)) showProfile();
        else if ("vehicles".equals(section)) showVehicles();
        else showDashboard();
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); openSection(intent.getStringExtra("section")); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("section", activeSection); if (navigation != null) navigation.save(state); super.onSaveInstanceState(state); }
    @Override protected void onDestroy() { migrationWorker.shutdown(); super.onDestroy(); }

}
