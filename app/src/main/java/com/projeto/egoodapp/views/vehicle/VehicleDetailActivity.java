package com.projeto.egoodapp.views.vehicle;

import com.projeto.egoodapp.models.Vehicle;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.DemoCatalog;
import com.projeto.egoodapp.views.VehiclePhotos;
import com.projeto.egoodapp.views.dealership.ConcessionariaDetailActivity;
import com.projeto.egoodapp.views.dealership.ConcessionariasActivity;
import android.widget.Toast;
import android.view.View;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projeto.egoodapp.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import com.projeto.egoodapp.views.user.HomeActivity;
import com.projeto.egoodapp.views.comparison.ComparisonActivity;
import com.projeto.egoodapp.views.vehicle.SolarActivity;
import com.projeto.egoodapp.views.user.ProfileActivity;
import com.projeto.egoodapp.views.user.UserChrome;

public class VehicleDetailActivity extends AppCompatActivity {

    private Vehicle currentVehicle;
    private UserChrome chrome;

    private ImageView ivVehicleImage;
    private TextView tvVehicleName, tvPrice, tvAutonomy, tvBattery, tvConsumption, tvCategory, tvPower, tvCharging;
    private Button badgeDetail, btnFindDealer;
    private LinearLayout btnBackContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_detail);

        initializeViews();
        loadVehicleData();
        if (savedInstanceState == null && currentVehicle != null) recordView();
        setupNavigation();
        chrome = new UserChrome(this, UserChrome.Section.VEHICLES);
    }

    private void recordView() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) LocalRepository.get(this).recordVehicleView(
                user.getUid(), currentVehicle.getId(), System.currentTimeMillis());
    }

    private void initializeViews() {
        ivVehicleImage = findViewById(R.id.ivVehicleImage);
        tvVehicleName = findViewById(R.id.tvVehicleName);
        tvPrice = findViewById(R.id.tvPrice);
        tvAutonomy = findViewById(R.id.tvAutonomy);
        tvBattery = findViewById(R.id.tvBattery);
        tvConsumption = findViewById(R.id.tvConsumption);
        tvCategory = findViewById(R.id.tvCategory);
        tvPower = findViewById(R.id.tvPower);
        tvCharging = findViewById(R.id.tvCharging);
        badgeDetail = findViewById(R.id.badgeDetail);
        btnBackContainer = findViewById(R.id.btnBackContainer);
        btnFindDealer = findViewById(R.id.btnFindDealer);

        TextView tvUserAvatar = findViewById(R.id.tvUserAvatar);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
            tvUserAvatar.setText(getInitials(currentUser.getDisplayName()));
        }
    }

    private void loadVehicleData() {
        String id = getIntent().getStringExtra("vehicleId");
        if (id == null) {
            int index = getIntent().getIntExtra("vehicleIndex", -1);
            if (index >= 0 && index < DemoCatalog.vehicles().size()) id = DemoCatalog.vehicles().get(index).getId();
        }
        currentVehicle = LocalRepository.get(this).vehicle(id);
        if (currentVehicle == null) {
            Toast.makeText(this, "Este veículo não está mais disponível", Toast.LENGTH_LONG).show(); finish(); return;
        }
        displayVehicleData(currentVehicle);
    }

    @Override protected void onResume() {
        super.onResume();
        if (chrome != null) chrome.refresh();
        loadVehicleData();
    }

    private void displayVehicleData(Vehicle vehicle) {
        VehiclePhotos.load(vehicle, ivVehicleImage);
        tvVehicleName.setText(vehicle.getNome());
        DecimalFormat df = new DecimalFormat("#,##0.##", new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR")));
        tvPrice.setText("R$ " + df.format(vehicle.getPreco()));
        tvAutonomy.setText(vehicle.getAutonomia() + " km"); tvBattery.setText(vehicle.getBateria() + " kWh");
        tvCategory.setText(vehicle.getCategoria());
        tvConsumption.setText(vehicle.getConsumo() == null ? "Não informado" : df.format(vehicle.getConsumo()) + " kWh/100km");
        tvPower.setText(vehicle.getPotencia() == null ? "Não informado" : vehicle.getPotencia() + " cv");
        tvCharging.setText(vehicle.getCarga() == null ? "Não informado" : vehicle.getCarga());
        badgeDetail.setVisibility(vehicle.getBadge() == null ? View.GONE : View.VISIBLE);
        badgeDetail.setText(vehicle.getBadge());
        TextView info = findViewById(R.id.tvPublishedInfo);
        info.setVisibility(vehicle.getConcessionariaId() == null ? View.GONE : View.VISIBLE);
        String color = vehicle.getCor() == null || vehicle.getCor().isEmpty() ? "Não informado" : vehicle.getCor();
        String description = vehicle.getDescricao() == null ? "" : vehicle.getDescricao();
        info.setText(vehicle.getAno() + " • " + vehicle.getQuilometragem() + " km\nCor: " + color + "\n" + description);
        btnFindDealer.setText(vehicle.getConcessionariaId() == null ? "Encontrar concessionárias" : "Ver concessionária");
    }

    private void setupNavigation() {
        btnBackContainer.setOnClickListener(v -> finish());
        
        LinearLayout btnHome = findViewById(R.id.btnHome);
        LinearLayout btnVehicles = findViewById(R.id.btnVehicles);
        LinearLayout btnComparison = findViewById(R.id.btnComparison);
        LinearLayout btnSolar = findViewById(R.id.btnSolar);
        LinearLayout btnProfile = findViewById(R.id.btnProfile);

        btnHome.setOnClickListener(v -> navigateToHome());
        btnVehicles.setOnClickListener(v -> {});
        btnComparison.setOnClickListener(v -> navigateToComparison());
        btnSolar.setOnClickListener(v -> navigateToSolar());
        btnProfile.setOnClickListener(v -> navigateToProfile());

        btnFindDealer.setOnClickListener(v -> {
            if (currentVehicle == null) return;
            if (currentVehicle.getConcessionariaId() == null) startActivity(new Intent(this, ConcessionariasActivity.class));
            else startActivity(new Intent(this, ConcessionariaDetailActivity.class)
                    .putExtra("dealerId", currentVehicle.getConcessionariaId()).putExtra("vehicleId", currentVehicle.getId()));
        });
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void navigateToComparison() {
        startActivity(new Intent(this, ComparisonActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void navigateToSolar() {
        startActivity(new Intent(this, SolarActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void navigateToProfile() {
        startActivity(new Intent(this, ProfileActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private String getInitials(String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        String first = parts[0].substring(0, 1);
        String second = parts[1].substring(0, 1);
        return (first + second).toUpperCase();
    }
}





